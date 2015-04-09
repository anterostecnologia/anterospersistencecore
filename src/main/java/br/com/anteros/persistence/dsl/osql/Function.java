package br.com.anteros.persistence.dsl.osql;

public interface Function<F, T> {
	  T apply(F input);

	  @Override
	  boolean equals(Object object);
	}
