/*******************************************************************************
 * 0 * Copyright 2012 Anteros Tecnologia
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
package br.com.anteros.persistence.handler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import br.com.anteros.core.utils.ObjectUtils;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.EntityCacheManager;
import br.com.anteros.persistence.metadata.EntityManaged;
import br.com.anteros.persistence.metadata.FieldEntityValue;
import br.com.anteros.persistence.metadata.annotation.type.FetchType;
import br.com.anteros.persistence.metadata.annotation.type.ScopeType;
import br.com.anteros.persistence.metadata.descriptor.DescriptionColumn;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.metadata.type.EntityStatus;
import br.com.anteros.persistence.proxy.LazyLoadFactory;
import br.com.anteros.persistence.proxy.collection.DefaultSQLList;
import br.com.anteros.persistence.proxy.collection.DefaultSQLSet;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.cache.Cache;
import br.com.anteros.persistence.session.lock.LockOptions;
import br.com.anteros.persistence.session.lock.LockScope;
import br.com.anteros.persistence.session.query.ExpressionFieldMapper;
import br.com.anteros.persistence.session.query.SQLQuery;
import br.com.anteros.persistence.session.query.SQLQueryAnalyserAlias;

/**
 * Handler para criação de Objeto baseado em SELECT com Expressões.
 * 
 */
public class EntityHandler implements ScrollableResultSetHandler {

	private static Boolean androidPresent = null;
	public static final boolean LOAD_ALL_FIELDS = true;
	protected Class<?> resultClass;
	protected transient EntityCacheManager entityCacheManager;
	protected SQLSession session;
	protected Cache transactionCache;
	protected EntityManaged entityManaged;
	protected Map<SQLQueryAnalyserAlias, Map<String, String[]>> columnAliases;
	protected Object objectToRefresh;
	protected LazyLoadFactory proxyFactory;
	protected boolean allowDuplicateObjects = false;
	protected int firstResult, maxResults;
	protected boolean readOnly = false;
	protected Map<Object, String> aliasesCache = new HashMap<Object, String>();
	private Map<String, Integer> cacheAliasIndex = new HashMap<String, Integer>();
	private boolean isIncompleteKey;
	private Set<ExpressionFieldMapper> expressionsFieldMapper;
	private LockOptions lockOptions;
	private String fieldsToForceLazy;

	public EntityHandler(LazyLoadFactory proxyFactory, Class<?> targetClass, EntityCacheManager entityCacheManager,
			Set<ExpressionFieldMapper> expressionsFieldMapper,
			Map<SQLQueryAnalyserAlias, Map<String, String[]>> columnAliases, SQLSession session, Cache transactionCache,
			boolean allowDuplicateObjects, int firstResult, int maxResults, boolean readOnly, LockOptions lockOptions,
			String fieldsToForceLazy) {
		this.resultClass = targetClass;
		this.session = session;
		this.entityCacheManager = entityCacheManager;
		this.transactionCache = transactionCache;
		this.proxyFactory = proxyFactory;
		this.columnAliases = columnAliases;
		this.allowDuplicateObjects = allowDuplicateObjects;
		this.firstResult = firstResult;
		this.readOnly = readOnly;
		this.expressionsFieldMapper = expressionsFieldMapper;
		this.lockOptions = lockOptions;
		this.fieldsToForceLazy = fieldsToForceLazy;
	}

	public EntityHandler(LazyLoadFactory proxyFactory, Class<?> targetClazz, EntityCacheManager entityCacheManager,
			SQLSession session, Cache transactionCache, boolean allowDuplicateObjects, int firstResult, int maxResults,
			String fieldsToForceLazy) {
		this(proxyFactory, targetClazz, entityCacheManager, new LinkedHashSet<ExpressionFieldMapper>(),
				new LinkedHashMap<SQLQueryAnalyserAlias, Map<String, String[]>>(), session, transactionCache,
				allowDuplicateObjects, firstResult, maxResults, false, LockOptions.NONE, fieldsToForceLazy);
	}

