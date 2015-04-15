package br.com.anteros.persistence.session.configuration;

import java.io.InputStream;

import br.com.anteros.persistence.session.configuration.exception.AnterosConfigurationException;

public interface BasicConfiguration {

	public BasicConfiguration configure() throws AnterosConfigurationException;

	public BasicConfiguration configure(String xmlFile) throws AnterosConfigurationException;
	
	public BasicConfiguration configure(InputStream is) throws AnterosConfigurationException;
	
}
