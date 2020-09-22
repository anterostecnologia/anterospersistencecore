package br.com.anteros.persistence.sql.command;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import br.com.anteros.cloud.integration.filesharing.CloudResultInfo;
import br.com.anteros.core.utils.IOUtils;
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
		
		File _file = new File(File.separator+"tmp"+File.separator+UUID.randomUUID().toString()+".tmp");
		
		FileOutputStream fos = new FileOutputStream(_file);				
		_file.setReadable(true);
		_file.setWritable(true);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(content);
	
		IOUtils.copyLarge(bais, fos);
		fos.flush();
		fos.close();
		bais.close();

		CloudResultInfo resultInfo = session.getExternalFileManager().uploadAndShareFile(folderName, fileName, _file, mimeType);
		_file.delete();
	
		return new CommandReturn(resultInfo.getSharedLink(), null);
	}

	@Override
	public void setEntityManaged() throws Exception {

	}

}
