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
package br.com.anteros.persistence.metadata;

import java.lang.reflect.Method;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import br.com.anteros.core.converter.ConversionHelper;
import br.com.anteros.core.utils.ObjectUtils;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.asm.ConstructorAccess;
import br.com.anteros.persistence.asm.FieldAccess;
import br.com.anteros.persistence.asm.MethodAccess;
import br.com.anteros.persistence.metadata.annotation.Code;
import br.com.anteros.persistence.metadata.annotation.EventType;
import br.com.anteros.persistence.metadata.annotation.JoinTable;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;
import br.com.anteros.persistence.metadata.annotation.type.GeneratedType;
import br.com.anteros.persistence.metadata.annotation.type.InheritanceType;
import br.com.anteros.persistence.metadata.annotation.type.ScopeType;
import br.com.anteros.persistence.metadata.descriptor.DescriptionColumn;
import br.com.anteros.persistence.metadata.descriptor.DescriptionConvert;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.metadata.descriptor.DescriptionGenerator;
import br.com.anteros.persistence.metadata.descriptor.DescriptionIndex;
import br.com.anteros.persistence.metadata.descriptor.DescriptionNamedQuery;
import br.com.anteros.persistence.metadata.descriptor.DescriptionPkJoinColumn;
import br.com.anteros.persistence.metadata.descriptor.DescriptionSQL;
import br.com.anteros.persistence.metadata.descriptor.DescriptionUniqueConstraint;
import br.com.anteros.persistence.metadata.descriptor.DescritionSecondaryTable;
import br.com.anteros.persistence.metadata.descriptor.ParamDescription;
import br.com.anteros.persistence.metadata.descriptor.type.ColumnType;
import br.com.anteros.persistence.metadata.descriptor.type.ConnectivityType;
import br.com.anteros.persistence.metadata.descriptor.type.SQLStatementType;
import br.com.anteros.persistence.metadata.identifier.IdentifierPath;
import br.com.anteros.persistence.proxy.collection.AnterosPersistentCollection;
import br.com.anteros.persistence.session.SQLSession;

public class EntityCache {
	private Set<DescriptionColumn> columns = new LinkedHashSet<DescriptionColumn>();
	private Class<?> entityClass;
	private String tableName;
	private String schema;
	private String catalog;
	private DescriptionColumn discriminatorColumn;
	private List<DescriptionColumn> primaryKey = new LinkedList<DescriptionColumn>();
	private List<DescriptionField> fields = new LinkedList<DescriptionField>();
	private List<DescriptionUniqueConstraint> uniqueConstraints = new LinkedList<DescriptionUniqueConstraint>();
	private String discriminatorValue;
	private ScopeType cacheScope = ScopeType.TRANSACTION;
	private int maxTimeCache = 0;
	private String aliasTableName;
	private List<DescriptionNamedQuery> namedQueries = new LinkedList<DescriptionNamedQuery>();
	private List<DescriptionIndex> indexes = new LinkedList<DescriptionIndex>();
	private Map<SQLStatementType, DescriptionSQL> descriptionSql = new LinkedHashMap<SQLStatementType, DescriptionSQL>();
	private boolean abstractClass;
	private String mobileActionImport;
	private String mobileActionExport;
	private String displayLabel;
	private int exportOrderToSendData;
	private String[] exportColumns;
	private Map<Integer, ParamDescription> exportParams;
	private Map<Integer, ParamDescription> importParams;
	private ConnectivityType importConnectivityType = ConnectivityType.ALL_CONNECTION;
	private ConnectivityType exportConnectivityType = ConnectivityType.ALL_CONNECTION;
	private List<DescriptionConvert> converts = new ArrayList<DescriptionConvert>();
	private Map<GeneratedType, DescriptionGenerator> generators = new HashMap<GeneratedType, DescriptionGenerator>();
	private int maxRecordBlockExport;
	private List<DescritionSecondaryTable> secondaryTables = new LinkedList<DescritionSecondaryTable>();
	private Set<String> fieldNames = new LinkedHashSet<String>();
	private List<DescriptionPkJoinColumn> primaryKeyJoinColumns = new LinkedList<DescriptionPkJoinColumn>();
	private String foreignKeyName = "";
	private InheritanceType inheritanceType;
	private List<EntityListener> entityListeners = new ArrayList<EntityListener>();
	private Map<Method,EventType> methodListeners = new HashMap<Method,EventType>();
	private FieldAccess fieldAccess;
	private MethodAccess methodAccess;
	private ConstructorAccess constructorAccess;

