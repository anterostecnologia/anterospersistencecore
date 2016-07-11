package br.com.anteros.persistence.session.query.filter;

public interface Visitable {
  public void accept(final FilterVisitor visitor) throws FilterException;
}
