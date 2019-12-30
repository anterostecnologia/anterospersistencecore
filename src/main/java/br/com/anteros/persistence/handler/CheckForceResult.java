package br.com.anteros.persistence.handler;

import br.com.anteros.persistence.metadata.annotation.type.FetchType;

public class CheckForceResult {
	
	private FetchType fetchType;
	
	private String continueFieldsToForceLazy;	
	

	public CheckForceResult(FetchType fetchType, String continueFieldsToForceLazy) {
		super();
		this.fetchType = fetchType;
		this.continueFieldsToForceLazy = continueFieldsToForceLazy;
	}

	public FetchType getFetchType() {
		return fetchType;
	}

	public void setFetchType(FetchType fetchType) {
		this.fetchType = fetchType;
	}

	public String getContinueFieldsToForceLazy() {
		return continueFieldsToForceLazy;
	}

	public void setContinueFieldsToForceLazy(String continueFieldsToForceLazy) {
		this.continueFieldsToForceLazy = continueFieldsToForceLazy;
	}
	
	

}
