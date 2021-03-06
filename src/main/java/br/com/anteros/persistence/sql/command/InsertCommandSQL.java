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
package br.com.anteros.persistence.sql.command;

import java.sql.SQLException;
import java.util.List;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.annotation.EventType;
import br.com.anteros.persistence.metadata.annotation.type.CallableType;
import br.com.anteros.persistence.metadata.descriptor.DescriptionColumn;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.metadata.descriptor.DescriptionSQL;
import br.com.anteros.persistence.metadata.identifier.Identifier;
import br.com.anteros.persistence.metadata.identifier.IdentifierColumn;
import br.com.anteros.persistence.metadata.identifier.IdentifierColumnList;
import br.com.anteros.persistence.metadata.identifier.IdentifierPostInsert;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.parameter.VersionNamedParameter;
import br.com.anteros.persistence.session.ProcedureResult;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.exception.SQLSessionException;
import br.com.anteros.persistence.session.query.ShowSQLType;

public class InsertCommandSQL extends CommandSQL {

	private IdentifierPostInsert identifierPostInsert;
	private DescriptionColumn identifyColumn;

	private static Logger LOG = LoggerProvider.getInstance().getLogger(InsertCommandSQL.class.getName());

	public InsertCommandSQL(SQLSession session, String sql, List<NamedParameter> namedParameters, Object targetObject,
			EntityCache entityCache, String targetTableName, ShowSQLType[] showSql,
			IdentifierPostInsert identifierPostInsert, DescriptionColumn identifyColumn, DescriptionSQL descriptionSQL,
			boolean inBatchMode) {
		super(session, sql, namedParameters, targetObject, entityCache, targetTableName, showSql, descriptionSQL,
				inBatchMode);
		this.identifierPostInsert = identifierPostInsert;
		this.identifyColumn = identifyColumn;
	}

	@Override
	public CommandReturn execute() throws Exception {
		boolean threwAnException = false;
		try {
			/*
			 * Troca os parâmetros que aguardam o identificador de outro objeto pelo valor
			 * do identificador gerado
			 */
			for (NamedParameter parameter : namedParameters) {
				if (parameter.getValue() instanceof IdentifierPostInsert)
					parameter.setValue(((IdentifierPostInsert) parameter.getValue()).generate());
			}
			/*
			 * Executa o SQL
			 */
			if (StringUtils.isNotEmpty(this.getSql())) {
				try {
					if ((descriptionSQL != null) && descriptionSQL.isCallable()) {
						ProcedureResult result = null;
						try {
							result = queryRunner.executeProcedure(session, session.getDialect(),
									descriptionSQL.getCallableType(), descriptionSQL.getSql(),
									namedParameters.toArray(new NamedParameter[] {}), showSql, 0, session.clientId());
							/*
							 * Verifica se houve sucesso na execução
							 */
							Object successValue;
							if (descriptionSQL.getCallableType() == CallableType.PROCEDURE)
								successValue = result.getOutPutParameter(descriptionSQL.getSuccessParameter());
							else
								successValue = result.getFunctionResult();

							if (ShowSQLType.contains(showSql, ShowSQLType.INSERT)) {
								LOG.debug("RESULT = " + successValue);
								LOG.debug("");
							}

							if (!descriptionSQL.getSuccessValue().equalsIgnoreCase(successValue.toString())) {
								threwAnException = true;
								throw new SQLSessionException(successValue.toString());
							}

							if (descriptionSQL.getParametersId().size() > 0) {
								Identifier identifier = session.getIdentifier(targetObject);
								EntityCache entityCache = session.getEntityCacheManager()
										.getEntityCache(targetObject.getClass());
								DescriptionField[] primaryKeyFields = entityCache.getPrimaryKeyFields();
								IdentifierColumnList identifierList = IdentifierColumn.list();
								for (DescriptionField descriptionField : primaryKeyFields) {
									for (DescriptionColumn column : descriptionField.getDescriptionColumns()) {
										identifierList.add(new IdentifierColumn(column.getColumnName(),
												result.getOutPutParameter(descriptionSQL
														.getParameterIdByColumnName(column.getColumnName()))));
									}
									identifier.setFieldValue(descriptionField.getName(),
											identifierList.toArray(new IdentifierColumn[] {}));
								}

							}
						} finally {
							if (result != null)
								result.close();
						}

					} else {
						if (identifierPostInsert != null) {
							if (descriptionSQL != null) {
								queryRunner.update(session, descriptionSQL.getSql(),
										descriptionSQL.processParameters(session.getEntityCacheManager(),
												namedParameters),
										identifierPostInsert, session.getDialect().getIdentitySelectString(), showSql,
										session.getListeners(), session.clientId());
							} else {
								queryRunner.update(session, sql, NamedParameter.getAllValues(namedParameters),
										identifierPostInsert, session.getDialect().getIdentitySelectString(), showSql,
										session.getListeners(), session.clientId());

								for (NamedParameter np : namedParameters) {
									if (np instanceof VersionNamedParameter) {
										DescriptionColumn versionColumn = entityCache.getVersionColumn();
										if (versionColumn != null) {
											ReflectionUtils.setObjectValueByFieldName(targetObject,
													versionColumn.getField().getName(), np.getValue());
										}
									}
								}
							}
							generatedId = identifierPostInsert.generate();
							ReflectionUtils.setObjectValueByFieldName(targetObject, identifyColumn.getField().getName(),
									generatedId);
						} else {
							if (descriptionSQL != null) {
								queryRunner.update(
										session, descriptionSQL.getSql(), descriptionSQL
												.processParameters(session.getEntityCacheManager(), namedParameters),
										showSql, session.getListeners(), session.clientId());
							} else {
								if (inBatchMode) {
									return new CommandReturn(sql, NamedParameter.getAllValues(namedParameters));
								} else {
									queryRunner.update(session, sql, NamedParameter.getAllValues(namedParameters),
											showSql, session.getListeners(), session.clientId());
								}
							}
						}
					}
					if (targetObject == null)
						return null;
				} catch (SQLException ex) {
					threwAnException = true;
					throw session.getDialect().convertSQLException(ex, "", sql);
				} catch (Exception ex1) {
					threwAnException = true;
					throw ex1;
				}
			}
			setEntityManaged();
			return null;
		} finally {
			if (!threwAnException) {
				session.notifyListeners(EventType.PostPersist, null, this.targetObject);
			}
		}

	}

	public IdentifierPostInsert getIdentifierPostInsert() {
		return identifierPostInsert;
	}

	public void setIdentifierPostInsert(IdentifierPostInsert identifierPostInsert) {
		this.identifierPostInsert = identifierPostInsert;
	}

	public DescriptionColumn getIdentifyColumn() {
		return identifyColumn;
	}

	public void setIdentifyColumn(DescriptionColumn identifyColumn) {
		this.identifyColumn = identifyColumn;
	}

	@Override
	public boolean isNewEntity() {
		return true;
	}

}
