package br.com.anteros.persistence.session.query.filter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.anteros.core.utils.ArrayUtils;
import br.com.anteros.core.utils.ObjectUtils;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.dsl.osql.BooleanBuilder;
import br.com.anteros.persistence.dsl.osql.DynamicEntityPath;
import br.com.anteros.persistence.dsl.osql.OSQLQuery;
import br.com.anteros.persistence.dsl.osql.support.Expressions;
import br.com.anteros.persistence.dsl.osql.types.Ops;
import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.Predicate;
import br.com.anteros.persistence.dsl.osql.types.expr.BooleanExpression;
import br.com.anteros.persistence.dsl.osql.types.expr.Param;
import br.com.anteros.persistence.dsl.osql.types.expr.params.BigDecimalParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.BigIntegerParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.DateParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.DateTimeParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.DoubleParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.FloatParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.LongParam;
import br.com.anteros.persistence.dsl.osql.types.expr.params.StringParam;
import br.com.anteros.persistence.dsl.osql.types.path.BooleanPath;
import br.com.anteros.persistence.dsl.osql.types.path.DatePath;
import br.com.anteros.persistence.dsl.osql.types.path.DateTimePath;
import br.com.anteros.persistence.dsl.osql.types.path.EntityPathBase;
import br.com.anteros.persistence.dsl.osql.types.path.EnumPath;
import br.com.anteros.persistence.dsl.osql.types.path.NumberPath;
import br.com.anteros.persistence.dsl.osql.types.path.StringPath;
import br.com.anteros.persistence.dsl.osql.types.path.TimePath;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.exception.SQLSessionException;
import br.com.anteros.persistence.session.repository.Page;
import br.com.anteros.persistence.session.repository.Pageable;
import br.com.anteros.persistence.session.repository.impl.PageImpl;

public class AnterosMultipleFieldsFilter<T> {

	private String fields;
	private String value;
	private SQLSession session;
	private Class<T> resultClass;
	private Pageable page;
	private EntityPathBase<T> entityPath;
	private OSQLQuery query;
	private String fieldsSort;
	private Predicate predicate;
	private Integer paramNumber;
	private String fieldsToForceLazy;
	private boolean readOnly = false;

	public AnterosMultipleFieldsFilter(EntityPathBase<T> entityPath, Predicate predicate) {
		super();
		this.entityPath = entityPath;
		this.predicate = predicate;
	}

	public AnterosMultipleFieldsFilter(EntityPathBase<T> entityPath) {
		super();
		this.entityPath = entityPath;
	}

	public AnterosMultipleFieldsFilter(Predicate predicate) {
		super();
		this.predicate = predicate;
	}

	public AnterosMultipleFieldsFilter() {
		super();
	}

	public AnterosMultipleFieldsFilter<T> filter(String value) {
		this.value = value;
		return this;
	}

	public AnterosMultipleFieldsFilter<T> fields(String fields) {
		this.fields = fields;
		return this;
	}

	public AnterosMultipleFieldsFilter<T> resultClass(Class<T> resultClass) {
		this.resultClass = resultClass;
		return this;
	}

	public AnterosMultipleFieldsFilter<T> session(SQLSession session) {
		this.session = session;
		return this;
	}

	public AnterosMultipleFieldsFilter<T> page(Pageable pageRequest) {
		this.page = pageRequest;
		return this;
	}

	public AnterosMultipleFieldsFilter<T> fieldsSort(String fieldsSort) {
		this.fieldsSort = fieldsSort;
		return this;
	}

	public EntityPathBase<T> getEntityPath() {
		if (entityPath == null) {
			this.entityPath = new DynamicEntityPath(resultClass, "P");
		}
		return this.entityPath;
	}

