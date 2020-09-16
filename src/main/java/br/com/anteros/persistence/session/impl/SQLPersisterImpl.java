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

package br.com.anteros.persistence.session.impl;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.tika.Tika;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.core.utils.MimeTypes;
import br.com.anteros.core.utils.PNGUtils;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.EntityManaged;
import br.com.anteros.persistence.metadata.FieldEntityValue;
import br.com.anteros.persistence.metadata.annotation.EventType;
import br.com.anteros.persistence.metadata.annotation.type.CascadeType;
import br.com.anteros.persistence.metadata.annotation.type.TemporalType;
import br.com.anteros.persistence.metadata.descriptor.DescriptionColumn;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.metadata.descriptor.type.SQLStatementType;
import br.com.anteros.persistence.metadata.identifier.Identifier;
import br.com.anteros.persistence.metadata.identifier.IdentifierGenerator;
import br.com.anteros.persistence.metadata.identifier.IdentifierGeneratorFactory;
import br.com.anteros.persistence.metadata.identifier.IdentifierPostInsert;
import br.com.anteros.persistence.metadata.type.EntityStatus;
import br.com.anteros.persistence.parameter.ExternalFileNamedParameter;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.parameter.VersionNamedParameter;
import br.com.anteros.persistence.session.FindParameters;
import br.com.anteros.persistence.session.SQLPersister;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.SQLSessionValidator;
import br.com.anteros.persistence.session.exception.SQLSessionException;
import br.com.anteros.persistence.session.lock.LockMode;
import br.com.anteros.persistence.session.lock.LockOptions;
import br.com.anteros.persistence.session.lock.OptimisticLockException;
import br.com.anteros.persistence.sql.command.CommandSQL;
import br.com.anteros.persistence.sql.command.Delete;
import br.com.anteros.persistence.sql.command.DeleteCommandSQL;
import br.com.anteros.persistence.sql.command.ExternalFileRemoveCommand;
import br.com.anteros.persistence.sql.command.ExternalFileSaveCommand;
import br.com.anteros.persistence.sql.command.Insert;
import br.com.anteros.persistence.sql.command.InsertCommandSQL;
import br.com.anteros.persistence.sql.command.PersisterCommand;
import br.com.anteros.persistence.sql.command.Select;
import br.com.anteros.persistence.sql.command.Update;
import br.com.anteros.persistence.sql.command.UpdateCommandSQL;
import br.com.anteros.persistence.util.AnterosBeanValidationHelper;
import br.com.anteros.persistence.validation.version.Versioning;

public class SQLPersisterImpl implements SQLPersister {

	protected static final String PREVIEW = "preview#";
	protected static final String DOWNLOAD = "download#";

	private Logger LOG = LoggerProvider.getInstance().getLogger(SQLPersister.class);

	private SQLSession session;

	private SQLSessionValidator validator;

	private int currentBatchSize = 0;

	private boolean beanValidationPresent;

	public SQLPersisterImpl() {
		this.beanValidationPresent = AnterosBeanValidationHelper.isBeanValidationPresent();
	}

	private Set<Long> objectsInSavingProcess = new HashSet<Long>();
	private Set<Object> objectsInSaving = new LinkedHashSet<Object>();

	@Override
	public SQLSessionValidator getValidator() {
		if (beanValidationPresent) {
			if (validator == null)
				validator = new SQLSessionValidatorImpl();
		}
		return validator;
	}

	@Override
	public Object save(SQLSession session, Object object, int batchSize) throws Exception {
		this.currentBatchSize = batchSize;
		save(session, object);
		return object;
	}

	public Object save(SQLSession session, Object object) throws Exception {
		this.session = session;
		Object result = null;
		try {
			MergeResult mergeResult = mergeIfNeed(object);

			if (getValidator() != null && session.validationIsActive()) {
				session.notifyListeners(EventType.PreValidate, null, mergeResult.getNewObject());
				getValidator().validateBean(mergeResult.getNewObject());
				session.notifyListeners(EventType.PostValidate, null, mergeResult.getNewObject());
			}

			result = save(mergeResult.getOldObject(), mergeResult.getNewObject(), null);
		} finally {
			objectsInSavingProcess.clear();
			for (Object ob : objectsInSaving) {
				EntityManaged entityManaged = session.getPersistenceContext().addEntityManaged(ob, false, false, true);
				entityManaged.updateLastValues(session, ob);
			}
			objectsInSaving.clear();
		}
		return result;
	}

	@Override
	public Object save(SQLSession session, Object object, Class<?>[] groups) throws Exception {
		this.session = session;
		Object result = null;
		try {
			MergeResult mergeResult = mergeIfNeed(object);

			if (getValidator() != null && session.validationIsActive()) {
				session.notifyListeners(EventType.PreValidate, null, mergeResult.getNewObject());
				getValidator().validateBean(mergeResult.getNewObject(), groups);
				session.notifyListeners(EventType.PostValidate, null, mergeResult.getNewObject());
			}

			result = save(mergeResult.getOldObject(), mergeResult.getNewObject(), null);
		} finally {
			objectsInSavingProcess.clear();
			for (Object ob : objectsInSaving) {
				EntityManaged entityManaged = session.getPersistenceContext().addEntityManaged(ob, false, false, true);
				entityManaged.updateLastValues(session, ob);
			}
			objectsInSaving.clear();
		}
		return result;
	}

	private MergeResult mergeIfNeed(Object newEntity) throws Exception {
		EntityCache entityCache = session.getEntityCacheManager().getEntityCache(newEntity.getClass());

		if (entityCache == null) {
			return MergeResult.of(null, newEntity);
		}

		if (session.getIdentifier(newEntity).hasIdentifier()) {
			if (existsRecordInDatabaseTable(entityCache.getTableName(),
					session.getIdentifier(newEntity).getDatabaseColumns())) {
				
				Object oldEntity = session
						.find(new FindParameters().identifier(session.getIdentifier(newEntity)).readOnly(true).lockOptions(
								entityCache.isVersioned() ? LockOptions.OPTIMISTIC_FORCE_INCREMENT : LockOptions.NONE));
				
				Object actualEntity = session
						.find(new FindParameters().identifier(session.getIdentifier(newEntity)).lockOptions(
								entityCache.isVersioned() ? LockOptions.OPTIMISTIC_FORCE_INCREMENT : LockOptions.NONE));
				entityCache.mergeValues(actualEntity, newEntity);
				
				EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(actualEntity);
				

				return MergeResult.of(oldEntity, actualEntity);
			}
		}
		return MergeResult.of(null, newEntity);
	}

	protected Object save(SQLSession session, Object object, List<PersisterCommand> stackCommands) throws Exception {
		if (getValidator() != null && session.validationIsActive())
			getValidator().validateBean(object);
		this.session = session;
		MergeResult mergeResult = mergeIfNeed(object);

		return save(mergeResult.getOldObject(), mergeResult.getNewObject(), stackCommands);
	}

	public void remove(SQLSession session, Object newObject) throws Exception {
		this.session = session;
		EntityCache entityCache = session.getEntityCacheManager().getEntityCache(newObject.getClass());

		if (entityCache == null) {
			throw new SQLSessionException("Objeto não pode ser removido pois a classe " + newObject.getClass().getName()
					+ " não foi localizada no Cache de Entidades.");
		}

		EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(newObject);
		if (entityManaged == null) {
			if (session.getIdentifier(newObject).hasIdentifier())
				throw new SQLSessionException(
						"Objeto não pode ser removido pois o mesmo não foi recuperado pela Sessão ou não possuí Chave(ID) definida."
								+ newObject);
		}
		if ((entityManaged != null) && (EntityStatus.DELETED.equals(entityManaged.getStatus())))
			session.flush();
		
		session.notifyListeners(EventType.PreRemove, null, newObject);

		List<PersisterCommand> commands = getCommandsToDeleteObject(newObject, entityCache);
		if (commands != null) {
			entityManaged.setStatus(EntityStatus.DELETED);
			session.getCommandQueue().addAll(commands);
		}
	}

