package br.com.anteros.persistence.session.query.filter;

import java.util.Arrays;
import java.util.List;

public abstract class GroupExpression extends FilterExpression {
  protected final List<FilterExpression> expressions;

  GroupExpression(final FilterExpression... filterExpressions) {
    super();
    this.expressions = Arrays.asList(filterExpressions);
  }

  @Override
  public FilterExpression applyOperation(final Operator operator, final Object value)
      throws FilterException {
    final int lastIndex = expressions.size() - 1;
    final FilterExpression lastClause = expressions.get(lastIndex);
    expressions.set(lastIndex, lastClause.applyOperation(operator, value));
    return this;
  }

  @Override
  public FilterExpression applyBetweenOperation(final Object valueStart,
      final Object valueEnd) throws FilterException {
    final int lastIndex = expressions.size() - 1;
    final FilterExpression lastClause = expressions.get(lastIndex);
    expressions.set(lastIndex, lastClause.applyBetweenOperation(valueStart,
        valueEnd));
    return this;
  }

  @Override
  public FilterExpression applyInOperation(final Object... values)
      throws FilterException {
    final int lastIndex = expressions.size() - 1;
    final FilterExpression lastClause = expressions.get(lastIndex);
    expressions.set(lastIndex, lastClause.applyInOperation(values));
    return this;
  }

  @Override
  public FilterExpression applyNotInOperation(final Object... values)
      throws FilterException {
    final int lastIndex = expressions.size() - 1;
    final FilterExpression lastClause = expressions.get(lastIndex);
    expressions.set(lastIndex, lastClause.applyNotInOperation(values));
    return this;
  }

  public void accept(final FilterVisitor visitor) throws FilterException {
    visitor.visit(this);
  }

  public abstract Operator getOperator();

  public List<FilterExpression> getExpressions() {
    return expressions;
  }
}