	protected void buildQuery() {
		
		if (ObjectUtils.isEmpty(value) && ObjectUtils.isEmpty(fields) && predicate==null) {
			throw new SQLSessionException("Informe os campos e valores para o filtro ou um predicado para consultar  a entidade "
					+ session.getEntityCacheManager().getEntityCache(resultClass).getEntityClass().getName());	
		}

		EntityCache[] entityCaches = session.getEntityCacheManager().getEntitiesBySuperClassIncluding(resultClass);

		String[] arrFields = null;
		String[] values = null;
		if (!ObjectUtils.isEmpty(value) && !ObjectUtils.isEmpty(fields)) {
			values = StringUtils.tokenizeToStringArray(value, ",");

			arrFields = StringUtils.tokenizeToStringArray(fields, ",");
			if (arrFields == null) {
				arrFields = new String[] { fields };
			}
		}

		if (entityPath == null) {
			this.entityPath = new DynamicEntityPath(resultClass, "P");
		}

		query = new OSQLQuery(session).from(entityPath);
		query.readOnly(readOnly);
		query.setFieldsToForceLazy(fieldsToForceLazy);

		BooleanBuilder builder = new BooleanBuilder();

		Map<Param<?>, Object> parameters = new LinkedHashMap<Param<?>, Object>();
		this.paramNumber = 0;

		if (arrFields != null && values != null) {
			for (String field : arrFields) {
				String[] fieldArr = StringUtils.tokenizeToStringArray(field, ".");
				processField(entityPath, entityCaches, fieldArr, values, parameters, builder);
			}
		}

		if (this.predicate != null) {
			builder.and(predicate);
		}

		DescriptionField tenantId = null;
		DescriptionField companyId = null;

		for (EntityCache entityCache : entityCaches) {
			tenantId = entityCache.getTenantId();
			if (tenantId != null)
				break;
		}
		for (EntityCache entityCache : entityCaches) {
			companyId = entityCache.getCompanyId();
			if (companyId != null)
				break;
		}

		if (tenantId != null) {
			if (session.getTenantId() == null) {
				throw new SQLSessionException("Informe o Tenant Id para consultar  a entidade "
						+ session.getEntityCacheManager().getEntityCache(resultClass).getEntityClass().getName());
			}
			StringPath predicateField = entityPath.createString(tenantId.getName());
			paramNumber++;
			StringParam param = new StringParam("P" + paramNumber);
			parameters.put(param, session.getTenantId());
			BooleanExpression expression = Expressions.predicate(Ops.EQ, predicateField, param);
			builder.and(expression);
		}

		if (companyId != null) {
			if (session.getCompanyId() == null) {
				throw new SQLSessionException("Informe o Company Id para consultar  a entidade "
						+ session.getEntityCacheManager().getEntityCache(resultClass).getEntityClass().getName());
			}
			StringPath predicateField = entityPath.createString(companyId.getName());
			paramNumber++;
			StringParam param = new StringParam("P" + paramNumber);
			parameters.put(param, session.getCompanyId());
			BooleanExpression expression = Expressions.predicate(Ops.EQ, predicateField, param);
			builder.and(expression);
		}

		query.where(builder);
		for (Param param : parameters.keySet()) {
			query.set(param, parameters.get(param));
		}

		if (!StringUtils.isEmpty(fieldsSort)) {
			List<OrderSpecifier> orderBy = AnterosSortFieldsHelper.convertFieldsToOrderby(session, entityPath,
					entityCaches, fieldsSort);
			query.orderBy(orderBy.toArray(new OrderSpecifier[] {}));
		}

	}

