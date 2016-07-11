package br.com.anteros.persistence.session.query.filter;

public class AndExpression extends GroupExpression {

	public AndExpression() {

	}
	
	@Override
	public Operator getOperator() {
		return Operator.AND;
	}

	public AndExpression(final FilterExpression... filterExpressions) {
		super(filterExpressions);
	}	

	public void setOperator(Operator operator) {

	}
}
