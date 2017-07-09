package br.com.anteros.persistence.session.query.filter;

public enum Operator {
  IS("IS"), BETWEEN("BETWEEN"), LIKE("LIKE"), EQ("="), NEQ("!="),GT(">"), LT("<"), GEQ(">="), LEQ(
      "<="), NOT("NOT"), IN("IN"), IS_NOT("IS NOT"), AND("AND"), OR("OR");

  private final String value;

  Operator(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