	public List<DescritionSecondaryTable> getSecondaryTables() {
		return secondaryTables;
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

	public EntityCache(Class<?> sourceClazz) {
		this.entityClass = sourceClazz;
		methodAccess = MethodAccess.get(this.entityClass);
		fieldAccess = FieldAccess.get(this.entityClass);
	}

	/**
	 * Retorna tempo de Cache do objeto (milisegundos)
	 * 
	 * @return
	 */
	public int getMaxTimeCache() {
		return maxTimeCache;
	}

	/**
	 * Seta tempo de Cache do objeto (milisegundos)
	 * 
	 * @return
	 */
	public void setMaxTimeCache(int maxTimeCache) {
		this.maxTimeCache = maxTimeCache;
	}

	/**
	 * Seta tipo de escopo do Objeto.
	 * 
	 * @return
	 */
	public ScopeType getCacheScope() {
		return cacheScope;
	}
	
	public DescriptionField getTenantId() {
		for (DescriptionField f : fields) {
			if (f.isTenant())
				return f;
		}
		return null;
	}
	
	public DescriptionField getCompanyId() {
		for (DescriptionField f : fields) {
			if (f.isCompany())
				return f;
		}
		return null;
	}

	public List<DescriptionField> getDescriptionFields() {
		return fields;
	}

	public List<DescriptionField> getDescriptionFieldsExcludingIds() {
		List<DescriptionField> result = new ArrayList<DescriptionField>();
		for (DescriptionField f : fields) {
			if (!f.isPrimaryKey())
				result.add(f);
		}
		return result;
	}

	public void setDescriptionFields(List<DescriptionField> fields) {
		this.fields = fields;
	}

	public void addDescriptionFields(DescriptionField descriptionField) {
		this.fields.add(descriptionField);
		this.fieldNames.add(descriptionField.getField().getName());
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void add(DescriptionColumn descriptionColumn) {
		if (descriptionColumn.getColumnType() == ColumnType.PRIMARY_KEY || descriptionColumn.isCompositeId()) {
			this.primaryKey.add(descriptionColumn);
		} else if (descriptionColumn.getColumnType() == ColumnType.DISCRIMINATOR)
			this.discriminatorColumn = descriptionColumn;
		this.columns.add(descriptionColumn);
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void addDiscriminatorColumn(DescriptionColumn discriminatorColumn) {
		this.discriminatorColumn = discriminatorColumn;
		this.columns.add(this.discriminatorColumn);
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public void addAllDescriptionColumn(Set<DescriptionColumn> columns) {
		for (DescriptionColumn c : columns) {
			add(c);
		}
	}

	public boolean hasCompositeKey() {
		for (DescriptionField descriptionField : fields) {
			if (descriptionField.isCompositeId()) {
				return true;
			}
		}
		return false;
	}

	public List<DescriptionColumn> getCompositeKeys() {
		return Collections.unmodifiableList(this.primaryKey);
	}

	public DescriptionColumn getDiscriminatorColumn() {
		return discriminatorColumn;
	}

	public Set<DescriptionColumn> getDescriptionColumns() {
		return columns;
	}

	public void addPrimaryKeyColumn(DescriptionColumn primaryKey) {
		add(primaryKey);
	}

	public List<DescriptionColumn> getPrimaryKeyColumns() {
		return Collections.unmodifiableList(primaryKey);
	}

	public boolean hasDiscriminatorValue() {
		return this.discriminatorValue != null;
	}

	public boolean hasDiscriminatorColumn() {
		return this.discriminatorColumn != null;
	}

	public Object newInstance() throws Exception {
		return entityClass.newInstance();
	}

	public DescriptionField getDescriptionField(String name) {
		for (DescriptionField f : fields) {
			if (name.equalsIgnoreCase(f.getField().getName())) {
				return f;
			}
		}
		return null;
	}

	public String getDiscriminatorValue() {
		return this.discriminatorValue;
	}

	public DescriptionColumn getDescriptionColumnByField(String fieldName) {
		for (DescriptionColumn c : columns) {
			if ((c.getField() != null) && (fieldName.equalsIgnoreCase(c.getField().getName()))) {
				return c;
			}
		}
		return null;

	}

	public DescriptionColumn[] getDescriptionColumns(String fieldName) {
		ArrayList<DescriptionColumn> result = new ArrayList<DescriptionColumn>();
		for (DescriptionColumn descriptionColumn : columns) {
			if ((descriptionColumn.getField() != null)
					&& (fieldName.equalsIgnoreCase(descriptionColumn.getField().getName()))) {
				result.add(descriptionColumn);
			}
		}
		return result.toArray(new DescriptionColumn[] {});

	}

	public DescriptionColumn getDescriptionColumnByName(String columnName) {
		for (DescriptionColumn descriptionColumn : columns) {
			if (columnName.equalsIgnoreCase(descriptionColumn.getColumnName()))
				return descriptionColumn;
		}
		return null;
	}

	public void addAllDescriptionField(List<DescriptionField> descriptionFields) {
		this.fields.addAll(descriptionFields);
		for (DescriptionField descriptionField : descriptionFields) {
			this.fieldNames.add(descriptionField.getField().getName());
		}

	}

	public DescriptionColumn getDescriptionColumnByColumnName(String columnName) {
		for (DescriptionColumn c : columns) {
			if (columnName.equals(c.getColumnName()))
				return c;
		}
		return null;
	}

	public void setEntityClass(Class<?> clazz) {
		this.entityClass = clazz;

	}

	@Override
	public String toString() {
		return getEntityClass().getName() + ":" + tableName + "";
	}

	/**
	 * Seta Escopo do Cache
	 * 
	 * @param scope
	 */
	public void setCacheScope(ScopeType scope) {
		this.cacheScope = scope;
	}

	public void addDescriptionColumns(List<DescriptionColumn> descriptionColumn) {
		for (DescriptionColumn d : descriptionColumn) {
			add(d);
		}
	}

	public String getCacheUniqueId(Object object) throws Exception {
		StringBuilder sb = new StringBuilder("");
		Map<String, Object> columns = new TreeMap<String, Object>();
		for (DescriptionColumn column : primaryKey)
			columns.put(column.getColumnName(), column.getDescriptionField().getObjectValue(object));

		for (Object value : columns.values()) {
			if (!"".equals(sb.toString()))
				sb.append("_");
			sb.append(value);
		}
		return sb.toString();
	}

	public void add(DescriptionField descriptionField) {
		this.fields.add(descriptionField);

	}

	private Object getValue(String columnName, Object object) throws Exception {
		for (DescriptionColumn column : this.getDescriptionColumns()) {
			if (columnName.equals(column.getColumnName())) {
				if (column.isForeignKey() && column.isPrimaryKey()) {
					try {
						Object value = column.getDescriptionField().getObjectValue(object);
						return column.getDescriptionField().getTargetEntity().getValue(
								(StringUtils.isEmpty(column.getReferencedColumnName()) ? column.getColumnName()
										: column.getReferencedColumnName()),
								value);
					} catch (Exception ex) {
					}
				} else if (column.isPrimaryKey()) {
					try {
						Object value = column.getDescriptionField().getObjectValue(object);
						return value;
					} catch (Exception ex) {
					}
				}
			}
		}
		return null;
	}

	private Object getDatabaseValue(String columnName, Object object) throws Exception {
		for (DescriptionColumn column : this.getDescriptionColumns()) {
			if (column.getColumnName().equals(columnName))
				if (column.isForeignKey() && column.isPrimaryKey()) {
					try {
						Object value = column
								.convertToDatabaseColumn(column.getDescriptionField().getObjectValue(object));
						return column.getDescriptionField().getTargetEntity().getValue(
								(StringUtils.isEmpty(column.getReferencedColumnName()) ? column.getColumnName()
										: column.getReferencedColumnName()),
								value);
					} catch (Exception ex) {
					}
				} else if (column.isPrimaryKey()) {
					try {
						Object value = column
								.convertToDatabaseColumn(column.getDescriptionField().getObjectValue(object));
						return value;
					} catch (Exception ex) {
					}
				}
		}
		return null;
	}

	public Map<String, Object> getPrimaryKeysAndValues(Object object) throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		for (DescriptionColumn column : this.columns) {
			if (column.isForeignKey() && column.isPrimaryKey()) {
				try {
					Object value = ReflectionUtils.getFieldValueByName(object, column.getField().getName());
					Object columnValue = column.getDescriptionField().getTargetEntity().getColumnValue(
							(StringUtils.isEmpty(column.getReferencedColumnName()) ? column.getColumnName()
									: column.getReferencedColumnName()),
							value);
					result.put(column.getColumnName(), ObjectUtils.cloneObject(columnValue));
				} catch (Exception ex) {
				}
			} else if (column.isPrimaryKey()) {
				try {
					Object value = column.getDescriptionField().getObjectValue(object);
					result.put(column.getColumnName(), ObjectUtils.cloneObject(value));
				} catch (Exception ex) {
				}
			}
		}
		return result;
	}

	public Map<String, Object> getPrimaryKeysAndDatabaseValues(Object object) throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		for (DescriptionColumn column : this.columns) {
			if (column.isForeignKey() && column.isPrimaryKey()) {
				try {
					Object value = column.convertToDatabaseColumn(
							ReflectionUtils.getFieldValueByName(object, column.getField().getName()));
					Object columnValue = column.getDescriptionField().getTargetEntity().getDatabaseValue(
							(StringUtils.isEmpty(column.getReferencedColumnName()) ? column.getColumnName()
									: column.getReferencedColumnName()),
							value);
					result.put(column.getColumnName(), ObjectUtils.cloneObject(columnValue));
				} catch (Exception ex) {
				}
			} else if (column.isPrimaryKey()) {
				try {
					Object value = column.convertToDatabaseColumn(column.getDescriptionField().getObjectValue(object));
					result.put(column.getColumnName(), ObjectUtils.cloneObject(value));
				} catch (Exception ex) {
				}
			}
		}
		return result;
	}

	private Object getColumnValue(String columnName, Object object) throws Exception {
		for (DescriptionColumn column : this.getDescriptionColumns()) {
			if (column.getColumnName().equals(columnName)) {
				if (column.isForeignKey() && column.isPrimaryKey()) {
					try {
						Object value = column.getDescriptionField().getObjectValue(object);
						return column.getDescriptionField().getTargetEntity().getValue(
								(StringUtils.isEmpty(column.getReferencedColumnName()) ? column.getColumnName()
										: column.getReferencedColumnName()),
								value);
					} catch (Exception ex) {
					}
				} else {
					try {
						Object value = column.getDescriptionField().getObjectValue(object);
						return value;
					} catch (Exception ex) {
					}
				}
			}
		}
		return null;
	}

	public Map<String, Object> getAllColumnValues(Object object) throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		for (DescriptionColumn column : this.columns) {
			if (column.isForeignKey() && column.isPrimaryKey()) {
				try {
					Object value = column.getDescriptionField().getObjectValue(object);
					result.put(column.getColumnName(),
							column.getDescriptionField().getTargetEntity()
									.getValue((StringUtils.isEmpty(column.getReferencedColumnName())
											? column.getColumnName()
											: column.getReferencedColumnName()), value));
				} catch (Exception ex) {
				}
			} else {
				try {
					Object value = column.getDescriptionField().getObjectValue(object);
					result.put(column.getColumnName(), value);
				} catch (Exception ex) {
				}
			}
		}
		return result;
	}

	public Map<String, IdentifierPath> getIdentifierColumns() {
		Map<String, IdentifierPath> result = new LinkedHashMap<String, IdentifierPath>();
		for (DescriptionColumn column : this.columns) {
			if (!column.isDiscriminatorColumn()) {
				String path = column.getField().getName();
				if (column.isForeignKey() && column.isPrimaryKey()) {
					try {
						column.getDescriptionField().getTargetEntity().getIdentifierPath(result, path);
					} catch (Exception ex) {
					}
				} else if (column.isPrimaryKey()) {
					try {
						result.put(column.getColumnName(), new IdentifierPath(column, path));
					} catch (Exception ex) {
					}
				}
			}
		}
		return result;
	}

	private void getIdentifierPath(Map<String, IdentifierPath> map, String path) throws Exception {
		for (DescriptionColumn c : this.getDescriptionColumns()) {
			if (!c.isDiscriminatorColumn()) {
				if (!"".equals(path))
					path += ".";
				path += c.getField().getName();
				if (c.isForeignKey() && c.isPrimaryKey()) {
					try {
						c.getDescriptionField().getTargetEntity().getIdentifierPath(map, path);
					} catch (Exception ex) {
					}
				} else if (c.isPrimaryKey()) {
					try {
						map.put(c.getColumnName(), new IdentifierPath(c, path));
					} catch (Exception ex) {
					}
				}
			}
		}
	}

	public List<DescriptionField> getFieldsModified(SQLSession session, Object object) throws Exception {
		List<DescriptionField> result = new ArrayList<DescriptionField>();
		FieldEntityValue lastFieldValue;
		FieldEntityValue newFieldValue;
		for (DescriptionField field : fields) {
			if (!field.isVersioned()) {
				if (fieldCanbeChanged(session, object, field.getField().getName())) {
					lastFieldValue = getLastFieldEntityValue(session, object, field.getField().getName());
					newFieldValue = field.getFieldEntityValue(session, object);
					if ((lastFieldValue != null) || (newFieldValue != null)) {
						if (((lastFieldValue == null) && (newFieldValue != null))
								|| ((lastFieldValue != null) && (newFieldValue == null))
								|| (newFieldValue.compareTo(lastFieldValue) != 0))
							result.add(field);
					}
				}
			}
		}
		return result;
	}

	public FieldEntityValue getOriginalFieldEntityValue(SQLSession session, Object object, String fieldName)
			throws Exception {
		EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(object);
		if (entityManaged != null) {
			for (FieldEntityValue field : entityManaged.getOriginalValues()) {
				if (field.getName().equals(fieldName))
					return field;
			}
		}
		return null;
	}

	public Object getOriginalValueByColumn(SQLSession session, Object object, DescriptionColumn column)
			throws Exception {
		return getValueByColumn(object, column,
				getOriginalFieldEntityValue(session, object, column.getField().getName()));
	}

	public FieldEntityValue getLastFieldEntityValue(SQLSession session, Object object, String fieldName)
			throws Exception {
		EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(object);
		if (entityManaged != null) {
			for (FieldEntityValue field : entityManaged.getLastValues()) {
				if (field.getName().equals(fieldName))
					return field;
			}
		}
		return null;
	}

	public Object getLastValueByColumn(SQLSession session, Object object, DescriptionColumn column) throws Exception {
		return getValueByColumn(object, column, getLastFieldEntityValue(session, object, column.getField().getName()));
	}

	public Object getNewValueByColumn(SQLSession session, Object object, DescriptionColumn column) throws Exception {
		return getValueByColumn(object, column, column.getDescriptionField().getFieldEntityValue(session, object));
	}

	public boolean fieldCanbeChanged(SQLSession session, Object object, String fieldName) throws Exception {
		EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(object);
		if (entityManaged != null) {
			for (String field : entityManaged.getFieldsForUpdate()) {
				if (field.equals(fieldName))
					return true;
			}
		}
		return false;
	}

	private Object getValueByColumn(Object object, DescriptionColumn column, FieldEntityValue fieldEntityValue)
			throws Exception {
		Object result = null;
		if ((fieldEntityValue != null) && (fieldEntityValue.getValue() != null)) {
			if (column.isDiscriminatorColumn())
				result = this.getDiscriminatorValue();
			else {
				if (column.isForeignKey())
					result = ((Map<?, ?>) fieldEntityValue.getValue()).get(column.getReferencedColumnName());
				else
					result = ((Map<?, ?>) fieldEntityValue.getValue()).get(column.getColumnName());
				if (column.getColumnDataType() == Types.DATE)
					result = ConversionHelper.getInstance().convert(result, java.sql.Date.class);
				else if (column.getColumnDataType() == Types.TIME)
					result = ConversionHelper.getInstance().convert(result, java.sql.Time.class);
				else if (column.getColumnDataType() == Types.TIMESTAMP)
					result = ConversionHelper.getInstance().convert(result, java.sql.Timestamp.class);
			}
		}
		return result;
	}

	public String getVersionColumnName() {
		for (DescriptionColumn column : columns) {
			if (column.isVersioned())
				return column.getColumnName();
		}
		return null;
	}

	public DescriptionColumn getVersionColumn() {
		for (DescriptionColumn column : columns) {
			if (column.isVersioned())
				return column;
		}
		return null;
	}

	public boolean isVersioned() {
		for (DescriptionColumn column : columns) {
			if (column.isVersioned())
				return true;
		}
		return false;
	}

	public boolean isTable() {
		return !this.hasDiscriminatorValue();
	}

	public Set<String> getAllFieldNames() {
		return fieldNames;
	}

	public boolean hasDescriptionField() {
		return this.fields != null && this.fields.size() > 0;
	}

	public void addNamedQuery(DescriptionNamedQuery namedQuery) {
		this.namedQueries.add(namedQuery);
	}

	public List<DescriptionNamedQuery> getDescriptionNamedQueries() {
		return this.namedQueries;
	}

	public DescriptionNamedQuery getDescriptionNamedQuery(String name) {
		for (DescriptionNamedQuery d : this.namedQueries)
			if (name.equals(d.getName()))
				return d;
		return null;
	}

	public boolean hasNamedQueries() {
		return this.namedQueries.size() > 0;
	}

	public void addSecondaryTable(DescritionSecondaryTable secondaryTable) {
		this.secondaryTables.add(secondaryTable);
	}

	public void addDescriptionIndex(DescriptionIndex index) {
		this.indexes.add(index);
	}

	public DescriptionIndex getDescriptionIndex(String name) {
		for (DescriptionIndex i : this.indexes)
			if (name.equals(i.getName()))
				return i;

		return null;
	}

	public List<DescriptionIndex> getDescriptionIndexes() {
		return this.indexes;
	}

	public boolean hasIndexes() {
		return this.indexes.size() > 0;
	}

	public boolean existsColumn(String columnName) {
		for (DescriptionColumn column : this.columns)
			if (columnName.equals(column.getColumnName()))
				return true;
		return false;
	}

	public boolean isCompositeId() {
		for (DescriptionColumn column : this.columns)
			if (column.isCompositeId())
				return true;
		return false;
	}

	public Map<SQLStatementType, DescriptionSQL> getDescriptionSql() {
		return descriptionSql;
	}

	public void setDescriptionSql(Map<SQLStatementType, DescriptionSQL> descriptionSql) {
		this.descriptionSql = descriptionSql;
	}

	public DescriptionSQL getDescriptionSqlByType(SQLStatementType type) {
		if (descriptionSql != null)
			return descriptionSql.get(type);
		return null;
	}

	public boolean isExistsDescriptionSQL() {
		return (descriptionSql.size() > 0);
	}

	public DescriptionField[] getPrimaryKeyFields() {
		List<DescriptionField> result = new ArrayList<DescriptionField>();
		for (DescriptionField field : fields) {
			if (field.isPrimaryKey())
				result.add(field);
		}
		return result.toArray(new DescriptionField[] {});
	}

	public boolean isAbstractClass() {
		return abstractClass;
	}

	public void setAbstractClass(boolean abstractClass) {
		this.abstractClass = abstractClass;
	}

	public List<DescriptionIndex> getIndexes() {
		if (indexes == null)
			indexes = new ArrayList<DescriptionIndex>();
		return indexes;
	}

	public void setIndexes(List<DescriptionIndex> indexes) {
		this.indexes = indexes;
	}

	public DescriptionField getDescriptionFieldUsesColumns(Class<?> sourceClass, List<String> columNames) {
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (descriptionField.isRelationShip()) {
				boolean containsColumns = descriptionField.isContainsColumns(columNames);
				Class<?> fc = descriptionField.getFieldClass();
				boolean isInheritance = ReflectionUtils.isExtendsClass(sourceClass, descriptionField.getFieldClass());
				if ((containsColumns) && ((fc == sourceClass) || (isInheritance)))
					return descriptionField;
			} else if (descriptionField.isCollectionEntity()) {
				boolean isInheritance = ReflectionUtils.isExtendsClass(sourceClass, descriptionField.getFieldClass());
				if ((descriptionField.getFieldClass() == sourceClass) || (isInheritance))
					return descriptionField;
			} else if (descriptionField.isJoinTable()) {
				boolean isInheritance = ReflectionUtils.isExtendsClass(sourceClass, descriptionField.getFieldClass());
				if ((descriptionField.getFieldClass() == sourceClass) || (isInheritance))
					return descriptionField;
			}
		}
		return null;
	}

