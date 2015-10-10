package br.com.anteros.persistence.sql.command;

public class CommandSQLReturn {
	
	private String sql;
	private Object[] parameters;
	
	public CommandSQLReturn(String sql, Object[] parameters){
		this.sql = sql;
		this.parameters = parameters;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

}
