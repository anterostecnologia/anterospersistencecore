package br.com.anteros.persistence.session;

import java.util.Map;

import br.com.anteros.persistence.dsl.osql.types.OrderSpecifier;
import br.com.anteros.persistence.dsl.osql.types.Predicate;
import br.com.anteros.persistence.metadata.identifier.Identifier;
import br.com.anteros.persistence.session.lock.LockOptions;
import br.com.anteros.persistence.session.repository.Pageable;

public class FindParameters<T> {

	public FindParameters() {

	}

	private Class<T> entityClass;
	private Object primaryKey;
	private Map<String, Object> properties;
	private LockOptions lockOptions = LockOptions.OPTIMISTIC_FORCE_INCREMENT;
	private Identifier<T> identifier;
	private Object id;
	private boolean readOnly = false;
	private String fieldsToForceLazy;
	private String sql;
	private Object parameters;
	private Pageable pageable;
	private Predicate predicate;
	private OrderSpecifier<?>[] orders;
	private boolean ignoreCompanyId=false;
	private boolean ignoreTenantId=false;

	public boolean isIgnoreCompanyId() {
		return ignoreCompanyId;
	}

	public FindParameters<T> ignoreCompanyId(boolean ignoreCompanyId) {
		this.ignoreCompanyId = ignoreCompanyId;
		return this;
	}

	public boolean isIgnoreTenantId() {
		return ignoreTenantId;
	}

	public FindParameters<T> ignoreTenantId(boolean ignoreTenantId) {
		this.ignoreTenantId = ignoreTenantId;
		return this;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public FindParameters<T> entityClass(Class<T> entityClass) {
		this.entityClass = entityClass;
		return this;
	}

	public Object getPrimaryKey() {
		return primaryKey;
	}

	public FindParameters<T> primaryKey(Object primaryKey) {
		this.primaryKey = primaryKey;
		return this;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public FindParameters<T> properties(Map<String, Object> properties) {
		this.properties = properties;
		return this;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public FindParameters<T> lockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	public Identifier<?> getIdentifier() {
		return identifier;
	}

	public FindParameters<T> identifier(Identifier<T> identifier) {
		this.identifier = identifier;
		return this;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public FindParameters<T> readOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}


	public Object getId() {
		return id;
	}

	public FindParameters<T> id(Object id) {
		this.id = id;
		return this;
	}

	public String getFieldsToForceLazy() {
		return fieldsToForceLazy;
	}

	public FindParameters<T> fieldsToForceLazy(String fieldsToForceLazy) {
		this.fieldsToForceLazy = fieldsToForceLazy;
		return this;
	}

	public String getSql() {
		return sql;
	}

	public FindParameters<T> sql(String sql) {
		this.sql = sql;
		return this;
	}

	public Object getParameters() {
		return parameters;
	}

	public FindParameters<T> parameters(Object parameters) {
		this.parameters = parameters;
		return this;
	}

	public Pageable getPageable() {
		return pageable;
	}

	public FindParameters<T> pageable(Pageable pageable) {
		this.pageable = pageable;
		return this;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public FindParameters<T> predicate(Predicate predicate) {
		this.predicate = predicate;
		return this;
	}

	public OrderSpecifier<?>[] getOrders() {
		return orders;
	}

	public FindParameters<T> setOrders(OrderSpecifier<?>[] orders) {
		this.orders = orders;
		return this;
	}

}