	public DescriptionField getDescriptionFieldUsesColumns(List<String> columNames) {
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (descriptionField.isContainsColumns(columNames)) {
				return descriptionField;
			}
		}
		return null;
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

	public List<DescriptionUniqueConstraint> getUniqueConstraints() {
		return uniqueConstraints;
	}

	public void setUniqueConstraints(List<DescriptionUniqueConstraint> uniqueConstraints) {
		this.uniqueConstraints = uniqueConstraints;
	}

	public void addUniqueConstraint(DescriptionUniqueConstraint uniqueConstraint) {
		uniqueConstraints.add(uniqueConstraint);
	}

	public void addIndex(DescriptionIndex index) {
		indexes.add(index);
	}

	public void addAllUniqueConstraints(List<DescriptionUniqueConstraint> uniqueConstraints) {
		this.uniqueConstraints.addAll(uniqueConstraints);
	}

	public void addAllDescriptionIndex(List<DescriptionIndex> descriptionIndexes) {
		this.indexes.addAll(descriptionIndexes);
	}

	public ConnectivityType getImportConnectivityType() {
		return importConnectivityType;
	}

	public void setImportConnectivityType(ConnectivityType importConnectivityType) {
		this.importConnectivityType = importConnectivityType;
	}

	public ConnectivityType getExportConnectivityType() {
		return exportConnectivityType;
	}

