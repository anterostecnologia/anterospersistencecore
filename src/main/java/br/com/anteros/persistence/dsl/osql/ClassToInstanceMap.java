package br.com.anteros.persistence.dsl.osql;

import java.util.Map;

public interface ClassToInstanceMap<B> extends Map<Class<? extends B>, B> {
	  <T extends B> T getInstance(Class<T> type);

	  <T extends B> T putInstance(Class<T> type, T value);
	}

