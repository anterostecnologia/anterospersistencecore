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
package br.com.anteros.persistence.session.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.handler.EntityHandler;
import br.com.anteros.persistence.handler.EntityHandlerException;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.EntityManaged;
import br.com.anteros.persistence.metadata.annotation.type.ScopeType;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.cache.Cache;

/**
 * Classe que representa uma expressão a ser processada para criação de um campo
 * no objeto. Usado pela classe EntityHandler para processar o ResultSet e criar
 * os objetos correspondentes a classe de resultado.
 * 
 * @author edson
 * @see EntityHandler
 */
public abstract class ExpressionFieldMapper {

	protected EntityCache targetEntityCache;
	protected DescriptionField descriptionField;
	protected String aliasColumnName;
	protected Set<ExpressionFieldMapper> children = new LinkedHashSet<ExpressionFieldMapper>();

	public ExpressionFieldMapper(EntityCache targetEntityCache, DescriptionField descriptionField,
			String aliasColumnName) {
		this.targetEntityCache = targetEntityCache;
		this.descriptionField = descriptionField;
		this.aliasColumnName = aliasColumnName;
	}

	/**
	 * Método responsável pela execução da criação do objeto e atribuição ao
	 * campo do objeto alvo.
	 * 
	 * @param session
	 *            Sessão
	 * @param resultSet
	 *            ResultSet contendo os dados do SQL
	 * @param entityManaged
	 *            Entidade sendo gerenciada
	 * @param targetObject
	 *            Objeto alvo
	 * @param transactionCache
	 *            Cache de objetos na transação(execução do SQL).
	 * @throws Exception
	 *             Exceção gerada
	 */
	public abstract void execute(SQLSession session, ResultSet resultSet, EntityManaged entityManaged,
			Object targetObject, Cache transactionCache) throws Exception;

	public EntityCache getTargetEntityCache() {
		return targetEntityCache;
	}

	public void setTargetEntityCache(EntityCache targetEntityCache) {
		this.targetEntityCache = targetEntityCache;
	}

	public DescriptionField getDescriptionField() {
		return descriptionField;
	}

	public void setDescriptionField(DescriptionField descriptionField) {
		this.descriptionField = descriptionField;
	}

	public String getAliasColumnName() {
		return aliasColumnName;
	}

	public void setAliasColumnName(String aliasColumnName) {
		this.aliasColumnName = aliasColumnName;
	}

	public Set<ExpressionFieldMapper> getChildren() {
		return Collections.unmodifiableSet(children);
	}

	public ExpressionFieldMapper addChild(ExpressionFieldMapper child) {
		children.add(child);
		return this;
	}

	/**
	 * Retorna o objeto ExpressionFieldMapper filho correspondente ao nome do
	 * campo informado.
	 * 
	 * @param name
	 *            Nome do campo
	 * @return Objeto ExpressionFieldMapper correspondente ao campo ou nulo caso
	 *         não exista.
	 */
	public ExpressionFieldMapper getExpressionFieldByName(String name) {
		for (ExpressionFieldMapper child : children) {
			if (child.getDescriptionField().getField().getName().equalsIgnoreCase(name)) {
				return child;
			}
		}
		return null;
	}

	public String toString(int level) {
		StringBuilder sb = new StringBuilder(StringUtils.repeat(" ", level * 4) + descriptionField.getField().getName()
				+ " -> " + targetEntityCache.getEntityClass().getSimpleName() + " : " + aliasColumnName);
		level = level + 1;
		for (ExpressionFieldMapper expressionFieldMapper : children) {
			sb.append("\n").append(expressionFieldMapper.toString(level));
		}
		return sb.toString();
	}