	public void setExportConnectivityType(ConnectivityType exportConnectivityType) {
		this.exportConnectivityType = exportConnectivityType;
	}

	public String getDisplayLabel() {
		return displayLabel;
	}

	public void setDisplayLabel(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	public Integer getExportOrderToSendData() {
		return exportOrderToSendData;
	}

	public void setExportOrderToSendData(Integer exportOrderToSendData) {
		this.exportOrderToSendData = exportOrderToSendData;
	}

	public String[] getExportColumns() {
		return exportColumns;
	}

	public void setExportColumns(String[] exportColumns) {
		this.exportColumns = exportColumns;
	}

	public DescriptionColumn getColumnIdSynchronism() {
		for (DescriptionColumn column : columns) {
			if (column.isIdSynchronism())
				return column;
		}
		return null;
	}

	public int exportColumnsCount() {
		if (exportColumns != null)
			return exportColumns.length;
		return 0;
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
			exportParams = new HashMap<Integer, ParamDescription>();
		return exportParams;
	}

	public void setExportParams(Map<Integer, ParamDescription> exportParams) {
		this.exportParams = exportParams;
	}

	public Map<Integer, ParamDescription> getImportParams() {
		if (importParams == null)
			importParams = new HashMap<Integer, ParamDescription>();
		return importParams;
	}

	public void setImportParams(Map<Integer, ParamDescription> importParams) {
		this.importParams = importParams;
	}

	public boolean isExportTable() {
		return StringUtils.isNotEmpty(mobileActionExport);
	}

	public boolean isImportTable() {
		return StringUtils.isNotEmpty(mobileActionImport);
	}

	public DescriptionColumn getColumnDescription(String columnName) {
		for (DescriptionColumn column : columns) {
			if (columnName.equals(column.getColumnName()))
				return column;
		}
		return null;
	}

	public List<DescriptionConvert> getConverts() {
		return converts;
	}

	public void setConverts(List<DescriptionConvert> converts) {
		this.converts = converts;
	}

	public boolean isInheritance() {
		if (discriminatorValue == null)
			return false;
		return (!"".equals(discriminatorValue));
	}

	public void setObjectValues(Object target, Map<String, Object> values) throws Exception {
		if ((values == null) || (target == null))
			return;
		for (String fieldName : values.keySet()) {
			DescriptionField descriptionField = getDescriptionField(fieldName);
			if (descriptionField == null) {
				throw new EntityCacheException("Não foi possível atribuir o mapa de valores ao objeto pois o campo "
						+ fieldName + " não encontrado na classe " + this.getEntityClass().getName());
			}
			descriptionField.setObjectValue(fieldName, values.get(fieldName));
		}
	}

	public DescriptionField getDescriptionFieldWithMappedBy(Class<?> sourceType, String mappedBy) {
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (mappedBy.equals(descriptionField.getMappedBy())) {
				if (descriptionField.getTargetClass().equals(sourceType))
					return descriptionField;
			}
		}
		return null;
	}

