/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.session.service;

import java.io.Serializable; 
import java.util.List;

import br.com.anteros.core.utils.Assert;
import br.com.anteros.core.utils.TypeResolver;
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
import br.com.anteros.persistence.session.repository.SQLRepository;
import br.com.anteros.persistence.session.repository.impl.GenericSQLRepository;
import br.com.anteros.persistence.transaction.Transaction;

public class GenericSQLService<T, ID extends Serializable> implements SQLService<T, ID> {

	protected SQLRepository<T, ID> repository;
	protected SQLSessionFactory sessionFactory;

	public GenericSQLService() {
	}

	public GenericSQLService(SQLSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		Class<?>[] typeArguments = TypeResolver.resolveRawArguments(GenericSQLService.class, getClass());
		if (typeArguments != null) {
			this.repository = new GenericSQLRepository<T, ID>(sessionFactory, typeArguments[0]);
		}
	}

	@Override
	public <S extends T> S save(S entity) {
		checkRepository();
		return repository.save(entity);
	}

	protected void checkRepository() {
		Assert.notNull(repository, "O repositório não foi criado. Verifique se a sessionFactory foi atribuida.");
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		checkRepository();
		return repository.save(entities);
	}

	@Override
	public <S extends T> S saveAndFlush(S entity) {
		checkRepository();
		return repository.saveAndFlush(entity);
	}

	@Override
	public void flush() {
		checkRepository();
		repository.flush();

	}

	@Override
	public T findOne(ID id, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOne(id, fieldsToForceLazy);
	}

	@Override
	public boolean exists(ID id) {
		checkRepository();
		return repository.exists(id);
	}

	@Override
	public List<T> findAll(String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(pageable, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, pageable, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, parameters, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, parameters, pageable, fieldsToForceLazy);
	}

