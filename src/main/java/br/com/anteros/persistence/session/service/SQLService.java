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
	
	void validate(T entity, Class<?>... groups) throws Exception;
	
	<S extends T> S save(S entity);

	<S extends T> Iterable<S> save(Iterable<S> entities);

	<S extends T> S saveAndFlush(S entity);

	void flush();

	T findOne(ID id, String fieldsToForceLazy);

	T findOneBySql(String sql, String fieldsToForceLazy);

	T findOneBySql(String sql, Object parameters, String fieldsToForceLazy);

	T findOne(ID id, boolean readOnly, String fieldsToForceLazy);

	T findOneBySql(String sql, boolean readOnly, String fieldsToForceLazy);

	T findOneBySql(String sql, Object parameters, boolean readOnly, String fieldsToForceLazy);

	T findOne(ID id, LockOptions lockOptions, String fieldsToForceLazy);

	T findOneBySql(String sql, LockOptions lockOptions, String fieldsToForceLazy);

	T findOneBySql(String sql, Object parameters, LockOptions lockOptions, String fieldsToForceLazy);

	T findOne(ID id, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	T findOneBySql(String sql, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	T findOneBySql(String sql, Object parameters, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	boolean exists(ID id);
	
	boolean exists(List<ID> ids);

	List<T> findAll(String fieldsToForceLazy);

	Page<T> findAll(Pageable pageable, String fieldsToForceLazy);

	List<T> findAll(boolean readOnly, String fieldsToForceLazy);

	Page<T> findAll(Pageable pageable, boolean readOnly, String fieldsToForceLazy);

	List<T> find(String sql, String fieldsToForceLazy);

	Page<T> find(String sql, Pageable pageable, String fieldsToForceLazy);

	List<T> find(String sql, Object parameters, String fieldsToForceLazy);

	Page<T> find(String sql, Object parameters, Pageable pageable, String fieldsToForceLazy);

	List<T> find(String sql, boolean readOnly, String fieldsToForceLazy);

	Page<T> find(String sql, Pageable pageable, boolean readOnly, String fieldsToForceLazy);

	List<T> find(String sql, Object parameters, boolean readOnly, String fieldsToForceLazy);

	Page<T> find(String sql, Object parameters, Pageable pageable, boolean readOnly, String fieldsToForceLazy);

	List<T> findByNamedQuery(String queryName, String fieldsToForceLazy);

	Page<T> findByNamedQuery(String queryName, Pageable pageable, String fieldsToForceLazy);

	List<T> findByNamedQuery(String queryName, Object parameters, String fieldsToForceLazy);

	Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, String fieldsToForceLazy);

	List<T> findByNamedQuery(String queryName, boolean readOnly, String fieldsToForceLazy);

	Page<T> findByNamedQuery(String queryName, Pageable pageable, boolean readOnly, String fieldsToForceLazy);

	List<T> findByNamedQuery(String queryName, Object parameters, boolean readOnly, String fieldsToForceLazy);

	Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, boolean readOnly, String fieldsToForceLazy);

	List<T> findAll(LockOptions lockOptions, String fieldsToForceLazy);

	Page<T> findAll(Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy);

	List<T> findAll(LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	Page<T> findAll(Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	List<T> find(String sql, LockOptions lockOptions, String fieldsToForceLazy);

	Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy);

	List<T> find(String sql, Object parameters, LockOptions lockOptions, String fieldsToForceLazy);

	Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy);

	List<T> find(String sql, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	List<T> find(String sql, Object parameters, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	T findOne(Predicate predicate, String fieldsToForceLazy);

	List<T> findAll(Predicate predicate, String fieldsToForceLazy);
	
	List<T> findAll(List<ID> ids, String fieldsToForceLazy);
	
	List<T> findAll(List<ID> ids, LockOptions lockOptions, String fieldsToForceLazy);
	
	List<T> findAll(List<ID> ids, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy);

	Iterable<T> findAll(Predicate predicate, String fieldsToForceLazy, OrderSpecifier<?>... orders);

	Page<T> findAll(Predicate predicate, Pageable pageable, String fieldsToForceLazy);

	Page<T> findAll(Predicate predicate, Pageable pageable, String fieldsToForceLazy, OrderSpecifier<?>... orders);
	
	
	Class<T> getResultClass();
	
	SQLSession getSession();

	void setSession(SQLSession session);

	SQLSession openSession() throws Exception;

	SQLSessionFactory getSQLSessionFactory() throws Exception;

	void refresh(T entity, String fieldsToForceLazy);

	void refresh(T entity, LockOptions lockOptions, String fieldsToForceLazy);

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
