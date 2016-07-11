package br.com.anteros.persistence.session.query.filter;

public enum Constant implements Visitable {
  NULL("NULL"), STAR("*");

  private final String value;

  Constant(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void accept(final FilterVisitor visitor) {
    visitor.visit(this);
  }
}
