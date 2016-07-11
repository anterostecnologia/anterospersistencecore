package br.com.anteros.persistence.session.query.filter;

public interface FilterVisitor {
  public void visit(FieldExpression field);

  public void visit(OperationExpression operationExpression) throws FilterException;

  public void visit(BetweenExpression betweenExpression) throws FilterException;

  public void visitValue(Object value);

  public void visit(GroupExpression groupExpression) throws FilterException;

  public void visit(InExpression inExpression) throws FilterException;

  public void visit(Constant constant);

  public void visit(Filter filter) throws FilterException;

  public void visit(Nullable nullable) throws FilterException;
}