	private void processSortField(DynamicEntityPath dynamicEntityPath, EntityCache[] entityCaches, String[] fieldArr) {
		String field = fieldArr[0];
		String fld = field;
		String order = "asc";
		if (field.contains(":")) {
			String[] v = StringUtils.tokenizeToStringArray(field, ":");
			fld = v[0];
			order = v[1];
		}

		DescriptionField descriptionField = getDescriptionField(entityCaches, fld);
		if (descriptionField == null) {
			throw new SQLSessionException(
					"Campo " + field + " não encontrado na lista de campos da classe  ou de suas heranças.");
		}

		if (descriptionField.isRelationShip()) {
			Class<?> targetClass = descriptionField.getTargetClass();
			DynamicEntityPath relationShipEntityPath = dynamicEntityPath.createEntityPath(targetClass, field);
			EntityCache[] newEntityCaches = session.getEntityCacheManager()
					.getEntitiesBySuperClassIncluding(targetClass);
			String[] restFields = ArrayUtils.remove(fieldArr, 0);
			if (restFields.length > 0) {
				processSortField(relationShipEntityPath, newEntityCaches, restFields);
			} else {
				addSort(dynamicEntityPath, descriptionField, order);
			}

		} else if (descriptionField.isSimple()) {
			addSort(dynamicEntityPath, descriptionField, order);
		} else if (descriptionField.isAnyCollection()) {
			throw new SQLSessionException("Campo " + field
					+ " é uma coleção. Ainda não é permitido fazer buscas em coleções até esta versão.");
		}

	}

	private void processField(EntityPathBase dynamicEntityPath, EntityCache[] entityCaches, String[] fieldArr,
			String[] values, Map<Param<?>, Object> parameters, BooleanBuilder builder) {

		String field = fieldArr[0];

		DescriptionField descriptionField = getDescriptionField(entityCaches, field);
		if (descriptionField == null) {
			throw new SQLSessionException(
					"Campo " + field + " não encontrado na lista de campos da classe  ou de suas heranças.");
		}

		if (descriptionField.isRelationShip()) {
			Class<?> targetClass = descriptionField.getTargetClass();
			DynamicEntityPath relationShipEntityPath = dynamicEntityPath.createEntityPath(targetClass, field);
			EntityCache[] newEntityCaches = session.getEntityCacheManager()
					.getEntitiesBySuperClassIncluding(targetClass);
			String[] restFields = ArrayUtils.remove(fieldArr, 0);
			if (restFields.length > 0) {
				processField(relationShipEntityPath, newEntityCaches, restFields, values, parameters, builder);
			} else {
				addCondition(dynamicEntityPath, values, descriptionField, parameters, builder);
			}

		} else if (descriptionField.isSimple()) {
			addCondition(dynamicEntityPath, values, descriptionField, parameters, builder);
		} else if (descriptionField.isAnyCollection()) {
			throw new SQLSessionException("Campo " + field
					+ " é uma coleção. Ainda não é permitido fazer buscas em coleções até esta versão.");
		}
	}

