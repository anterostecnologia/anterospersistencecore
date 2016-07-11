package br.com.anteros.persistence.session.query.filter;

public class InExpression extends FilterExpression {
	private FieldExpression field;
	private Object[] values;
	private boolean negative;

	public InExpression() {
		super();
	}

	public InExpression(final FieldExpression field, final boolean negative, final Object... values) {
		super();
		this.field = field;
		this.values = values;
		this.negative = negative;
	}

	@Override
	public FilterExpression applyInOperation(final Object... newValues) throws FilterException {
		throw new FilterException("Cannot apply IN operation on an IN expression.");
	}

	@Override
	public FilterExpression applyNotInOperation(final Object... values) throws FilterException {
		throw new FilterException("Cannot apply NOT IN operation on an IN expression.");
	}

	@Override
	public FilterExpression applyOperation(final Operator operator, final Object value) throws FilterException {
		throw new FilterException("Cannot apply " + operator + " operation on an IN expression.");
	}

	@Override
	public FilterExpression applyBetweenOperation(final Object valueStart, final Object valueEnd)
			throws FilterException {
		throw new FilterException("Cannot apply IN on a BETWEEN expression.");
	}

	public void accept(final FilterVisitor visitor) throws FilterException {
		visitor.visit(this);
	}

	public FieldExpression getField() {
		return field;
	}

	public Object[] getValues() {
		return values;
	}

	public boolean isNegative() {
		return negative;
	}
}
