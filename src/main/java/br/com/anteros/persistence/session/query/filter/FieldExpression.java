package br.com.anteros.persistence.session.query.filter;

public class FieldExpression extends JacksonBase implements Visitable {
	
	private String name;
	
	public FieldExpression(){
		
	}

	public FieldExpression(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FieldExpression))
			return false;

		final FieldExpression otherObj = (FieldExpression) obj;
		if (name == null)
			return otherObj.getName() == null;
		else
			return name.equals(otherObj.getName());
	}

	@Override
	public String toString() {
		return name;
	}

	public void accept(final FilterVisitor visitor) {
		visitor.visit(this);
	}

	public void setName(String name) {
		this.name = name;
	}

}
