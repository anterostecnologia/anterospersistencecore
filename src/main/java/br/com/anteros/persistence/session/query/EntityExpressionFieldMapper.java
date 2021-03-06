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
package br.com.anteros.persistence.session.query;

import java.sql.ResultSet;
import java.sql.SQLException;

import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.handler.EntityHandlerException;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.EntityManaged;
import br.com.anteros.persistence.metadata.FieldEntityValue;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.metadata.type.EntityStatus;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.cache.Cache;

/**
 * Classe responsável por criar um Entidade com base nos dados da expressão e
 * atribuir ao objeto alvo.
 * 
 * @author edson
 *
 */
public class EntityExpressionFieldMapper extends ExpressionFieldMapper {

	private String aliasTable;
	private String aliasDiscriminatorColumnName;
	private boolean isAbstract;
	private String[] aliasPrimaryKeyColumns;

	public EntityExpressionFieldMapper(EntityCache targetEntityCache, DescriptionField descriptionField,
			String aliasTable, String aliasDiscriminatorColumnName, String[] aliasPrimaryKeyColumns) {
		super(targetEntityCache, descriptionField, "");
		this.aliasTable = aliasTable;
		this.aliasDiscriminatorColumnName = aliasDiscriminatorColumnName;
		this.isAbstract = !StringUtils.isEmpty(aliasDiscriminatorColumnName);
		this.aliasPrimaryKeyColumns = aliasPrimaryKeyColumns;
	}

	@Override
	public void execute(SQLSession session, ResultSet resultSet, EntityManaged entityManaged, Object targetObject,
			Cache transactionCache) throws Exception {
		if (descriptionField == null)
			return;

		DescriptionField newDescriptionField = descriptionField;

		if (!session.getEntityCacheManager().getEntityCache(targetObject.getClass())
				.containsDescriptionField(descriptionField)) {
			descriptionField = session.getEntityCacheManager().getEntityCache(targetObject.getClass())
					.getDescriptionFieldUsesColumns(newDescriptionField.getDescriptionColumnsStr());
			if (descriptionField == null)
				return;

		}

		Object newObject = null;
		EntityManaged newEntityManaged = null;
		/*
		 * Se o campo do objeto alvo ainda não foi inicializado
		 */

		if (descriptionField.isNull(targetObject)) {
			/*
			 * Se for um campo abstrato
			 */
			boolean createdNewObject = false;
			if (isAbstract) {
				try {
					/*
					 * Pega o valor da coluna no resultSet relativo ao discriminatorColumn o que é
					 * necessário para determinar qual classe concreta será instanciada
					 */
					String discriminatorValue = resultSet.getString(aliasDiscriminatorColumnName);

					/*
					 * Se não encontrou o valor não cria o objeto e retorna
					 */
					if (discriminatorValue == null)
						return;

					/*
					 * Se encontrou o valor do discriminator busca a classe concreta referente ao
					 * valor.
					 */
					EntityCache concreteEntityCache = session.getEntityCacheManager()
							.getEntityCache(descriptionField.getField().getType(), discriminatorValue);
					/*
					 * Busca os valores da chave do objeto no resultSet.
					 */
					String uniqueId = getUniqueId(resultSet);
					/*
					 * Caso não encontre não cria o objeto e retorna
					 */
					if (uniqueId == null)
						return;

					/*
					 * Busca o objeto no cache da transação SQL.
					 */
					newObject = getObjectFromCache(session, concreteEntityCache, uniqueId, transactionCache);
					/*
					 * Se não encontrou instancia um novo objeto
					 */
					if (newObject == null) {
						newObject = concreteEntityCache.getEntityClass().newInstance();
						createdNewObject = true;
						/*
						 * Adiciona o objeto instanciado no cache com sua chave única para ser usado
						 * quando houver necessidade em outro ponto da árvore do objeto principal
						 * evitando assim criar objetos repetidos para a mesma chave.
						 */
						addObjectToCache(session, concreteEntityCache, newObject, uniqueId, transactionCache);
					}
				} catch (Exception e) {
					throw new EntityHandlerException("Para que seja criado o objeto da "
							+ descriptionField.getField().getType() + " será necessário adicionar no sql a coluna "
							+ targetEntityCache.getDiscriminatorColumn().getColumnName()
							+ " que informe que tipo de classe será usada para instanciar o objeto.");
				}

			} else {
				/*
				 * Busca os valores da chave do objeto no resultSet.
				 */
				String uniqueId = getUniqueId(resultSet);
				/*
				 * Caso não encontre não cria o objeto e retorna
				 */
				if (uniqueId == null)
					return;
				/*
				 * Busca o objeto no cache da transação SQL.
				 */
				newObject = getObjectFromCache(session, targetEntityCache, uniqueId, transactionCache);
				if (newObject == null) {
					newObject = targetEntityCache.getEntityClass().newInstance();
					createdNewObject = true;
					/*
					 * Adiciona o objeto instanciado no cache com sua chave única para ser usado
					 * quando houver necessidade em outro ponto da árvore do objeto principal
					 * evitando assim criar objetos repetidos para a mesma chave.
					 */
					addObjectToCache(session, targetEntityCache, newObject, uniqueId, transactionCache);
				}
			}
			/*
			 * Adiciona o objeto na lista de entidades gerenciadas pelo contexto da sessão
			 * porém como somente leitura
			 */
			newEntityManaged = session.getPersistenceContext().addEntityManaged(newObject, true, false,
					!createdNewObject);
		} else {
			/*
			 * Caso já tenha sido criado pega o objeto do field
			 */
			newObject = descriptionField.getObjectValue(targetObject);
			newEntityManaged = session.getPersistenceContext().getEntityManaged(newObject);
			if (newEntityManaged == null)
				newEntityManaged = session.getPersistenceContext().addEntityManaged(newObject, true, false, false);
		}

		/*
		 * Executa lista de expressões filhas para atribuir os valores ao novo objeto e
		 * assim sucessivamente até terminar a árvore de expressões.
		 */
		for (ExpressionFieldMapper expField : children) {
			expField.execute(session, resultSet, newEntityManaged, newObject, transactionCache);
		}

		/*
		 * Atribui o novo objeto ao campo do objeto alvo.
		 */
		descriptionField.setObjectValue(targetObject, newObject);

		/*
		 * Guarda o valor na lista de valores anteriores
		 */
		if (entityManaged.getStatus() != EntityStatus.READ_ONLY) {
			FieldEntityValue fieldEntityValue = descriptionField.getFieldEntityValue(session, targetObject);
			entityManaged.addOriginalValue(fieldEntityValue);
			entityManaged.addLastValue(fieldEntityValue);
			entityManaged.getFieldsForUpdate().add(descriptionField.getField().getName());
			/*
			 * Adiciona o campo na lista de campos que poderão ser alterados. Se o campo não
			 * for buscado no select não poderá ser alterado.
			 */
			entityManaged.getFieldsForUpdate().add(descriptionField.getField().getName());
		}
	}

