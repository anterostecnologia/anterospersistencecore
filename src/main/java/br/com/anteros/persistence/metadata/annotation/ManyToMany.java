package br.com.anteros.persistence.metadata.annotation;

import br.com.anteros.persistence.metadata.annotation.type.CascadeType;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;

public @interface ManyToMany {

	Class<?> targetEntity() default void.class;

	CascadeType[] cascade() default {};

	FetchType fetch() default FetchType.LAZY;

	String mappedBy() default "";

}