	/**
	 * Processa o ResultSet e cria os objetos
	 */
	public Object handle(ResultSet resultSet) throws Exception {
		Collection<Object> result = null;

		try {
			EntityHandler.validateDuplicateColumns(resultSet, resultClass);

			/*
			 * Se o ResultSet não estiver vazio
			 */
			if (resultSet.next()) {
				if (allowDuplicateObjects)
					result = new ArrayList<Object>();
				else
					result = new LinkedHashSet<Object>();
				/*
				 * Faz o loop para processar os registros do ResultSet
				 */
				do {
					readRow(resultSet, result);
				} while (resultSet.next());
			}
		} catch (SQLException ex) {
			throw new EntityHandlerException(
					"Erro processando handler para criação da classe " + resultClass.getName() + ". " + ex.getMessage(),
					ex);
		}

		return result;
	}

	protected void readRow(ResultSet resultSet, Collection<Object> result)
			throws EntityHandlerException, SQLException, Exception {
		/*
		 * Se a classe passada para o handler for uma entidade abstrata localiza no
		 * entityCache a classe correspondente ao discriminator colum
		 */
		EntityCache entityCache = getEntityCacheByResultSetRow(resultClass, resultSet);
		/*
		 * Processa a linha do resultSet montando o objeto da classe de resultado
		 */
		if (entityCache != null) {
			handleRow(resultSet, result, entityCache, LOAD_ALL_FIELDS);
		}
	}

