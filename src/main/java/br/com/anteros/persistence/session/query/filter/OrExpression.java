package br.com.anteros.persistence.session.query.filter;

public class OrExpression extends GroupExpression {

	public OrExpression() {

	}
	
	@Override
	public Operator getOperator() {
		return Operator.OR;
	}
	
	public OrExpression(final FilterExpression... filterExpressions) {
		super(filterExpressions);
	}
	
	public void setOperator(Operator operator){
		
	}
}
