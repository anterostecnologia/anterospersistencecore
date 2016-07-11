package br.com.anteros.persistence.session.query.filter;

import br.com.anteros.core.utils.StringUtils;

public class DefaultVisitor extends BaseVisitor {

  public void visitValue(final Object value) {
  }

  public void visit(final FieldExpression column) {
  }

  public void visit(final OperationExpression exp) throws FilterException {
    acceptOrVisitValue(exp.getLhsValue());
    acceptOrVisitValue(exp.getRhsValue());
  }

  public void visit(final BetweenExpression betweenExp) throws FilterException {
    betweenExp.getField().accept(this);
    acceptOrVisitValue(betweenExp.getValueStart());
    acceptOrVisitValue(betweenExp.getValueEnd());
  }

  public void visit(final GroupExpression expSeq) throws FilterException {
    for (final FilterExpression clause : expSeq.getExpressions())
      clause.accept(this);
  }

  public void visit(final InExpression inExp) throws FilterException {
    inExp.getField().accept(this);
    if (!StringUtils.isEmpty(inExp.getValues()))
      for (final Object value : inExp.getValues())
        acceptOrVisitValue(value);
  }

  public void visit(final Constant constant) {
  }

  public void visit(final Filter query) throws FilterException {
    final FilterExpression whereClause = query.getFilterExpression();
    if (whereClause != null)
      whereClause.accept(this);
  }

  public void visit(final Nullable nullable) throws FilterException {
    acceptOrVisitValue(nullable.getValue());
  }
}
