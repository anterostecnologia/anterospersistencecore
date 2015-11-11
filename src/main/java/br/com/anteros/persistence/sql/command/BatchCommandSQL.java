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

	public BatchCommandSQL(SQLSession session, CommandSQL[] commands, int batchSize, boolean showSql) {
		super(session, null, null, null, null, null, showSql, null, true);
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
		int batchCount = 0;
		for (CommandSQLReturn ret : returns) {
			if ((ret.getSql().equals(sql) || (sql == null)) && (batchCount < batchSize)) {
				batchParameters.add(ret.getParameters());
				sql = ret.getSql();
				batchCount++;
			} else {
				/*
				 * Executa e zera fila, reinicia contagem
				 */
				queryRunner.batch(session.getConnection(), sql, batchParameters.toArray(new Object[][] {}), showSql, session.isFormatSql(), null, null);
				batchParameters.clear();
				batchCount = 0;
				/*
				 * Adiciona e incrementa 1(um)
				 */
				batchParameters.add(ret.getParameters());
				sql = ret.getSql();
				batchCount++;
			}
		}
		if (batchCount > 0) {
			queryRunner.batch(session.getConnection(), sql, batchParameters.toArray(new Object[][] {}), showSql, session.isFormatSql(), null, null);
		}

		return null;
	}

	@Override
	public boolean isNewEntity() {
		return false;
	}

}
