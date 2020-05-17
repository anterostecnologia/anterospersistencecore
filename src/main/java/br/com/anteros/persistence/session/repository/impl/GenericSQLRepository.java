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
package br.com.anteros.persistence.session.repository.impl;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.anteros.core.utils.Assert;
import br.com.anteros.core.utils.TypeResolver;
import br.com.anteros.persistence.dsl.osql.BooleanBuilder;
import br.com.anteros.persistence.dsl.osql.DynamicEntityPath;
import br.com.anteros.persistence.dsl.osql.EntityPathResolver;
import br.com.anteros.persistence.dsl.osql.OSQLQuery;
import br.com.anteros.persistence.dsl.osql.SimpleEntityPathResolver;
import br.com.anteros.persistence.dsl.osql.support.Expressions;
import br.com.anteros.persistence.dsl.osql.types.EntityPath;
import br.com.anteros.persistence.dsl.osql.types.Ops;
import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.Predicate;
import br.com.anteros.persistence.dsl.osql.types.expr.BooleanExpression;
import br.com.anteros.persistence.dsl.osql.types.expr.BooleanOperation;
import br.com.anteros.persistence.dsl.osql.types.expr.params.StringParam;
import br.com.anteros.persistence.dsl.osql.types.path.PathBuilder;
import br.com.anteros.persistence.dsl.osql.types.path.StringPath;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.descriptor.DescriptionColumn;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.metadata.descriptor.DescriptionNamedQuery;
import br.com.anteros.persistence.metadata.identifier.Identifier;
import br.com.anteros.persistence.parameter.InClauseSubstitutedParameter;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.session.FindParameters;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.SQLSessionFactory;
import br.com.anteros.persistence.session.exception.SQLSessionException;
import br.com.anteros.persistence.session.lock.LockOptions;
import br.com.anteros.persistence.session.query.SQLQueryException;
import br.com.anteros.persistence.session.query.TypedSQLQuery;
import br.com.anteros.persistence.session.repository.Page;
import br.com.anteros.persistence.session.repository.Pageable;
import br.com.anteros.persistence.session.repository.SQLRepository;
import br.com.anteros.persistence.session.repository.SQLRepositoryException;
import br.com.anteros.persistence.transaction.Transaction;

@SuppressWarnings("unchecked")
public class GenericSQLRepository<T, ID extends Serializable> implements SQLRepository<T, ID> {

	private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;
	public static final String COUNT_QUERY_STRING = "select count(*) as qt from %s x";
	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";

	protected SQLSession session;
	protected SQLSessionFactory sessionFactory;
	protected Class<?> persistentClass;
	protected EntityPath<T> path;
	protected PathBuilder<T> builder;

	public GenericSQLRepository(SQLSession session) {
		this.setSession(session);
		Class<?>[] typeArguments = TypeResolver.resolveRawArguments(GenericSQLRepository.class, getClass());
		if (typeArguments != null) {
			this.persistentClass = typeArguments[0];
		}
	}

	public GenericSQLRepository(SQLSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		Class<?>[] typeArguments = TypeResolver.resolveRawArguments(GenericSQLRepository.class, getClass());
		if (typeArguments != null) {
			this.persistentClass = typeArguments[0];
		}
	}

	public GenericSQLRepository(SQLSession session, Class<?> type) {
		this.setSession(session);
		this.persistentClass = type;
	}

	public GenericSQLRepository(SQLSessionFactory sessionFactory, Class<?> type) {
		this.sessionFactory = sessionFactory;
		this.persistentClass = type;
	}