	private void addSort(DynamicEntityPath entityPath, DescriptionField descriptionField, String order) {
		if (descriptionField.isSimple()) {
			if (ReflectionUtils.isExtendsClass(String.class, descriptionField.getField().getType())) {
				StringPath predicateField = entityPath.createString(descriptionField.getName());
				if ("asc".equals(order)) {
					query.orderBy(predicateField.asc());
				} else {
					query.orderBy(predicateField.desc());
				}
			} else if (ReflectionUtils.isExtendsClass(Number.class, descriptionField.getField().getType())) {
				if (ReflectionUtils.isExtendsClass(Double.class, descriptionField.getField().getType())) {
					NumberPath<Double> predicateField = entityPath.createNumber(descriptionField.getName(),
							Double.class);
					if ("asc".equals(order)) {
						query.orderBy(predicateField.asc());
					} else {
						query.orderBy(predicateField.desc());
					}
				} else if (ReflectionUtils.isExtendsClass(Float.class, descriptionField.getField().getType())) {
					NumberPath<Float> predicateField = entityPath.createNumber(descriptionField.getName(), Float.class);
					if ("asc".equals(order)) {
						query.orderBy(predicateField.asc());
					} else {
						query.orderBy(predicateField.desc());
					}
				} else if (ReflectionUtils.isExtendsClass(BigDecimal.class, descriptionField.getField().getType())) {
					NumberPath<BigDecimal> predicateField = entityPath.createNumber(descriptionField.getName(),
							BigDecimal.class);
					if ("asc".equals(order)) {
						query.orderBy(predicateField.asc());
					} else {
						query.orderBy(predicateField.desc());
					}
				} else if (ReflectionUtils.isExtendsClass(BigInteger.class, descriptionField.getField().getType())) {
					NumberPath<BigInteger> predicateField = entityPath.createNumber(descriptionField.getName(),
							BigInteger.class);
					if ("asc".equals(order)) {
						query.orderBy(predicateField.asc());
					} else {
						query.orderBy(predicateField.desc());
					}
				} else {
					NumberPath<Long> predicateField = entityPath.createNumber(descriptionField.getName(), Long.class);
					if ("asc".equals(order)) {
						query.orderBy(predicateField.asc());
					} else {
						query.orderBy(predicateField.desc());
					}
				}
			} else if (ReflectionUtils.isExtendsClass(Date.class, descriptionField.getField().getType())) {
				if (descriptionField.isTemporalDate()) {
					DatePath<Date> predicateField = entityPath.createDate(descriptionField.getName(), Date.class);
					if ("asc".equals(order)) {
						query.orderBy(predicateField.asc());
					} else {
						query.orderBy(predicateField.desc());
					}

				}
			} else if (descriptionField.isTemporalDateTime()) {
				DateTimePath<Date> predicateField = entityPath.createDateTime(descriptionField.getName(), Date.class);
				if ("asc".equals(order)) {
					query.orderBy(predicateField.asc());
				} else {
					query.orderBy(predicateField.desc());
				}

			} else if (descriptionField.isTemporalTime()) {
				TimePath<Date> predicateField = entityPath.createTime(descriptionField.getName(), Date.class);
				if ("asc".equals(order)) {
					query.orderBy(predicateField.asc());
				} else {
					query.orderBy(predicateField.desc());
				}

			}
		}
	}

