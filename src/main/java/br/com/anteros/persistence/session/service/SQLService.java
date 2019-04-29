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
package br.com.anteros.persistence.session.service;

import java.io.Serializable;
import java.util.List;

import br.com.anteros.persistence.dsl.osql.OSQLQuery;
import br.com.anteros.persistence.dsl.osql.types.EntityPath;
import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.Predicate;
import br.com.anteros.persistence.metadata.descriptor.DescriptionNamedQuery;
import br.com.anteros.persistence.metadata.identifier.Identifier;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.SQLSessionFactory;
import br.com.anteros.persistence.session.lock.LockOptions;
import br.com.anteros.persistence.session.repository.Page;
import br.com.anteros.persistence.session.repository.Pageable;
import br.com.anteros.persistence.transaction.Transaction;

public interface SQLService<T, ID extends Serializable> {

	void validate(T entity) throws Exception;
	
	<S extends T> S save(S entity);

	<S extends T> Iterable<S> save(Iterable<S> entities);

	<S extends T> S saveAndFlush(S entity);

	void flush();

	T findOne(ID id);

	T findOneBySql(String sql);

	T findOneBySql(String sql, Object parameters);

	T findOne(ID id, boolean readOnly);

	T findOneBySql(String sql, boolean readOnly);

	T findOneBySql(String sql, Object parameters, boolean readOnly);

	T findOne(ID id, LockOptions lockOptions);

	T findOneBySql(String sql, LockOptions lockOptions);

	T findOneBySql(String sql, Object parameters, LockOptions lockOptions);

	T findOne(ID id, LockOptions lockOptions, boolean readOnly);

	T findOneBySql(String sql, LockOptions lockOptions, boolean readOnly);

	T findOneBySql(String sql, Object parameters, LockOptions lockOptions, boolean readOnly);

	boolean exists(ID id);
	
	boolean exists(List<ID> ids);

	List<T> findAll();

	Page<T> findAll(Pageable pageable);

	List<T> findAll(boolean readOnly);

	Page<T> findAll(Pageable pageable, boolean readOnly);

	List<T> find(String sql);

	Page<T> find(String sql, Pageable pageable);

	List<T> find(String sql, Object parameters);

	Page<T> find(String sql, Object parameters, Pageable pageable);

	List<T> find(String sql, boolean readOnly);

	Page<T> find(String sql, Pageable pageable, boolean readOnly);

	List<T> find(String sql, Object parameters, boolean readOnly);

	Page<T> find(String sql, Object parameters, Pageable pageable, boolean readOnly);

	List<T> findByNamedQuery(String queryName);

	Page<T> findByNamedQuery(String queryName, Pageable pageable);

	List<T> findByNamedQuery(String queryName, Object parameters);

	Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable);

	List<T> findByNamedQuery(String queryName, boolean readOnly);

	Page<T> findByNamedQuery(String queryName, Pageable pageable, boolean readOnly);

	List<T> findByNamedQuery(String queryName, Object parameters, boolean readOnly);

	Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, boolean readOnly);

	List<T> findAll(LockOptions lockOptions);

	Page<T> findAll(Pageable pageable, LockOptions lockOptions);

	List<T> findAll(LockOptions lockOptions, boolean readOnly);

	Page<T> findAll(Pageable pageable, LockOptions lockOptions, boolean readOnly);

	List<T> find(String sql, LockOptions lockOptions);

	Page<T> find(String sql, Pageable pageable, LockOptions lockOptions);

	List<T> find(String sql, Object parameters, LockOptions lockOptions);

	Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions);

	List<T> find(String sql, LockOptions lockOptions, boolean readOnly);

	Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, boolean readOnly);

	List<T> find(String sql, Object parameters, LockOptions lockOptions, boolean readOnly);

	Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions, boolean readOnly);

	T findOne(Predicate predicate);

	List<T> findAll(Predicate predicate);
	
	List<T> findAll(List<ID> ids);
	
	List<T> findAll(List<ID> ids, LockOptions lockOptions);
	
	List<T> findAll(List<ID> ids, LockOptions lockOptions, boolean readOnly);

	Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	Page<T> findAll(Predicate predicate, Pageable pageable);

	Page<T> findAll(Predicate predicate, Pageable pageable, OrderSpecifier<?>... orders);
	
	
	Class<T> getResultClass();
	
	SQLSession getSession();

	void setSession(SQLSession session);

	SQLSession openSession() throws Exception;

	SQLSessionFactory getSQLSessionFactory() throws Exception;

	void refresh(T entity);

	void refresh(T entity, LockOptions lockOptions);

	long count();

	long count(Predicate predicate);

	void remove(ID id);

	void remove(T entity);

	void remove(Iterable<? extends T> entities);

	void removeAll();
	
	Boolean removeAll(List<ID> ids) throws Exception;

	Transaction getTransaction() throws Exception;

	Identifier<T> createIdentifier() throws Exception;

	Identifier<T> getIdentifier(T owner) throws Exception;

	OSQLQuery createObjectQuery();
	
	String getTableName() throws Exception;

	DescriptionNamedQuery getNamedQuery(String queryName) throws Exception;
	
	public EntityPath<T> getEntityPath();

	
		
}
