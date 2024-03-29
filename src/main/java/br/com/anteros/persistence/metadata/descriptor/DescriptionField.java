/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.metadata.descriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.anteros.core.utils.ObjectUtils;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.asm.FieldAccess;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.EntityCacheException;
import br.com.anteros.persistence.metadata.EntityManaged;
import br.com.anteros.persistence.metadata.FieldEntityValue;
import br.com.anteros.persistence.metadata.RandomAliasName;
import br.com.anteros.persistence.metadata.accessor.PropertyAccessor;
import br.com.anteros.persistence.metadata.annotation.type.CascadeType;
import br.com.anteros.persistence.metadata.annotation.type.FetchMode;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;
import br.com.anteros.persistence.metadata.annotation.type.ReturnType;
import br.com.anteros.persistence.metadata.annotation.type.TemporalType;
import br.com.anteros.persistence.metadata.descriptor.type.ConnectivityType;
import br.com.anteros.persistence.metadata.descriptor.type.FieldType;
import br.com.anteros.persistence.metadata.descriptor.type.SQLStatementType;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.sql.binder.DateParameterBinding;
import br.com.anteros.persistence.sql.binder.DateTimeParameterBinding;
import br.com.anteros.persistence.sql.binder.LobParameterBinding;
import br.com.anteros.persistence.sql.dialect.DatabaseDialect;
import br.com.anteros.persistence.sql.dialect.type.SQLiteDate;

public class DescriptionField {
	private Field field;
	private FieldType fieldType = FieldType.SIMPLE;
	private String statement;
	private FetchType fetchType;
	private FetchMode modeType;
	private String orderByClause;
	private EntityCache targetEntity;
	private DescriptionMappedBy mappedBy;
	private List<DescriptionColumn> columns = new ArrayList<DescriptionColumn>();
	private String tableName;
	private String schema;
	private String catalog;
	private CascadeType[] cascadeTypes = { CascadeType.NONE };
	private EntityCache entityCache;
	private Class<?> targetClass;
	private DescriptionColumn mapKeyColumn;
	private DescriptionColumn elementColumn;
	private String aliasTableName;
	private Map<SQLStatementType, DescriptionSQL> descriptionSql = new LinkedHashMap<SQLStatementType, DescriptionSQL>();
	private String comment = "";
	private List<DescriptionIndex> indexes = new ArrayList<DescriptionIndex>();
	private List<DescriptionUniqueConstraint> uniqueConstraints = new ArrayList<DescriptionUniqueConstraint>();
	private List<DescriptionConvert> converts = new ArrayList<DescriptionConvert>();
	private String foreignKeyName;
	private String mobileActionImport;
	private String mobileActionExport;
	private String displayLabel;
	private Map<Integer, ParamDescription> exportParams;
	private Map<Integer, ParamDescription> importParams;
	private int exportOrderToSendData;
	private ConnectivityType importConnectivityType = ConnectivityType.ALL_CONNECTION;
	private ConnectivityType exportConnectivityType = ConnectivityType.ALL_CONNECTION;
	private String[] exportColumns;
	private PropertyAccessor propertyAccessor = null;
	private DescriptionColumn simpleColumn;

	public DescriptionField(EntityCache entityCache, Field field) {
		setField(field);
		this.entityCache = entityCache;
	}

	public String generateAndGetAliasTableName() {
		generateAliasTableName();
		return this.aliasTableName;
	}

	public void generateAliasTableName() {
		this.aliasTableName = RandomAliasName.randomTableName();
	}

	public String getAliasTableName() {
		if (this.aliasTableName == null)
			generateAliasTableName();
		return this.aliasTableName;
	}

	public boolean isRelationShip() {
		return this.fieldType == FieldType.RELATIONSHIP;
	}

