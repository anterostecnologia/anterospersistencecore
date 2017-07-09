package br.com.anteros.persistence.session.query.filter;

public class AnterosFilterDsl {

	private static Class<? extends DefaultFilterBuilder> filterbuilderClass = DefaultFilterBuilder.class;

	public static void setDefaultQueryBuilder(final Class<? extends DefaultFilterBuilder> queryBuilderClass) {
		filterbuilderClass = queryBuilderClass;
	}

	public static void resetDefaultQueryBuilder() {
		filterbuilderClass = DefaultFilterBuilder.class;
	}

	public static DefaultFilterBuilder getFilterBuilder() {
		try {
			return filterbuilderClass.newInstance();
		} catch (final Exception e) {
		}
		return null;
	}

	public static Filter createFilter() {
		return new Filter();
	}

}