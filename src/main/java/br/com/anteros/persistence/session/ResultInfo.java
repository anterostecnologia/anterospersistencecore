package br.com.anteros.persistence.session;

public class ResultInfo {
	
	String sharedLink;
	Long fileSize;
	String fileName;
	
	private ResultInfo(String url, Long fileSize, String fileName) {
		this.sharedLink = url;
		this.fileSize = fileSize;
		this.fileName = fileName;
	}
	
	public String getSharedLink() {
		return sharedLink;
	}
	public void setSharedLink(String sharedLink) {
		this.sharedLink = sharedLink;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
	public static ResultInfo of(String url, Long fileSize, String fileName) {
		return new ResultInfo(url, fileSize, fileName);
	}
	

}
