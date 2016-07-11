package br.com.anteros.persistence.session.query.filter;

public class Nullable implements Visitable {
  private final Object value;

  public Nullable(final Object value) {
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  public boolean isNull() {
    return value == null;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null)
      return false;
    if (!(obj instanceof Nullable))
      return false;

    final Nullable otherObj = (Nullable) obj;
    if (value == null)
      return otherObj.getValue() == null;
    else
      return value.equals(otherObj.getValue());
  }

  public void accept(final FilterVisitor visitor) throws FilterException {
    visitor.visit(this);
  }
}
