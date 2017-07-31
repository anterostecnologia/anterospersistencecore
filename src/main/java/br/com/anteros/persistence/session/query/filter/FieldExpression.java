package br.com.anteros.persistence.session.query.filter;

public class FieldExpression extends JacksonBase implements Visitable {
	
	private String name;
	private String nameSql;
	
	public FieldExpression(){
		
	}

	public FieldExpression(String name) {
		this.name = name;
	}
	
	public FieldExpression(String name, String nameSql) {
		this.name = name;
		this.nameSql = nameSql;
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

	public String getNameSql() {
		if (nameSql==null || nameSql=="")
			return name;
		
		return nameSql;
	}

	public void setNameSql(String nameSql) {
		this.nameSql = nameSql;
	}

}