	public boolean hasDescriptionFieldWithMappedBy(Class<?> sourceType, String mappedBy) {
		DescriptionField result = getDescriptionFieldWithMappedBy(sourceType, mappedBy);
		return (result != null);
	}

	public boolean isIncompletePrimaryKeyValue(Object source) throws Exception {
		if (source == null)
			return true;
		for (DescriptionField descriptionField : this.getPrimaryKeyFields()) {
			if (descriptionField.getObjectValue(source) == null)
				return true;
		}
		return false;
	}

	public void setPrimaryKeyValue(Object source, Object target) throws Exception {
		if ((source == null) || (target == null))
			return;
		if (!(source.getClass().equals(target.getClass())))
			return;

		for (DescriptionField descriptionField : this.getPrimaryKeyFields()) {
			descriptionField.setObjectValue(target, descriptionField.getObjectValue(source));
		}
	}

	public String getSimpleName() {
		if (entityClass != null)
			return entityClass.getSimpleName();
		return "";
	}

	public boolean containsDescriptionField(DescriptionField descriptionField) {
		for (DescriptionField descField : fields) {
			if (descField.equals(descriptionField))
				return true;
		}
		return false;
	}

	public Map<GeneratedType, DescriptionGenerator> getGenerators() {
		return Collections.unmodifiableMap(generators);
	}

