package br.com.anteros.persistence.session.query.filter;

public class OperationExpression extends FilterExpression {
  private FieldExpression lhsValue;
  private Object rhsValue;
  private Operator operator;
  
  public OperationExpression(){
	  
  }

  public OperationExpression(FieldExpression lhsValue) {
    super();
    this.lhsValue = lhsValue;
    this.rhsValue = null;
    this.operator = null;
  }

  public OperationExpression(FieldExpression lhsValue, Operator operator,
      final Object rhsValue) {
    super();
    this.lhsValue = lhsValue;
    this.rhsValue = rhsValue;
    this.operator = operator;
  }

  @Override
  public FilterExpression applyOperation(Operator newOperator, Object newRhsValue)
      throws FilterException {
    if (operator != null) {
      throw new FilterException("Cannot apply " + newOperator.getValue()
          + " operation on an " + operator.getValue() + " expression.");
    }
    return new OperationExpression(lhsValue, newOperator, newRhsValue);
  }

  @Override
  public FilterExpression applyInOperation(final Object... values)
      throws FilterException {
    if (!(lhsValue instanceof FieldExpression))
      throw new FilterException(
          "Can only apply 'in' operation on a Column");
    return new InExpression((FieldExpression) lhsValue, false, values);
  }

  @Override
  public FilterExpression applyNotInOperation(final Object... values)
      throws FilterException {
    if (!(lhsValue instanceof FieldExpression))
      throw new FilterException(
          "Can only apply 'in' operation on a Column");
    return new InExpression((FieldExpression) lhsValue, true, values);
  }

  @Override
  public FilterExpression applyBetweenOperation(final Object valueStart,
      final Object valueEnd) throws FilterException {
    if (!(lhsValue instanceof FieldExpression))
      throw new FilterException(
          "Can only apply 'between' operation on a Column");
    return new BetweenExpression((FieldExpression) lhsValue, valueStart, valueEnd);
  }

  public void accept(final FilterVisitor visitor) throws FilterException {
    visitor.visit(this);
  }

  public FieldExpression getLhsValue() {
    return lhsValue;
  }

  public Operator getOperator() {
    return operator;
  }

  public Object getRhsValue() {
    return rhsValue;
  }
}