	public void remove(SQLSession session, Object[] objects) throws Exception {
		for (Object obj : objects)
			remove(session, obj);
	}

	protected Object save(Object oldObject, Object newObject, List<PersisterCommand> stackCommands) throws Exception {
		Long hashCodeObject = new Long(System.identityHashCode(newObject));
		if (!objectsInSavingProcess.contains(hashCodeObject)) {
			objectsInSavingProcess.add(hashCodeObject);
			objectsInSaving.add(newObject);
			EntityCache entityCache = session.getEntityCacheManager().getEntityCache(newObject.getClass());
			if (entityCache == null)
				throw new SQLSessionException("Objeto não pode ser salvo pois a classe "
						+ newObject.getClass().getName() + " não foi localizada no cache de Entidades.");

			EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(newObject);

			/*
			 * Se o objeto estiver sendo gerenciado então faz o update no objeto
			 */
			if (entityManaged != null) {
				/*
				 * Se o objeto foi selecionado parcialmente não permite alteração
				 */
				if (entityManaged.getStatus().equals(EntityStatus.READ_ONLY))
					throw new SQLSessionException("Objeto " + newObject.getClass().getSuperclass() + " ID "
							+ session.getIdentifier(newObject).getDatabaseColumns()
							+ " não pode ser salvo pois é somente para leitura.");
				session.notifyListeners(EventType.PreUpdate, oldObject, newObject);
				saveUsingStatement(entityCache, SQLStatementType.UPDATE, oldObject, newObject, stackCommands);
			} else {
				/*
				 * Se o objeto não estiver sendo gerenciado porém já possuí um ID será preciso
				 * verificar se o ID já existe no banco de dados
				 */
				if (session.getIdentifier(newObject).hasIdentifier()) {
					if (!existsRecordInDatabaseTable(entityCache.getTableName(),
							session.getIdentifier(newObject).getDatabaseColumns())) {
						session.notifyListeners(EventType.PrePersist, null, newObject);
						saveUsingStatement(entityCache, SQLStatementType.INSERT, oldObject, newObject, stackCommands);
					} else {
						session.notifyListeners(EventType.PreUpdate, oldObject, newObject);
						saveUsingStatement(entityCache, SQLStatementType.UPDATE, oldObject, newObject, stackCommands);
					}
				} else {
					session.notifyListeners(EventType.PrePersist, null, newObject);
					saveUsingStatement(entityCache, SQLStatementType.INSERT, oldObject, newObject, stackCommands);
				}
			}
		}
		return newObject;
	}

	protected boolean existsRecordInDatabaseTable(String tableName, Map<String, Object> identifier) throws Exception {
		HashSet<String> tbs = new HashSet<String>();
		tbs.add(tableName);
		session.forceFlush(tbs);

		Select select = new Select(session.getDialect());
		select.addColumn("count(*)", "numRows");
		select.addTableName(tableName);
		ArrayList<NamedParameter> params = new ArrayList<NamedParameter>();
		boolean appendOperator = false;
		for (String key : identifier.keySet()) {
			Object value = identifier.get(key);
			if (appendOperator)
				select.and();
			select.addCondition(key, "=", ":P" + key);
			if (value instanceof Date) {
				params.add(new NamedParameter("P" + key, value, TemporalType.DATE));
			} else {
				params.add(new NamedParameter("P" + key, value));
			}
			appendOperator = true;
		}

		ResultSet rs = session.createQuery(select.toStatementString(), params.toArray(new NamedParameter[] {}))
				.executeQuery();
		if (rs.next()) {
			boolean result = rs.getInt("numRows") > 0;
			rs.getStatement().close();
			rs.close();
			return result;
		}
		rs.close();
		return false;
	}

	protected void saveUsingStatement(EntityCache entityCache, SQLStatementType statement, Object oldObject,
			Object newObject, List<PersisterCommand> stackCommands) throws Exception {
		List<PersisterCommand> commands = null;
		if (statement.equals(SQLStatementType.INSERT))
			commands = getCommandsToInsertObject(newObject, entityCache, stackCommands);
		else if (statement.equals(SQLStatementType.UPDATE))
			commands = getCommandsToUpdateObject(oldObject, newObject, entityCache);
		else if (statement.equals(SQLStatementType.DELETE))
			commands = getCommandsToDeleteObject(newObject, entityCache);

		if (commands != null) {
			List<PersisterCommand> commandsToAdd = new ArrayList<PersisterCommand>();
			for (PersisterCommand command : commands) {
				if (command instanceof InsertCommandSQL) {
					commandsToAdd.add(command);
				} else {
					commandsToAdd.add(command);
					command.setEntityManaged();
				}
			}
			if (stackCommands != null)
				stackCommands.addAll(commandsToAdd);
			else
				session.getCommandQueue().addAll(commandsToAdd);
		}
	}

	protected List<PersisterCommand> getCommandsToInsertObject(Object newObject, EntityCache entityCache,
			List<PersisterCommand> stackCommands) throws Exception {
		List<PersisterCommand> result = new ArrayList<PersisterCommand>();
		try {
			LinkedHashMap<String, NamedParameter> namedParameters = new LinkedHashMap<String, NamedParameter>();
			IdentifierResult identifierResult = insertParametersKey(newObject, entityCache, namedParameters);
			insertRelationships(newObject, entityCache, result);
			insertCommonsParameters(newObject, entityCache, namedParameters);
			insertObject(newObject, entityCache, result, namedParameters, identifierResult.identifierPostInsert,
					identifierResult.identifyColumn, stackCommands);
			insertChildrenCollections(newObject, entityCache, result, identifierResult.identifierPostInsert,
					identifierResult.identifyColumn, session.getIdentifier(newObject).getDatabaseColumns());
		} finally {
			session.getCacheIdentifier().remove(newObject);
		}
		return result;
	}

	protected void insertObject(Object newObject, EntityCache entityCache, List<PersisterCommand> result,
			LinkedHashMap<String, NamedParameter> namedParameters, IdentifierPostInsert identifierPostInsert,
			DescriptionColumn identifyColumn, List<PersisterCommand> stackCommands) throws Exception {
		/*
		 * Salva o objeto
		 */
		if (entityCache.hasDiscriminatorColumn())
			namedParameters.put(entityCache.getDiscriminatorColumn().getColumnName(), new NamedParameter(
					entityCache.getDiscriminatorColumn().getColumnName(), entityCache.getDiscriminatorValue()));

		InsertCommandSQL insertCommandSQL = new InsertCommandSQL(session,
				generateSql(entityCache.getTableName(), SQLStatementType.INSERT,
						NamedParameter.convertToList(namedParameters.values())),
				NamedParameter.convertToList(namedParameters.values()), newObject, entityCache,
				entityCache.getTableName(), session.getShowSql(), identifierPostInsert, identifyColumn,
				entityCache.getDescriptionSqlByType(SQLStatementType.INSERT), executeInBatchMode());

		if (insertCommandSQL.getIdentifierPostInsert() != null) {
			if (stackCommands != null) {
				stackCommands.add(insertCommandSQL);
				session.getCommandQueue().addAll(stackCommands);
				stackCommands.clear();
				session.flush();
			} else {
				try {
					insertCommandSQL.execute();
				} catch (SQLException ex) {
					if (insertCommandSQL instanceof CommandSQL)
						throw session.getDialect().convertSQLException(ex, "Erro enviando comando sql.",
								((CommandSQL) insertCommandSQL).getSql());
					else {
						throw ex;
					}
				}
			}
		} else
			result.add(insertCommandSQL);
	}

	protected boolean executeInBatchMode() {
		return session.getBatchSize() > 0 || currentBatchSize > 0;
	}

