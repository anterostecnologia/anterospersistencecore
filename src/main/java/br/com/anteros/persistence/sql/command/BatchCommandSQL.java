package br.com.anteros.persistence.sql.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.descriptor.DescriptionSQL;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.session.SQLSession;

public class BatchCommandSQL extends CommandSQL {

	private CommandSQL[] commands;
	private int batchSize;

	private BatchCommandSQL(SQLSession session, String sql, List<NamedParameter> namedParameters, Object targetObject, EntityCache entityCache,
			String targetTableName, boolean showSql, DescriptionSQL descriptionSQL) {
		super(session, sql, namedParameters, targetObject, entityCache, targetTableName, showSql, descriptionSQL, true);
	}

	public BatchCommandSQL(SQLSession session, CommandSQL[] commands, int batchSize) {
		super(session, null, null, null, null, null, false, null, true);
		this.commands = commands;
		this.batchSize = batchSize;
	}

	@Override
	public CommandSQLReturn execute() throws Exception {

		List<CommandSQLReturn> returns = new ArrayList<CommandSQLReturn>();

		for (CommandSQL command : commands) {
			CommandSQLReturn sqlReturn = command.execute();
			if (sqlReturn != null) {
				returns.add(sqlReturn);
			}
		}
		
		List<Object[]> batchParameters = new ArrayList<Object[]>();
		String sql = null;
		int batchCount = 1;
		for (CommandSQLReturn ret : returns) {
			if ((ret.getSql().equals(sql) || (sql == null)) && (batchCount <= batchSize)) {
				batchParameters.add(ret.getParameters());
				sql = ret.getSql();
				batchCount++;
			} else {
				queryRunner.batch(session.getConnection(), sql, batchParameters.toArray(new Object[][] {}));
				batchParameters.clear();
				batchParameters.add(ret.getParameters());
				sql = ret.getSql();
				batchCount = 1;
			}
		}
		if (batchSize > 0) {
			queryRunner.batch(session.getConnection(), sql, batchParameters.toArray(new Object[][] {}));
		}

		return null;
	}

	@Override
	public boolean isNewEntity() {
		return false;
	}

}
