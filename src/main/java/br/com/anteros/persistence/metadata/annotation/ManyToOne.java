package br.com.anteros.persistence.metadata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import br.com.anteros.persistence.metadata.annotation.type.CascadeType;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;

@Target({ FIELD })
@Retention(RUNTIME)
public @interface ManyToOne {

	/**
	 * (Opcional) As operações podem ser cascateadas para o alvo da associação.
	 *
	 * <p>
	 * Por padrão as operações não executadas em cascata.
	 */
	CascadeType[] cascade() default {};

	/**
	 * (Opcional) Indica se a associação deve carregada imediatamente ao buscar o pai ou posteriormente quando o usuário acessá-la. 
	 */
	FetchType fetch() default FetchType.EAGER;

	/**
	 * (Opcional) Se a associação é opcional. Se definido como false, então, o relacionamento não pode ser nulo.
	 */
	boolean optional() default true;
}
