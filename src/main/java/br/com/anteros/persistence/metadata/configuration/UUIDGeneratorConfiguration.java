package br.com.anteros.persistence.metadata.configuration;

import br.com.anteros.persistence.metadata.annotation.UUIDGenerator;

public class UUIDGeneratorConfiguration {
	
	private Class<?> uuidClassGenerator;
	
	public UUIDGeneratorConfiguration(UUIDGenerator uuidGenerator){
		this.uuidClassGenerator = uuidGenerator.value();
	}
	
	public UUIDGeneratorConfiguration uuidClassGenerator(Class<?> uuidClassGenerator){
		this.uuidClassGenerator = uuidClassGenerator;
		return this;
	}
	
	public Class<?> getUUIDClassGenerator(){
		return uuidClassGenerator;
	}

}
