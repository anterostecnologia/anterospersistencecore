package br.com.anteros.persistence.sql.command;

import br.com.anteros.persistence.session.ResultInfo;
import br.com.anteros.persistence.session.SQLSession;

public class ExternalFileSaveCommand implements PersisterCommand {

	private SQLSession session;
	private String folderName;
	private String fileName;
	private byte[] content;
	private String mimeType;

	public ExternalFileSaveCommand(SQLSession session, String folderName, String fileName, byte[] content,
			String mimeType) {
		this.session = session;
		this.folderName = folderName;
		this.fileName = fileName;
		this.content = content;
		this.mimeType = mimeType;
	}

	@Override
	public CommandReturn execute() throws Exception {
		ResultInfo resultInfo = session.getExternalFileManager().saveFile(folderName, fileName, content, mimeType);
		return new CommandReturn(resultInfo.getSharedLink(), null);
	}

	@Override
	public void setEntityManaged() throws Exception {

	}

}
