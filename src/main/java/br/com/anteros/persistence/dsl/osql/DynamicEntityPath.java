package br.com.anteros.persistence.dsl.osql;

import static br.com.anteros.persistence.dsl.osql.types.PathMetadataFactory.forVariable;

import br.com.anteros.persistence.dsl.osql.types.PathMetadata;
import br.com.anteros.persistence.dsl.osql.types.path.ArrayPath;
import br.com.anteros.persistence.dsl.osql.types.path.DatePath;
import br.com.anteros.persistence.dsl.osql.types.path.DateTimePath;
import br.com.anteros.persistence.dsl.osql.types.path.EntityPathBase;
import br.com.anteros.persistence.dsl.osql.types.path.NumberPath;
import br.com.anteros.persistence.dsl.osql.types.path.PathInits;
import br.com.anteros.persistence.dsl.osql.types.path.StringPath;
import br.com.anteros.persistence.dsl.osql.types.path.TimePath;

public class DynamicEntityPath extends EntityPathBase {

	private static final long serialVersionUID = 1923255001L;

	private static final PathInits INITS = PathInits.DIRECT2;


	public DynamicEntityPath(Class<?> resultClass, String variable) {
		this(resultClass, forVariable(variable), INITS);
	}

	public DynamicEntityPath(Class<?> resultClass, PathMetadata<?> metadata, PathInits inits) {
		super(resultClass, metadata, inits);
	}

	public <A extends Number & Comparable<?>> NumberPath<A> createFieldNumber(String property, Class<? super A> type) {
		return super.createNumber(property, type);
	}
	
	public StringPath createFieldString(String property) {
		return super.createString(property);
	}
	
	public <A extends Comparable> DatePath<A> createFieldDate(String property, Class<? super A> type) {
		return super.createDate(property, type);
	}
	
	public <A extends Comparable> DateTimePath<A> createFieldDateTime(String property, Class<? super A> type) {
		return super.createDateTime(property, type);
	}
	
	public <A, E> ArrayPath<A, E> createFieldArray(String property, Class<? super A> type) {
		return super.createArray(property, type);
	}
	
	public <A extends Comparable> TimePath<A> createFieldTime(String property, Class<? super A> type) {
		return super.createTime(property, type);
	}
	
}