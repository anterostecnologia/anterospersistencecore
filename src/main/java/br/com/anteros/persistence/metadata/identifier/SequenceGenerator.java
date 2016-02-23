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
package br.com.anteros.persistence.metadata.identifier;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.query.ShowSQLType;

public class SequenceGenerator implements IdentifierGenerator {

	private static Logger log = LoggerProvider.getInstance().getLogger(SequenceGenerator.class.getName());

	private String catalogName;
	private String schemaName;
	private String sequenceName;
	private String sql;
	private SQLSession session;
	private String parameters;
	private Type type;
	private int initialValue;
	private int allocationSize;

	public SequenceGenerator(SQLSession session, String catalogName, String schemaName, String sequenceName, String parameters, Type type, int initialValue,
			int allocationSize) throws Exception {
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.sequenceName = sequenceName;
		this.session = session;
		this.parameters = parameters;
		this.type = type;
		this.initialValue = initialValue;
		this.allocationSize = allocationSize;
		if (this.sequenceName.indexOf('.') < 0) {
			StringBuilder sb = new StringBuilder();
			if ((this.catalogName != null) && (!"".equals(this.catalogName)))
				sb.append(session.getDialect().quote(this.catalogName)).append(".");
			if ((this.schemaName != null) && (!"".equals(this.schemaName)))
				sb.append(session.getDialect().quote(this.schemaName)).append(".");
			sb.append(session.getDialect().quote(sequenceName));
			this.sequenceName = sb.toString();
		}
		sql = session.getDialect().getSequenceNextValString(this.sequenceName) + " --session id " + session.clientId();
	}

	public Serializable generate() throws Exception {

		Long nextVal = Long.valueOf(0);
		if (!session.hasNextValFromCacheSequence(sequenceName)) {
			Long nextValDatabase = getNextvalFromDatabase();
			session.storeNextValToCacheSession(sequenceName, nextValDatabase, nextValDatabase+allocationSize-1);
		}
		nextVal = session.getNextValFromCacheSequence(sequenceName);

		if (type == Long.class)
			return nextVal;
		else if (type == Integer.class)
			return new Integer(nextVal.intValue());
		else if (type == Double.class)
			return new Double(nextVal.doubleValue());
		else if (type == Float.class)
			return new Float(nextVal.floatValue());
		else if (type == BigDecimal.class)
			return new BigDecimal(nextVal.longValue());
		return null;
	}

	protected Long getNextvalFromDatabase() throws Exception {
		ResultSet rs = session.createQuery(sql).showSql(ShowSQLType.NONE).executeQuery();
		try {
			Long value = new Long(1);
			if (rs.next())
				value = rs.getLong(1);
			return value;
		} finally {
			rs.close();
			rs.getStatement().close();
		}
	}

	public String getCatalogName() {
		return catalogName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getAllocationSize() {
		return allocationSize;
	}

}
