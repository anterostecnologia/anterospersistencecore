package br.com.anteros.persistence.sql.command;

public interface PersisterCommand {
	
	public abstract CommandReturn execute() throws Exception;
	
	public void setEntityManaged() throws Exception;

}
