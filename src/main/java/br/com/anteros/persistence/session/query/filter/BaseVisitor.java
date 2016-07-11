package br.com.anteros.persistence.session.query.filter;

public abstract class BaseVisitor implements FilterVisitor {
  public void acceptOrVisitValue(final Object param) throws FilterException {
    if (param instanceof Visitable)
      ((Visitable) param).accept(this);
    else
      visitValue(param);
  }
}
