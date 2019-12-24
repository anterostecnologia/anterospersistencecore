package br.com.anteros.persistence.sql.command;

import br.com.anteros.persistence.session.SQLSession;

public class ExternalFileRemoveCommand implements PersisterCommand {
	
	private SQLSession session;
	private String folderName;
	private String fileName;

	public ExternalFileRemoveCommand(SQLSession session, String folderName, String fileName) {
		this.session = session;
		this.folderName = folderName;
		this.fileName = fileName;
	}
	
	@Override
	public CommandReturn execute() throws Exception {
		session.getExternalFileManager().removeFile(folderName, fileName);		
		return null;
	}

	@Override
	public void setEntityManaged() throws Exception {
		
	}

}
