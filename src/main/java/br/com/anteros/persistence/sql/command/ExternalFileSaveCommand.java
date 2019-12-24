package br.com.anteros.persistence.sql.command;

import br.com.anteros.persistence.session.ResultInfo;
import br.com.anteros.persistence.session.SQLSession;

public class ExternalFileSaveCommand implements PersisterCommand {
	
	private SQLSession session;
	private String folderName;
	private String fileName;
	private byte[] content;

	public ExternalFileSaveCommand(SQLSession session, String folderName, String fileName, byte[] content) {
		this.session = session;
		this.folderName = folderName;
		this.fileName = fileName;
		this.content = content;
	}	

	@Override
	public CommandReturn execute() throws Exception {
		ResultInfo resultInfo = session.getExternalFileManager().saveFile(folderName,
				fileName, content);		
		String sharedLink = resultInfo.getSharedLink()+"/preview#"+fileName;
		return new CommandReturn(sharedLink, null);
	}

	@Override
	public void setEntityManaged() throws Exception {
		
	}

}