	@Override
	public <S extends T> S save(S entity) {
		try {
			return (S) getSession().save(entity);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}
	
	@Override
	public <S extends T> S save(S entity, Class<?>... groups) {
		try {
			return (S) getSession().save(entity, groups);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		List<S> result = new ArrayList<S>();

		if (entities == null) {
			return result;
		}

		for (S entity : entities) {
			result.add(save(entity));
		}

		return result;
	}

	@Override
	public <S extends T> S saveAndFlush(S entity) {
		S result = save(entity);
		flush();

		return result;
	}

	@Override
	public void flush() {
		try {
			getSession().flush();
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public T findOne(ID id, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(id, "O id não pode ser nulo.");

		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");
		try {
			return (T) getSession().find(new FindParameters().entityClass(persistentClass).id(id)
					.lockOptions(lockOptions).readOnly(readOnly).fieldsToForceLazy(fieldsToForceLazy));
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public T findOneBySql(String sql, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		List<T> result = find(sql, readOnly, fieldsToForceLazy);
		if ((result != null) && (result.size() > 0))
			return result.get(0);
		return null;
	}

	@Override
	public T findOneBySql(String sql, Object parameters, LockOptions lockOptions, boolean readOnly,
			String fieldsToForceLazy) {
		List<T> result = find(sql, parameters, readOnly, fieldsToForceLazy);
		if ((result != null) && (result.size() > 0))
			return result.get(0);
		return null;
	}

	@Override
	public boolean exists(ID id) {
		Assert.notNull(id, "O id não pode ser nulo.");

		try {
			return (findOne(id, true, "") != null);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public List<T> findAll(LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		try {
			DescriptionField tenantId = getTenantId();
			DescriptionField companyId = getCompanyId();

			String sql = "select * from " + getEntityCache().getTableName() + " P ";
			boolean hasWhere = false;
			if (tenantId != null) {
				if (getSession().getTenantId() == null) {
					throw new SQLQueryException("Informe o Tenant ID para realizar consulta na entidade "
							+ getEntityCache().getEntityClass().getName());
				}
				hasWhere = true;
				sql = sql + " where P." + tenantId.getSimpleColumn().getColumnName() + " = " + "'"
						+ getSession().getTenantId().toString() + "'";
			}

			if (companyId != null) {
				if (getSession().getCompanyId() == null) {
					throw new SQLQueryException("Informe o Company ID para realizar consulta na entidade "
							+ getEntityCache().getEntityClass().getName());
				}
				if (!hasWhere) {
					sql = sql + " where ";
				} else {
					sql = sql + " and ";
				}
				sql = sql + "P." + companyId.getSimpleColumn().getColumnName() + " = " + "'"
						+ getSession().getCompanyId().toString() + "'";
			}

			TypedSQLQuery<?> query = getSession().createQuery(sql, persistentClass);
			query.setLockOptions(lockOptions);
			query.setReadOnly(readOnly);
			query.setFieldsToForceLazy(fieldsToForceLazy);
			return (List<T>) query.getResultList();
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	protected EntityCache getEntityCache() {
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		return getSession().getEntityCacheManager().getEntityCache(persistentClass);
	}

	protected DescriptionField getTenantId() {
		List<EntityCache> entityCaches = getSession().getEntityCacheManager()
				.getEntityCachesByTableName(getEntityCache().getTableName());
		for (EntityCache entityCache : entityCaches) {
			if (entityCache.getTenantId() != null) {
				return entityCache.getTenantId();
			}
		}
		return null;
	}

	protected DescriptionField getCompanyId() {
		List<EntityCache> entityCaches = getSession().getEntityCacheManager()
				.getEntityCachesByTableName(getEntityCache().getTableName());
		for (EntityCache entityCache : entityCaches) {
			if (entityCache.getCompanyId() != null) {
				return entityCache.getCompanyId();
			}
		}
		return null;
	}

	@Override
	public Page<T> findAll(Pageable pageable, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {

		if (null == pageable) {
			List<T> result = findAll(fieldsToForceLazy);
			return new PageImpl<T>(result);
		}

		TypedSQLQuery<?> query;
		try {
			DescriptionField tenantId = getTenantId();
			DescriptionField companyId = getCompanyId();

			String sql = "select * from " + getEntityCache().getTableName() + " P ";
			boolean hasWhere = false;
			if (tenantId != null) {
				if (getSession().getTenantId() == null) {
					throw new SQLQueryException("Informe o Tenant ID para realizar consulta na entidade "
							+ getEntityCache().getEntityClass().getName());
				}
				hasWhere = true;
				sql = sql + " where P." + tenantId.getSimpleColumn().getColumnName() + " = " + "'"
						+ getSession().getTenantId().toString() + "'";
			}
			if (companyId != null) {
				if (getSession().getCompanyId() == null) {
					throw new SQLQueryException("Informe o Company ID para realizar consulta na entidade "
							+ getEntityCache().getEntityClass().getName());
				}
				if (!hasWhere) {
					sql = sql + " where ";
				} else {
					sql = sql + " and ";
				}
				sql = sql + "P." + companyId.getSimpleColumn().getColumnName() + " = " + "'"
						+ getSession().getCompanyId().toString() + "'";
			}
			query = getSession().createQuery(sql, persistentClass);

			query.setFirstResult(pageable.getOffset());
			query.setMaxResults(pageable.getPageSize());
			query.setReadOnly(readOnly);
			query.setLockOptions(lockOptions);
			query.setFieldsToForceLazy(fieldsToForceLazy);

			Long total = count();
  			List<T> content = (List<T>) (total > pageable.getOffset() ? query.getResultList()
					: Collections.<T>emptyList());

			return new PageImpl<T>(content, pageable, total);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public List<T> find(String sql, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		try {
			TypedSQLQuery<?> query = getSession().createQuery(sql, persistentClass);
			query.setReadOnly(readOnly);
			query.setLockOptions(lockOptions);
			query.setFieldsToForceLazy(fieldsToForceLazy);
			return (List<T>) query.getResultList();
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, boolean readOnly,
			String fieldsToForceLazy) {
		if (null == pageable) {
			return new PageImpl<T>(this.find(sql, fieldsToForceLazy));
		}

		TypedSQLQuery<?> query;
		try {
			query = getSession().createQuery(sql, persistentClass);
			query.setFirstResult(pageable.getOffset());
			query.setMaxResults(pageable.getPageSize());
			query.setReadOnly(readOnly);
			query.setLockOptions(lockOptions);
			query.setFieldsToForceLazy(fieldsToForceLazy);

			Long total = doCount(getCountQueryString("(" + sql + ")"));
			List<T> content = (List<T>) (total > pageable.getOffset() ? query.getResultList()
					: Collections.<T>emptyList());

			return new PageImpl<T>(content, pageable, total);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public List<T> find(String sql, Object parameters, LockOptions lockOptions, boolean readOnly,
			String fieldsToForceLazy) {
		try {
			TypedSQLQuery<?> query = getSession().createQuery(sql, persistentClass, parameters);
			query.setReadOnly(readOnly);
			query.setLockOptions(lockOptions);
			query.setFieldsToForceLazy(fieldsToForceLazy);
			return (List<T>) query.getResultList();
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions, boolean readOnly,
			String fieldsToForceLazy) {
		if (null == pageable) {
			return new PageImpl<T>(find(sql, parameters, fieldsToForceLazy));
		}

		TypedSQLQuery<?> query;
		try {
			query = getSession().createQuery(sql, persistentClass);
			query.setFirstResult(pageable.getOffset());
			query.setMaxResults(pageable.getPageSize());
			query.setParameters(parameters);
			query.setReadOnly(readOnly);
			query.setLockOptions(lockOptions);
			query.setFieldsToForceLazy(fieldsToForceLazy);
			Long total = doCount(getCountQueryString("(" + sql + ")"), parameters);
			List<T> content = (List<T>) (total > pageable.getOffset() ? query.getResultList()
					: Collections.<T>emptyList());

			return new PageImpl<T>(content, pageable, total);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public List<T> findByNamedQuery(String queryName, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(queryName, "O nome da query não pode ser nulo.");
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		EntityCache cache = getSession().getEntityCacheManager().getEntityCache(persistentClass);
		DescriptionNamedQuery namedQuery = cache.getDescriptionNamedQuery(queryName);
		if (namedQuery == null)
			throw new SQLQueryException("Query nomeada " + queryName + " não encontrada.");
		return find(namedQuery.getQuery(), namedQuery.getLockOptions(), readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(queryName, "O nome da query não pode ser nulo.");
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		EntityCache cache = getSession().getEntityCacheManager().getEntityCache(persistentClass);
		DescriptionNamedQuery namedQuery = cache.getDescriptionNamedQuery(queryName);
		if (namedQuery == null)
			throw new SQLQueryException("Query nomeada " + queryName + " não encontrada.");
		return find(namedQuery.getQuery(), pageable, namedQuery.getLockOptions(), readOnly, fieldsToForceLazy);

	}

	@Override
	public List<T> findByNamedQuery(String queryName, Object parameters, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(queryName, "O nome da query não pode ser nulo.");
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		EntityCache cache = getSession().getEntityCacheManager().getEntityCache(persistentClass);
		DescriptionNamedQuery namedQuery = cache.getDescriptionNamedQuery(queryName);
		if (namedQuery == null)
			throw new SQLQueryException("Query nomeada " + queryName + " não encontrada.");
		return find(namedQuery.getQuery(), parameters, namedQuery.getLockOptions(), readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, boolean readOnly,
			String fieldsToForceLazy) {
		Assert.notNull(queryName, "O nome da query não pode ser nulo.");
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		EntityCache cache = getSession().getEntityCacheManager().getEntityCache(persistentClass);
		DescriptionNamedQuery namedQuery = cache.getDescriptionNamedQuery(queryName);
		if (namedQuery == null)
			throw new SQLQueryException("Query nomeada " + queryName + " não encontrada.");
		return find(namedQuery.getQuery(), parameters, pageable, namedQuery.getLockOptions(), readOnly,
				fieldsToForceLazy);

	}

	@Override
	public T findOne(Predicate predicate, String fieldsToForceLazy) {
		return createQuery(predicate).uniqueResult(getEntityPath());
	}

	public EntityPath<T> getEntityPath() {
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		if (path == null) {
			// this.path = (EntityPath<T>)
			// DEFAULT_ENTITY_PATH_RESOLVER.createPath(persistentClass);
			this.path = new DynamicEntityPath(persistentClass, persistentClass.getSimpleName() + "_P");
		}
		return path;
	}

	protected PathBuilder<T> getPathBuilder() {
		if (builder == null)
			this.builder = new PathBuilder<T>(getEntityPath().getType(), path.getMetadata());
		return builder;
	}

	@Override
	public List<T> findAll(Predicate predicate, String fieldsToForceLazy) {
		return createQuery(predicate).list(getEntityPath());
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, String fieldsToForceLazy, OrderSpecifier<?>... orders) {
		OSQLQuery query = createQuery(predicate).orderBy(orders);
		return query.list(getEntityPath());
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable, String fieldsToForceLazy) {

		predicate = addTenantAndCompanyId(predicate);

		OSQLQuery countQuery = createQuery(predicate);
		Long total = countQuery.count();

		OSQLQuery query = createQuery(predicate);
		query.offset(pageable.getOffset());
		query.limit(pageable.getPageSize());
		query.setFieldsToForceLazy(fieldsToForceLazy);

		List<T> content = total > pageable.getOffset() ? query.list(path) : Collections.<T>emptyList();

		return new PageImpl<T>(content, pageable, total);
	}

	
	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable, boolean readOnly,  String fieldsToForceLazy) {

		predicate = addTenantAndCompanyId(predicate);

		OSQLQuery countQuery = createQuery(predicate); 
		Long total = countQuery.count();

		OSQLQuery query = createQuery(predicate);
		query.setFieldsToForceLazy(fieldsToForceLazy);
		query.offset(pageable.getOffset());
		query.limit(pageable.getPageSize());
		query.readOnly(readOnly);

		List<T> content = total > pageable.getOffset() ? query.list(path) : Collections.<T>emptyList();

		return new PageImpl<T>(content, pageable, total);
	}

	
	protected Predicate addTenantAndCompanyId(Predicate predicate) {
		if (predicate == null) {
			predicate = new BooleanBuilder();
		}
		EntityCache[] entityCaches = getSession().getEntityCacheManager()
				.getEntitiesBySuperClassIncluding(this.getResultClass());
		DynamicEntityPath entityPath = (DynamicEntityPath) this.getEntityPath();
		DescriptionField tenantId = null;
		DescriptionField companyId = null;

		for (EntityCache entityCache : entityCaches) {
			tenantId = entityCache.getTenantId();
			if (tenantId != null)
				break;
		}
		for (EntityCache entityCache : entityCaches) {
			companyId = entityCache.getCompanyId();
			if (companyId != null)
				break;
		}

		if (tenantId != null) {
			if (getSession().getTenantId() == null) {
				throw new SQLSessionException("Informe o Tenant Id para consultar  a entidade " + getSession()
						.getEntityCacheManager().getEntityCache(this.getResultClass()).getEntityClass().getName());
			}
			StringPath predicateField = entityPath.createFieldString(tenantId.getName());
			BooleanExpression expression = Expressions.predicate(Ops.EQ, predicateField,
					Expressions.constant(getSession().getTenantId().toString()));

			if (predicate instanceof BooleanOperation) {
				((BooleanOperation) predicate).and(expression);
			} else {
				((BooleanBuilder) predicate).and(expression);
			}
		}

		if (companyId != null) {
			if (this.getSession().getCompanyId() == null) {
				throw new SQLSessionException("Informe o Company Id para consultar  a entidade " + this.getSession()
						.getEntityCacheManager().getEntityCache(this.getResultClass()).getEntityClass().getName());
			}
			StringPath predicateField = entityPath.createFieldString(companyId.getName());
			BooleanExpression expression = Expressions.predicate(Ops.EQ, predicateField,
					Expressions.constant(this.getSession().getCompanyId().toString()));
			if (predicate instanceof BooleanOperation) {
				((BooleanOperation) predicate).and(expression);
			} else {
				((BooleanBuilder) predicate).and(expression);
			}
		}
		return predicate;
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable, String fieldsToForceLazy,
			OrderSpecifier<?>... orders) {
		return this.findAll(predicate, false, pageable, fieldsToForceLazy, orders);
	}

	@Override
	public SQLSession getSession() {
		if (session != null) {
			try {
				if (session.getConnection().isClosed()) {
					session.invalidateConnection();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return session;
		} else if (sessionFactory != null) {
			try {
				return sessionFactory.getCurrentSession();
			} catch (Exception e) {
				throw new SQLRepositoryException(e);
			}
		}
		throw new SQLRepositoryException(
				"Não foi configurado nenhuma SQLSession ou SQLSessionFactory para o repositório.");
	}

	@Override
	public void refresh(T entity, LockOptions lockOptions, String fieldsToForceLazy) {
		try {
			getSession().refresh(entity);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public long count() {
		DescriptionField tenantId = getTenantId();
		DescriptionField companyId = getCompanyId();
		String sql = getCountQueryString(getEntityCache().getTableName());
		boolean hasWhere = false;
		if (tenantId != null) {
			if (getSession().getTenantId() == null) {
				throw new SQLQueryException("Informe o Tenant ID para realizar consulta na entidade "
						+ getEntityCache().getEntityClass().getName());
			}
			hasWhere = true;
			sql = sql + " where x." + tenantId.getSimpleColumn().getColumnName() + " = " + "'"
					+ getSession().getTenantId().toString() + "'";
		}

		if (companyId != null) {
			if (getSession().getCompanyId() == null) {
				throw new SQLQueryException("Informe o Company ID para realizar consulta na entidade "
						+ getEntityCache().getEntityClass().getName());
			}
			if (!hasWhere) {
				sql = sql + " where ";
			} else {
				sql = sql + " and ";
			}
			sql = sql + "x." + companyId.getSimpleColumn().getColumnName() + " = " + "'"
					+ getSession().getCompanyId().toString() + "'";
		}

		return doCount(sql);
	}

	protected long doCount(String countSql) {
		try {
			ResultSet rs = getSession().createQuery(countSql).executeQuery();
			rs.next();
			Long value = rs.getLong(1);
			rs.close();
			return value;
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	protected long doCount(String countSql, Object parameters) {
		try {
			ResultSet rs = getSession().createQuery(countSql).setParameters(parameters).executeQuery();
			rs.next();
			Long value = rs.getLong(1);
			rs.close();
			return value;
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public long count(Predicate predicate) {
		return createQuery(predicate).count();
	}

	@Override
	public void remove(ID id) {
		try {
			Assert.notNull(id, "O id não pode ser nulo.");
			Assert.notNull(persistentClass,
					"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

			T entity = findOne(id, "");
			if (entity == null) {
				throw new SQLRepositoryException(
						String.format("Não foi encontrada nenhuma entidade %s com o id %s.", persistentClass, id));
			}
			remove(entity);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public void remove(T entity) {
		try {
			getSession().remove(entity);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public void remove(Iterable<? extends T> entities) {
		Assert.notNull(entities, "A lista de entidades não pode ser nula.");

		for (T entity : entities) {
			remove(entity);
		}
	}

	@Override
	public void removeAll() {
		for (T element : findAll("")) {
			remove(element);
		}
	}

	public SQLSessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SQLSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setSession(SQLSession session) {
		this.session = session;
	}

	protected OSQLQuery createQuery(Predicate... predicate) {
		OSQLQuery query = new OSQLQuery(getSession()).from(getEntityPath());
		if (predicate == null || predicate.length == 0)
			return query;
		query.where(predicate);
		return query;
	}

	private String getCountQueryString(String tableName) {
		return String.format(COUNT_QUERY_STRING, tableName);
	}

	public Class<?> getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(Class<?> persistentClass) {
		this.persistentClass = persistentClass;
	}

	@Override
	public Transaction getTransaction() throws Exception {
		return getSession().getTransaction();
	}

	@Override
	public Identifier<T> createIdentifier() throws Exception {
		return (Identifier<T>) getSession().createIdentifier(getPersistentClass());
	}

	@Override
	public Identifier<T> getIdentifier(T owner) throws Exception {
		return getSession().getIdentifier(owner);
	}

	@Override
	public SQLSession openSession() throws Exception {
		if (sessionFactory == null)
			throw new SQLRepositoryException(
					"Nenhuma fábrica de sessões foi atribuída ao repositório não é possível criar uma nova sessão SQL.");
		return sessionFactory.openSession();
	}

	@Override
	public SQLSessionFactory getSQLSessionFactory() throws Exception {
		return sessionFactory;
	}

	@Override
	public List<T> find(String sql, LockOptions lockOptions, String fieldsToForceLazy) {
		return find(sql, false, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy) {
		return find(sql, pageable, false, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, LockOptions lockOptions, String fieldsToForceLazy) {
		return find(sql, parameters, false, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, LockOptions lockOptions,
			String fieldsToForceLazy) {
		return find(sql, parameters, pageable, false, fieldsToForceLazy);
	}

	@Override
	public T findOne(ID id, LockOptions lockOptions, String fieldsToForceLazy) {
		return findOne(id, lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, LockOptions lockOptions, String fieldsToForceLazy) {
		return findOneBySql(sql, lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, LockOptions lockOptions, String fieldsToForceLazy) {
		return findOneBySql(sql, parameters, lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(LockOptions lockOptions, String fieldsToForceLazy) {
		return findAll(lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, LockOptions lockOptions, String fieldsToForceLazy) {
		return findAll(pageable, lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public T findOne(ID id, String fieldsToForceLazy) {
		return findOne(id, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, String fieldsToForceLazy) {
		return findOneBySql(sql, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, String fieldsToForceLazy) {
		return findOneBySql(sql, parameters, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public T findOne(ID id, boolean readOnly, String fieldsToForceLazy) {
		return findOne(id, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, boolean readOnly, String fieldsToForceLazy) {
		return findOneBySql(sql, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public T findOneBySql(String sql, Object parameters, boolean readOnly, String fieldsToForceLazy) {
		return findOneBySql(sql, parameters, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(String fieldsToForceLazy) {
		return findAll(LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, String fieldsToForceLazy) {
		return findAll(pageable, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(boolean readOnly, String fieldsToForceLazy) {
		return findAll(LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		return findAll(pageable, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, String fieldsToForceLazy) {
		return find(sql, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, String fieldsToForceLazy) {
		return find(sql, pageable, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, String fieldsToForceLazy) {
		return find(sql, parameters, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, String fieldsToForceLazy) {
		return find(sql, parameters, pageable, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, boolean readOnly, String fieldsToForceLazy) {
		return find(sql, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		return find(sql, pageable, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public List<T> find(String sql, Object parameters, boolean readOnly, String fieldsToForceLazy) {
		return find(sql, parameters, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> find(String sql, Object parameters, Pageable pageable, boolean readOnly, String fieldsToForceLazy) {
		return find(sql, parameters, pageable, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public void refresh(T entity, String fieldsToForceLazy) {
		refresh(entity, LockOptions.NONE, fieldsToForceLazy);
	}

	@Override
	public List<T> findByNamedQuery(String queryName, String fieldsToForceLazy) {
		return findByNamedQuery(queryName, false, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Pageable pageable, String fieldsToForceLazy) {
		return findByNamedQuery(queryName, pageable, false, fieldsToForceLazy);
	}

	@Override
	public List<T> findByNamedQuery(String queryName, Object parameters, String fieldsToForceLazy) {
		return findByNamedQuery(queryName, parameters, false, fieldsToForceLazy);
	}

	@Override
	public Page<T> findByNamedQuery(String queryName, Object parameters, Pageable pageable, String fieldsToForceLazy) {
		return findByNamedQuery(queryName, parameters, pageable, false, fieldsToForceLazy);
	}

	@Override
	public OSQLQuery createObjectQuery() {
		return new OSQLQuery(getSession());
	}

	@Override
	public boolean exists(List<ID> ids) {
		return (findAll(ids, "") != Collections.EMPTY_LIST);
	}

	@Override
	public List<T> findAll(List<ID> ids, String fieldsToForceLazy) {
		return findAll(ids, LockOptions.NONE, false, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(List<ID> ids, LockOptions lockOptions, String fieldsToForceLazy) {
		return findAll(ids, lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public List<T> findAll(List<ID> ids, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		if (getEntityCache().hasCompositeKey()) {
			throw new SQLRepositoryException(
					"Não é possível usar o método findAll(List<ID> ids) com objetos que tenham chave composta. Neste caso será necessário fazer um método específico para isto.");
		}

		try {
			DescriptionColumn idDescriptionColumn = getEntityCache().getPrimaryKeyColumns().iterator().next();
			TypedSQLQuery<?> query = getSession().createQuery("select * from " + getEntityCache().getTableName()
					+ " where " + idDescriptionColumn.getColumnName() + " in (:pids) ", persistentClass);
			query.setLockOptions(lockOptions);
			query.setReadOnly(readOnly);
			query.setFieldsToForceLazy(fieldsToForceLazy);
			query.setParameters(new NamedParameter[] { new InClauseSubstitutedParameter("pids", ids.toArray()) });
			return (List<T>) query.getResultList();
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public Boolean removeAll(List<ID> ids) throws Exception {
		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		if (getEntityCache().hasCompositeKey()) {
			throw new SQLRepositoryException(
					"Não é possível usar o método removeAll(List<ID> ids) com objetos que tenham chave composta. Neste caso será necessário fazer um método específico para isto.");
		}

		try {
			DescriptionColumn idDescriptionColumn = getEntityCache().getPrimaryKeyColumns().iterator().next();
			return (getSession().update(
					"delete from " + getEntityCache().getTableName() + " where " + idDescriptionColumn.getColumnName()
							+ " in (:pids) ",
					new NamedParameter[] { new InClauseSubstitutedParameter("pids", ids.toArray()) }) > 0);
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
	}

	@Override
	public String getTableName() throws Exception {
		return getEntityCache().getTableName();
	}

	@Override
	public DescriptionNamedQuery getNamedQuery(String queryName) throws Exception {
		EntityCache cache = getSession().getEntityCacheManager().getEntityCache(persistentClass);
		DescriptionNamedQuery namedQuery = cache.getDescriptionNamedQuery(queryName);
		if (namedQuery == null)
			throw new SQLQueryException("Query nomeada " + queryName + " não encontrada.");
		return namedQuery;
	}

	@Override
	public Class<T> getResultClass() {
		return (Class<T>) persistentClass;
	}

	@Override
	public void validate(T entity) throws Exception {
		if (!getSession().validationIsActive()) {
			throw new SQLQueryException("A validação está desativada no servidor.");
		}

		getSession().validate(entity);
	}

	@Override
	public void validate(T entity, Class<?>... groups) throws Exception {
		if (!getSession().validationIsActive()) {
			throw new SQLQueryException("A validação está desativada no servidor.");
		}

		getSession().validate(entity, groups);
	}

	@Override
	public List<T> findAll(List<ID> ids, boolean readOnly, String fieldsToForceLazy) {
		return this.findAll(ids, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public Page<T> findAll(Predicate predicate, boolean readOnly, Pageable pageable, String fieldsToForceLazy,
			OrderSpecifier<?>... orders) {
		predicate = addTenantAndCompanyId(predicate);

		OSQLQuery countQuery = createQuery(predicate);
		Long total = countQuery.count();

		OSQLQuery query = createQuery(predicate);
		query.offset(pageable.getOffset());
		query.limit(pageable.getPageSize());
		query.setFieldsToForceLazy(fieldsToForceLazy);
		query.orderBy(orders);
		query.readOnly(readOnly);

		List<T> content = total > pageable.getOffset() ? query.list(path) : Collections.<T>emptyList();

		return new PageImpl<T>(content, pageable, total);
	}

	@Override
	public T findByCode(String code, String fieldsToForceLazy) {
		return this.findByCode(code, LockOptions.NONE, false, fieldsToForceLazy);
	}

	@Override
	public T findByCode(String code, boolean readOnly, String fieldsToForceLazy) {
		return this.findByCode(code, LockOptions.NONE, readOnly, fieldsToForceLazy);
	}

	@Override
	public T findByCode(String code, LockOptions lockOptions, String fieldsToForceLazy) {
		return this.findByCode(code, lockOptions, false, fieldsToForceLazy);
	}

	@Override
	public T findByCode(String code, LockOptions lockOptions, boolean readOnly, String fieldsToForceLazy) {
		Assert.notNull(code, "O CODE não pode ser nulo.");

		Assert.notNull(persistentClass,
				"A classe de persistência não foi informada. Verifique se usou a classe GenericSQLRepository diretamente, se usou será necessário passar a classe de persistência como parâmetro. Se preferir pode extender a classe GenericSQLRepository e definir os parâmetros do genérics da classe.");

		Assert.notNull(getEntityCache().getCodeField(),
				"A classe de persistência não possui um campo CODE.");
				
		getEntityCache().getCodeField();	
		
		try {
			StringParam pCode = new StringParam("PCODE");
			DynamicEntityPath entityPath = new DynamicEntityPath(persistentClass, "P");	
			StringPath predicateField = entityPath.createString(getEntityCache().getCodeField().getName());
			OSQLQuery query = createObjectQuery();
			query.from(entityPath).where(predicateField.eq(pCode)).set(pCode, code);
			query.readOnly(readOnly);
			query.setLockOptions(lockOptions);
			query.setFieldsToForceLazy(fieldsToForceLazy);
			
			List list = query.list(entityPath);
			if (list.size()>0)
				return (T) list.get(0);
			
		} catch (Exception e) {
			throw new SQLRepositoryException(e);
		}
		return null;
	}

	

}