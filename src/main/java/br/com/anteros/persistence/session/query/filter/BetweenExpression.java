package br.com.anteros.persistence.session.query.filter;

public class BetweenExpression extends FilterExpression {
	private FieldExpression field;
	private Object valueStart;
	private Object valueEnd;

	public BetweenExpression() {
		super();
	}

	public BetweenExpression(final FieldExpression field, final Object valueStart, final Object valueEnd) {
		super();
		this.field = field;
		this.valueStart = valueStart;
		this.valueEnd = valueEnd;
	}

	@Override
	public FilterExpression applyInOperation(final Object... newValues) throws FilterException {
		throw new FilterException("Cannot apply IN operation on an BETWEEN expression.");
	}

	@Override
	public FilterExpression applyNotInOperation(final Object... values) throws FilterException {
		throw new FilterException("Cannot apply NOT IN operation on an BETWEEN expression.");
	}

	@Override
	public FilterExpression applyOperation(final Operator operator, final Object value) throws FilterException {
		throw new FilterException("Cannot apply " + operator + " operation on an BETWEEN expression.");
	}

	@Override
	public FilterExpression applyBetweenOperation(final Object start, final Object end) throws FilterException {
		throw new FilterException("Cannot apply BETWEEN on a BETWEEN expression.");
	}

	public void accept(final FilterVisitor visitor) throws FilterException {
		visitor.visit(this);
	}

	public FieldExpression getField() {
		return field;
	}

	public Object getValueStart() {
		return valueStart;
	}

	public Object getValueEnd() {
		return valueEnd;
	}
}