	public boolean hasPrimaryKey() {
		for (DescriptionColumn column : columns) {
			if (column.isPrimaryKey())
				return true;
		}
		return false;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
		this.field.setAccessible(true);
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public void setStatement(String statement) {
		if (!"".equals(statement)) {
			this.statement = statement;
		}
	}

	public String getStatement() {
		return statement;
	}

	public void setFetchType(FetchType type) {
		this.fetchType = type;

	}

	public void setFetchMode(FetchMode mode) {
		this.modeType = mode;

	}

	public FetchType getFetchType() {
		return fetchType;
	}

	public FetchMode getModeType() {
		return modeType;
	}

	public boolean isAnyCollectionOrMap() {
		return this.fieldType == FieldType.COLLECTION_ENTITY || isMapTable() || isCollectionTable();
	}

	public boolean isAnyCollection() {
		return this.fieldType == FieldType.COLLECTION_ENTITY || isCollectionTable();
	}

	public boolean isCollectionEntity() {
		return this.fieldType == FieldType.COLLECTION_ENTITY;
	}

	public boolean isSimple() {
		return this.fieldType == FieldType.SIMPLE;
	}

	public boolean isLob() {
		if (simpleColumn == null)
			return false;

		return simpleColumn.isLob();
	}

	public boolean isString() {
		return (this.getField().getType() == String.class);
	}

	public boolean isTemporalDate() {
		if (simpleColumn == null)
			return false;
		return simpleColumn.getTemporalType() == TemporalType.DATE;
	}

	public boolean isTemporalDateTime() {
		if (simpleColumn == null)
			return false;
		return simpleColumn.getTemporalType() == TemporalType.DATE_TIME;
	}

	public boolean isTemporalTime() {
		if (simpleColumn == null)
			return false;
		return simpleColumn.getTemporalType() == TemporalType.TIME;
	}

	public boolean hasOrderByClause() {
		return this.orderByClause != null;
	}

	public String getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(String orderByClause) {
		this.orderByClause = orderByClause;
	}

	public void setModeType(FetchMode modeType) {
		this.modeType = modeType;
	}

	public EntityCache getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(EntityCache targetEntity) {
		this.targetEntity = targetEntity;

	}

	public Object getObjectValue(Object object) throws Exception {
		Method method = ReflectionUtils.getGetterAccessor(entityCache.getEntityClass(), field);

		try {
			return entityCache.getMethodAccess().invoke(object, method.getName());
		} catch (NullPointerException e) {
			return null;
		}

//		if (propertyAccessor != null) {
//			return propertyAccessor.get(object);
//		} else {
//			return field.get(object);
//		}
	}

	public Object setObjectValue(Object object, Object value) throws Exception {
		Field field = this.getField();
		try {
			if ("".equals(value) && isEnumerated()) {
				value = null;
			} else if (isEnumerated()) {
				if (value != null && !ReflectionUtils.isEnum(value.getClass())) {
					value = convertObjectToEnum(value);
				}
			} else if (isBoolean()) {
				value = convertObjectToBoolean(value);
			} else if (isRelationShip() || isCollectionEntity()) {
				//
			} else if ((value == null) || (value.getClass() == field.getType())) {
				//
			} else if ("".equals(value) && (ReflectionUtils.isExtendsClass(Number.class, field.getType())
					|| (field.getType() == SQLiteDate.class) || (field.getType() == java.sql.Date.class)
					|| (field.getType() == Date.class))) {
				value = null;
			} else if ((field.getType() == Date.class) && (value instanceof String)) {
				value = convertStringValueToDate(String.valueOf(value));
			} else if ((field.getType() == java.sql.Date.class) && (value instanceof String)) {
				value = convertStringValueToDateSQL(String.valueOf(value));
			} else if (value instanceof byte[]) {
				if (isLob()) {
					value = convertBytesValueToLob((byte[]) value);
				} else if (field.getType() == String.class) {
					value = new String((byte[]) value);
				} else
					value = ObjectUtils.convert(value, field.getType());
			} else if ((value instanceof String) && (isLob())) {
				value = convertStringValueToLob(String.valueOf(value));
			} else {
				value = ObjectUtils.convert(value, field.getType());
			}
			setValue(object, value);
		} catch (Exception ex) {
			throw new EntityCacheException("Erro convertendo o valor do campo " + this.getName() + " valor=" + value
					+ " para " + field.getType() + " na classe " + entityCache.getEntityClass().getName(), ex);
		}
		return value;
	}

	public void setValue(Object source, Object value) throws Exception {
		Method method = ReflectionUtils.getSetterAccessor(entityCache.getEntityClass(), field);
		entityCache.getMethodAccess().invoke(source, method.getName(), value);
//		if (propertyAccessor != null) {
//			propertyAccessor.set(source, value);
//		} else {
//			field.set(source, value);
//		}
	}

	public Object convertStringValueToDateSQL(String value) throws Exception {
		TemporalType temporalType = this.getSimpleColumn().getTemporalType();
		if (temporalType == TemporalType.DATE) {
			String datePattern = this.getSimpleColumn().getDatePattern() != "" ? this.getSimpleColumn().getDatePattern()
					: DatabaseDialect.DATE_PATTERN;
			Date date = new SimpleDateFormat(datePattern).parse(value);
			if (date != null)
				return new java.sql.Date(date.getTime());
		} else if (temporalType == TemporalType.DATE_TIME) {
			String dateTimePattern = !(this.getSimpleColumn().getDateTimePattern()).equals("")
					? this.getSimpleColumn().getDateTimePattern()
					: DatabaseDialect.DATETIME_PATTERN;
			Date date = new SimpleDateFormat(dateTimePattern).parse(value);
			if (date != null)
				return new java.sql.Date(date.getTime());
		}
		return null;
	}

	public Object convertStringValueToDate(String value) throws Exception {
		if (value instanceof String) {
			TemporalType temporalType = this.getSimpleColumn().getTemporalType();
			if (temporalType == TemporalType.DATE) {
				String datePattern = !(this.getSimpleColumn().getDatePattern()).equals("")
						? this.getSimpleColumn().getDatePattern()
						: DatabaseDialect.DATE_PATTERN;
				return new SimpleDateFormat(datePattern).parse(value);
			} else if (temporalType == TemporalType.DATE_TIME) {
				String dateTimePattern = !("".equals(this.getSimpleColumn().getDateTimePattern()))
						? this.getSimpleColumn().getDateTimePattern()
						: DatabaseDialect.DATETIME_PATTERN;
				return new SimpleDateFormat(dateTimePattern).parse(value);
			}
		}
		return null;
	}

	public Object convertStringValueToLob(String value) throws Exception {
		if (field.getType() == byte[].class)
			return ((String) value).getBytes();
		else if (field.getType() == Byte[].class)
			return ObjectUtils.toByteArray(((String) value).getBytes());
		else if (field.getType() == char[].class)
			return ObjectUtils.toPrimitiveCharacterArray(((String) value).getBytes());
		else if (field.getType() == Character[].class)
			return ObjectUtils.toCharacterArray(((String) value).getBytes());
		return null;
	}

	public Object convertBytesValueToLob(byte[] value) throws Exception {
		if (field.getType() == byte[].class)
			return value;
		else if (field.getType() == Byte[].class)
			return ObjectUtils.toByteArray(value);
		else if (field.getType() == char[].class)
			return ObjectUtils.toPrimitiveCharacterArray(value);
		else if (field.getType() == Character[].class)
			return ObjectUtils.toCharacterArray(value);
		else if (field.getType() == String.class)
			return new String(value);
		return null;
	}

	public Object convertObjectToBoolean(Object value) {
		if (value == null) {
			if (getSimpleColumn().getBooleanReturnType() == ReturnType.TRUE)
				value = new Boolean(true);
			else
				value = new Boolean(false);
		} else {
			if (getSimpleColumn().getTrueValue().equals(value.toString())) {
				value = new Boolean(true);
			} else
				value = new Boolean(false);
		}
		return value;
	}

	public Object convertObjectToEnum(Object value) throws EntityCacheException {
		if (!"".equals(value) && (value != null)) {
			String enumValue = getEnumValue((String) value);
			if ((enumValue == null) || (enumValue.equals(""))) {
				throw new EntityCacheException(
						"Valor " + value + " não encontrado na lista do Enum do campo " + this.getName()
								+ (this.getEntityCache() == null ? ""
										: " da classe " + this.getEntityCache().getEntityClass().getName())
								+ ". Verifique se o tipo enum " + field.getType()
								+ "  foi customizado e se foi adicionado na lista de classes anotadas.");
			}
			for (Object enu : field.getType().getEnumConstants()) {
				if (enu != null && enu.toString().equals(enumValue)) {
					value = enu;
					break;
				}
			}
		}
		return value;
	}

	public boolean hasDescriptionColumn() {
		return (this.columns != null) && (this.columns.size() > 0);
	}

	public Class<?> getFieldClass() {
		if ((this.fieldType == FieldType.SIMPLE) || (this.fieldType == FieldType.RELATIONSHIP))
			return field.getType();

		return ReflectionUtils.getGenericType(field);

	}

	public void setDescriptionMappedBy(DescriptionMappedBy mapped) {
		this.mappedBy = mapped;
	}

	public DescriptionMappedBy getDescriptionMappedBy() {
		return mappedBy;
	}

	public String getMappedBy() {
		if (mappedBy != null)
			return this.mappedBy.getMappedBy();

		return null;
	}

	public void addDescriptionColumns(List<DescriptionColumn> columns) {
		for (DescriptionColumn c : columns) {
			/**
			 * Seta DescriptionField no DescriptionColumn
			 */
			c.setDescriptionField(this);
			this.columns.add(c);
		}
		this.simpleColumn = this.columns.get(0);
	}

	public List<DescriptionColumn> getDescriptionColumns() {
		return Collections.unmodifiableList(columns);
	}

	public List<String> getDescriptionColumnsStr() {
		List<String> result = new ArrayList<String>();
		for (DescriptionColumn column : columns) {
			result.add(column.getColumnName());
		}
		return result;
	}

	public void setMappedBy(DescriptionMappedBy mappedBy) {
		this.mappedBy = mappedBy;
	}

	public void add(DescriptionColumn descriptionColumn) throws Exception {
		for (DescriptionColumn column : columns) {
			if (column.getColumnName().equals(descriptionColumn.getColumnName())) {
				throw new EntityCacheException("Coluna " + descriptionColumn.getColumnName()
						+ " já adicionada no campo " + this.getName() + (this.getEntityCache() == null ? ""
								: " da classe " + this.getEntityCache().getEntityClass().getName()));
			}
		}
		descriptionColumn.setDescriptionField(this);
		if (descriptionColumn.isMapKeyColumn())
			this.mapKeyColumn = descriptionColumn;
		if (descriptionColumn.isElementColumn())
			this.elementColumn = descriptionColumn;
		this.columns.add(descriptionColumn);
		this.simpleColumn = this.columns.get(0);
	}

	public DescriptionColumn getElementColumn() {
		return elementColumn;
	}

	public DescriptionColumn getMapKeyColumn() {
		return mapKeyColumn;
	}

	public boolean hasMapKeyColumn() {
		return this.mapKeyColumn != null;
	}

	public boolean isRequired() {
		if (!this.isAnyCollectionOrMap()) {
			for (DescriptionColumn column : columns) {
				if (column.isRequired())
					return true;
			}
		}
		return false;
	}

	public void setMapKeyColumn(DescriptionColumn mapKeyColumn) {
		this.mapKeyColumn = mapKeyColumn;
	}

	public List<DescriptionColumn> getPrimaryKeys() {
		List<DescriptionColumn> result = new ArrayList<DescriptionColumn>();
		for (DescriptionColumn column : columns) {
			if (column.isPrimaryKey())
				result.add(column);
		}
		return result;
	}

	@Override
	public String toString() {
		return "fieldName " + field.getName() + ", fieldType=" + fieldType + ", statement=" + statement + ", fechType="
				+ fetchType + ", mode=" + modeType + ", orderBy=" + orderByClause + ", targetEntity="
				+ (targetEntity == null ? "" : targetEntity.getEntityClass().getName()) + " mappedBy="
				+ (mappedBy == null ? null : mappedBy.toString());
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Retorna se é um field de ColectionTable simples (Ex: List, Set);
	 * 
	 * @return
	 */
	public boolean isCollectionTable() {
		return this.fieldType == FieldType.COLLECTION_TABLE;
	}

	/**
	 * Retorna se é um field de ElementCollection
	 * 
	 * @return
	 */
	public boolean isElementCollection() {
		return this.isMapTable() || this.isCollectionTable();
	}

	/**
	 * Retorna se é um field de CollectionMapKey (Ex: Map)
	 * 
	 * @return
	 */
	public boolean isMapTable() {
		return this.fieldType == FieldType.COLLECTION_MAP_TABLE;
	}

	public boolean isJoinTable() {
		return this.fieldType == FieldType.JOIN_TABLE;
	}

	public String getTableName() {
		if (StringUtils.isEmpty(tableName)) {
			return entityCache.getTableName();
		}
		return tableName;
	}

	public void setCascadeTypes(CascadeType[] types) {
		this.cascadeTypes = types;
	}

	public CascadeType[] getCascadeTypes() {
		return cascadeTypes;
	}

	public EntityCache getEntityCache() {
		return entityCache;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public boolean hasTargetClass() {
		return this.targetClass != null;
	}

	public FieldEntityValue getFieldEntityValue(SQLSession session, Object object) throws Exception {
		return getFieldEntityValue(session, object, false);
	}

	public FieldEntityValue getFieldEntityValue(SQLSession session, Object object, boolean forceInitialize)
			throws Exception {
		Object fieldValue = this.getObjectValue(object);
		if (forceInitialize) {
			if (session.isProxyObject(fieldValue)) {
				if (!session.proxyIsInitialized(fieldValue)) {
					if (fieldValue instanceof Collection) {
						((Collection<?>) fieldValue).size();
					}
				}
			}
		}
		return getFieldEntityValue(session, object, fieldValue);
	}

	public boolean isInitialized(SQLSession session, Object object) throws Exception {
		if (session.isProxyObject(object)) {
			return session.proxyIsInitialized(object);
		}
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FieldEntityValue getFieldEntityValue(SQLSession session, Object object, Object fieldValue) throws Exception {
		if (this.isSimple()) {
			Map<String, Object> tempSimple = new LinkedHashMap<String, Object>();
			tempSimple.put(this.getSimpleColumn().getColumnName(), ObjectUtils.cloneObject(fieldValue));
			return new FieldEntityValue(this.getField().getName(), tempSimple, object);
		}
		EntityManaged entityManaged = null;
		if (this.isCollectionTable()) {
			List listTemp = new ArrayList();
			if ((fieldValue != null) && this.isInitialized(session, fieldValue)) {
				Iterator it = ((Collection) fieldValue).iterator();
				Object value;
				Map<String, Object> tempSimple;
				while (it.hasNext()) {
					value = it.next();
					tempSimple = new LinkedHashMap<String, Object>();
					tempSimple.put(this.getElementColumn().getColumnName(), ObjectUtils.cloneObject(value));
					listTemp.add(new FieldEntityValue(this.getField().getName(), tempSimple, value));
				}
			}
			return new FieldEntityValue(this.getField().getName(), listTemp.toArray(new FieldEntityValue[] {}),
					((Collection) fieldValue));
		} else if (this.isMapTable()) {
			List listTemp = new ArrayList();
			if ((fieldValue != null) && this.isInitialized(session, fieldValue)) {
				Iterator it = ((Map) fieldValue).keySet().iterator();
				Object value;
				Object key;
				Map<Object, Object> newValue;
				while (it.hasNext()) {
					key = it.next();
					value = ((Map) fieldValue).get(key);

					newValue = new LinkedHashMap<Object, Object>();
					newValue.put(ObjectUtils.cloneObject(key), ObjectUtils.cloneObject(value));
					listTemp.add(new FieldEntityValue(this.getField().getName(), newValue, key));
				}
			}
			return new FieldEntityValue(this.getField().getName(), listTemp.toArray(new FieldEntityValue[] {}),
					((Map) fieldValue));
		} else if ((this.isCollectionEntity()) || (this.isJoinTable())) {
			List listTemporary = new ArrayList();
			if (fieldValue instanceof Collection) {
				if ((fieldValue != null) && this.isInitialized(session, fieldValue)) {
					for (Object entity : ((Collection) fieldValue))
						listTemporary.add(new FieldEntityValue(session.getIdentifier(entity).getDatabaseUniqueId(),
								session.getIdentifier(entity).getColumns(), entity));
				}
				return new FieldEntityValue(this.getField().getName(), listTemporary.toArray(new FieldEntityValue[] {}),
						((Collection) fieldValue));
			}
		} else if (this.isRelationShip()) {
			Map<String, Object> primaryKey;
			if ((fieldValue != null) && this.isInitialized(session, fieldValue)) {
				primaryKey = session.getIdentifier(fieldValue).getColumns();
			} else
				primaryKey = new LinkedHashMap<String, Object>();
			return new FieldEntityValue(this.getField().getName(), primaryKey, fieldValue);
		}
		return null;
	}

	public String getName() {
		return field.getName();
	}

	public DescriptionColumn getDescriptionColumnByName(String columnName) {
		for (DescriptionColumn descriptionColumn : this.columns)
			if (columnName.equals(descriptionColumn.getColumnName()))
				return descriptionColumn;
		return null;
	}

	public DescriptionColumn getDescriptionColumnByReferencedColumnName(String columnName) {
		for (DescriptionColumn descriptionColumn : this.columns)
			if (columnName.equals(descriptionColumn.getReferencedColumnName()))
				return descriptionColumn;
		return null;
	}

	public boolean isVersioned() {
		for (DescriptionColumn descriptionColumn : this.columns)
			if (descriptionColumn.isVersioned())
				return true;
		return false;
	}

	public boolean hasGenerator() {
		for (DescriptionColumn descriptionColumn : this.columns)
			if (descriptionColumn.hasGenerator())
				return true;
		return false;
	}

	public String getSequenceName() {
		for (DescriptionColumn descriptionColumn : this.columns)
			if (descriptionColumn.hasGenerator())
				return descriptionColumn.getSequenceName();
		return "";
	}

	public boolean isTable() {
		return isCollectionTable() || isJoinTable() || isMapTable();
	}

	public DescriptionColumn getSimpleColumn() {
		if (simpleColumn == null) {
			if (columns.size() > 0)
				simpleColumn = columns.get(0);
		}
		return simpleColumn;
	}

	public String getEnumValue(String value) {
		return getSimpleColumn().getEnumValue(value);
	}

	public Object getBooleanValue(Boolean value) {
		if (value)
			return getSimpleColumn().getTrueValue();
		else
			return getSimpleColumn().getFalseValue();
	}

	public String getValueEnum(String key) {
		return getSimpleColumn().getValueEnum(key);
	}

	public boolean isEnumerated() {
		if (simpleColumn == null)
			return false;
		return simpleColumn.isEnumerated();
	}

	public boolean isBoolean() {
		if (simpleColumn == null)
			return false;
		return simpleColumn.isBoolean();
	}

	public boolean isNumber() {
		return ReflectionUtils.isNumberField(field);
	}

	public DescriptionSQL getDescriptionSqlByType(SQLStatementType type) {
		if (descriptionSql != null)
			return descriptionSql.get(type);
		return null;
	}

	public Map<SQLStatementType, DescriptionSQL> getDescriptionSql() {
		return descriptionSql;
	}

	public void setDescriptionSql(Map<SQLStatementType, DescriptionSQL> descriptionSql) {
		this.descriptionSql = descriptionSql;
	}

	public boolean isNull(Object object) throws Exception {
		return getObjectValue(object) == null;
	}

	public Object getColumnValue(String columnName, Object object) throws Exception {
		DescriptionColumn column = getDescriptionColumnByName(columnName);
		if (column == null)
			throw new EntityCacheException("Coluna " + columnName + " não encontrada no campo " + getName() + " "
					+ getEntityCache().getEntityClass().getName());
		return column.getColumnValue(object);
	}

	public boolean isPrimaryKey() {
		if (isRelationShip() || isSimple()) {
			for (DescriptionColumn descriptionColumn : columns) {
				if (descriptionColumn.isPrimaryKey())
					return true;
			}
		}
		return false;
	}

	public boolean isCompositeId() {
		for (DescriptionColumn descriptionColumn : columns) {
			if (descriptionColumn.isCompositeId())
				return true;
		}
		return false;
	}

	public boolean isMappedBy() {
		return this.mappedBy != null;
	}

	public boolean hasPrimaryKeyColumns() {
		boolean result = false;
		if (hasDescriptionColumn()) {
			for (DescriptionColumn descriptionColumn : this.columns) {
				if (descriptionColumn.isPrimaryKey())
					result = true;
			}
		}
		return result;
	}

	public boolean existsColumn(DescriptionColumn column) {
		for (DescriptionColumn descriptionColumn : columns) {
			if (column == descriptionColumn.getReferencedColumn())
				return true;
		}
		return false;
	}

	public boolean isContainsColumns(List<String> columNames) {
		int count = 0;
		for (String colum : columNames) {
			for (DescriptionColumn descriptionColumn : getDescriptionColumns()) {
				if (descriptionColumn.getColumnName().equalsIgnoreCase(colum)) {
					count++;
					break;
				}
			}
		}
		return count == columNames.size();
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public List<DescriptionIndex> getIndexes() {
		return indexes;
	}

	public void setIndexes(List<DescriptionIndex> indexes) {
		this.indexes = indexes;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	public List<DescriptionUniqueConstraint> getUniqueConstraints() {
		return uniqueConstraints;
	}

	public void setUniqueConstraints(List<DescriptionUniqueConstraint> uniqueConstraints) {
		this.uniqueConstraints = uniqueConstraints;
	}

	public void addUniqueConstraint(DescriptionUniqueConstraint uniqueConstraint) {
		uniqueConstraints.add(uniqueConstraint);
	}

	public List<DescriptionConvert> getConverts() {
		return converts;
	}

	public void setConverts(List<DescriptionConvert> converts) {
		this.converts = converts;
	}

	public boolean hasConverts() {
		return ((converts != null) && (converts.size() > 0));
	}

	public int exportColumnsCount() {
		if (exportColumns != null)
			return exportColumns.length;
		return 0;
	}

	public ConnectivityType getImportConnectivityType() {
		return importConnectivityType;
	}

	public void setImportConnectivityType(ConnectivityType importConnectivityType) {
		this.importConnectivityType = importConnectivityType;
	}

	public void setExportOrderToSendData(int exportOrderToSendData) {
		this.exportOrderToSendData = exportOrderToSendData;
	}

	public ConnectivityType getExportConnectivityType() {
		return exportConnectivityType;
	}

	public void setExportConnectivityType(ConnectivityType exportConnectivityType) {
		this.exportConnectivityType = exportConnectivityType;
	}

	public boolean isExternalFile() {
		return getSimpleColumn().isExternalFile();
	}

	public String getDisplayLabel() {
		return displayLabel;
	}

	public void setDisplayLabel(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	public String getMobileActionImport() {
		return mobileActionImport;
	}

	public void setMobileActionImport(String mobileActionImport) {
		this.mobileActionImport = mobileActionImport;
	}

	public String getMobileActionExport() {
		return mobileActionExport;
	}

	public void setMobileActionExport(String mobileActionExport) {
		this.mobileActionExport = mobileActionExport;
	}

	public Map<Integer, ParamDescription> getExportParams() {
		if (exportParams == null)
			exportParams = new LinkedHashMap<Integer, ParamDescription>();
		return exportParams;
	}

	public void setExportParams(Map<Integer, ParamDescription> exportParams) {
		this.exportParams = exportParams;
	}

	public Map<Integer, ParamDescription> getImportParams() {
		if (importParams == null)
			importParams = new LinkedHashMap<Integer, ParamDescription>();
		return importParams;
	}

	public void setImportParams(Map<Integer, ParamDescription> importParams) {
		this.importParams = importParams;
	}

	public Integer getExportOrderToSendData() {
		return exportOrderToSendData;
	}

	public void setExportOrderToSendData(Integer exportOrderToSendData) {
		this.exportOrderToSendData = exportOrderToSendData;
	}

	public boolean isExportTable() {
		return ((mobileActionExport != null) && (!"".equals(mobileActionExport)));
	}

	public boolean isImportTable() {
		return ((mobileActionImport != null) && (!"".equals(mobileActionImport)));
	}

	public String[] getExportColumns() {
		return exportColumns;
	}

	public void setExportColumns(String[] exportColumns) {
		this.exportColumns = exportColumns;
	}

	public List<Map<DescriptionColumn, Object>> getMapTableColumnValues(SQLSession session, Object object)
			throws Exception {
		List<Map<DescriptionColumn, Object>> result = new ArrayList<Map<DescriptionColumn, Object>>();
		if (this.isMapTable()) {
			Map<String, Object> primaryKeyOwner = session.getIdentifier(object).getColumns();
			Object fieldValue = this.getObjectValue(object);
			for (Object key : ((Map<?, ?>) fieldValue).keySet()) {
				Object value = ((Map<?, ?>) fieldValue).get(key);
				Map<DescriptionColumn, Object> record = new LinkedHashMap<DescriptionColumn, Object>();
				for (DescriptionColumn column : this.getDescriptionColumns()) {
					if (column.isForeignKey()) {
						record.put(column, primaryKeyOwner.get(column.getReferencedColumnName()));
					} else {
						if (column.isMapKeyColumn())
							record.put(column, key);
						else
							record.put(column, value);
					}
				}
				result.add(record);
			}
		}
		return result;
	}

	public List<Map<DescriptionColumn, Object>> getCollectionTableColumnValues(SQLSession session, Object object)
			throws Exception {
		List<Map<DescriptionColumn, Object>> result = new ArrayList<Map<DescriptionColumn, Object>>();
		if (this.isCollectionTable()) {
			Map<String, Object> primaryKeyOwner = session.getIdentifier(object).getColumns();
			Object fieldValue = this.getObjectValue(object);
			for (Object value : ((Collection<?>) fieldValue)) {
				Map<DescriptionColumn, Object> record = new LinkedHashMap<DescriptionColumn, Object>();
				for (DescriptionColumn column : this.getDescriptionColumns()) {
					if (column.isForeignKey()) {
						record.put(column, primaryKeyOwner.get(column.getReferencedColumnName()));
					} else
						record.put(column, value);
				}
				result.add(record);
			}
		}
		return result;
	}

	public List<Map<DescriptionColumn, Object>> getJoinTableColumnValues(SQLSession session, Object object)
			throws Exception {
		List<Map<DescriptionColumn, Object>> result = new ArrayList<Map<DescriptionColumn, Object>>();
		if (this.isJoinTable()) {
			Map<String, Object> primaryKeyOwner = session.getIdentifier(object).getColumns();
			Object fieldValue = this.getObjectValue(object);
			for (Object value : ((Collection<?>) fieldValue)) {
				Map<String, Object> foreignKeyRight = session.getIdentifier(value).getColumns();
				Map<DescriptionColumn, Object> record = new LinkedHashMap<DescriptionColumn, Object>();
				for (DescriptionColumn column : this.getDescriptionColumns()) {
					if (column.isForeignKey()) {
						if (foreignKeyRight.containsKey(column.getColumnName()))
							record.put(column, foreignKeyRight.get(column.getColumnName()));
						else
							record.put(column, primaryKeyOwner.get(column.getReferencedColumnName()));
					}
				}
				result.add(record);
			}
		}
		return result;
	}

	public NamedParameter getNamedParameterFromDatabaseObjectValue(SQLSession session, Object sourceObject,
			DescriptionColumn sourceColumn) throws Exception {
		Map<String, Object> primaryKeyOwner = null;
		Object columnValue = sourceColumn.convertToDatabaseColumn(this.getObjectValue(sourceObject));
		if (this.isRelationShip()) {
			if (columnValue != null) {
				primaryKeyOwner = session.getIdentifier(columnValue).getColumns();
				columnValue = primaryKeyOwner.get(sourceColumn.getReferencedColumnName());
			}
		}

		if (columnValue == null) {
			return new NamedParameter(sourceColumn.getColumnName(), columnValue);
		} else if (this.isEnumerated())
			return new NamedParameter(sourceColumn.getColumnName(), this.getValueEnum(columnValue.toString()));
		else if (this.isBoolean())
			return new NamedParameter(sourceColumn.getColumnName(), this.getBooleanValue((Boolean) columnValue));
		else if (this.isTemporalDate())
			return new NamedParameter(sourceColumn.getColumnName(), new DateParameterBinding(columnValue));
		else if (this.isTemporalDateTime())
			return new NamedParameter(sourceColumn.getColumnName(), new DateTimeParameterBinding(columnValue));
		else if (this.isLob())
			return new NamedParameter(sourceColumn.getColumnName(), new LobParameterBinding(columnValue));
		else
			return new NamedParameter(sourceColumn.getColumnName(), columnValue);
	}

	public DescriptionColumn getLastJoinColumn() {
		DescriptionColumn result = null;
		for (DescriptionColumn descriptionColumn : getDescriptionColumns()) {
			if (descriptionColumn.isJoinColumn()) {
				result = descriptionColumn;
			}
		}
		return result;
	}

	public DescriptionColumn getLastInversedColumn() {
		DescriptionColumn result = null;
		for (DescriptionColumn descriptionColumn : getDescriptionColumns()) {
			if (descriptionColumn.isInversedJoinColumn()) {
				result = descriptionColumn;
			}
		}
		return result;
	}

	public boolean hasModeType() {
		return modeType != null;
	}

	public String getColumnsToString() {
		String result = "";
		boolean appendDelimiter = false;
		for (DescriptionColumn descriptionColumn : getDescriptionColumns()) {
			if (appendDelimiter)
				result += "_";
			result += descriptionColumn.getColumnName();
		}
		return result;
	}

	public PropertyAccessor getPropertyAccessor() {
		return propertyAccessor;
	}

	public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
		this.propertyAccessor = propertyAccessor;
	}

	public boolean hasReferencedDescriptionColumn(String columnName) {
		for (DescriptionColumn descriptionColumn : getDescriptionColumns()) {
			if (descriptionColumn.getReferencedColumnName().equals(columnName))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityCache == null) ? 0 : entityCache.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DescriptionField other = (DescriptionField) obj;
		if (entityCache == null) {
			if (other.entityCache != null)
				return false;
		} else if (!entityCache.equals(other.entityCache))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		return true;
	}

	public boolean isTenant() {
		if (getSimpleColumn() != null) {
			return getSimpleColumn().isTenant();
		}
		return false;
	}

	public boolean isCompany() {
		if (getSimpleColumn() != null) {
			return getSimpleColumn().isCompany();
		}
		return false;
	}

}
