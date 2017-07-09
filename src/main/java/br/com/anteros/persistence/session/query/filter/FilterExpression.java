package br.com.anteros.persistence.session.query.filter;


import java.util.Collection;

import br.com.anteros.core.utils.StringUtils;



public abstract class FilterExpression extends JacksonBase implements Visitable {
	
  public AndExpression AND(final FieldExpression andField, final Operator andOperator,
      final Object andValue) {
    return new AndExpression(this, new OperationExpression(andField, andOperator, andValue));
  }

  public AndExpression AND(final FieldExpression andField) {
    return new AndExpression(this, new OperationExpression(andField));
  }

  public AndExpression AND(final FilterExpression filterExpression) {
    return new AndExpression(this, filterExpression);
  }

  public OrExpression OR(final FieldExpression orField, final Operator orOperator,
      final Object orValue, final boolean orValueIsFieldName) {
    return new OrExpression(this, new OperationExpression(orField, orOperator, orValue));
  }

  public OrExpression OR(final FieldExpression orField) {
    return new OrExpression(this, new OperationExpression(orField));
  }

  public OrExpression OR(final FilterExpression filterExpression) {
    return new OrExpression(this, filterExpression);
  }

  public FilterExpression EQ(final Object value) throws FilterException {
    return applyOperation(Operator.EQ, value);
  }

  public FilterExpression NEQ(final Object value) throws FilterException {
    return applyOperation(Operator.NEQ, value);
  }

  public FilterExpression GEQ(final Object value) throws FilterException {
    return applyOperation(Operator.GEQ, value);
  }

  public FilterExpression LEQ(final Object value) throws FilterException {
    return applyOperation(Operator.LEQ, value);
  }
  

  public FilterExpression GT(final Object value) throws FilterException {
    return applyOperation(Operator.GT, value);
  }

  public FilterExpression LT(final Object value) throws FilterException {
    return applyOperation(Operator.LT, value);
  }

  public FilterExpression LIKE(final String value) throws FilterException {
    return applyOperation(Operator.LIKE, value);
  }

  public FilterExpression BETWEEN(final Object valueStart, final Object valueEnd)
      throws FilterException {
    return applyBetweenOperation(valueStart, valueEnd);
  }

  public FilterExpression STARTSWITH(String value) throws FilterException {
	  boolean empty = StringUtils.isEmpty(value);
    value =  empty ? null : value.concat("%");
    return applyOperation(Operator.LIKE, value);
  }

  public FilterExpression CONTAINS(String value) throws FilterException {
	boolean empty = StringUtils.isEmpty(value);
    value = empty ? null : "%".concat(value.concat("%"));
    return applyOperation(Operator.LIKE, value);
  }

  public FilterExpression IN(final Object... values) throws FilterException {
    return applyInOperation(values);
  }

  public FilterExpression IN(final Collection<Object> values) throws FilterException {
    if (values == null) {
      return this;
    } else {
      return applyInOperation(values.toArray(new Object[values.size()]));
    }
  }

  public FilterExpression NOTIN(final Object... values) throws FilterException {
    return applyNotInOperation(values);
  }

  public FilterExpression NOTIN(final Collection<Object> values)
      throws FilterException {
    if (values == null) {
      return this;
    } else {
      return applyNotInOperation(values.toArray(new Object[values.size()]));
    }
  }

  public FilterExpression ISNULL() throws FilterException {
    return applyOperation(Operator.IS, Constant.NULL);
  }

  public FilterExpression ISNOTNULL() throws FilterException {
    return applyOperation(Operator.IS_NOT, Constant.NULL);
  }

  public FilterExpression betweenOrOp(final Operator op, final Object valueStart,
      final Object valueEnd) throws FilterException {
    if (Operator.BETWEEN.equals(op)) {
      return applyBetweenOperation(valueStart, valueEnd);
    } else {
      return applyOperation(op, valueStart);
    }
  }

  public abstract FilterExpression applyOperation(final Operator operator, final Object value)
      throws FilterException;

  public abstract FilterExpression applyInOperation(final Object... values)
      throws FilterException;

  public abstract FilterExpression applyNotInOperation(final Object... values)
      throws FilterException;

  public abstract FilterExpression applyBetweenOperation(final Object valueStart,
      final Object valueEnd) throws FilterException;
}
