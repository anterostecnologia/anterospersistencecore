package br.com.anteros.persistence.session.query.filter;

import java.util.Map;

public class AnterosFilterDsl {

	private static Class<? extends DefaultFilterBuilder> filterbuilderClass = DefaultFilterBuilder.class;

	private static DefaultFilterBuilder builder = AnterosFilterDsl.getFilterBuilder();

	public static void setDefaultQueryBuilder(final Class<? extends DefaultFilterBuilder> queryBuilderClass) {
		filterbuilderClass = queryBuilderClass;
	}

	public static void resetDefaultQueryBuilder() {
		filterbuilderClass = DefaultFilterBuilder.class;
	}

	
	public static String toSql(Filter filter) throws FilterException {
		filter.runVisitors();
		filter.accept(builder);
		final String sqlQuery = builder.getResult().toString();
		return sqlQuery;
	}

	public static DefaultFilterBuilder getFilterBuilder() {
		try {
			return filterbuilderClass.newInstance();
		} catch (final Exception e) {
		}
		return null;
	}


	public Map<String, Object> getParams() {
		return builder.getParams();
	}

	public static Filter createFilter() {
		return new Filter();
	}
	

}