	@Override
	public List<T> findByNamedQuery(String queryName, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Pageable pageable, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, pageable, fieldsToForceLazy);
	}

	@Override
	public List<T> findByNamedQuery(String queryName, Object parameters, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, parameters, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, parameters, pageable, fieldsToForceLazy);
	}

	@Override
	public T findOne(Predicate predicate, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOne(predicate, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(Predicate predicate, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(predicate, fieldsToForceLazy);
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, String fieldsToForceLazy, OrderSpecifier<?>... orders) {
		checkRepository();
		return repository.findAll(predicate, fieldsToForceLazy, orders);
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(predicate, pageable, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable, String fieldsToForceLazy, OrderSpecifier<?>... orders) {
		checkRepository();
		return repository.findAll(predicate, pageable, fieldsToForceLazy, orders);
	}

	@Override
	public void refresh(T entity, String fieldsToForceLazy) {
		checkRepository();
		repository.refresh(entity, fieldsToForceLazy);
	}

	@Override
	public long count() {
		checkRepository();
		return repository.count();
	}

	@Override
	public long count(Predicate predicate) {
		checkRepository();
		return repository.count(predicate);
	}

	@Override
	public void remove(ID id) {
		checkRepository();
		repository.remove(id);
	}

	@Override
	public void remove(T entity) {
		checkRepository();
		repository.remove(entity);
	}

	@Override
	public void remove(Iterable<? extends T> entities) {
		checkRepository();
		repository.remove(entities);
	}

	@Override
	public void removeAll() {
		checkRepository();
		repository.removeAll();
	}

	public SQLSessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SQLSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		Class<?>[] typeArguments = TypeResolver.resolveRawArguments(GenericSQLService.class, getClass());
		if (typeArguments != null) {
			this.repository = new GenericSQLRepository<T, ID>(sessionFactory, typeArguments[0]);
		}
	}

	@Override
	public T findOneBySql(String sql, String fieldsToForceLazy) {
		checkRepository();
		return this.repository.findOneBySql(sql, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, String fieldsToForceLazy) {
		checkRepository();
		return this.repository.findOneBySql(sql, parameters, fieldsToForceLazy);
	}

	@Override
	public Identifier<T> createIdentifier() throws Exception {
		checkRepository();
		return repository.createIdentifier();
	}

	@Override
	public Identifier<T> getIdentifier(T owner) throws Exception {
		return repository.getIdentifier(owner);
	}

	@Override
	public T findOne(ID id, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOne(id, readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOneBySql(sql,readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOneBySql(sql,parameters,readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOne(ID id, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOne(id,lockOptions, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOneBySql(sql,lockOptions, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOneBySql(sql,parameters,lockOptions, fieldsToForceLazy);
	}

	@Override
	public T findOne(ID id, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOne(id,lockOptions,readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOneBySql(sql,lockOptions,readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findOneBySql(sql,parameters,lockOptions,readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(pageable, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,pageable,readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,parameters,readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,parameters,pageable,readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> findByNamedQuery(String queryName, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, pageable, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> findByNamedQuery(String queryName, Object parameters, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, parameters, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findByNamedQuery(queryName, parameters,pageable,readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(lockOptions, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(pageable, lockOptions, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(lockOptions, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(pageable, lockOptions, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, lockOptions, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, pageable, lockOptions, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,parameters,lockOptions, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,parameters,pageable,lockOptions, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql,lockOptions,readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, pageable, lockOptions, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, parameters, lockOptions, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.find(sql, parameters, pageable, lockOptions, readOnly, fieldsToForceLazy);
	}


	@Override
	public SQLSession getSession() {
		checkRepository();
		return repository.getSession();
	}

	@Override
	public void setSession(SQLSession session) {
		checkRepository();
		repository.setSession(session);
	}

	@Override
	public SQLSession openSession() throws Exception {
		checkRepository();
		return repository.openSession();
	}

	@Override
	public SQLSessionFactory getSQLSessionFactory() throws Exception {
		checkRepository();
		return repository.getSQLSessionFactory();
	}

	@Override
	public void refresh(T entity, LockOptions lockOptions, String fieldsToForceLazy) {
		checkRepository();
		repository.refresh(entity, lockOptions, fieldsToForceLazy);
	}

	@Override
	public Transaction getTransaction() throws Exception {
		checkRepository();
		return repository.getTransaction();
	}

	@Override
	public OSQLQuery createObjectQuery() {
		return new OSQLQuery(repository.getSession());
	}

	@Override
	public boolean exists(List<ID> ids) {
		return repository.exists(ids);
	}

	@Override
	public List<T> findAll(List<ID> ids, String fieldsToForceLazy) {
		return repository.findAll(ids, fieldsToForceLazy);
	}
	
	@Override
	public List<T> findAll(List<ID> ids, LockOptions lockOptions, String fieldsToForceLazy) {
		return repository.findAll(ids,lockOptions, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(List<ID> ids, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		return findAll(ids,lockOptions,readOnly, fieldsToForceLazy);
	}

	@Override
	public Boolean removeAll(List<ID> ids) throws Exception {
		return removeAll(ids);
	}

	@Override
	public String getTableName() throws Exception {
		return repository.getTableName();
	}

	@Override
	public DescriptionNamedQuery getNamedQuery(String queryName) throws Exception {
		return repository.getNamedQuery(queryName);
	}

	@Override
	public Class<T> getResultClass() {
		return repository.getResultClass();
	}

	@Override
	public void validate(T entity) throws Exception {
		repository.validate(entity);		
	}

	@Override
	public void validate(T entity, Class<?>... groups) throws Exception {
		repository.validate(entity, groups);		
	}

	@Override
	public EntityPath<T> getEntityPath() {
		return repository.getEntityPath();
	}

	@Override
	public List<T> findAll(List<ID> ids, boolean readOnly, String fieldsToForceLazy) {
		checkRepository();
		return repository.findAll(ids,fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Predicate predicate, boolean readOnly, Pageable pageable, String fieldsToForceLazy,
			OrderSpecifier<?>... orders) {
		checkRepository();
		return repository.findAll(predicate, readOnly, pageable, fieldsToForceLazy, orders);
	}

	
}