	/**
	 * Com base nos atributos da expressão retorna o valor correspondente a
	 * coluna no ResultSet.
	 * 
	 * @param resultSet
	 * @return
	 * @throws EntityHandlerException
	 */
	protected Object getValueByColumnName(ResultSet resultSet) throws EntityHandlerException {
		try {
			Object value = resultSet.getObject(aliasColumnName);
			/*
			 * Tratamento diferenciado para os tipos de data.
			 */
			if ((value instanceof Date) || (value instanceof java.sql.Date))
				value = resultSet.getTimestamp(aliasColumnName);
			return value;
		} catch (SQLException ex) {
			throw new EntityHandlerException("Erro processando campo " + descriptionField.getField().getName()
					+ " na classe " + targetEntityCache.getEntityClass().getName() + " coluna " + aliasColumnName + ". "
					+ ex.getMessage());
		}
	}

	/**
	 * Busca um objeto no cache usando a Entidade e a chave para localização.
	 * 
	 * @param session
	 *            Sessão
	 * @param targetEntityCache
	 *            Entidade
	 * @param uniqueId
	 *            Chave primária do objeto
	 * @param transactionCache
	 *            Cache da transação
	 * @return Objeto correspondente a entidade e chave informada ou nulo caso
	 *         não exista no cache.
	 */
	protected Object getObjectFromCache(SQLSession session, EntityCache targetEntityCache, String uniqueId,
			Cache transactionCache) {
		Object result = null;
		/*
		 * Se a classe for abstrata pega todas as implementações não abstratas e
		 * verifica se existe um objeto da classe + ID no entityCache
		 */
		if (targetEntityCache.isAbstractClass()) {
			EntityCache[] entitiesCache = session.getEntityCacheManager().getEntitiesBySuperClass(targetEntityCache);
			for (EntityCache entityCache : entitiesCache) {
				StringBuilder sb = new StringBuilder();
				sb.append(entityCache.getEntityClass().getName()).append("_").append(uniqueId);
				result = transactionCache.get(sb.toString());
				if (result != null)
					break;
				result = session.getPersistenceContext().getObjectFromCache(sb.toString());
				if (result != null)
					break;
			}
		} else {
			/*
			 * Caso não seja abstrata localiza classe+ID no entityCache
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
	 * Adiciona o objeto criado no cache da transação ou do contexto.
	 * 
	 * @param session
	 *            Sessão
	 * @param entityCache
	 *            Entidade
	 * @param targetObject
	 *            Objeto a ser adicionado no cache
	 * @param uniqueId
	 *            Chave do objeto
	 * @param transactionCache
	 *            Cache de transação
	 */
	protected void addObjectToCache(SQLSession session, EntityCache entityCache, Object targetObject, String uniqueId,
			Cache transactionCache) {
		/*
		 * Adiciona o objeto no Cache da sessão ou da transação para evitar
		 * buscar o objeto novamente no mesmo processamento
		 */
		StringBuilder sb = new StringBuilder();
		sb.append(entityCache.getEntityClass().getName()).append("_").append(uniqueId);
		if ((entityCache.getCacheScope().equals(ScopeType.TRANSACTION)) && (transactionCache != null)) {
			transactionCache.put(sb.toString(), targetObject,
					entityCache.getMaxTimeCache());
		} else {
			session.getPersistenceContext().addObjectToCache(sb.toString(),
					targetObject, entityCache.getMaxTimeCache());
		}
	}

	@Override
	public String toString() {
		return (targetEntityCache == null ? "" : targetEntityCache.getEntityClass()) + ":"
				+ descriptionField.getField().getName() + ":" + aliasColumnName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aliasColumnName == null) ? 0 : aliasColumnName.hashCode());
		result = prime * result + ((descriptionField == null) ? 0 : descriptionField.hashCode());
		result = prime * result + ((targetEntityCache == null) ? 0 : targetEntityCache.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionFieldMapper other = (ExpressionFieldMapper) obj;
		if (aliasColumnName == null) {
			if (other.aliasColumnName != null)
				return false;
		} else if (!aliasColumnName.equals(other.aliasColumnName))
			return false;
		if (descriptionField == null) {
			if (other.descriptionField != null)
				return false;
		} else if (!descriptionField.equals(other.descriptionField))
			return false;
		if (targetEntityCache == null) {
			if (other.targetEntityCache != null)
				return false;
		} else if (!targetEntityCache.equals(other.targetEntityCache))
			return false;
		return true;
	}

}