	public void add(GeneratedType type, DescriptionGenerator descriptionGenerator) {
		generators.put(type, descriptionGenerator);
	}

	public DescriptionGenerator getGeneratorByName(String generator) {
		for (DescriptionGenerator descriptionGenerator : generators.values()) {
			if (descriptionGenerator.getValue().equalsIgnoreCase(generator)) {
				return descriptionGenerator;
			}
		}
		return null;
	}

	public boolean hasGenerators() {
		return getGenerators().size() > 0;
	}

	public int getMaxRecordBlockExport() {
		return maxRecordBlockExport;
	}

	public void setMaxRecordBlockExport(int maxRecordBlockExport) {
		this.maxRecordBlockExport = maxRecordBlockExport;
	}

	public boolean containsSecondaryTable(String tableName) {
		for (DescritionSecondaryTable secondaryTable : getSecondaryTables()) {
			if (secondaryTable.getTableName().equalsIgnoreCase(tableName)) {
				return true;
			}
		}
		return false;
	}

	public List<DescriptionPkJoinColumn> getPrimaryKeyJoinColumns() {
		return primaryKeyJoinColumns;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public void setInheritanceType(InheritanceType inheritanceType) {
		this.inheritanceType = inheritanceType;
	}

	public void mergeValues(Object actualEntity, Object newEntity) throws Exception {
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()) {
				if (descriptionField.getFetchType() != null && descriptionField.getFetchType().equals(FetchType.LAZY)) {
					if (descriptionField.getObjectValue(newEntity) != null) {
						Object value = descriptionField.getObjectValue(actualEntity);
						if (value instanceof AnterosPersistentCollection)
							((AnterosPersistentCollection) value).initialize();
						descriptionField.setValue(actualEntity, descriptionField.getObjectValue(newEntity));
					}
				} else {
					descriptionField.setValue(actualEntity, descriptionField.getObjectValue(newEntity));
				}
			} else {
				descriptionField.setValue(actualEntity, descriptionField.getObjectValue(newEntity));
			}
		}
	}

	public boolean hasDependencyFrom(EntityCache entityCache2, Object source, Object target) {
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (descriptionField.isRelationShip()) {
				try {
					if ((descriptionField.getObjectValue(source) != null)
							&& descriptionField.getObjectValue(source).equals(target)) {
						return true;
					}
				} catch (Exception e) {
					return false;
				}
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityClass == null) ? 0 : entityClass.hashCode());
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
		EntityCache other = (EntityCache) obj;
		if (entityClass == null) {
			if (other.entityClass != null)
				return false;
		} else if (!entityClass.equals(other.entityClass))
			return false;
		return true;
	}

	public List<EntityListener> getEntityListeners() {
		return entityListeners;
	}

	public void setEntityListeners(List<EntityListener> entityListeners) {
		this.entityListeners = entityListeners;
	}

	public Map<Method, EventType> getMethodListeners() {
		return methodListeners;
	}

	public void setMethodListeners(Map<Method, EventType> methodListeners) {
		this.methodListeners = methodListeners;
	}
	
	public MethodAccess getMethodAccess() {
		if (methodAccess == null) {
			methodAccess = MethodAccess.get(this.getEntityClass());
		}
		return methodAccess;
	}

	
	public FieldAccess getFieldAccess() {
		if (fieldAccess == null) {
			fieldAccess = FieldAccess.get(this.getEntityClass());
		}
		return fieldAccess;
	}

	public ConstructorAccess getConstructorAccess() {
		if (constructorAccess == null) {
			constructorAccess = ConstructorAccess.get(this.getEntityClass());
		}
		return constructorAccess;
	}

	public DescriptionField getCodeField() {
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (descriptionField.getField().isAnnotationPresent(Code.class)) {
				return descriptionField;
			}
		}
		return null;		
	}

	public boolean hasTenantId() {
		return getTenantId()!=null;
	}

	public boolean hasCompanyId() {
		return getCompanyId()!=null;
	}

	public List<DescriptionField> getJoinTables() {
		ArrayList<DescriptionField> result = new ArrayList<DescriptionField>();
		for (DescriptionField descriptionField : getDescriptionFields()) {
			if (descriptionField.getField().isAnnotationPresent(JoinTable.class)) {
				result.add(descriptionField);
			}
		}	
		return result;
	}

}
