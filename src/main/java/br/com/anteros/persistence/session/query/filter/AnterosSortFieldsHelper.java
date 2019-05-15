package br.com.anteros.persistence.session.query.filter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.com.anteros.core.utils.ArrayUtils;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.dsl.osql.DynamicEntityPath;
import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.path.DatePath;
import br.com.anteros.persistence.dsl.osql.types.path.DateTimePath;
import br.com.anteros.persistence.dsl.osql.types.path.NumberPath;
import br.com.anteros.persistence.dsl.osql.types.path.StringPath;
import br.com.anteros.persistence.dsl.osql.types.path.TimePath;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.exception.SQLSessionException;

public class AnterosSortFieldsHelper {

	public static List<OrderSpecifier> convertFieldsToOrderby(SQLSession session, DynamicEntityPath entityPath, EntityCache[] entityCaches,
			String fieldsSort) {
		List<OrderSpecifier> result = new ArrayList<OrderSpecifier>();
		
		String[] arrFieldsSort = StringUtils.tokenizeToStringArray(fieldsSort, ",");
		if (arrFieldsSort == null) {
			arrFieldsSort = new String[] { fieldsSort };
		}

		for (String field : arrFieldsSort) {
			String[] fieldArr = StringUtils.tokenizeToStringArray(field, ".");
			processSortField(session, entityPath, entityCaches, fieldArr, result);

		}
		
		return result;
	}

	private static void processSortField(SQLSession session, DynamicEntityPath dynamicEntityPath, EntityCache[] entityCaches, String[] fieldArr, List<OrderSpecifier> result) {
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
				processSortField(session, relationShipEntityPath, newEntityCaches, restFields, result);
			} else {
				addSort(dynamicEntityPath, descriptionField, order, result);
			}

		} else if (descriptionField.isSimple()) {
			addSort(dynamicEntityPath, descriptionField, order, result);
		} else if (descriptionField.isAnyCollection()) {
			throw new SQLSessionException("Campo " + field
					+ " é uma coleção. Ainda não é permitido fazer buscas em coleções até esta versão.");
		}

	}
	
	private static DescriptionField getDescriptionField(EntityCache[] entityCaches, String field) {
		for (EntityCache entityCache : entityCaches) {
			DescriptionField descriptionField = entityCache.getDescriptionField(field);
			if (descriptionField != null) {
				return descriptionField;
			}
		}
		return null;
	}
	
	
	private static void addSort(DynamicEntityPath entityPath, DescriptionField descriptionField, String order, List<OrderSpecifier> result) {
		if (descriptionField.isSimple()) {
			if (ReflectionUtils.isExtendsClass(String.class, descriptionField.getField().getType())) {
				StringPath predicateField = entityPath.createFieldString(descriptionField.getName());
				if ("asc".equals(order)) {
					result.add(predicateField.asc());
				} else {
					result.add(predicateField.desc());
				}
			} else if (ReflectionUtils.isExtendsClass(Number.class, descriptionField.getField().getType())) {
				if (ReflectionUtils.isExtendsClass(Double.class, descriptionField.getField().getType())) {
					NumberPath<Double> predicateField = entityPath.createFieldNumber(descriptionField.getName(),
							Double.class);
					if ("asc".equals(order)) {
						result.add(predicateField.asc());
					} else {
						result.add(predicateField.desc());
					}
				} else if (ReflectionUtils.isExtendsClass(Float.class, descriptionField.getField().getType())) {
					NumberPath<Float> predicateField = entityPath.createFieldNumber(descriptionField.getName(),
							Float.class);
					if ("asc".equals(order)) {
						result.add(predicateField.asc());
					} else {
						result.add(predicateField.desc());
					}
				} else if (ReflectionUtils.isExtendsClass(BigDecimal.class,
						descriptionField.getField().getType())) {
					NumberPath<BigDecimal> predicateField = entityPath
							.createFieldNumber(descriptionField.getName(), BigDecimal.class);
					if ("asc".equals(order)) {
						result.add(predicateField.asc());
					} else {
						result.add(predicateField.desc());
					}
				} else if (ReflectionUtils.isExtendsClass(BigInteger.class,
						descriptionField.getField().getType())) {
					NumberPath<BigInteger> predicateField = entityPath
							.createFieldNumber(descriptionField.getName(), BigInteger.class);
					if ("asc".equals(order)) {
						result.add(predicateField.asc());
					} else {
						result.add(predicateField.desc());
					}
				} else {
					NumberPath<Long> predicateField = entityPath.createFieldNumber(descriptionField.getName(),
							Long.class);
					if ("asc".equals(order)) {
						result.add(predicateField.asc());
					} else {
						result.add(predicateField.desc());
					}
				}
			} else if (ReflectionUtils.isExtendsClass(Date.class, descriptionField.getField().getType())) {
				if (descriptionField.isTemporalDate()) {
					DatePath<Date> predicateField = entityPath.createFieldDate(descriptionField.getName(),
							Date.class);
					if ("asc".equals(order)) {
						result.add(predicateField.asc());
					} else {
						result.add(predicateField.desc());
					}

				}
			} else if (descriptionField.isTemporalDateTime()) {
				DateTimePath<Date> predicateField = entityPath.createFieldDateTime(descriptionField.getName(),
						Date.class);
				if ("asc".equals(order)) {
					result.add(predicateField.asc());
				} else {
					result.add(predicateField.desc());
				}

			} else if (descriptionField.isTemporalTime()) {
				TimePath<Date> predicateField = entityPath.createFieldTime(descriptionField.getName(),
						Date.class);
				if ("asc".equals(order)) {
					result.add(predicateField.asc());
				} else {
					result.add(predicateField.desc());
				}

			}
		}
	}

}