	static void validateDuplicateColumns(ResultSet resultSet, Class<?> resultClass)
			throws SQLException, EntityHandlerException {
		Set<String> columns = new HashSet<String>();
		/*
		 * Verifica a existência de colunas duplicadas
		 */
		for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
			String columnName = resultSet.getMetaData().getColumnLabel(i);
			if (columns.contains(columnName)) {
				String msg = "Não é possível instanciar objetos a partir de um ResultSet que contenha nomes de colunas duplicadas.";
				if (resultClass != null)
					msg += " Classe " + resultClass.getName() + " coluna " + resultSet.getMetaData().getColumnName(i);
				else
					msg += " coluna " + resultSet.getMetaData().getColumnName(i);
				throw new EntityHandlerException(msg);
			}
			columns.add(resultSet.getMetaData().getColumnLabel(i).toUpperCase());
		}
	}

	@Override
	public Object[] readCurrentRow(ResultSet resultSet) throws Exception {
		List<Object> result = new ArrayList<Object>();
		readRow(resultSet, result);
		return result.toArray();
	}

	Object handleRow(ResultSet resultSet, Collection<Object> result, EntityCache entityCache, boolean loadAllFields)
			throws EntityHandlerException, SQLException, Exception {
		/*
		 * Cria o objeto
		 */
		Object createdOject = createObject(entityCache.getEntityClass(), resultSet, result);

		/*
		 * Carrega coleções e relacionamentos. Preenche a árvore do objeto considerando
		 * as estratégias configuradas em cada field com ForeignKey e Fetch
		 */
		if (loadAllFields)
			loadCollectionsRelationShipAndLob(createdOject, entityCache, resultSet);

		return createdOject;
	}

	EntityCache getEntityCacheByResultSetRow(Class<?> resultClass, ResultSet resultSet)
			throws EntityHandlerException, SQLException {
		EntityCache result = entityCacheManager.getEntityCache(resultClass);
		try {
			/*
			 * Busca a classe referente ao discriminator value
			 */
			if (result.hasDiscriminatorColumn()) {
				String dsValue = resultSet
						.getString(getAliasColumnName(result, result.getDiscriminatorColumn().getColumnName()));
				result = entityCacheManager.getEntityCache(resultClass, dsValue);
				if (result != null) {
					if (!ReflectionUtils.isExtendsClass(resultClass, result.getEntityClass())) {
						return null;
					}
				}
			}
		} catch (SQLException e) {
			throw new EntityHandlerException("Para que seja criado o objeto da classe "
					+ result.getEntityClass().getName() + " será necessário adicionar no sql a coluna "
					+ result.getDiscriminatorColumn().getColumnName()
					+ " que informe que tipo de classe será usada para instanciar o objeto. Verifique também se o discriminator value "
					+ resultSet.getString(getAliasColumnName(result, result.getDiscriminatorColumn().getColumnName()))
					+ " é o mesmo que está configurado na classe herdada.");
		}
		return result;
	}

	/**
	 * Método responsável por criar o objeto.
	 * 
	 * @param targetClass Classe para criação do objeto
	 * @param resultSet   Resultado do SQL.
	 * @param result      Objeto criado
	 * @return
	 * @throws Exception
	 */
	protected Object createObject(Class<?> targetClass, ResultSet resultSet, Collection<Object> result)
			throws Exception {
		EntityCache entityCache = entityCacheManager.getEntityCache(targetClass);

		Object mainObject = null;
		boolean createdNewObject = false;

		/*
		 * Busca chave do objeto no resultSet
		 */
		String uniqueId = getUniqueId(resultSet, entityCacheManager.getEntityCache(resultClass), null);
		/*
		 * Se foi atribuido um objeto para ser atualizado usa o objeto e não cria um
		 * novo
		 */
		if (objectToRefresh != null) {
			mainObject = objectToRefresh;
			objectToRefresh = null;
		} else {
			/*
			 * Verifica se o objeto já foi criado e está no cache
			 */
			mainObject = getObjectFromCache(entityCache, uniqueId, transactionCache);
			if (mainObject == null) {
				/*
				 * Se não encontrou no cache cria uma nova instância
				 */

				mainObject = entityCache.getConstructorAccess().newInstance();
				createdNewObject = true;
			}
		}

		/*
		 * Adiciona o objeto como sendo uma entidade gerenciada no contexto
		 */
		entityManaged = session.getPersistenceContext().addEntityManaged(mainObject, readOnly, false,
				!createdNewObject);
		entityManaged.setLockMode(lockOptions.getLockMode());

		/*
		 * Processa as expressões para gerar os fields do objeto
		 */
		for (ExpressionFieldMapper expression : expressionsFieldMapper) {
			expression.execute(session, resultSet, entityManaged, mainObject, transactionCache);
		}

		result.add(mainObject);
		/*
		 * Adiciona o objeto no Cache da sessão ou da transação para evitar buscar o
		 * objeto novamente no mesmo processamento
		 */

		addObjectToCache(entityCache, mainObject, uniqueId);

		if (entityCache.isVersioned()) {
			entityManaged.setOriginalVersion(ObjectUtils.cloneObject(ReflectionUtils.getFieldValueByName(mainObject,
					entityCache.getVersionColumn().getField().getName())));
			entityManaged.setOldVersion(entityManaged.getOriginalVersion());
			entityManaged.setCurrentVersion(entityManaged.getOriginalVersion());
		}

		return mainObject;
	}

	/**
	 * Retorna o alias da coluna de uma entidade no resultSet.
	 * 
	 * @param sourceEntityCache Entidade
	 * @param columnName        Nome da coluna
	 * @return Alias da coluna
	 */
	private String getAliasColumnName(EntityCache sourceEntityCache, String columnName) {
		StringBuilder sb = new StringBuilder();
		sb.append(sourceEntityCache.getEntityClass().getName()).append(":").append(columnName);
		String result = aliasesCache.get(sb.toString());
		if (result != null)
			return result;
		for (SQLQueryAnalyserAlias queryAnalyserAlias : columnAliases.keySet()) {
			if (queryAnalyserAlias.getEntity().equals(sourceEntityCache)
					|| (ReflectionUtils.isExtendsClass(queryAnalyserAlias.getEntity().getEntityClass(),
							sourceEntityCache.getEntityClass()))) {
				String[] alias = null;
				for (String column : columnAliases.get(queryAnalyserAlias).keySet()) {
					if (column.equalsIgnoreCase(columnName)) {
						alias = columnAliases.get(queryAnalyserAlias).get(column);
						break;
					}
				}
				result = (alias == null || alias.length == 0 ? columnName : alias[alias.length - 1]);

				sb = new StringBuilder();
				sb.append(sourceEntityCache.getEntityClass().getName()).append(":").append(columnName);
				aliasesCache.put(sb.toString(), result);
				return result;
			}
		}
		return columnName;
	}

	/**
	 * Retorna o alias da coluna de uma entidade no resultSet.
	 * 
	 * @param sourceAlias Nome do alias da tabela
	 * @param columnName  Nome da coluna
	 * @return Alias da coluna
	 */
	private String getAliasColumnName(String sourceAlias, String columnName) {
		StringBuilder sb = new StringBuilder();
		sb.append(sourceAlias).append(":").append(columnName);
		String result = aliasesCache.get(sb.toString());
		if (result != null)
			return result;
		for (SQLQueryAnalyserAlias queryAnalyserAlias : columnAliases.keySet()) {
			if (queryAnalyserAlias.getAlias().equals(sourceAlias)) {
				String[] alias = null;
				for (String column : columnAliases.get(queryAnalyserAlias).keySet()) {
					if (column.equalsIgnoreCase(columnName)) {
						alias = columnAliases.get(queryAnalyserAlias).get(column);
						break;
					}
				}
				result = (alias == null || alias.length <= 1 ? columnName : alias[1]);
				sb = new StringBuilder();
				sb.append(sourceAlias).append(":").append(columnName);
				aliasesCache.put(sb.toString(), result);
				return result;
			}
		}
		return columnName;
	}

	/**
	 * Adiciona um objeto no cache da transação SQL ou da persistência
	 * 
	 * @param entityCache  Entidade
	 * @param targetObject Objeto
	 * @param uniqueId     Chave do objeto
	 */
	private void addObjectToCache(EntityCache entityCache, Object targetObject, String uniqueId) {
		/*
		 * Adiciona o objeto no cache da sessão ou da transação para evitar buscar o
		 * objeto novamente no mesmo processamento
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(entityCache.getEntityClass().getName()).append("_").append(uniqueId);
		if ((entityCache.getCacheScope().equals(ScopeType.TRANSACTION)) && (transactionCache != null)) {
			transactionCache.put(sb.toString(), targetObject, entityCache.getMaxTimeCache());
		} else {
			session.getPersistenceContext().addObjectToCache(sb.toString(), targetObject,
					entityCache.getMaxTimeCache());
		}
	}

	/**
	 * Retorna a chave única do objeto buscando os valores no resultSet.
	 * 
	 * @param resultSet   Resultado do SQL.
	 * @param entityCache Entidade
	 * @param alias       Alias da tabela
	 * @return Chave única da entidade
	 * @throws SQLException
	 */
	private String getUniqueId(ResultSet resultSet, EntityCache entityCache, String alias) throws SQLException {
		Integer index;
		String aliasColumnName = null;

		StringBuilder uniqueIdTemp = new StringBuilder("");
		boolean appendSeparator = false;
		for (DescriptionColumn column : entityCache.getPrimaryKeyColumns()) {
			/*
			 * Busca o alias do nome da coluna
			 */
			if (alias != null) {
				aliasColumnName = getAliasColumnName(alias, column.getColumnName());
			} else {
				aliasColumnName = getAliasColumnName(entityCache, column.getColumnName());
			}

			/*
			 * Busca índice da coluna dentro do resultSet
			 */
			index = cacheAliasIndex.get(aliasColumnName);
			if (index == null) {
				index = resultSet.findColumn(aliasColumnName);
				cacheAliasIndex.put(aliasColumnName, index);
			}
			if (index < 0) {
				/*
				 * Esta exception não deverá ocorrer nunca pois as colunas estão sendo parseadas
				 * pela análise do SQL. Se isto ocorrer pode ser um erro na análise.
				 */
				throw new SQLException("NÃO ACHOU COLUNA " + column.getColumnName());
			}
			/*
			 * Concatena o valor da coluna na chave do objeto
			 */
			if (appendSeparator)
				uniqueIdTemp.append("_");

			if (column.getDescriptionField().isTemporalDateTime()) {
				uniqueIdTemp.append(resultSet.getTimestamp(index));
			} else {
				uniqueIdTemp.append(resultSet.getObject(index));
			}

			appendSeparator = true;
		}
		/*
		 * Retorna o chave única. Se for uma string "null" retorna como nula
		 */
		return (uniqueIdTemp.toString().contains("null") ? null : uniqueIdTemp.toString());
	}

	/**
	 * Busca um objeto no cache usando a Entidade e a chave para localização.
	 * 
	 * @param session           Sessão
	 * @param targetEntityCache Entidade
	 * @param uniqueId          Chave primária do objeto
	 * @param transactionCache  Cache da transação
	 * @return Objeto correspondente a entidade e chave informada ou nulo caso não
	 *         exista no cache.
	 */
	private Object getObjectFromCache(EntityCache targetEntityCache, String uniqueId, Cache transactionCache) {
		Object result = null;

		/*
		 * Se a classe for abstrata pega todas as implementações não abstratas e
		 * verifica se existe um objeto da classe + ID no Cache
		 */
		if (targetEntityCache.isAbstractClass()) {
			EntityCache[] entitiesCache = session.getEntityCacheManager().getEntitiesBySuperClass(targetEntityCache);
			for (EntityCache entityCache : entitiesCache) {
				StringBuilder sb = new StringBuilder();
				sb.append(entityCache.getEntityClass().getName()).append("_").append(uniqueId);
				result = transactionCache.get(sb.toString());
				if (result != null) {
					break;
				}
				result = session.getPersistenceContext().getObjectFromCache(sb.toString());
				if (result != null)
					break;
			}
		} else {
			/*
			 * Caso não seja abstrata localiza classe+ID no Cache
			 */
			StringBuilder sb = new StringBuilder();
			sb.append(targetEntityCache.getEntityClass().getName()).append("_").append(uniqueId);
			result = transactionCache.get(sb.toString());
			if (result == null)
				result = session.getPersistenceContext().getObjectFromCache(sb.toString());

		}
		return result;
	}

	/**
	 * Carrega as coleções, relacionamentos e Lob que não foram carregados no SQL.
	 * Adota o padrão definido no mapeamento para criar objetos ou criar proxies
	 * para os campos.
	 * 
	 * @param targetObject Objeto alvo
	 * @param entityCache  Entidade
	 * @param resultSet    Resultado do SQL.
	 * @throws Exception
	 */
	protected void loadCollectionsRelationShipAndLob(Object targetObject, EntityCache entityCache, ResultSet resultSet)
			throws Exception {
		/*
		 * Faz um loop apenas nos fields que tenham sido configuradas com ForeignKey,
		 * Lob e Fetch. Os demais campos simples já foram processados.
		 */
		for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
			/*
			 * Gera o valor do field somente se o usuário não incluiu a expressão
			 * manualmente no sql. Se ele adicionou a expressão será criado o objeto e
			 * alimentado apenas com os dados da expressão no método processExpression
			 */
			if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()
					|| descriptionField.isRelationShip() || descriptionField.isLob()) {
				Object assignedValue = descriptionField.getObjectValue(targetObject);

				/*
				 * Verifica se há necessidade de processar o field. Se o field possuir uma
				 * expressão ou já tiver um valor não é necessário processá-lo. Caso o valor
				 * seja uma chave e estiver incompleta, irá buscar o objeto novamente.
				 */
				if (checkNeedsProcessDescriptionField(entityCache, descriptionField, assignedValue)) {
					EntityCache targetEntityCache = getTargetEntityCacheByDescriptionField(entityCache,
							descriptionField);

					/*
					 * Busca os valores da chave do objeto
					 */
					Map<String, Object> columnKeyValue = getColumnKeyValue(targetObject, entityCache, resultSet,
							descriptionField);

					/*
					 * Se não foi possível obter a chave do field no resultSet é porque o usuário
					 * não quer que este field seja carregado.
					 */
					if (columnKeyValue != null) {

						/*
						 * Se a estratégia for EAGER busca os dados do field, se for LAZY cria um proxy.
						 */
						FetchType fetchType = descriptionField.getFetchType();
						/*
						 * Se estiver no android e não for uma coleção então assume EAGER pois no
						 * Android não é possível usar ferramentas de manipulação de bytecode e assim
						 * não é possível implementar lazy em alguns casos.
						 */
						if (EntityHandler.androidIsPresent() && !(descriptionField.isAnyCollectionOrMap()))
							fetchType = FetchType.EAGER;

						/*
						 * Se for um campo LOB assume o que estiver configurado.
						 */
						if (descriptionField.isLob())
							fetchType = descriptionField.getFetchType();

						CheckForceResult checkForceResult = this
								.checkForceFetchTypeByField(descriptionField.getField().getName(), fetchType);

						/*
						 * Somente busca relacionamentos pois LOB já é criado junto com os demais
						 * campos. Somente cria o LOB se for LAZY. Ai cria como um proxy.
						 */
						if (checkForceResult.getFetchType() == FetchType.EAGER) {
							if (!descriptionField.isLob()) {
								Object result = null;
								/*
								 * Se a chave estiver incompleta, carrega o objeto completo novamente.
								 */
								if (isIncompleteKey) {
									StringBuilder sb = new StringBuilder("");
									sb.append(targetEntityCache.getEntityClass().getName());
									for (String key : columnKeyValue.keySet()) {
										if (!"".equals(sb.toString())) {
											sb.append("_");
										}
										sb.append(columnKeyValue.get(key));
									}
									/*
									 * Remove o objeto do cache
									 */
									String uniqueId = sb.toString();
									transactionCache.remove(uniqueId);
									/*
									 * Cria a query e busca novamente o objeto completo
									 */
									SQLQuery query = session.createQuery("");
									query.allowDuplicateObjects(true);
									query.setFieldsToForceLazy(checkForceResult.getContinueFieldsToForceLazy());
									/*
									 * Extende o lock para as coleções e tabelas de junção
									 */
									if ((descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable())
											&& (lockOptions.getLockScope() == LockScope.EXTENDED)) {
										LockOptions lockOpts = LockOptions.copy(lockOptions, new LockOptions());
										lockOpts.clearAliasesToLock();
										query.setLockOptions(lockOpts);
									} else
										query.setLockOptions(LockOptions.NONE);

									result = query.loadData(targetEntityCache, targetObject, descriptionField,
											columnKeyValue, transactionCache);

									EntityCache fieldentEntityCache = session.getEntityCacheManager()
											.getEntityCache(descriptionField.getFieldClass());
									fieldentEntityCache.setPrimaryKeyValue(result, assignedValue);
									result = assignedValue;
								} else {
									/*
									 * Busca o objeto que será atribuido ao campo do objeto alvo
									 */
									SQLQuery query = session.createQuery("");
									query.allowDuplicateObjects(true);
									query.setFieldsToForceLazy(checkForceResult.getContinueFieldsToForceLazy());

									/*
									 * Extende o lock para as coleções e tabelas de junção
									 */
									if ((descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable())
											&& (lockOptions.getLockScope() == LockScope.EXTENDED)) {
										LockOptions lockOpts = LockOptions.copy(lockOptions, new LockOptions());
										lockOpts.clearAliasesToLock();
										query.setLockOptions(lockOptions);
									} else
										query.setLockOptions(LockOptions.NONE);

									query.setReadOnly(readOnly);
									result = query.loadData(targetEntityCache, targetObject, descriptionField,
											columnKeyValue, transactionCache);
								}

								descriptionField.setObjectValue(targetObject, result);
								/*
								 * Se a consulta não for somente leitura armazena os valores dos campos.
								 */
								if (entityManaged.getStatus() != EntityStatus.READ_ONLY) {
									FieldEntityValue fieldEntityValue = descriptionField.getFieldEntityValue(session,
											targetObject);
									entityManaged.addOriginalValue(fieldEntityValue);
									entityManaged.addLastValue(fieldEntityValue);
									entityManaged.getFieldsForUpdate().add(descriptionField.getField().getName());
								}
							}
						} else {
							/*
							 * Se for LAZY cria apenas o proxy.
							 */
							LockOptions lockOpts = LockOptions.copy(lockOptions, new LockOptions());
							lockOpts.clearAliasesToLock();
							Object newObject = proxyFactory.createProxy(session, targetObject, descriptionField,
									targetEntityCache, columnKeyValue, transactionCache,
									(lockOptions.getLockScope() == LockScope.EXTENDED ? lockOpts : LockOptions.NONE));
							descriptionField.getField().set(targetObject, newObject);

							if (entityManaged.getStatus() != EntityStatus.READ_ONLY) {
								FieldEntityValue value = descriptionField.getFieldEntityValue(session, targetObject);
								entityManaged.addOriginalValue(value);
								entityManaged.addLastValue(value);
							}
						}
					}
				}
			}
		}
	}

	private CheckForceResult checkForceFetchTypeByField(String fieldName, FetchType current) {
		if (StringUtils.isEmpty(fieldsToForceLazy)) {
			return new CheckForceResult(current, null);
		}
		String[] spNames = fieldsToForceLazy.split(",");
		String continueForceFieldsToLazy = "";
		FetchType result = current;
		boolean appendContinue = false;
		for (String name : spNames) {
			if (appendContinue) {
				continueForceFieldsToLazy += ",";
			}
			name = StringUtils.trimAllWhitespace(name);
			String[] spSub = name.split("\\.");
			if (fieldName.equals(spSub[0])) {
				result = FetchType.EAGER;
				if (spSub.length > 1) {
					boolean appendDelimiter = false;
					for (int i = 1; i < spSub.length; i++) {
						if (appendDelimiter) {
							continueForceFieldsToLazy += ".";
						}
						continueForceFieldsToLazy += spSub[i];
						appendDelimiter = true;
					}

					appendContinue = true;
				}

			}
		}
		if (StringUtils.isNotBlank(continueForceFieldsToLazy)) {
			return new CheckForceResult(result, continueForceFieldsToLazy);
		} else {
			return new CheckForceResult(result, null);
		}
	}

	/**
	 * Verifica se um determinado campo em uma entidade tem necessidade de ser
	 * processado para criar o objeto. No caso de chaves estrangeiras que não foram
	 * trazidas no SQL e objetos com chave parcial devem ser considerados como
	 * necessário criar o objeto do campo novamente.
	 * 
	 * @param entityCache      Entidade
	 * @param descriptionField Campo
	 * @param assignedValue    Valor atribuído ao campo atual
	 * @return Verdadeiro se há necessidade de criar o objeto e atribuir ao campo
	 * @throws Exception
	 */
	private boolean checkNeedsProcessDescriptionField(EntityCache entityCache, DescriptionField descriptionField,
			Object assignedValue) throws Exception {
		Boolean process = (assignedValue == null);
		Boolean existsExpression = existsExpressionForProcessing(entityCache, descriptionField.getField().getName());

		isIncompleteKey = false;

		if (assignedValue != null) {
			/*
			 * Processa se for uma collection que não seja herança de DefaultSQLList our
			 * DefaultSQLSet
			 */
			if (descriptionField.isAnyCollectionOrMap()
					&& ((assignedValue instanceof DefaultSQLList) || (assignedValue instanceof DefaultSQLSet)))
				process = false;
			else if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()) {
				process = true;
			} else if (descriptionField.isRelationShip()) {
				/*
				 * Processa se a chave estiver incompleta
				 */
				EntityCache fieldentEntityCache = session.getEntityCacheManager()
						.getEntityCache(descriptionField.getFieldClass());
				if (fieldentEntityCache != null) {
					/*
					 * Verifica se a chave do objeto está incompleta. Isto ocorre em casos com chave
					 * composta.
					 */
					isIncompleteKey = fieldentEntityCache.isIncompletePrimaryKeyValue(assignedValue);
					if (!isIncompleteKey) {
						process = false;
					} else {
						process = true;
						existsExpression = false;
					}
				}
				if (!process){
					CheckForceResult checkForceResult = checkForceFetchTypeByField(descriptionField.getField().getName(), FetchType.LAZY);
					if (FetchType.EAGER.equals(checkForceResult.getFetchType())){
						process = true;
						existsExpression = false;
					}
				}
			}
		}

		return process && !existsExpression;
	}

	/**
	 * Retorna a entidade destino de um campo em uma determinada entidade
	 * 
	 * @param entityCache      Entidade de origem
	 * @param descriptionField Campo da entidade origem
	 * @return Entidade destino
	 * @throws EntityHandlerException
	 */
	private EntityCache getTargetEntityCacheByDescriptionField(EntityCache entityCache,
			DescriptionField descriptionField) throws EntityHandlerException {
		EntityCache targetEntityCache = null;
		if (descriptionField.isLob()) {
			targetEntityCache = entityCache;
		} else {
			/*
			 * Busca a EntityCache da classe destino do field
			 */
			if (!descriptionField.isElementCollection() && !descriptionField.isJoinTable()) {
				targetEntityCache = entityCacheManager
						.getEntityCache(descriptionField.getTargetEntity().getEntityClass());

				/*
				 * Não encontrou a classe no dicionário gera uma exceção avisando.
				 */
				if (targetEntityCache == null)
					throw new EntityHandlerException("Para que seja criado o objeto da classe "
							+ descriptionField.getTargetEntity().getEntityClass().getName()
							+ " é preciso adicionar a Entity relacionada à classe na configuração da sessão. "
							+ (descriptionField.getDescriptionColumns() == null ? ""
									: "Coluna(s) " + descriptionField));
			}
		}
		return targetEntityCache;
	}

	/**
	 * Retorna os valores da chave de um determinado campo da entidade no resultSet.
	 * 
	 * @param targetObject     Objeto alvo
	 * @param entityCache      Entidade
	 * @param resultSet        Resultado do SQL
	 * @param descriptionField Campo da entidade
	 * @return Mapa com os valores da chave
	 * @throws EntityHandlerException
	 */
	private Map<String, Object> getColumnKeyValue(Object targetObject, EntityCache entityCache, ResultSet resultSet,
			DescriptionField descriptionField) throws EntityHandlerException {

		/*
		 * Se o DescriptionField for um FK guarda o valor da coluna. Apenas monta o
		 * objeto da chave estrangeira caso o usuário tenha incluido as colunas da chave
		 * no SQL.
		 */
		String columnName = "";
		try {
			Map<String, Object> columnKeyValue = new TreeMap<String, Object>();
			/*
			 * Se o campo possuí uma ou mais colunas e não é um Lob.
			 */
			if (descriptionField.hasDescriptionColumn() && !(descriptionField.isLob())) {
				for (DescriptionColumn descriptionColumn : descriptionField.getDescriptionColumns()) {
					/*
					 * Se o campo é uma chave estrangeira e não é uma inversedJoinColumn
					 */
					if (descriptionColumn.isForeignKey() && !descriptionColumn.isInversedJoinColumn()) {
						columnName = descriptionColumn.getColumnName();
						String aliasColumnName = getAliasColumnName(entityCache, descriptionColumn.getColumnName());
						if (descriptionField.isJoinTable()
								&& StringUtils.isNotEmpty(descriptionColumn.getReferencedColumnName())) {
							aliasColumnName = getAliasColumnName(entityCache,
									descriptionColumn.getReferencedColumnName());
						}

						int index = resultSet.findColumn(aliasColumnName);
						if (index < 0) {
							return null;
						}
						/*
						 * Adiciona o valor no mapa para retornar
						 */
						columnKeyValue.put(descriptionColumn.getReferencedColumnName(),
								resultSet.getObject(aliasColumnName));
					}
				}
			} else
				/*
				 * Se for um campo anotado com @Fetch guarda a PK do pai
				 */
				columnKeyValue = session.getIdentifier(targetObject).getColumns();
			return columnKeyValue;
		} catch (Exception ex) {
			/*
			 * Se não for um DescriptionField do tipo COLLECTION_TABLE, continua iteracao do
			 * proximo field.
			 */
			if (!descriptionField.isElementCollection() && !descriptionField.isJoinTable() && isIncompleteKey) {
				throw new EntityHandlerException("Para que seja criado o objeto do tipo "
						+ descriptionField.getTargetEntity().getEntityClass().getSimpleName()
						+ " que será atribuído ao campo " + descriptionField.getField().getName() + " na classe "
						+ entityCache.getEntityClass() + " é preciso adicionar a coluna " + columnName + " da tabela "
						+ entityCache.getTableName() + " no sql. Erro " + ex.getMessage());
			}
		}
		return null;
	}

	/**
	 * Retorna se existe uma expressão para ser processada para um determinado campo
	 * na entidade.
	 * 
	 * @param entityCache Entidade
	 * @param fieldName   Nome do campo
	 * @return Verdadeiro se encontrou a expressão associada ao campo da entidade
	 */
	private boolean existsExpressionForProcessing(EntityCache entityCache, String fieldName) {
		StringBuilder sb = new StringBuilder();
		sb.append(entityCache.getEntityClass().getName()).append(":").append(fieldName);
		String result = aliasesCache.get(sb.toString());
		if (result != null)
			return true;

		for (ExpressionFieldMapper expression : expressionsFieldMapper) {
			if (expression.getDescriptionField() != null
					&& expression.getDescriptionField().getField().getName().equalsIgnoreCase(fieldName)) {
				/*
				 * Armazena no cache para acelerar a próxima busca
				 */
				aliasesCache.put(sb.toString(), "S");
				return true;
			}
		}
		return false;
	}

	/**
	 * Atribui um objeto para ser atualizado. Se for atribuido um objeto não será
	 * criado uma nova instância para a classe de resultado.
	 * 
	 * @param objectToRefresh Objeto a ser atualizado
	 * @throws EntityHandlerException
	 */
	public void setObjectToRefresh(Object objectToRefresh) throws EntityHandlerException {
		if (objectToRefresh == null)
			return;

		if (!(objectToRefresh.getClass().equals(resultClass))) {
			throw new EntityHandlerException("Classe do objeto para refresh " + objectToRefresh.getClass()
					+ " difere da classe de resultado " + resultClass);
		}
		this.objectToRefresh = objectToRefresh;
	}

	public static boolean androidIsPresent() {
		if (androidPresent == null) {
			try {
				Class.forName("br.com.anteros.android.persistence.session.AndroidSQLSession");
				androidPresent = true;
			} catch (ClassNotFoundException e) {
				androidPresent = false;
			}
		}
		return androidPresent;
	}

}
