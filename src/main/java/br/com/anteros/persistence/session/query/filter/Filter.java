package br.com.anteros.persistence.session.query.filter;

import java.util.HashSet;
import java.util.Set;

public class Filter implements Visitable {

	protected FilterExpression filterExpression;


	private static Set<FilterVisitor> visitors = new HashSet<FilterVisitor>();

	public FilterExpression getFilterExpression() {
		return filterExpression;
	}

	public static FieldExpression field(final String name) {
		return new FieldExpression(name);
	}

	public static OperationExpression exp(final FieldExpression field) {
		return new OperationExpression(field);
	}	

	public void runVisitors() throws FilterException {
		for (final FilterVisitor visitor : visitors) {
			accept(visitor);
		}
	}

	public static void addVisitor(final FilterVisitor visitor) {
		visitors.add(visitor);
	}

	public static void clearVisitors() {
		visitors.clear();
	}

	public Filter WHERE(final FieldExpression column) {
		filterExpression = new OperationExpression(column);
		return this;
	}

	public Filter WHERE(final FilterExpression newWhereClause) {
		filterExpression = newWhereClause;
		return this;
	}

	public Filter WHERE(final FieldExpression column, final Operator operator, final Object value) {
		if (value != null)
			filterExpression = new OperationExpression(column, operator, value);
		return this;
	}

	public Filter EQ(final Object value) throws FilterException {
		assertWhereClauseIsInitialized("eq");
		filterExpression = filterExpression.EQ(value);
		return this;
	}

	public Filter eqOrIsNull(final Object value) throws FilterException {
		assertWhereClauseIsInitialized("eq");
		filterExpression = filterExpression.EQ(new Nullable(value));
		return this;
	}

	public Filter NEQ(final Object value) throws FilterException {
		assertWhereClauseIsInitialized("neq");
		filterExpression = filterExpression.NEQ(value);
		return this;
	}

	public Filter neqNullable(final Object value) throws FilterException {
		assertWhereClauseIsInitialized("neq");
		filterExpression = filterExpression.NEQ(new Nullable(value));
		return this;
	}

	public Filter GEQ(final Object value) throws FilterException {
		assertWhereClauseIsInitialized("geq");
		filterExpression = filterExpression.GEQ(value);
		return this;
	}

	public Filter LEQ(final Object value) throws FilterException {
		assertWhereClauseIsInitialized("leq");
		filterExpression = filterExpression.LEQ(value);
		return this;
	}

	public Filter LIKE(final String value) throws FilterException {
		assertWhereClauseIsInitialized("like");
		filterExpression = filterExpression.LIKE(value);
		return this;
	}

	public Filter IN(final Object... values) throws FilterException {
		assertWhereClauseIsInitialized("in");
		filterExpression = filterExpression.IN(values);
		return this;
	}

	public Filter NOTIN(final Object... values) throws FilterException {
		assertWhereClauseIsInitialized("not in");
		filterExpression = filterExpression.NOTIN(values);
		return this;
	}

	public Filter AND(final FieldExpression column) throws FilterException {
		assertWhereClauseIsInitialized("and");
		filterExpression = filterExpression.AND(column);
		return this;
	}

	public Filter AND(final FilterExpression exp) throws FilterException {
		assertWhereClauseIsInitialized("and");
		filterExpression = filterExpression.AND(exp);
		return this;
	}
	
	public Filter OR(final FieldExpression fieldExpression) throws FilterException {
		assertWhereClauseIsInitialized("or");
		filterExpression = filterExpression.OR(fieldExpression);
		return this;
	}

	public Filter OR(final FilterExpression filterExpression) throws FilterException {
		assertWhereClauseIsInitialized("or");
		this.filterExpression = this.filterExpression.OR(filterExpression);
		return this;
	}

	public Filter ISNULL() throws FilterException {
		assertWhereClauseIsInitialized("isNull");
		filterExpression = filterExpression.ISNULL();
		return this;
	}

	public Filter ISNOTNULL() throws FilterException {
		assertWhereClauseIsInitialized("isNotNull");
		filterExpression = filterExpression.ISNOTNULL();
		return this;
	}

	public Filter betweenOrOp(final Operator op, final Object valueStart, final Object valueEnd)
			throws FilterException {
		assertWhereClauseIsInitialized(op == null ? null : op.getValue());
		filterExpression = filterExpression.betweenOrOp(op, valueStart, valueEnd);
		return this;
	}

	public Filter BETWEEN(final Object valueStart, final Object valueEnd) throws FilterException {
		assertWhereClauseIsInitialized("between");
		filterExpression = filterExpression.BETWEEN(valueStart, valueEnd);
		return this;
	}

	public Filter OP(final Operator op, final Object value) throws FilterException {
		assertWhereClauseIsInitialized(op == null ? null : op.getValue());
		filterExpression = filterExpression.applyOperation(op, value);
		return this;
	}

	public Filter STARTSWITH(final String value) throws FilterException {
		assertWhereClauseIsInitialized("startsWith");
		filterExpression = filterExpression.STARTSWITH(value);
		return this;
	}

	public Filter CONTAINS(final String value) throws FilterException {
		assertWhereClauseIsInitialized("contains");
		filterExpression = filterExpression.CONTAINS(value);
		return this;
	}

	private void assertWhereClauseIsInitialized(final String operation) throws FilterException {
		if (filterExpression == null)
			throw new FilterException("Cannot apply '" + operation + "' operator if no where clause exist");
	}

	public void accept(final FilterVisitor visitor) throws FilterException {
		visitor.visit(this);
	}

	public static Filter createFilter() {
		return new Filter();
	}

	public void setFilterExpression(FilterExpression filterExpression) {
		this.filterExpression = filterExpression;
	}
	

}