	protected void insertRelationships(Object targetObject, EntityCache entityCache, List<PersisterCommand> result)
			throws Exception {
		Object fieldValue;
		/*
		 * Salva os relacionamentos FK
		 */
		for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
			/*
			 * Se for um RelationShip
			 */
			fieldValue = descriptionField.getObjectValue(targetObject);
			if (fieldValue != null) {
				if (descriptionField.isRelationShip())
					saveRelationShip(fieldValue, descriptionField, result);
			}
		}
	}

	protected void insertChildrenCollections(Object targetObject, EntityCache entityCache,
			List<PersisterCommand> result, IdentifierPostInsert identifierPostInsert, DescriptionColumn identifyColumn,
			Map<String, Object> primaryKeyOwner) throws Exception, IllegalAccessException {
		Object fieldValue;
		/*
		 * Salva as coleções filhas do objeto
		 */
		for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
			/*
			 * Se for uma coleção ou joinTable (muitos para muitos)
			 */
			fieldValue = descriptionField.getObjectValue(targetObject);
			if (fieldValue != null) {
				if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()) {
					if (descriptionField.isMapTable()) {
						if (fieldValue instanceof Map) {
							for (Object key : ((Map<?, ?>) fieldValue).keySet()) {
								Object value = ((Map<?, ?>) fieldValue).get(key);
								result.addAll(getSQLMapTableCommands(key, value, SQLStatementType.INSERT,
										descriptionField, identifyColumn, identifierPostInsert, primaryKeyOwner));
							}
						}
					} else if (descriptionField.isCollectionTable()) {
						for (Object value : ((Collection<?>) fieldValue))
							result.addAll(getSQLCollectionTableCommands(value, SQLStatementType.INSERT,
									descriptionField, identifyColumn, identifierPostInsert, primaryKeyOwner));
					} else if (descriptionField.isJoinTable()) {
						for (Object value : ((Collection<?>) fieldValue))
							result.addAll(getSQLJoinTableCommands(value, SQLStatementType.INSERT, descriptionField,
									identifyColumn, identifierPostInsert, primaryKeyOwner));
					} else if (descriptionField.isCollectionEntity()) {
						if (fieldValue instanceof Collection) {
							for (Object value : ((Collection<?>) fieldValue)) {
								if (value != null) {
									if ((descriptionField.getMappedBy() != null)
											&& (!"".equals(descriptionField.getMappedBy()))) {
										Field mappedByField = descriptionField.getTargetEntity()
												.getDescriptionField(descriptionField.getMappedBy()).getField();
										if (mappedByField.get(value) == null)
											mappedByField.set(value, targetObject);
									}
									if ((Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
											|| Arrays.asList(descriptionField.getCascadeTypes())
													.contains(CascadeType.SAVE))) {
										MergeResult mergeResult = mergeIfNeed(value);
										save(mergeResult.getOldObject(), mergeResult.getNewObject(), result);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	protected IdentifierResult insertParametersKey(Object targetObject, EntityCache entityCache,
			LinkedHashMap<String, NamedParameter> namedParameters) throws Exception {
		IdentifierPostInsert identifierPostInsert = null;
		DescriptionColumn identifyColumn = null;
		/*
		 * Gera os campos da chave do insert
		 */
		NamedParameter namedParameterField = null;
		for (DescriptionField fieldModified : entityCache.getDescriptionFields()) {
			if (!fieldModified.isAnyCollectionOrMap() && !fieldModified.isVersioned() && !fieldModified.isJoinTable()) {
				for (DescriptionColumn columnModified : fieldModified.getDescriptionColumns()) {
					if (columnModified.isPrimaryKey()) {
						namedParameterField = fieldModified.getNamedParameterFromDatabaseObjectValue(session,
								targetObject, columnModified);
						/*
						 * Se a coluna possuí um Generator e o valor ainda não foi gerado
						 */
						if ((columnModified.hasGenerator()) && (namedParameterField.getValue() == null)) {
							IdentifierGenerator identifierGenerator = IdentifierGeneratorFactory
									.createGenerator(session, columnModified);
							/*
							 * Se o Generator for IDENTIY será gerado após a execução do INSERT pelo
							 * getGeneratedKeys nativo JDBC
							 */
							if (identifierGenerator instanceof IdentifierPostInsert) {
								identifierPostInsert = (IdentifierPostInsert) identifierGenerator;
								identifyColumn = columnModified;
								Map<DescriptionColumn, IdentifierPostInsert> identifierTemp = new LinkedHashMap<DescriptionColumn, IdentifierPostInsert>();
								identifierTemp.put(identifyColumn, identifierPostInsert);
								session.getCacheIdentifier().put(targetObject, identifierTemp);
							} else {
								/*
								 * Gera o próximo número da sequência
								 */
								namedParameterField.setValue(identifierGenerator.generate());
								/*
								 * Seta o valor no objeto
								 */
								ReflectionUtils.setObjectValueByFieldName(targetObject,
										columnModified.getField().getName(), namedParameterField.getValue());
							}
						}
						if ((namedParameterField.getValue() == null) && (identifierPostInsert == null))
							throw new SQLSessionException("Coluna chave " + columnModified.getColumnName()
									+ " da tabela " + entityCache.getTableName()
									+ " deve ter um valor. Verifique se o Campo " + columnModified.getField().getName()
									+ " da classe " + entityCache.getEntityClass()
									+ " possui anotação para geração do identificador.");
						namedParameters.put(columnModified.getColumnName(), namedParameterField);
					}
				}
			}
		}
		return new IdentifierResult(identifyColumn, identifierPostInsert);
	}

	protected void insertCommonsParameters(Object targetObject, EntityCache entityCache,
			LinkedHashMap<String, NamedParameter> namedParameters) throws Exception {
		/*
		 * Gera os demais campos do insert
		 */
		for (DescriptionField fieldModified : entityCache.getDescriptionFields()) {
			if (!fieldModified.isAnyCollectionOrMap() && !fieldModified.isVersioned() && !fieldModified.isJoinTable()) {
				if ((fieldModified.getObjectValue(targetObject) != null) || (fieldModified.hasGenerator())) {
					for (DescriptionColumn columnModified : fieldModified.getDescriptionColumns()) {
						if (!columnModified.isPrimaryKey()) {
							if (!namedParameters.containsKey(columnModified.getColumnName())) {
								if (fieldModified.isLob() && session.getExternalFileManager() != null) {
									Object columnValue = fieldModified.getObjectValue(targetObject);
									if (columnValue == null) {
										namedParameters.put(columnModified.getColumnName(),
												fieldModified.getNamedParameterFromDatabaseObjectValue(session,
														targetObject, columnModified));
									} else {
										Tika tika = new Tika();
										String mimeType = tika.detect((byte[]) columnValue);
										if (MimeTypes.MIME_TEXT_PLAIN.equals(mimeType)
												|| MimeTypes.MIME_TEXT_HTML.equals(mimeType)
												|| MimeTypes.MIME_APPLICATION_OCTET_STREAM.equals(mimeType)) {
											namedParameters.put(columnModified.getColumnName(),
													fieldModified.getNamedParameterFromDatabaseObjectValue(session,
															targetObject, columnModified));
										} else {
											byte[] data = (byte[]) columnValue;
											if (session.isEnableImageCompression()
													&& MimeTypes.MIME_IMAGE_PNG.equals(mimeType)) {
												data = PNGUtils.compressPNG(data);
											}
											String fileName = UUID.randomUUID().toString() + "."
													+ mimeType.split("/")[1];
											String folderName = "";
											if (session.getTenantId() != null) {
												folderName = session.getTenantId().toString();
												if (session.getCompanyId() != null) {
													folderName += "/" + session.getCompanyId().toString();
												}
											}
											namedParameters.put(columnModified.getColumnName(),
													new ExternalFileNamedParameter(columnModified.getColumnName(),
															new ExternalFileSaveCommand(session, folderName, fileName,
																	data, mimeType)));
										}
									}
								} else {
									namedParameters.put(columnModified.getColumnName(),
											fieldModified.getNamedParameterFromDatabaseObjectValue(session,
													targetObject, columnModified));
								}
							}
						}
					}
				}
			}
		}

		DescriptionColumn versionColumn = entityCache.getVersionColumn();
		if (versionColumn != null) {
			Object newVersion = Versioning.incrementVersion(null, versionColumn.getField().getType());
			namedParameters.put(versionColumn.getColumnName(),
					new VersionNamedParameter(versionColumn.getColumnName(), newVersion));
		}
	}

	protected List<PersisterCommand> getCommandsToUpdateObject(Object oldObject, Object newObject,
			EntityCache entityCache) throws Exception {
		List<DescriptionField> fieldsModified = entityCache.getDescriptionFieldsExcludingIds();
		boolean hasFieldsModified = true;
		EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(newObject);
		entityManaged.updateLastValues(session, oldObject);

		if (entityManaged != null) {
			if (!entityCache.isExistsDescriptionSQL()) {
				List<DescriptionField> fieldModified = entityCache.getFieldsModified(session, newObject);
				fieldsModified = fieldModified;
				hasFieldsModified = (fieldsModified != null) && (fieldsModified.size() > 0);
			}
		}

		ArrayList<NamedParameter> namedParameters = new ArrayList<NamedParameter>();
		List<PersisterCommand> result = new ArrayList<PersisterCommand>();

		updateRelationships(newObject, entityCache, result);

		if ((hasFieldsModified)
				|| ((entityManaged != null) && (entityManaged.containsLockMode(LockMode.OPTIMISTIC_FORCE_INCREMENT,
						LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.WRITE)))) {
			if (hasFieldsModified) {
				if (updateCommonsParameters(oldObject, newObject, entityCache, fieldsModified, namedParameters)) {
					Object oldVersion = updateVersion(newObject, entityCache, namedParameters, entityManaged);
					updateParametersKey(newObject, entityCache, namedParameters);
					updateObject(oldObject, newObject, entityCache, namedParameters, result, oldVersion);
				}
			}
		} else if ((hasFieldsModified) && (entityManaged != null)
				&& (entityManaged.containsLockMode(LockMode.OPTIMISTIC, LockMode.READ))
				|| ((hasFieldsModified) && (entityManaged != null) && (entityCache.isVersioned()))) {
			Identifier<Object> identifier = session.getIdentifier(newObject);
			Map<String, Object> params = identifier.getDatabaseColumnsValues();
			params.put(entityCache.getVersionColumnName(), entityManaged.getOldVersion());
			if (!existsRecordInDatabaseTable(entityCache.getTableName(), params)) {
				throw new OptimisticLockException("Não foi possível alterar a Entidade " + entityCache.getSimpleName()
						+ " pois foi a mesma não foi encontrada ou possui uma nova versão. Id "
						+ identifier.getDatabaseValues() + " Versão " + entityManaged.getOldVersion());
			}

		}

		updateRelationshipsOnCollectionFields(newObject, entityCache, result, entityManaged,
				session.getIdentifier(newObject).getDatabaseColumns());

		return result;
	}

	protected void updateRelationshipsOnCollectionFields(Object targetObject, EntityCache entityCache,
			List<PersisterCommand> result, EntityManaged entityManaged, Map<String, Object> primaryKeyOwner)
			throws IllegalAccessException, Exception {
		FieldEntityValue[] sourceList = null;
		FieldEntityValue[] targetList = null;
		FieldEntityValue lastFieldEntityValue = null;
		FieldEntityValue newFieldEntityValue = null;
		Object newColumnValue;
		/*
		 * Salva as chaves estrangeiras que foram alteradas e que possuam CascadeType =
		 * ALL ou SAVE. Se foi adicionado ou removido filhos nas coleções gera DMLObject
		 * dos itens para execução posterior Se for um objeto gerenciado compara
		 * lastValues com newValues e gera insert,update,delete
		 */
		if (entityManaged != null) {
			for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
				/*
				 * Se for uma coleção de entidades ou relacionamento joinTable (muitos para
				 * muitos)
				 */
				if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()) {
					newColumnValue = descriptionField.getField().get(targetObject);
					if ((session.isProxyObject(newColumnValue)) && (!session.proxyIsInitialized(newColumnValue))) {
						continue;
					}

					lastFieldEntityValue = entityCache.getLastFieldEntityValue(session, targetObject,
							descriptionField.getName());
					newFieldEntityValue = descriptionField.getFieldEntityValue(session, targetObject);
					sourceList = null;
					targetList = null;
					if (lastFieldEntityValue != null)
						sourceList = (FieldEntityValue[]) lastFieldEntityValue.getValue();
					if (newFieldEntityValue != null)
						targetList = (FieldEntityValue[]) newFieldEntityValue.getValue();
					/*
					 * Gera insert ou delete de item na coleção caso o field tenha sido anotado com
					 * Cascade = ALL, SAVE ou DELETE_ORPHAN
					 */
					boolean nullValues = (lastFieldEntityValue == null) && (newFieldEntityValue == null);
					if (!nullValues) {
						/*
						 * Gera update dos itens da coleção caso o field tenha sido anotado com Cascade
						 * = ALL ou SAVE
						 */
						updateChangedItems(sourceList, targetList, descriptionField, result);

						if (((sourceList == null) && (targetList != null))
								|| ((sourceList != null) && (targetList == null))
								|| (lastFieldEntityValue.compareTo(newFieldEntityValue) != 0)) {
							/*
							 * Se existe em A(old) e não existe em B(new) gera delete e se for entidade
							 * delete somente se Cascade=DELETE_ORPHAN
							 */
							deleteItensRemoved(result, primaryKeyOwner, sourceList, targetList, descriptionField);

							/*
							 * Se existe em B(new) e não existe em A(old) gera insert se Cascade=ALL ou SAVE
							 */
							insertNewItens(result, primaryKeyOwner, sourceList, targetList, descriptionField);
						}
					}
				}
			}
		} else {
			/*
			 * Se for um objeto não gerenciado deleta as listas (list,map) e insere
			 * novamente. Caso seja uma lista de entidades insere ou atualiza os itens da
			 * lista.
			 */
			for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
				/*
				 * Se for uma coleção de entidades ou relacionamento joinTable (muitos para
				 * muitos) deleta os itens atuais e insere novamente
				 */
				if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()) {
					Object fieldValue = descriptionField.getObjectValue(targetObject);
					if (fieldValue != null) {
						if (descriptionField.isMapTable()) {
							/*
							 * Remove todos os itens da Coleção
							 */
							result.addAll(getSQLMapTableCommands(null, null, SQLStatementType.DELETE_ALL,
									descriptionField, null, null, primaryKeyOwner));
							/*
							 * Insere novamente
							 */
							for (Object key : ((Map<?, ?>) fieldValue).keySet()) {
								Object value = ((Map<?, ?>) fieldValue).get(key);
								result.addAll(getSQLMapTableCommands(key, value, SQLStatementType.INSERT,
										descriptionField, null, null, primaryKeyOwner));
							}
						} else if (descriptionField.isCollectionTable()) {
							/*
							 * Remove todos os itens da coleção
							 */
							result.addAll(getSQLCollectionTableCommands(null, SQLStatementType.DELETE_ALL,
									descriptionField, null, null, primaryKeyOwner));
							/*
							 * Insere os itens
							 */
							for (Object value : (Collection<?>) fieldValue)
								result.addAll(getSQLCollectionTableCommands(value, SQLStatementType.INSERT,
										descriptionField, null, null, primaryKeyOwner));
						} else if (descriptionField.isCollectionEntity()) {
							/*
							 * Remove todos os itens da coleção
							 */
							if ((Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
									|| Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.SAVE))) {
								result.addAll(getSQLCollectionTableCommands(null, SQLStatementType.DELETE_ALL,
										descriptionField, null, null, primaryKeyOwner));
							}

							for (Object value : ((Collection<?>) fieldValue)) {
								if (value != null) {
									if ((descriptionField.getMappedBy() != null)
											&& (!"".equals(descriptionField.getMappedBy()))) {
										Field mappedByField = descriptionField.getTargetEntity()
												.getDescriptionField(descriptionField.getMappedBy()).getField();
										if (mappedByField.get(value) == null)
											mappedByField.set(value, targetObject);
									}
									if ((Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
											|| Arrays.asList(descriptionField.getCascadeTypes())
													.contains(CascadeType.SAVE))) {
										MergeResult mergeIfNeed = mergeIfNeed(value);
										save(mergeIfNeed.getOldObject(), mergeIfNeed.getNewObject(), result);
									}
								}
							}
						} else if (descriptionField.isJoinTable()) {
							/*
							 * Remove todos os itens da coleção
							 */
							result.addAll(getSQLJoinTableCommands(null, SQLStatementType.DELETE_ALL, descriptionField,
									null, null, primaryKeyOwner));
							/*
							 * Insere os itens
							 */
							for (Object value : (Collection<?>) fieldValue)
								result.addAll(getSQLJoinTableCommands(value, SQLStatementType.INSERT, descriptionField,
										null, null, primaryKeyOwner));
						}
					}
				}
			}
		}
	}

	protected void updateChangedItems(FieldEntityValue[] sourceList, FieldEntityValue[] targetList,
			DescriptionField descriptionField, List<PersisterCommand> result) throws Exception {
		/*
		 * Gera update dos itens da coleção caso o field tenha sido anotado com Cascade
		 * = ALL ou SAVE
		 */
		if (descriptionField.isCollectionEntity() || descriptionField.isJoinTable()) {
			if ((Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
					|| Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.SAVE))
					&& (sourceList != null)) {
				for (FieldEntityValue sourceValue : sourceList) {
					if (targetList != null) {
						for (FieldEntityValue targetValue : targetList) {
							if (targetValue.getName().equals(sourceValue.getName())
									&& (targetValue.getSource() != null)) {
								EntityManaged tempEntityManaged = session.getPersistenceContext()
										.getEntityManaged(targetValue.getSource());
								if (((tempEntityManaged != null)
										&& (!tempEntityManaged.getStatus().equals(EntityStatus.READ_ONLY)))
										|| (tempEntityManaged == null))
									save(session, targetValue.getSource(), result);
							}
						}
					}
				}
			}
		}
	}

	protected void insertNewItens(List<PersisterCommand> result, Map<String, Object> primaryKeyOwner,
			FieldEntityValue[] sourceList, FieldEntityValue[] targetList, DescriptionField descriptionField)
			throws Exception {
		/*
		 * Se existe em B(new) e não existe em A(old) gera insert se Cascade=ALL ou SAVE
		 */
		if (targetList != null) {
			for (FieldEntityValue targetValue : targetList) {
				boolean found = false;
				if (sourceList != null) {
					for (FieldEntityValue sourceValue : sourceList) {
						if (sourceValue.compareTo(targetValue) == 0) {
							found = true;
							break;
						}
					}
				}
				/*
				 * não existe B(new) em A(old) gera insert
				 */
				if (!found) {
					if (descriptionField.isMapTable()) {
						Object key = targetValue.getSource();
						Object value = ((Map<?, ?>) targetValue.getValue()).get(key);
						result.addAll(getSQLMapTableCommands(key, value, SQLStatementType.INSERT, descriptionField,
								null, null, primaryKeyOwner));
					} else if (descriptionField.isCollectionTable())
						result.addAll(getSQLCollectionTableCommands(targetValue.getSource(), SQLStatementType.INSERT,
								descriptionField, null, null, primaryKeyOwner));
					else if (descriptionField.isCollectionEntity()) {
						if ((Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
								|| Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.SAVE)))
							save(session, targetValue.getSource(), result);
					} else if (descriptionField.isJoinTable())
						result.addAll(getSQLJoinTableCommands(targetValue.getSource(), SQLStatementType.INSERT,
								descriptionField, null, null, primaryKeyOwner));

				}

			}
		}
	}

	protected void deleteItensRemoved(List<PersisterCommand> result, Map<String, Object> primaryKeyOwner,
			FieldEntityValue[] sourceList, FieldEntityValue[] targetList, DescriptionField descriptionField)
			throws Exception {
		/*
		 * Se existe em A(old) e não existe em B(new) gera delete e se for entidade
		 * delete somente se Cascade=DELETE_ORPHAN
		 */
		if (sourceList != null) {
			List<Object> objectsToSaveRemove = new ArrayList<Object>();
			for (FieldEntityValue sourceValue : sourceList) {
				boolean found = false;
				if (targetList != null) {
					for (FieldEntityValue targetValue : targetList) {
						if (targetValue.compareTo(sourceValue) == 0) {
							found = true;
							break;
						}
					}
				}
				/*
				 * não existe A(old) em B(new) gera delete
				 */
				if (!found) {
					if (descriptionField.isMapTable()) {
						Object key = sourceValue.getSource();
						Object value = ((Map<?, ?>) sourceValue.getValue()).get(key);
						result.addAll(getSQLMapTableCommands(key, value, SQLStatementType.DELETE, descriptionField,
								null, null, primaryKeyOwner));
					} else if (descriptionField.isCollectionTable())
						result.addAll(getSQLCollectionTableCommands(sourceValue.getSource(), SQLStatementType.DELETE,
								descriptionField, null, null, primaryKeyOwner));
					else if (descriptionField.isCollectionEntity()) {
						if ((Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
								|| Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.DELETE)
								|| Arrays.asList(descriptionField.getCascadeTypes())
										.contains(CascadeType.DELETE_ORPHAN))) {
							objectsToSaveRemove.add(sourceValue.getSource());
						}
					} else if (descriptionField.isJoinTable()) {
						result.addAll(getSQLJoinTableCommands(sourceValue.getSource(), SQLStatementType.DELETE,
								descriptionField, null, null, primaryKeyOwner));
					}
				}
			}

			if (!objectsToSaveRemove.isEmpty()) {
				Collections.sort(objectsToSaveRemove, new Comparator<Object>() {
					public int compare(Object obj1, Object obj2) {
						if (obj1 == null || obj2 == null)
							return 0;

						if (obj1 == obj2)
							return 0;

						EntityCache entityCache1 = session.getEntityCacheManager().getEntityCache(obj1.getClass());
						EntityCache entityCache2 = session.getEntityCacheManager().getEntityCache(obj2.getClass());

						if (entityCache1.hasDependencyFrom(entityCache2, obj1, obj2)) {
							return -1;
						}

						if (entityCache2.hasDependencyFrom(entityCache1, obj2, obj1)) {
							return 1;
						}

						return 0;
					}
				});

				for (Object obj : objectsToSaveRemove) {
					save(session, obj, result);
					session.getCommandQueue().addAll(result);
					result.clear();
					remove(session, obj);
				}
			}
		}

	}

	protected void updateObject(Object oldObject, Object newObject, EntityCache entityCache,
			ArrayList<NamedParameter> namedParameters, List<PersisterCommand> result, Object oldVersion) {
		/*
		 * Seta a versão anterior caso o objeto tenha sido anotado com
		 * 
		 * @Version
		 */
		if (entityCache.isVersioned()) {
			if (oldVersion != null) {
				namedParameters.add(new NamedParameter(entityCache.getVersionColumnName(), oldVersion, true));
			}
		}
		String sql = generateSql(entityCache.getTableName(), SQLStatementType.UPDATE, namedParameters);
		result.add(new UpdateCommandSQL(session, sql, namedParameters, oldObject, newObject, entityCache,
				entityCache.getTableName(), session.getShowSql(),
				entityCache.getDescriptionSqlByType(SQLStatementType.UPDATE), executeInBatchMode()));
	}

	protected void updateRelationships(Object targetObject, EntityCache entityCache, List<PersisterCommand> result)
			throws Exception {
		/*
		 * Salva primeiro os relacionamentos(FK) com o objeto
		 */
		for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
			if (descriptionField.isRelationShip()) {
				Object fieldValue = descriptionField.getObjectValue(targetObject);
				if (fieldValue != null)
					saveRelationShip(fieldValue, descriptionField, result);
			}
		}
	}

	protected void updateParametersKey(Object targetObject, EntityCache entityCache,
			ArrayList<NamedParameter> namedParameters) throws Exception {
		/*
		 * Gera as chaves do update.
		 */
		Map<String, Object> primaryKey = session.getIdentifier(targetObject).getDatabaseColumns();
		for (DescriptionColumn column : entityCache.getPrimaryKeyColumns()) {
			if (session.getPersistenceContext().isExistsEntityManaged(targetObject)) {
				if (!NamedParameter.contains(namedParameters, column.getColumnName()))
					namedParameters.add(new NamedParameter(column.getColumnName(),
							entityCache.getLastValueByColumn(session, targetObject, column), true));
			} else {
				if (!NamedParameter.contains(namedParameters, column.getColumnName()))
					namedParameters.add(
							new NamedParameter(column.getColumnName(), primaryKey.get(column.getColumnName()), true));
			}
		}
	}

	protected Object updateVersion(Object targetObject, EntityCache entityCache,
			ArrayList<NamedParameter> namedParameters, EntityManaged entityManaged)
			throws Exception, IllegalAccessException, InvocationTargetException {
		Object oldVersion = null;
		/*
		 * Incrementa a versão atual caso o objeto tenha sido anotado com
		 * 
		 * @Version e a estratégia do lock seja OPTIMIST_FORCE_INCREMENT,
		 * PESSIMISTIC_FORCE_INCREMENT ou WRITE ou NONE
		 */
		if (entityCache.isVersioned()) {
			Object newVersion = null;
			if (entityManaged != null) {
				oldVersion = entityManaged.getOldVersion();
				newVersion = entityManaged.getOldVersion();
				if (entityManaged.containsLockMode(LockMode.OPTIMISTIC_FORCE_INCREMENT,
						LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.WRITE, LockMode.NONE)) {
					newVersion = Versioning.incrementVersion(entityManaged.getCurrentVersion(),
							entityCache.getVersionColumn().getField().getType());
					entityManaged.setCurrentVersion(newVersion);
					ReflectionUtils.setObjectValueByField(targetObject, entityCache.getVersionColumn().getField(),
							newVersion);
				}
			} else {
				oldVersion = ReflectionUtils.getFieldValueByName(targetObject,
						entityCache.getVersionColumn().getField().getName());
				newVersion = Versioning.incrementVersion(oldVersion,
						entityCache.getVersionColumn().getField().getType());
			}
			namedParameters.add(new VersionNamedParameter(entityCache.getVersionColumn().getColumnName(), newVersion));
		}
		return oldVersion;
	}

	protected boolean updateCommonsParameters(Object oldObject, Object newObject, EntityCache entityCache,
			List<DescriptionField> fieldsModified, ArrayList<NamedParameter> namedParameters)
			throws SQLSessionException, IllegalAccessException, InvocationTargetException, Exception {
		Object newColumnValue;
		/*
		 * Gera os campos do update.
		 */
		Map<String, Object> foreignKey = null;
		for (DescriptionField fieldModified : fieldsModified) {
			if (!fieldModified.isAnyCollectionOrMap() && !fieldModified.isVersioned()
					&& (!fieldModified.isJoinTable())) {
				if (fieldModified.isPrimaryKey())
					throw new SQLSessionException(
							"Não é permitido alterar a chave de um Objeto sendo gerenciado. Verifique o campo "
									+ fieldModified.getName() + " do objeto da classe "
									+ entityCache.getEntityClass().getName());

				newColumnValue = ReflectionUtils.getFieldValue(newObject, fieldModified.getField());
				if (fieldModified.isInitialized(session, newColumnValue)) {
					if (fieldModified.getTargetEntity() != null) {
						if (session.isProxyObject(newColumnValue)) {
							if (!session.proxyIsInitialized(newColumnValue))
								continue;
						}
						foreignKey = new LinkedHashMap<String, Object>();
						Map<String, Object> foreignKeyTemporary = new LinkedHashMap<String, Object>();
						if (newColumnValue != null)
							foreignKeyTemporary = session.getIdentifier(newColumnValue).getDatabaseColumns();

						for (String key : foreignKeyTemporary.keySet()) {
							Object value = foreignKeyTemporary.get(key);
							if (value == null)
								foreignKey.put(key, getIdentifierPostInsertObject(newColumnValue, key));
							else
								foreignKey.put(key, value);
						}
					} else
						foreignKey = null;
					for (DescriptionColumn columnModified : fieldModified.getDescriptionColumns()) {
						if (!columnModified.isDiscriminatorColumn()) {
							boolean allowChangeField = true;
							if (session.getPersistenceContext().isExistsEntityManaged(newObject)
									&& (!entityCache.isExistsDescriptionSQL()))
								allowChangeField = entityCache.fieldCanbeChanged(session, newObject,
										columnModified.getField().getName());

							if (allowChangeField) {
								if (columnModified.isForeignKey()) {
									if (!NamedParameter.contains(namedParameters, columnModified.getColumnName())) {
										namedParameters.add(new NamedParameter(columnModified.getColumnName(),
												foreignKey.get(columnModified.getReferencedColumnName())));
									}
								} else {
									if (!NamedParameter.contains(namedParameters, columnModified.getColumnName())) {
										if (fieldModified.isLob() && session.getExternalFileManager() != null) {

											Object oldValue = fieldModified.getObjectValue(oldObject);
											String oldColumnValue = null;
											if (oldValue != null) {
												oldColumnValue = new String((byte[]) oldValue);
											}

											Object columnValue = fieldModified.getObjectValue(newObject);
											if (columnValue == null) {
												namedParameters.add(
														fieldModified.getNamedParameterFromDatabaseObjectValue(session,
																newObject, columnModified));
											} else {
												ByteArrayInputStream bais = new ByteArrayInputStream(
														(byte[]) columnValue);
												Tika tika = new Tika();
												String mimeType = tika.detect(bais);
												if (MimeTypes.MIME_TEXT_PLAIN.equals(mimeType)
														|| MimeTypes.MIME_TEXT_HTML.equals(mimeType)
														|| MimeTypes.MIME_APPLICATION_OCTET_STREAM.equals(mimeType)) {
													namedParameters
															.add(fieldModified.getNamedParameterFromDatabaseObjectValue(
																	session, newObject, columnModified));
												} else {
													byte[] data = (byte[]) columnValue;
													if (session.isEnableImageCompression()
															&& MimeTypes.MIME_IMAGE_PNG.equals(mimeType)) {
														data = PNGUtils.compressPNG(data);
													}
													String folderName = "";
													if (session.getTenantId() != null) {
														folderName = session.getTenantId().toString();
														if (session.getCompanyId() != null) {
															folderName += "/" + session.getCompanyId().toString();
														}
													}

													String newFileName = UUID.randomUUID().toString() + "."
															+ mimeType.split("/")[1];

													ExternalFileSaveCommand saveCommand = new ExternalFileSaveCommand(
															session, folderName, newFileName, data, mimeType);
													ExternalFileRemoveCommand removeCommand = null;
													if (oldColumnValue != null && isURL(oldColumnValue)) {
														if (oldColumnValue.contains(PREVIEW)
																|| oldColumnValue.contains(DOWNLOAD)) {
															String oldFileName = oldColumnValue.split("\\#")[1];
															removeCommand = new ExternalFileRemoveCommand(session,
																	folderName, oldFileName);
														}
													}
													namedParameters.add(new ExternalFileNamedParameter(
															columnModified.getColumnName(), saveCommand,
															removeCommand));
												}
											}
										} else {
											namedParameters.add(fieldModified.getNamedParameterFromDatabaseObjectValue(
													session, newObject, columnModified));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return namedParameters.size() > 0;
	}

	protected boolean isURL(String url) {
		try {
			new URL(url);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected List<PersisterCommand> getCommandsToDeleteObject(Object targetObject, EntityCache entityCache)
			throws Exception {
		List<NamedParameter> keyParameters = new ArrayList<NamedParameter>();
		List<NamedParameter> keyChildParameters = new ArrayList<NamedParameter>();
		List<PersisterCommand> result = new ArrayList<PersisterCommand>();

		EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(targetObject);

		/*
		 * Gera as chaves do update ou delete. Se for delete remove os itens das
		 * coleções antes de remover o objeto.
		 */
		Map<String, Object> primaryKeyOwner = session.getIdentifier(targetObject).getDatabaseColumns();
		for (String key : primaryKeyOwner.keySet()) {
			keyParameters.add(new NamedParameter(key, primaryKeyOwner.get(key), true));
			keyChildParameters.add(new NamedParameter(key, primaryKeyOwner.get(key), true));
		}

		/*
		 * Remove os itens das coleções antes de remover o objeto.
		 */

		for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
			if (descriptionField.isAnyCollectionOrMap() || descriptionField.isJoinTable()) {
				Object fieldValue = descriptionField.getObjectValue(targetObject);
				if (fieldValue != null) {
					String tableName = descriptionField.getTableName();
					if ((tableName == null) || ("".equals(tableName)))
						tableName = descriptionField.getTargetEntity().getTableName();

					if (descriptionField.isCollectionEntity()) {
						if (!(Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.ALL)
								|| Arrays.asList(descriptionField.getCascadeTypes()).contains(CascadeType.DELETE)
								|| Arrays.asList(descriptionField.getCascadeTypes())
										.contains(CascadeType.DELETE_ORPHAN)))
							continue;

						if (fieldValue instanceof Collection) {
							for (Object value : ((Collection<?>) fieldValue)) {
								remove(session, value);
							}
						}
					} else
						result.add(new DeleteCommandSQL(session,
								generateSql(tableName, SQLStatementType.DELETE, keyChildParameters), keyChildParameters,
								null, null, tableName, session.getShowSql(),
								descriptionField.getDescriptionSqlByType(SQLStatementType.DELETE),
								executeInBatchMode()));
				}
			}
		}

		/*
		 * Seta a versão anterior caso o objeto tenha sido anotado com
		 * 
		 * @Version
		 */
		if (entityCache.isVersioned())
			keyParameters
					.add(new NamedParameter(entityCache.getVersionColumnName(), entityManaged.getOldVersion(), true));

		result.add(new DeleteCommandSQL(session,
				generateSql(entityCache.getTableName(), SQLStatementType.DELETE, keyParameters), keyParameters,
				targetObject, entityCache, entityCache.getTableName(), session.getShowSql(),
				entityCache.getDescriptionSqlByType(SQLStatementType.DELETE), executeInBatchMode()));

		for (DescriptionField descriptionField : entityCache.getDescriptionFields()) {
			if (descriptionField.isLob() && session.getExternalFileManager() != null) {
				Object columnValue = descriptionField.getObjectValue(targetObject);
				String oldColumnValue = null;
				if (columnValue != null) {
					oldColumnValue = new String((byte[]) columnValue);
				}

				if (oldColumnValue != null) {
					ByteArrayInputStream bais = new ByteArrayInputStream(oldColumnValue.getBytes());
					Tika tika = new Tika();
					String mimeType = tika.detect(bais);
					if ("text/plain".equals(mimeType)) {
						if (isURL(oldColumnValue)) {
							String folderName = "";
							if (session.getTenantId() != null) {
								folderName = session.getTenantId().toString();
								if (session.getCompanyId() != null) {
									folderName += "/" + session.getCompanyId().toString();
								}
							}

							String fileName = oldColumnValue.split("\\#")[1];
							result.add(new ExternalFileRemoveCommand(session, folderName, fileName));
						}
					}
				}
			}
		}

		return result;
	}

	protected void saveRelationShip(Object fieldValue, DescriptionField field, List<PersisterCommand> result)
			throws Exception {
		/*
		 * Se for uma entidade de relacionamento ForeignKey e Cascade = ALL ou SAVE e o
		 * objeto não for somente leitura salva o objeto
		 */
		if (Arrays.asList(field.getCascadeTypes()).contains(CascadeType.ALL)
				|| Arrays.asList(field.getCascadeTypes()).contains(CascadeType.SAVE)) {
			EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(fieldValue);
			if (entityManaged != null) {
				if (!entityManaged.getStatus().equals(EntityStatus.READ_ONLY))
					save(session, fieldValue, result);
			} else
				save(session, fieldValue, result);
		}
	}

	protected String generateSql(String tableName, SQLStatementType statement, List<NamedParameter> parameters) {
		if (statement.equals(SQLStatementType.INSERT)) {
			if (NamedParameter.getNames(parameters).length > 0)
				return new Insert(session.getDialect()).setTableName(tableName)
						.addColumns(NamedParameter.getNames(parameters)).toStatementString();
		} else if (statement.equals(SQLStatementType.UPDATE)) {
			if (NamedParameter.getNames(parameters).length > 0)
				return new Update(session.getDialect()).setTableName(tableName)
						.addColumns(NamedParameter.getNames(parameters))
						.addPrimaryKeyColumns(NamedParameter.getNamesKey(parameters)).toStatementString();
		} else if (statement.equals(SQLStatementType.DELETE)) {
			if (NamedParameter.getNamesKey(parameters).length > 0)
				return new Delete().setTableName(tableName).addPrimaryKeyColumns(NamedParameter.getNamesKey(parameters))
						.toStatementString();
		}

		return "";
	}

	protected List<CommandSQL> getSQLMapTableCommands(Object key, Object value, SQLStatementType statement,
			DescriptionField field, DescriptionColumn identifyColumn, IdentifierPostInsert identifierPostInsert,
			Map<String, Object> primaryKeyOwner) {
		List<CommandSQL> result = new ArrayList<CommandSQL>();
		if (key != null) {
			ArrayList<NamedParameter> namedParameters = new ArrayList<NamedParameter>();
			if (statement.equals(SQLStatementType.INSERT)) {
				for (DescriptionColumn descriptionColumn : field.getDescriptionColumns()) {
					if (descriptionColumn.isForeignKey()) {
						if (descriptionColumn.getColumnName()
								.equals(identifyColumn == null ? null : identifyColumn.getColumnName()))
							namedParameters
									.add(new NamedParameter(descriptionColumn.getColumnName(), identifierPostInsert));
						else
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
									primaryKeyOwner.get(descriptionColumn.getReferencedColumnName())));
					} else {
						Object parameterValue = null;
						if (descriptionColumn.isMapKeyColumn())
							parameterValue = key;
						else
							parameterValue = value;

						if (field.isEnumerated())
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
									descriptionColumn.getValueEnum(parameterValue.toString())));
						else if (field.isTemporalDate())
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(), parameterValue,
									TemporalType.DATE));
						else if (field.isTemporalDateTime())
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(), parameterValue,
									TemporalType.DATE_TIME));
						else
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(), parameterValue));
					}
				}
				result.add(new InsertCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(), null, null,
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			} else if (statement.equals(SQLStatementType.DELETE)) {
				for (DescriptionColumn descriptionColumn : field.getDescriptionColumns()) {
					if (descriptionColumn.isPrimaryKey()) {
						if (descriptionColumn.isForeignKey()) {
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
									primaryKeyOwner.get(descriptionColumn.getReferencedColumnName()), true));
						} else {
							Object parameterValue = null;
							if (descriptionColumn.isMapKeyColumn())
								parameterValue = key;
							else
								parameterValue = value;

							if (field.isEnumerated())
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										descriptionColumn.getValueEnum(parameterValue.toString())));
							else if (field.isTemporalDate())
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										parameterValue, TemporalType.DATE));
							else if (field.isTemporalDateTime())
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										parameterValue, TemporalType.DATE_TIME));
							else
								namedParameters
										.add(new NamedParameter(descriptionColumn.getColumnName(), parameterValue));

						}
					}
				}
				result.add(new DeleteCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(),
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			} else if (statement.equals(SQLStatementType.DELETE_ALL)) {
				for (DescriptionColumn column : field.getDescriptionColumns()) {
					if (column.isPrimaryKey()) {
						if (column.isForeignKey()) {
							namedParameters.add(new NamedParameter(column.getColumnName(),
									primaryKeyOwner.get(column.getReferencedColumnName()), true));
						}
					}
				}
				result.add(new DeleteCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(),
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			}
		}
		return result;
	}

	protected List<PersisterCommand> getSQLJoinTableCommands(Object value, SQLStatementType statement,
			DescriptionField field, DescriptionColumn identifyColumn, IdentifierPostInsert identifierPostInsert,
			Map<String, Object> primaryKeyOwner) throws Exception {
		List<PersisterCommand> result = new ArrayList<PersisterCommand>();
		ArrayList<NamedParameter> namedParameters = new ArrayList<NamedParameter>();
		if (value != null) {
			if (statement.equals(SQLStatementType.INSERT)) {
				if (Arrays.asList(field.getCascadeTypes()).contains(CascadeType.ALL)
						|| Arrays.asList(field.getCascadeTypes()).contains(CascadeType.SAVE)) {
					EntityManaged entityManaged = session.getPersistenceContext().getEntityManaged(value);
					if (entityManaged != null) {
						if (!entityManaged.getStatus().equals(EntityStatus.READ_ONLY))
							save(session, value, result);
					} else
						save(session, value, result);
				} else {
					if (!session.getIdentifier(value).hasIdentifier())
						throw new SQLSessionException("Objeto " + value.getClass()
								+ " não possuí anotação para salvar em cascata e também não possuí identificador. ");
				}
				Map<String, Object> foreignKeyRight = session.getIdentifier(value).getDatabaseColumns();

				for (DescriptionColumn descriptionColumn : field.getDescriptionColumns()) {
					if (descriptionColumn.isForeignKey()) {
						if (descriptionColumn.getColumnName()
								.equals(identifyColumn == null ? null : identifyColumn.getColumnName()))
							namedParameters
									.add(new NamedParameter(descriptionColumn.getColumnName(), identifierPostInsert));
						else {
							if (foreignKeyRight.containsKey(descriptionColumn.getColumnName()))
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										foreignKeyRight.get(descriptionColumn.getColumnName())));
							else
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										primaryKeyOwner.get(descriptionColumn.getReferencedColumnName())));
						}
					}
				}
				result.add(new InsertCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(), null, null,
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			} else if (statement.equals(SQLStatementType.DELETE)) {
				Map<String, Object> foreignKeyRight = session.getIdentifier(value).getColumns();
				for (DescriptionColumn descriptionColumn : field.getDescriptionColumns()) {
					if (descriptionColumn.isPrimaryKey()) {
						if (descriptionColumn.isForeignKey()) {
							if (foreignKeyRight.containsKey(descriptionColumn.getColumnName()))
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										foreignKeyRight.get(descriptionColumn.getColumnName()), true));
							else
								namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
										primaryKeyOwner.get(descriptionColumn.getReferencedColumnName()), true));
						} else
							namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(), value, true));
					}
				}
				result.add(new DeleteCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(),
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			}
		}

		if (statement.equals(SQLStatementType.DELETE_ALL)) {
			for (DescriptionColumn descriptionColumn : field.getDescriptionColumns()) {
				if (descriptionColumn.isPrimaryKey()) {
					if (descriptionColumn.isForeignKey()
							&& primaryKeyOwner.containsKey(descriptionColumn.getReferencedColumnName())) {
						namedParameters.add(new NamedParameter(descriptionColumn.getColumnName(),
								primaryKeyOwner.get(descriptionColumn.getReferencedColumnName()), true));
					}
				}
			}
			result.add(new DeleteCommandSQL(session,
					generateSql(field.getTableName(), SQLStatementType.DELETE, namedParameters), namedParameters, null,
					null, field.getTableName(), session.getShowSql(), field.getDescriptionSqlByType(statement),
					executeInBatchMode()));
		}

		return result;
	}

	protected List<CommandSQL> getSQLCollectionTableCommands(Object value, SQLStatementType statement,
			DescriptionField field, DescriptionColumn identifyColumn, IdentifierPostInsert identifierPostInsert,
			Map<String, Object> primaryKeyOwner) {
		List<CommandSQL> result = new ArrayList<CommandSQL>();
		ArrayList<NamedParameter> namedParameters = new ArrayList<NamedParameter>();
		if (value != null) {
			if (statement.equals(SQLStatementType.INSERT)) {
				for (DescriptionColumn column : field.getDescriptionColumns()) {
					if (column.isForeignKey()) {
						if (column.getColumnName()
								.equals(identifyColumn == null ? null : identifyColumn.getColumnName())) {
							namedParameters.add(new NamedParameter(column.getColumnName(), identifierPostInsert));
						} else
							namedParameters.add(new NamedParameter(column.getColumnName(),
									primaryKeyOwner.get(column.getReferencedColumnName())));
					} else
						namedParameters.add(new NamedParameter(column.getColumnName(), value));
				}
				result.add(new InsertCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(), null, null,
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			} else if (statement.equals(SQLStatementType.DELETE)) {
				for (DescriptionColumn column : field.getDescriptionColumns()) {
					if (column.isPrimaryKey()) {
						if (column.isForeignKey())
							namedParameters.add(new NamedParameter(column.getColumnName(),
									primaryKeyOwner.get(column.getReferencedColumnName()), true));
						else
							namedParameters.add(new NamedParameter(column.getColumnName(), value, true));
					}
				}
				result.add(new DeleteCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
						namedParameters, null, null, field.getTableName(), session.getShowSql(),
						field.getDescriptionSqlByType(statement), executeInBatchMode()));
			}
		}
		if (statement.equals(SQLStatementType.DELETE_ALL)) {
			for (DescriptionColumn column : field.getDescriptionColumns()) {
				if (column.isPrimaryKey()) {
					if (column.isForeignKey())
						namedParameters.add(new NamedParameter(column.getColumnName(),
								primaryKeyOwner.get(column.getReferencedColumnName()), true));
				}
			}
			result.add(new DeleteCommandSQL(session, generateSql(field.getTableName(), statement, namedParameters),
					namedParameters, null, null, field.getTableName(), session.getShowSql(),
					field.getDescriptionSqlByType(statement), executeInBatchMode()));
		}
		return result;
	}

	protected IdentifierPostInsert getIdentifierPostInsertObject(Object object, String columnName) {
		/*
		 * Procura nos comandos Insert da fila
		 */
		for (PersisterCommand command : session.getCommandQueue()) {
			if (command instanceof InsertCommandSQL) {
				if (((InsertCommandSQL) command).getTargetObject().equals(object)) {
					if (((InsertCommandSQL) command).getIdentifyColumn() != null) {
						if (((InsertCommandSQL) command).getIdentifyColumn().getColumnName().equals(columnName))
							return ((InsertCommandSQL) command).getIdentifierPostInsert();
					}
				}
			}
		}
		/*
		 * Procura no entityCache de identificadores
		 */
		Map<DescriptionColumn, IdentifierPostInsert> identifierTemp = session.getCacheIdentifier().get(object);
		if (identifierTemp != null) {
			Iterator<DescriptionColumn> it = identifierTemp.keySet().iterator();
			while (it.hasNext()) {
				DescriptionColumn column = it.next();
				if (column.getColumnName().equals(columnName))
					return identifierTemp.get(column);
			}
		}
		return null;
	}

	public void save(SQLSession session, Class<?> clazz, String[] columns, String[] values) throws Exception {
		throw new SQLSessionException("Método não suportado.");
	}

	private class IdentifierResult {
		public DescriptionColumn identifyColumn = null;
		public IdentifierPostInsert identifierPostInsert = null;

		public IdentifierResult(DescriptionColumn identifyColumn, IdentifierPostInsert identifierPostInsert) {
			super();
			this.identifyColumn = identifyColumn;
			this.identifierPostInsert = identifierPostInsert;
		}

	}

	@Override
	public void save(SQLSession session, Object[] objects) throws Exception {
		for (Object obj : objects) {
			save(session, obj);
		}
	}

	@Override
	public void save(SQLSession session, Collection<?> objects) throws Exception {
		for (Object obj : objects) {
			save(session, obj);
		}
	}

}
