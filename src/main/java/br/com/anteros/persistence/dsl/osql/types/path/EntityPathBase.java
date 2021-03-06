/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.dsl.osql.types.path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.anteros.persistence.dsl.osql.ColumnMetadata;
import br.com.anteros.persistence.dsl.osql.DynamicEntityPath;
import br.com.anteros.persistence.dsl.osql.OSQLQueryException;
import br.com.anteros.persistence.dsl.osql.types.EntityPath;
import br.com.anteros.persistence.dsl.osql.types.Path;
import br.com.anteros.persistence.dsl.osql.types.PathMetadata;

/**
 * EntityPathBase provides a base class for EntityPath implementations
 *
 * @author tiwe modified by: Edson Martins
 *
 * @param <T>
 *            entity type
 */
public class EntityPathBase<T> extends BeanPath<T> implements EntityPath<T> {

	private static final long serialVersionUID = -8610055828414880996L;
	
	private static final PathInits INITS = PathInits.DIRECT2;

	private final Map<Path<?>, ColumnMetadata> columnMetadata = new LinkedHashMap<Path<?>, ColumnMetadata>();

	private final Set<Path<?>> customProjection = new LinkedHashSet<Path<?>>();
	private final Set<Path<?>> excludeProjection = new LinkedHashSet<Path<?>>();

	public EntityPathBase(Class<? extends T> type, String variable) {
		super(type, variable);
	}

	public EntityPathBase(Class<? extends T> type, PathMetadata<?> metadata) {
		super(type, metadata);
	}

	public EntityPathBase(Class<? extends T> type, PathMetadata<?> metadata, PathInits inits) {
		super(type, metadata, inits);
	}

	@Override
	public Object getMetadata(Path<?> property) {
		return columnMetadata.get(property);
	}

	protected <P extends Path<?>> P addMetadata(P path, ColumnMetadata metadata) {
		columnMetadata.put(path, metadata);
		return path;
	}

	public Path<?>[] all() {
		Path<?>[] all = new Path[columnMetadata.size()];
		columnMetadata.keySet().toArray(all);
		return all;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Path<?>> getCustomProjection() {
		return customProjection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Path<?>> getExcludeProjection() {
		return excludeProjection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityPath<T> customProjection(Path<?>... args) {
		for (Path<?> arg : args) {
			if (arg instanceof SetPath<?, ?>)
				throw new OSQLQueryException("Não possível fazer join de coleções diretamente. Use o método " + arg
						+ ".any() da coleção para projetá-la ou realizar operações. ");
			if (arg.equals(this))
				throw new OSQLQueryException("Não possível fazer criar uma projeção customizada pois foi detectado um loop. Use os campos da entidade para projeções customizadas para "+this);
			customProjection.add(arg);
		}
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityPath<T> customProjection(Object... args) {
		return customProjection(buildNewCustomProjectionArguments(args).toArray(new Path<?>[]{}));
	}

	protected List<Path<?>> buildNewCustomProjectionArguments(Object... args) {
		List<Path<?>> newArgs = new ArrayList<Path<?>>();
		for (Object arg : args){
			if (!((arg instanceof Path<?>) || (arg instanceof Path<?>[])))
					throw new OSQLQueryException("Para criar uma projeção customizada informe um ou mais caminhos no formato Path<?> ou Path<?>[]. Argumento inválido "+arg);
			if (arg instanceof Path<?>)
				newArgs.add((Path<?>) arg);
			else if (arg instanceof Path<?>[]){
				for (Path<?> arg2 : (Path<?>[])arg){
					newArgs.add(arg2);
				}
			}
		}
		return newArgs;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityPath<T> excludeProjection(Path<?>... args) {
		for (Path<?> arg : args) {
			excludeProjection.add(arg);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityPath<T> excludeProjection(Object... args) {
		return excludeProjection(buildNewCustomProjectionArguments(args).toArray(new Path<?>[]{}));
	}
	
	public DynamicEntityPath createEntityPath(Class<?> resultClass, String variable) {
		return new DynamicEntityPath(resultClass, forProperty(variable),INITS.get(variable));
	}
	
	

}