	private void addCondition(EntityPathBase entityPath, String[] values, DescriptionField descriptionField,
			Map<Param<?>, Object> parameters, BooleanBuilder builder) {

		for (String vl : values) {

			if (descriptionField.isSimple()) {
				BooleanBuilder builderAND = new BooleanBuilder();

				BooleanExpression predicate = null;

				String[] andValues = StringUtils.tokenizeToStringArray(vl, "&");
				for (String andValue : andValues) {

					String value1 = andValue;
					String value2 = null;
					if (andValue.contains(":")) {
						String[] v = StringUtils.tokenizeToStringArray(andValue, ":");
						value1 = v[0];
						value2 = v[1];
					}

					if (ReflectionUtils.isEnum(descriptionField.getField().getType())) {
						EnumPath predicateField = entityPath.createEnum(descriptionField.getName(), Enum.class);
						paramNumber++;
						StringParam param = new StringParam("P" + paramNumber);
						parameters.put(param, value1);
						predicate = Expressions.predicate(Ops.EQ_IGNORE_CASE, predicateField, param);
					} else if (descriptionField.isBoolean()) {
						if (isBoolean(value1)) {
							BooleanPath predicateField = entityPath.createBoolean(descriptionField.getName());
							paramNumber++;
							StringParam param = new StringParam("P" + paramNumber);
							parameters.put(param, value1);
							predicate = Expressions.predicate(Ops.EQ, predicateField, param);
						}
					} else if (ReflectionUtils.isExtendsClass(String.class, descriptionField.getField().getType())) {
						if (!isDate(value1) && (!isDateTime(value1))) {
							if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
								StringPath predicateField = entityPath.createString(descriptionField.getName());
								paramNumber++;
								StringParam param1 = new StringParam("P" + paramNumber);
								parameters.put(param1, value1);

								paramNumber++;
								StringParam param2 = new StringParam("P" + paramNumber);
								parameters.put(param2, value2);

								predicate = Expressions.predicate(Ops.BETWEEN, predicateField, param1, param2);
							} else {
								StringPath predicateField = entityPath.createString(descriptionField.getName());
								paramNumber++;
								StringParam param = new StringParam("P" + paramNumber);
								parameters.put(param, "%" + value1 + "%");
								predicate = Expressions.predicate(Ops.LIKE, predicateField, param);
							}
						}
					}

					if (ReflectionUtils.isExtendsClass(Number.class, descriptionField.getField().getType())) {
						if (ReflectionUtils.isExtendsClass(Double.class, descriptionField.getField().getType())) {
							if (StringUtils.isNumber(value1)) {
								if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
									NumberPath<Double> predicateField = entityPath
											.createNumber(descriptionField.getName(), Double.class);
									paramNumber++;
									DoubleParam param1 = new DoubleParam("P" + paramNumber);
									parameters.put(param1, Double.parseDouble(value1));

									paramNumber++;
									DoubleParam param2 = new DoubleParam("P" + paramNumber);
									parameters.put(param2, Double.parseDouble(value2));

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, param1, param2);
								} else {
									NumberPath<Double> predicateField = entityPath
											.createNumber(descriptionField.getName(), Double.class);
									paramNumber++;
									DoubleParam param = new DoubleParam("P" + paramNumber);
									parameters.put(param, Double.parseDouble(value1));
									predicate = Expressions.predicate(Ops.EQ, predicateField, param);
								}
							}
						} else if (ReflectionUtils.isExtendsClass(Float.class, descriptionField.getField().getType())) {
							if (StringUtils.isNumber(value1)) {
								if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {

									NumberPath<Float> predicateField = entityPath
											.createNumber(descriptionField.getName(), Float.class);
									paramNumber++;
									FloatParam param1 = new FloatParam("P" + paramNumber);
									parameters.put(param1, Float.parseFloat(value1));

									paramNumber++;
									FloatParam param2 = new FloatParam("P" + paramNumber);
									parameters.put(param2, Float.parseFloat(value2));

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, param1, param2);

								} else {
									NumberPath<Float> predicateField = entityPath
											.createNumber(descriptionField.getName(), Float.class);
									paramNumber++;
									FloatParam param = new FloatParam("P" + paramNumber);
									parameters.put(param, Float.parseFloat(value));
									predicate = Expressions.predicate(Ops.EQ, predicateField, param);
								}
							}
						} else if (ReflectionUtils.isExtendsClass(BigDecimal.class,
								descriptionField.getField().getType())) {
							if (StringUtils.isNumber(value1)) {
								if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
									NumberPath<BigDecimal> predicateField = entityPath
											.createNumber(descriptionField.getName(), BigDecimal.class);

									paramNumber++;
									BigDecimalParam param1 = new BigDecimalParam("P" + paramNumber);
									parameters.put(param1, new BigDecimal(value1));

									paramNumber++;
									BigDecimalParam param2 = new BigDecimalParam("P" + paramNumber);
									parameters.put(param2, new BigDecimal(value2));

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, param1, param2);
								} else {
									NumberPath<BigDecimal> predicateField = entityPath
											.createNumber(descriptionField.getName(), BigDecimal.class);

									paramNumber++;
									BigDecimalParam param = new BigDecimalParam("P" + paramNumber);
									parameters.put(param, new BigDecimal(value1));
									predicate = Expressions.predicate(Ops.EQ, predicateField, param);
								}
							}
						} else if (ReflectionUtils.isExtendsClass(BigInteger.class,
								descriptionField.getField().getType())) {
							if (StringUtils.isInteger(value1)) {
								if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
									NumberPath<BigInteger> predicateField = entityPath
											.createNumber(descriptionField.getName(), BigInteger.class);

									paramNumber++;
									BigIntegerParam param1 = new BigIntegerParam("P" + paramNumber);
									parameters.put(param1, new BigInteger(value1));

									paramNumber++;
									BigIntegerParam param2 = new BigIntegerParam("P" + paramNumber);
									parameters.put(param2, new BigInteger(value2));

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, param1, param2);
								} else {
									NumberPath<BigInteger> predicateField = entityPath
											.createNumber(descriptionField.getName(), BigInteger.class);
									paramNumber++;
									BigIntegerParam param = new BigIntegerParam("P" + paramNumber);
									parameters.put(param, new BigInteger(value1));
									predicate = Expressions.predicate(Ops.EQ, predicateField, param);
								}
							}
						} else {
							if (StringUtils.isInteger(value1)) {
								if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
									NumberPath<Long> predicateField = entityPath
											.createNumber(descriptionField.getName(), Long.class);
									paramNumber++;
									LongParam param1 = new LongParam("P" + paramNumber);
									parameters.put(param1, Long.parseLong(value1));

									paramNumber++;
									LongParam param2 = new LongParam("P" + paramNumber);
									parameters.put(param2, Long.parseLong(value2));

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, param1, param2);
								} else {
									NumberPath<Long> predicateField = entityPath
											.createNumber(descriptionField.getName(), Long.class);
									paramNumber++;
									LongParam param = new LongParam("P" + paramNumber);
									parameters.put(param, Long.parseLong(value1));
									predicate = Expressions.predicate(Ops.EQ, predicateField, param);
								}
							}
						}

					}

					if (ReflectionUtils.isExtendsClass(Date.class, descriptionField.getField().getType())) {
						if (isDate(value1)) {
							if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
								if (descriptionField.isTemporalDate()) {
									DatePath<Date> predicateField = entityPath.createDate(descriptionField.getName(),
											Date.class);

									Date dt1 = getDate(value1);
									paramNumber++;
									DateParam dtParam1 = new DateParam("P" + paramNumber);
									parameters.put(dtParam1, dt1);

									Date dt2 = getDate(value2);
									paramNumber++;
									DateParam dtParam2 = new DateParam("P" + paramNumber);
									parameters.put(dtParam2, dt2);

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, dtParam1, dtParam2);
								}
							} else {
								Date dt = getDate(value1);
								if (descriptionField.isTemporalDate()) {
									DatePath<Date> predicateField = entityPath.createDate(descriptionField.getName(),
											Date.class);
									paramNumber++;
									DateParam dtParam = new DateParam("P" + paramNumber);
									parameters.put(dtParam, dt);
									predicate = Expressions.predicate(Ops.EQ, predicateField, dtParam);
								}
							}
						} else if (isDateTime(value1)) {

							if (!StringUtils.isEmpty(value1) && !StringUtils.isEmpty(value2)) {
								if (descriptionField.isTemporalDateTime()) {
									DateTimePath<Date> predicateField = entityPath
											.createDateTime(descriptionField.getName(), Date.class);
									paramNumber++;

									Date dt1 = getDateTime(value1);
									DateTimeParam dtTimeParam1 = new DateTimeParam("P" + paramNumber);
									parameters.put(dtTimeParam1, dt1);

									paramNumber++;

									Date dt2 = getDateTime(value2);
									DateTimeParam dtTimeParam2 = new DateTimeParam("P" + paramNumber);
									parameters.put(dtTimeParam2, dt2);

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, dtTimeParam1,
											dtTimeParam2);
								} else if (descriptionField.isTemporalTime()) {
									TimePath<Date> predicateField = entityPath.createTime(descriptionField.getName(),
											Date.class);

									Date dt1 = getDateTime(value1);
									Calendar calendar = Calendar.getInstance();
									calendar.set(Calendar.HOUR, dt1.getHours());
									calendar.set(Calendar.MINUTE, dt1.getMinutes());
									calendar.set(Calendar.SECOND, dt1.getSeconds());
									paramNumber++;
									DateTimeParam dtTimeParam1 = new DateTimeParam("P" + paramNumber);
									parameters.put(dtTimeParam1, calendar.getTime());

									Date dt2 = getDateTime(value2);
									paramNumber++;
									calendar.set(Calendar.HOUR, dt2.getHours());
									calendar.set(Calendar.MINUTE, dt2.getMinutes());
									calendar.set(Calendar.SECOND, dt2.getSeconds());
									DateTimeParam dtTimeParam2 = new DateTimeParam("P" + paramNumber);
									parameters.put(dtTimeParam2, calendar.getTime());

									predicate = Expressions.predicate(Ops.BETWEEN, predicateField, dtTimeParam1,
											dtTimeParam2);
								}
							} else {
								Date dt = getDateTime(value1);
								if (descriptionField.isTemporalDateTime()) {
									DateTimePath<Date> predicateField = entityPath
											.createDateTime(descriptionField.getName(), Date.class);
									paramNumber++;
									DateTimeParam dtTimeParam = new DateTimeParam("P" + paramNumber);
									parameters.put(dtTimeParam, dt);
									predicate = Expressions.predicate(Ops.EQ, predicateField, dtTimeParam);
								} else if (descriptionField.isTemporalTime()) {
									TimePath<Date> predicateField = entityPath.createTime(descriptionField.getName(),
											Date.class);

									Calendar calendar = Calendar.getInstance();
									calendar.set(Calendar.HOUR, dt.getHours());
									calendar.set(Calendar.MINUTE, dt.getMinutes());
									calendar.set(Calendar.SECOND, dt.getSeconds());

									paramNumber++;
									DateTimeParam dtTimeParam = new DateTimeParam("P" + paramNumber);
									parameters.put(dtTimeParam, calendar.getTime());
									predicate = Expressions.predicate(Ops.EQ, predicateField, dtTimeParam);
								}
							}
						}
					}

					if (predicate != null) {
						builderAND.and(predicate);
					}
				}

				if (predicate != null) {
					builder.or(builderAND);
				}

			}
		}
	}

	public List<T> buildAndGetList() {
		buildQuery();
		if (page != null) {
			query.limit(page.getPageSize()).offset(page.getOffset());
		}
		return query.list(entityPath);
	}

	public List buildAndGetIds() {
		buildQuery();
		if (page != null) {
			query.limit(page.getPageSize()).offset(page.getOffset());
		}
		EntityCache entityCache = session.getEntityCacheManager().getEntityCache(resultClass);

		DescriptionField[] descriptionFields = entityCache.getPrimaryKeyFields();

		if (ReflectionUtils.isExtendsClass(String.class, descriptionFields[0].getField().getType())) {
			StringPath predicateField = entityPath.createString(descriptionFields[0].getName());
			return query.list(predicateField);
		} else if (ReflectionUtils.isExtendsClass(Number.class, descriptionFields[0].getField().getType())) {
			if (ReflectionUtils.isExtendsClass(Double.class, descriptionFields[0].getField().getType())) {
				NumberPath<Double> predicateField = entityPath.createNumber(descriptionFields[0].getName(),
						Double.class);
				return query.list(predicateField);
			} else if (ReflectionUtils.isExtendsClass(Float.class, descriptionFields[0].getField().getType())) {
				NumberPath<Float> predicateField = entityPath.createNumber(descriptionFields[0].getName(), Float.class);
				return query.list(predicateField);
			} else if (ReflectionUtils.isExtendsClass(BigDecimal.class, descriptionFields[0].getField().getType())) {
				NumberPath<BigDecimal> predicateField = entityPath.createNumber(descriptionFields[0].getName(),
						BigDecimal.class);
				return query.list(predicateField);
			} else if (ReflectionUtils.isExtendsClass(BigInteger.class, descriptionFields[0].getField().getType())) {
				NumberPath<BigInteger> predicateField = entityPath.createNumber(descriptionFields[0].getName(),
						BigInteger.class);
				return query.list(predicateField);
			} else {
				NumberPath<Long> predicateField = entityPath.createNumber(descriptionFields[0].getName(), Long.class);
				return query.list(predicateField);
			}
		} else if (ReflectionUtils.isExtendsClass(Date.class, descriptionFields[0].getField().getType())) {
			StringPath fieldString = entityPath.createString(descriptionFields[0].getName());
			return query.list(fieldString);
		} else if (ReflectionUtils.isExtendsClass(Date.class, descriptionFields[0].getField().getType())) {
			if (descriptionFields[0].isTemporalDate()) {
				DatePath<Date> predicateField = entityPath.createDate(descriptionFields[0].getName(), Date.class);
				return query.list(predicateField);
			}
		} else if (descriptionFields[0].isTemporalDateTime()) {
			DateTimePath<Date> predicateField = entityPath.createDateTime(descriptionFields[0].getName(), Date.class);
			return query.list(predicateField);
		} else if (descriptionFields[0].isTemporalTime()) {
			TimePath<Date> predicateField = entityPath.createTime(descriptionFields[0].getName(), Date.class);
			return query.list(predicateField);
		}
		return null;

	}

	public Long buildAndGetTotal() {
		buildQuery();
		return query.count();
	}

	public Page<T> buildAndGetPage() {
		if (page == null) {
			throw new SQLSessionException("Paginação não foi informada.");
		}
		buildQuery();
		long total = query.count();

		buildQuery();
		if (page != null) {
			query.limit(page.getPageSize()).offset(page.getOffset());
		}
		List<T> result = query.list(entityPath);
		return new PageImpl(result, page, total);
	}

	private DescriptionField getDescriptionField(EntityCache[] entityCaches, String field) {
		for (EntityCache entityCache : entityCaches) {
			DescriptionField descriptionField = entityCache.getDescriptionField(field);
			if (descriptionField != null) {
				return descriptionField;
			}
		}
		return null;
	}

	public static int isListValues(String value) {
		String[] split = StringUtils.split(value, ";");
		return split.length;
	}

	public static boolean isDate(String inDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		try {
			dateFormat.parse(inDate.trim());
			return true;
		} catch (ParseException pe) {
			dateFormat = new SimpleDateFormat("dd-MM-yyyy");
			try {
				dateFormat.parse(inDate.trim());
				return true;
			} catch (ParseException e) {
				return false;
			}
		}
	}

	public static boolean isBoolean(String value) {
		return "true".equals(value) || "false".equals(value) || "sim".equalsIgnoreCase(value)
				|| "não".equalsIgnoreCase(value) || "S".equalsIgnoreCase(value) || "N".equalsIgnoreCase(value);
	}

	public static Date getDate(String inDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		try {
			return dateFormat.parse(inDate.trim());
		} catch (ParseException pe) {
			dateFormat = new SimpleDateFormat("dd-MM-yyyy");
			try {
				return dateFormat.parse(inDate.trim());
			} catch (ParseException e) {
				return null;
			}
		}
	}

	public static boolean isDateTime(String inDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:ms");
		try {
			dateFormat.parse(inDate.trim());
			return true;
		} catch (ParseException pe) {
			dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:ms");
			try {
				dateFormat.parse(inDate.trim());
				return true;
			} catch (ParseException e) {
				return false;
			}
		}
	}

	public static Date getDateTime(String inDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:ms");
		dateFormat.setLenient(false);
		try {
			return dateFormat.parse(inDate.trim());
		} catch (ParseException pe) {
			dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:ms");
			dateFormat.setLenient(false);
			try {
				return dateFormat.parse(inDate.trim());
			} catch (ParseException e) {
				return null;
			}
		}
	}

	public AnterosMultipleFieldsFilter<T> fieldsToForceLazy(String forceLazy) {
		this.fieldsToForceLazy = forceLazy;
		return this;
	}

	public AnterosMultipleFieldsFilter<T> readOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

}
