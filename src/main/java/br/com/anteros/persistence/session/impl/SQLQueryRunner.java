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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.anteros.core.utils.SQLFormatter;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.handler.ResultSetHandler;
import br.com.anteros.persistence.metadata.annotation.type.CallableType;
import br.com.anteros.persistence.metadata.identifier.IdentifierPostInsert;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.parameter.OutputNamedParameter;
import br.com.anteros.persistence.parameter.SubstitutedParameter;
import br.com.anteros.persistence.schema.definition.StoredParameterSchema;
import br.com.anteros.persistence.schema.definition.StoredProcedureSchema;
import br.com.anteros.persistence.schema.definition.type.StoredParameterType;
import br.com.anteros.persistence.session.ProcedureResult;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.SQLSessionListener;
import br.com.anteros.persistence.session.SQLSessionResult;
import br.com.anteros.persistence.session.query.AbstractSQLRunner;
import br.com.anteros.persistence.session.query.SQLQueryException;
import br.com.anteros.persistence.session.query.ShowSQLType;
import br.com.anteros.persistence.sql.dialect.DatabaseDialect;
import br.com.anteros.persistence.sql.statement.NamedParameterStatement;

/**
 * Classe responsável pela execução de SQL via JDBC no Banco de Dados
 * 
 * @author Edson Martins - Anteros
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SQLQueryRunner extends AbstractSQLRunner {

	private Map<String, StoredProcedureSchema> cacheStoredProcedures = new HashMap<String, StoredProcedureSchema>();

	public int[] batch(SQLSession session, String sql, Object[][] parameters, ShowSQLType[] showSql, boolean formatSql,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		PreparedStatement statement = null;
		int[] rows = null;
		boolean retry = true;
		while (true) {
			try {
				statement = this.prepareStatement(session.getConnection(), sql);

				for (int i = 0; i < parameters.length; i++) {
					if (ShowSQLType.contains(showSql, ShowSQLType.ALL)
							|| (sql.toLowerCase().contains("insert")
									&& ShowSQLType.contains(showSql, ShowSQLType.INSERT))
							|| (sql.toLowerCase().contains("update")
									&& ShowSQLType.contains(showSql, ShowSQLType.UPDATE))
							|| (sql.toLowerCase().contains("delete")
									&& ShowSQLType.contains(showSql, ShowSQLType.DELETE))) {
						showSQLAndParameters(sql, parameters[i], formatSql, listeners, clientId);
					}
					this.fillStatement(statement, parameters[i]);
					statement.addBatch();
				}
				rows = statement.executeBatch();
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, "");
			} finally {
				close(statement);
			}
		}
		return rows;
	}

	public Object query(SQLSession session, String sql, ResultSetHandler resultSetHandler, Object[] parameters,
			ShowSQLType[] showSql, boolean formatSql, int timeOut, List<SQLSessionListener> listeners, String clientId)
			throws Exception {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		Object result = null;
		boolean retry = true;
		while (true) {
			try {
				statement = this.prepareStatement(session.getConnection(), sql);
				if (timeOut > 0)
					statement.setQueryTimeout(timeOut);

				this.fillStatement(statement, parameters);
				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}

				resultSet = this.wrap(statement.executeQuery());
				result = resultSetHandler.handle(resultSet);
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				try {
					close(resultSet);
				} finally {
					close(statement);
				}
			}
		}
		return result;
	}

	public Object query(SQLSession session, String sql, ResultSetHandler resultSetHandler, NamedParameter[] parameters,
			ShowSQLType[] showSql, boolean formatSql, int timeOut, List<SQLSessionListener> listeners, String clientId)
			throws Exception {

		ResultSet resultSet = null;
		Object result = null;
		NamedParameterStatement statement = null;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, parameters);
				if (timeOut > 0)
					statement.getStatement().setQueryTimeout(timeOut);

				for (NamedParameter param : parameters) {
					if (!(param instanceof SubstitutedParameter))
						statement.setObject(param.getName(), param.getValue());
				}
				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}

				resultSet = this.wrap(statement.executeQuery());
				result = resultSetHandler.handle(resultSet);
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				statement = null;
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				try {
					close(resultSet);
				} finally {
					close(statement);
					statement = null;
				}
			}
		}
		return result;
	}

	public SQLSessionResult queryWithResultSet(SQLSession session, String sql, ResultSetHandler resultSetHandler,
			NamedParameter[] parameters, ShowSQLType[] showSql, boolean formatSql, int timeOut,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		SQLSessionResult result = new SQLSessionResult();
		ResultSet resultSet = null;
		NamedParameterStatement statement = null;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, parameters);
				if (timeOut > 0)
					statement.getStatement().setQueryTimeout(timeOut);

				for (NamedParameter param : parameters) {
					if (!(param instanceof SubstitutedParameter))
						statement.setObject(param.getName(), param.getValue());
				}
				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}

				resultSet = this.wrap(statement.executeQuery());
				Object resultHandler = resultSetHandler.handle(resultSet);
				result.setResultSet(resultSet);
				result.setResultList(((List) resultHandler));
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				statement = null;
				this.rethrow(e, sql, parameters, clientId);
			}
		}

		return result;
	}

	public SQLSessionResult queryWithResultSet(SQLSession session, String sql, ResultSetHandler resultSetHandler,
			Object[] parameters, ShowSQLType[] showSql, boolean formatSql, int timeOut,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		SQLSessionResult result = new SQLSessionResult();
		ResultSet resultSet = null;
		PreparedStatement statement = null;
		boolean retry = true;
		while (true) {
			try {
				statement = this.prepareStatement(session.getConnection(), sql);
				if (timeOut > 0)
					statement.setQueryTimeout(timeOut);

				this.fillStatement(statement, parameters);
				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}

				resultSet = this.wrap(statement.executeQuery());
				Object resultHandler = null;
				if (resultSetHandler != null)
					resultHandler = resultSetHandler.handle(resultSet);

				if (resultHandler == null)
					resultHandler = Collections.EMPTY_LIST;

				if (!(resultHandler instanceof List)) {
					if (resultHandler instanceof Collection)
						resultHandler = new ArrayList<Object>((Collection<?>) resultHandler);
					else {
						resultHandler = Arrays.asList(resultHandler);
					}
				}
				result.setResultSet(resultSet);
				result.setResultList((List<?>) resultHandler);
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				close(statement);
				this.rethrow(e, sql, parameters, clientId);
			}
		}

		return result;
	}

	public Object query(SQLSession session, String sql, ResultSetHandler resultSetHandler,
			Map<String, Object> parameters, ShowSQLType[] showSql, boolean formatSql, int timeOut,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		ResultSet resultSet = null;
		Object result = null;
		NamedParameterStatement statement = null;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, null);
				if (timeOut > 0)
					statement.getStatement().setQueryTimeout(timeOut);

				Iterator<String> it = parameters.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					statement.setObject(key, parameters.get(key));
				}

				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}

				resultSet = this.wrap(statement.executeQuery());
				result = resultSetHandler.handle(resultSet);
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				close(statement);
				statement = null;
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				try {
					close(resultSet);
				} finally {
					close(statement);
				}
			}
		}
		return result;
	}

	public Object queryProcedure(SQLSession session, DatabaseDialect dialect, CallableType type, String name,
			ResultSetHandler resultSetHandler, NamedParameter[] parameters, ShowSQLType[] showSql, int timeOut,
			String clientId) throws Exception {
		CallableStatement statement = null;
		ResultSet resultSet = null;
		Object result = null;
		boolean retry = true;
		while (true) {
			try {
				String[] split = name.split("\\(");
				List<NamedParameter> newParameters = adjustNamedParametersStoredProcedure(session, split[0], parameters,
						type);
				statement = dialect.prepareCallableStatement(session.getConnection(), type, split[0],
						newParameters.toArray(new NamedParameter[] {}), timeOut,
						(ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)), clientId);

				if (type == CallableType.FUNCTION) {
					if (statement.execute()) {
						Object object = statement.getObject(1);
						if (object instanceof ResultSet)
							resultSet = (ResultSet) object;
					}
				} else {
					if (statement.execute())
						resultSet = statement.getResultSet();
				}
				if (resultSet != null)
					result = resultSetHandler.handle(resultSet);
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, "", new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, "", new Object[] {}, clientId);

			} finally {
				try {
					close(resultSet);
				} finally {
					close(statement);
				}
			}
		}

		return result;
	}

	protected List<NamedParameter> adjustNamedParametersStoredProcedure(SQLSession session, String spName,
			NamedParameter[] parameters, CallableType type) throws Exception {

		StoredProcedureSchema storedProcedure = cacheStoredProcedures.get(spName);
		if (storedProcedure == null) {
			Set<StoredProcedureSchema> storedProcedures = null;
			if (type == CallableType.FUNCTION) {
				storedProcedures = new LinkedHashSet<StoredProcedureSchema>(
						session.getDialect().getStoredFunctions(session.getConnection(), spName, true));
			} else {
				storedProcedures = session.getDialect().getStoredProcedures(session.getConnection(), spName, true);
			}

			if (storedProcedures.size() == 0) {
				throw new SQLQueryException("Procedimento/função " + spName + " não encontrado.");
			}
			storedProcedure = storedProcedures.iterator().next();
			cacheStoredProcedures.put(spName, storedProcedure);
		}
		List<NamedParameter> newParameters = new ArrayList<NamedParameter>();

		int size = (parameters == null ? 0 : parameters.length);
		if (type == CallableType.FUNCTION)
			size++;

		if (((parameters == null) && (storedProcedure.getParameters().size() > 1))
				|| ((storedProcedure.getParameters().size() != size))) {
			throw new SQLQueryException(
					"Número de parâmetros informados para execução do procedimento/função " + spName + " incorretos.");
		}

		for (StoredParameterSchema p : storedProcedure.getParameters()) {
			if ((p.getParameterType() == StoredParameterType.RETURN_VALUE)
					|| (p.getParameterType() == StoredParameterType.RETURN_RESULTSET)) {
				newParameters.add(new OutputNamedParameter("RESULT", p.getParameterType(), p.getDataTypeSql()));
			} else {
				NamedParameter namedParameter = NamedParameter.getNamedParameterByName(parameters, p.getName());
				if (namedParameter == null) {
					throw new SQLQueryException("Parâmetro " + p.getName()
							+ " não encontrado na lista de parâmetros informados para execução do procedimento/função "
							+ spName);
				}
				if (((p.getParameterType() == StoredParameterType.OUT)
						|| (p.getParameterType() == StoredParameterType.IN_OUT))
						&& !(namedParameter instanceof OutputNamedParameter)) {
					throw new SQLQueryException("Parâmetro " + p.getName()
							+ " é um parâmetro de saída. Use OuputNamedParameter para parâmetros deste tipo. Procedimento/função "
							+ spName);
				}

				if (namedParameter instanceof OutputNamedParameter) {
					((OutputNamedParameter) namedParameter).setDataTypeSql(p.getDataTypeSql());
				}

				newParameters.add(namedParameter);
			}
		}
		return newParameters;
	}

	public ProcedureResult executeProcedure(SQLSession session, DatabaseDialect dialect, CallableType type, String name,
			NamedParameter[] parameters, ShowSQLType[] showSql, int timeOut, String clientId) throws Exception {
		CallableStatement statement = null;
		ProcedureResult result = new ProcedureResult();
		boolean retry = true;
		while (true) {
			try {
				String[] split = name.split("\\(");
				log.debug("Preparando CallableStatement " + split[0] + " ##" + clientId);
				List<NamedParameter> newParameters = adjustNamedParametersStoredProcedure(session, split[0], parameters,
						type);

				statement = dialect.prepareCallableStatement(session.getConnection(), type, split[0],
						newParameters.toArray(new NamedParameter[] {}), timeOut,
						(ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)), clientId);

				if (type == CallableType.FUNCTION) {
					if (statement.execute()) {
						Object object = statement.getObject(1);
						if (object instanceof ResultSet)
							result.setResultSet((ResultSet) object);
					}
				} else {
					statement.execute();
					result.setResultSet(statement.getResultSet());
				}
				if (NamedParameter.hasOutputParameters(newParameters)) {
					int i = 1;
					for (NamedParameter p : newParameters) {
						if (p instanceof OutputNamedParameter) {
							result.getOutputParameters().put(p.getName(), statement.getObject(i));
						}
						i++;
					}
				}
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, "", new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, "", new Object[] {}, clientId);
			}

			if (result.getResultSet() == null) {
				statement.close();
			}
		}

		return result;

	}

	public Object query(SQLSession session, String sql, ResultSetHandler resultSetHandler, ShowSQLType[] showSql,
			boolean formatSql, List<SQLSessionListener> listeners, String clientId) throws Exception {
		return this.query(session, sql, resultSetHandler, (Object[]) null, showSql, formatSql, 0, listeners, clientId);
	}

	public Object query(SQLSession session, String sql, ResultSetHandler resultSetHandler, ShowSQLType[] showSql,
			boolean formatSql, int timeOut, List<SQLSessionListener> listeners, String clientId) throws Exception {
		return this.query(session, sql, resultSetHandler, (Object[]) null, showSql, formatSql, timeOut, listeners,
				clientId);
	}

	public ResultSet executeQuery(SQLSession session, String sql, NamedParameter[] parameters, ShowSQLType[] showSql,
			boolean formatSql, List<SQLSessionListener> listeners, String clientId) throws Exception {
		return executeQuery(session, sql, parameters, showSql, formatSql, 0, listeners, clientId);
	}

	public ResultSet executeQuery(SQLSession session, String sql, ShowSQLType[] showSql, boolean formatSql, int timeOut,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		return executeQuery(session, sql, (Object[]) null, showSql, formatSql, timeOut, listeners, clientId);
	}

	public ResultSet executeQuery(SQLSession session, String sql, NamedParameter[] parameters, ShowSQLType[] showSql,
			boolean formatSql, int timeOut, List<SQLSessionListener> listeners, String clientId) throws Exception {
		ResultSet result = null;
		NamedParameterStatement statement = null;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, parameters);
				for (NamedParameter namedParameter : parameters)
					statement.setObject(namedParameter.getName(), namedParameter.getValue());
				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}
				result = this.wrap(statement.executeQuery());
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				statement = null;
				this.rethrow(e, sql, parameters, clientId);
			} finally {
			}
		}

		return result;
	}

	public ResultSet executeQuery(SQLSession session, String sql, Object[] parameters, ShowSQLType[] showSql,
			boolean formatSql, int timeOut, List<SQLSessionListener> listeners, String clientId) throws Exception {
		PreparedStatement statement = null;
		ResultSet result = null;
		boolean retry = true;
		while (true) {
			try {
				statement = this.prepareStatement(session.getConnection(), sql);
				if (timeOut > 0)
					statement.setQueryTimeout(timeOut);
				this.fillStatement(statement, parameters);
				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}
				result = this.wrap(statement.executeQuery());
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);
			}
		}
		return result;
	}

	public ResultSet executeQuery(SQLSession session, String sql, Map<String, Object> parameters, ShowSQLType[] showSql,
			boolean formatSql, int timeOut, List<SQLSessionListener> listeners, String clientId) throws Exception {
		ResultSet resultSet = null;
		NamedParameterStatement statement = null;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, null);
				if (timeOut > 0)
					statement.getStatement().setQueryTimeout(timeOut);

				Iterator<String> it = parameters.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					statement.setObject(key, parameters.get(key));
				}

				if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT)) {
					showSQLAndParameters(sql, parameters, formatSql, listeners, clientId);
				}

				resultSet = this.wrap(statement.executeQuery());
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				close(statement);
				statement = null;
			}
		}

		return resultSet;
	}

	public int update(SQLSession session, String sql, List<SQLSessionListener> listeners) throws Exception {
		return this.update(session, sql, (Object[]) null, listeners);
	}

	public int update(SQLSession session, String sql, Object[] parameters, List<SQLSessionListener> listeners)
			throws Exception {

		return this.update(session, sql, parameters, new ShowSQLType[] { ShowSQLType.NONE }, listeners, "");
	}

	public int update(SQLSession session, String sql, NamedParameter[] parameters, List<SQLSessionListener> listeners)
			throws Exception {

		return this.update(session, sql, parameters, new ShowSQLType[] { ShowSQLType.NONE }, listeners, "");
	}

	public int update(SQLSession session, String sql, Object[] parameters, ShowSQLType[] showSql,
			List<SQLSessionListener> listeners, String clientId) throws Exception {

		PreparedStatement statement = null;
		int rows = 0;
		boolean retry = true;
		while (true) {
			try {
				statement = this.prepareStatement(session.getConnection(), sql);
				this.fillStatement(statement, parameters);

				if (sql.toLowerCase().contains("insert") && ShowSQLType.contains(showSql, ShowSQLType.INSERT)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("update") && ShowSQLType.contains(showSql, ShowSQLType.UPDATE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("delete") && ShowSQLType.contains(showSql, ShowSQLType.DELETE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (ShowSQLType.contains(showSql, ShowSQLType.ALL)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				}

				System.out.println(sql);
				rows = statement.executeUpdate();
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);

			} finally

			{
				close(statement);
			}
		}

		return rows;

	}

	public int update(SQLSession session, String sql, Object[] parameters, IdentifierPostInsert identifierPostInsert,
			String identitySelectString, ShowSQLType[] showSql, List<SQLSessionListener> listeners, String clientId)
			throws Exception {
		PreparedStatement statement = null;
		PreparedStatement statementGeneratedKeys = null;
		ResultSet rsGeneratedKeys;
		int rows = 0;
		boolean retry = true;
		while (true) {
			try {
				statement = session.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				this.fillStatement(statement, parameters);
				if (sql.toLowerCase().contains("insert") && ShowSQLType.contains(showSql, ShowSQLType.INSERT)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("update") && ShowSQLType.contains(showSql, ShowSQLType.UPDATE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("delete") && ShowSQLType.contains(showSql, ShowSQLType.DELETE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (ShowSQLType.contains(showSql, ShowSQLType.ALL)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				}

				rows = statement.executeUpdate();

				rsGeneratedKeys = statement.getGeneratedKeys();
				if (rsGeneratedKeys.next()) {
					identifierPostInsert.setGeneratedValue(rsGeneratedKeys);
					close(rsGeneratedKeys);
				} else {
					close(rsGeneratedKeys);
					if ((identitySelectString != null) && ("".equals(identitySelectString))) {
						statementGeneratedKeys = session.getConnection().prepareStatement(identitySelectString);
						rsGeneratedKeys = statementGeneratedKeys.executeQuery();
						if (rsGeneratedKeys.next())
							identifierPostInsert.setGeneratedValue(rsGeneratedKeys);
						close(rsGeneratedKeys);
					}
				}
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				close(statement);
				close(statementGeneratedKeys);
			}
		}

		return rows;
	}

	public int update(SQLSession session, String sql, NamedParameter[] parameters, ShowSQLType[] showSql,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		NamedParameterStatement statement = null;
		int rows = 0;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, parameters);
				for (NamedParameter namedParameter : parameters) {
					statement.setObject(namedParameter.getName(), namedParameter.getValue());
				}
				if (sql.toLowerCase().contains("insert") && ShowSQLType.contains(showSql, ShowSQLType.INSERT)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("update") && ShowSQLType.contains(showSql, ShowSQLType.UPDATE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("delete") && ShowSQLType.contains(showSql, ShowSQLType.DELETE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (ShowSQLType.contains(showSql, ShowSQLType.ALL)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				}

				rows = statement.executeUpdate();
				break;
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				close(statement);
				statement = null;
			}
		}

		return rows;
	}

	public int update(SQLSession session, String sql, NamedParameter[] parameters,
			IdentifierPostInsert identifierPostInsert, String identitySelectString, ShowSQLType[] showSql,
			List<SQLSessionListener> listeners, String clientId) throws Exception {
		NamedParameterStatement statement = null;
		PreparedStatement stmtGeneratedKeys = null;
		ResultSet rsGeneratedKeys;
		int rows = 0;
		boolean retry = true;
		while (true) {
			try {
				statement = new NamedParameterStatement(session.getConnection(), sql, parameters,
						Statement.RETURN_GENERATED_KEYS);
				for (NamedParameter namedParameter : parameters) {
					statement.setObject(namedParameter.getName(), namedParameter.getValue());
				}
				if (sql.toLowerCase().contains("insert") && ShowSQLType.contains(showSql, ShowSQLType.INSERT)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("update") && ShowSQLType.contains(showSql, ShowSQLType.UPDATE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (sql.toLowerCase().contains("delete") && ShowSQLType.contains(showSql, ShowSQLType.DELETE)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				} else if (ShowSQLType.contains(showSql, ShowSQLType.ALL)) {
					showSQLAndParameters(sql, parameters, true, listeners, clientId);
				}

				rows = statement.executeUpdate();

				rsGeneratedKeys = statement.getStatement().getGeneratedKeys();
				if (rsGeneratedKeys.next()) {
					identifierPostInsert.setGeneratedValue(rsGeneratedKeys);
					close(rsGeneratedKeys);
				} else {
					close(rsGeneratedKeys);
					if ((identitySelectString != null) && ("".equals(identitySelectString))) {
						stmtGeneratedKeys = session.getConnection().prepareStatement(identitySelectString);
						rsGeneratedKeys = stmtGeneratedKeys.executeQuery();
						if (rsGeneratedKeys.next()) {
							identifierPostInsert.setGeneratedValue(rsGeneratedKeys);
						}
						close(rsGeneratedKeys);
					}
				}
				break;

			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					close(statement);
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, parameters, clientId);

			} finally {
				if (statement != null)
					close(statement.getStatement());
				close(stmtGeneratedKeys);
				statement = null;
				stmtGeneratedKeys = null;
			}
		}

		return rows;
	}

	public ResultSet executeQuery(SQLSession session, String sql, ShowSQLType[] showSql, boolean formatSql,
			String clientId) throws Exception {
		if (ShowSQLType.contains(showSql, ShowSQLType.ALL, ShowSQLType.SELECT))
			log.debug("Sql-> " + (formatSql == true ? SQLFormatter.format(sql) : sql) + " ##" + clientId);
		boolean retry = true;
		while (true) {
			try {
				return session.getConnection().prepareStatement(sql).executeQuery();
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, sql, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, sql, new Object[] {}, clientId);
			}
		}
	}

	@Override
	public int update(SQLSession session, String sql, Object parameter, List<SQLSessionListener> listeners)
			throws Exception {
		return update(session, sql, new Object[] { parameter }, new ShowSQLType[] { ShowSQLType.NONE }, listeners, "");
	}

	@Override
	public void executeDDL(SQLSession session, String ddl, ShowSQLType[] showSql, boolean formatSql, String clientId)
			throws Exception {
		if (ShowSQLType.contains(showSql, ShowSQLType.ALL))
			log.debug("DDL-> " + (formatSql == true ? SQLFormatter.format(ddl) : ddl) + " ##" + clientId);
		boolean retry = true;
		while (true) {
			try {
				session.getConnection().prepareStatement(ddl).executeUpdate();
			} catch (SQLException e) {
				String message = e.getCause().getMessage();
				if (retry && StringUtils.isNotEmpty(message)
						&& (message.toLowerCase().contains("connection")
								&& ((message.toLowerCase().contains("closed")
										|| message.toLowerCase().contains("lost"))))) {
					retry = false;
					try {
						session.invalidateConnection();
					} catch (SQLException ex1) {
						this.rethrow(ex1, ddl, new Object[] {}, clientId);
					}
					continue;
				}
				this.rethrow(e, ddl, new Object[] {}, clientId);
			}
		}
	}

	protected void showSQLAndParameters(String sql, Object[] parameters, boolean formatSql,
			List<SQLSessionListener> listeners, String clientId) {
		String sqlFormatted = (formatSql == true ? SQLFormatter.format(sql) : sql);
		System.out.println("Sql-> " + sqlFormatted + " ##" + clientId);

		if ((parameters != null) && (parameters.length > 0)) {
			StringBuilder sb = new StringBuilder("Parameters -> ");
			boolean append = false;
			for (Object p : parameters) {
				if (append)
					sb.append(", ");
				sb.append(p + "");
				append = true;
			}
			System.out.println(sb.toString() + " ##" + clientId);
		}

		if (listeners != null)
			for (SQLSessionListener listener : listeners)
				listener.onExecuteSQL(sqlFormatted, parameters);
	}

	protected void showSQLAndParameters(String sql, NamedParameter[] parameters, boolean formatSql,
			List<SQLSessionListener> listeners, String clientId) {
		String sqlFormatted = (formatSql == true ? SQLFormatter.format(sql) : sql);
		System.out.println("Sql-> " + sqlFormatted + " ##" + clientId);
		if ((parameters != null) && (parameters.length > 0)) {
			StringBuilder sb = new StringBuilder("Parâmetros -> ");
			boolean append = false;
			for (NamedParameter p : parameters) {
				if (append)
					sb.append(", ");
				sb.append(p.toString());
				append = true;
			}
			System.out.println(sb.toString() + " ##" + clientId);
		}

		if (listeners != null) {
			for (SQLSessionListener listener : listeners)
				listener.onExecuteSQL(sqlFormatted, parameters);
		}
	}

	protected void showSQLAndParameters(String sql, Map<String, Object> parameters, boolean formatSql,
			List<SQLSessionListener> listeners, String clientId) {
		String sqlFormatted = (formatSql == true ? SQLFormatter.format(sql) : sql);
		System.out.println("Sql-> " + sqlFormatted + " ##" + clientId);
		if ((parameters != null) && (parameters.size() > 0)) {
			StringBuilder sb = new StringBuilder("Parâmetros -> ");
			boolean append = false;
			for (String param : parameters.keySet()) {
				if (append)
					sb.append(", ");
				param += "=" + parameters.get(param);
				sb.append(param);
				append = true;
			}
			System.out.println(sb.toString() + " ##" + clientId);

			if (listeners != null) {
				for (SQLSessionListener listener : listeners)
					listener.onExecuteSQL(sqlFormatted, parameters);
			}
		}
	}

}