	/**
	 * Retorna a chave única do objeto buscando os valores no resultSet.
	 * 
	 * @param resultSet Resultado do SQL
	 * @return Chave única
	 * @throws SQLException
	 */
	protected String getUniqueId(ResultSet resultSet) throws Exception {
		int index;
		StringBuilder uniqueIdTemp = new StringBuilder("");
		boolean appendSeparator = false;
		for (String aliasColumnName : aliasPrimaryKeyColumns) {
			/*
			 * Busca indice da coluna dentro do resultSet
			 */
			try {
				index = resultSet.findColumn(aliasColumnName);
				if (index < 0) {
					/*
					 * Esta exception não deverá ocorrer nunca pois as colunas estão sendo parseadas
					 * pela análise do SQL. Se isto ocorrer pode ser um erro na análise.
					 */
					throw new SQLException("NÃO ACHOU COLUNA " + aliasColumnName);
				}
			} catch (Exception e) {
				throw new EntityExpressionException("Ocorreu um erro localizando coluna " + aliasColumnName
						+ " referente ao campo " + descriptionField.getField().getName() + " da classe "
						+ targetEntityCache.getEntityClass(), e);
			}

			/*
			 * Concatena o valor da coluna na chave do objeto
			 */
			if (appendSeparator)
				uniqueIdTemp.append("_");
			uniqueIdTemp.append(resultSet.getObject(index));
			appendSeparator = true;
		}
		/*
		 * Retorna o chave única. Se for uma string "null" retorna como nula
		 */
		return (uniqueIdTemp.toString().equals("null") ? null : uniqueIdTemp.toString());
	}

	@Override
	public String toString(int level) {
		StringBuilder sb = new StringBuilder(StringUtils.repeat(" ", level * 4) + descriptionField.getField().getName()
				+ " -> " + targetEntityCache.getEntityClass().getSimpleName() + " : " + aliasColumnName
				+ ("".equals(aliasDiscriminatorColumnName) ? ""
						: " discriminator column " + aliasDiscriminatorColumnName));
		level = level + 1;
		for (ExpressionFieldMapper expressionFieldMapper : children) {
			sb.append("\n").append(expressionFieldMapper.toString(level));
		}
		return sb.toString();
	}

	public String getAliasTable() {
		return aliasTable;
	}

	public void setAliasTable(String aliasTable) {
		this.aliasTable = aliasTable;
	}

	public String getAliasDiscriminatorColumnName() {
		return aliasDiscriminatorColumnName;
	}

	public void setAliasDiscriminatorColumnName(String aliasDiscriminatorColumnName) {
		this.aliasDiscriminatorColumnName = aliasDiscriminatorColumnName;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public String[] getAliasPrimaryKeyColumns() {
		return aliasPrimaryKeyColumns;
	}

	public void setAliasPrimaryKeyColumns(String[] aliasPrimaryKeyColumns) {
		this.aliasPrimaryKeyColumns = aliasPrimaryKeyColumns;
	}

}
