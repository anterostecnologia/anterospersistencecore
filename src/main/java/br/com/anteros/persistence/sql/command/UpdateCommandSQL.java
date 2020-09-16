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
import br.com.anteros.persistence.metadata.descriptor.DescriptionSQL;
import br.com.anteros.persistence.metadata.identifier.IdentifierPostInsert;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.parameter.VersionNamedParameter;
import br.com.anteros.persistence.session.ProcedureResult;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.exception.SQLSessionException;
import br.com.anteros.persistence.session.query.ShowSQLType;

public class UpdateCommandSQL extends CommandSQL {

	private static Logger LOG = LoggerProvider.getInstance().getLogger(UpdateCommandSQL.class.getName());
	private Object oldObject;

	public UpdateCommandSQL(SQLSession session, String sql, List<NamedParameter> params, Object oldObject,
			Object newObject, EntityCache entityCache, String targetTableName, ShowSQLType[] showSql,
			DescriptionSQL descriptionSQL, boolean inBatchMode) {
		super(session, sql, params, newObject, entityCache, targetTableName, showSql, descriptionSQL, inBatchMode);
		this.oldObject = oldObject;
	}

	@Override
	public CommandReturn execute() throws Exception {
		boolean threwAnException = false;
		try {
			/*
			 * Executa o SQL
			 */
			if (StringUtils.isNotEmpty(this.getSql())) {
				/*
				 * Troca os parâmetros que aguardam o identificador de outro objeto pelo valor
				 * do identificador gerado
				 */
				for (NamedParameter namedParameter : namedParameters) {
					if (namedParameter.getValue() instanceof IdentifierPostInsert)
						namedParameter.setValue(((IdentifierPostInsert) namedParameter.getValue()).generate());
				}
				try {
					if ((descriptionSQL != null) && descriptionSQL.isCallable()) {
						ProcedureResult result = null;
						try {
							result = queryRunner.executeProcedure(session, session.getDialect(),
									descriptionSQL.getCallableType(), descriptionSQL.getSql(),
									NamedParameter.toArray(namedParameters), showSql, 0, session.clientId());
							/*
							 * Verifica se houve sucesso na execução
							 */
							Object successValue;
							if (descriptionSQL.getCallableType() == CallableType.PROCEDURE)
								successValue = result.getOutPutParameter(descriptionSQL.getSuccessParameter());
							else
								successValue = result.getFunctionResult();

							if (ShowSQLType.contains(showSql, ShowSQLType.UPDATE)) {
								LOG.debug("RESULT = " + successValue);
								LOG.debug("");
							}

							if (!descriptionSQL.getSuccessValue().equalsIgnoreCase(successValue.toString())) {
								threwAnException = true;
								throw new SQLSessionException(successValue.toString());
							}
						} finally {
							if (result != null)
								result.close();
						}
					} else {
						int rowsUpdated;
						if (descriptionSQL != null)
							rowsUpdated = queryRunner.update(session, descriptionSQL.getSql(),
									descriptionSQL.processParameters(session.getEntityCacheManager(), namedParameters),
									showSql, session.getListeners(), session.clientId());
						else {
							if (inBatchMode) {
								return new CommandReturn(sql, NamedParameter.getAllValues(namedParameters));
							} else {
								rowsUpdated = queryRunner.update(this.getSession(), sql,
										NamedParameter.getAllValues(namedParameters), showSql, session.getListeners(),
										session.clientId());
								if (rowsUpdated == 0) {
									threwAnException = true;
									if (entityCache.isVersioned()) {
										throw new SQLException("Objeto foi atualizado ou removido por outra transação. "
												+ this.getObjectId());
									} else {
										throw new SQLException(
												"Não foi possível atualizar o objeto " + this.getObjectId()
														+ " pois o mesmo não foi encontrado. Verifique os parâmetros.");
									}
								} else {
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
							}
						}
					}

					/*
					 * Se o objeto alvo não for uma entidade for um List<String> ou
					 * Map<String,Object> retorna
					 */

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
		} finally {
			if (!threwAnException) {
				session.notifyListeners(EventType.PostUpdate, oldObject, targetObject);
			}
		}
		return null;
	}

	@Override
	public boolean isNewEntity() {
		return false;
	}

}
