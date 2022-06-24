package br.com.anteros.persistence.session.query.filter;

import java.util.ArrayList;
import java.util.List;

import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.metadata.EntityCache;
import br.com.anteros.persistence.metadata.descriptor.DescriptionField;
import br.com.anteros.persistence.parameter.NamedParameter;
import br.com.anteros.persistence.session.SQLSession;
import br.com.anteros.persistence.session.exception.SQLSessionException;

public class DefaultFilterBuilder extends BaseVisitor {
	public static final String DEFAULT_BASE_VARIABLE_NAME = "param";
	private static final String OPEN_BRACKET = "(";
	private static final String BETWEEN = "BETWEEN";
	private static final String CLOSE_BRACKET = ")";

	protected StringBuffer result = new StringBuffer();
	private int variableIndex = 1;
	private List<NamedParameter> params = new ArrayList<NamedParameter>();
	private SQLSession session;
	private Class<?> resultClass;
	private String alias;

	public StringBuffer getResult() {
		return result;
	}

	public List<NamedParameter> getParams() {
		return params;
	}

	protected String addVariable(final Object value, final String baseColumnName) {
		final String columnName = "P" + baseColumnName + variableIndex++;
		params.add(new NamedParameter(columnName, value));
		return columnName;
	}

	private String getVariableName(final Object obj, final String defaultName) {
		if (obj instanceof FieldExpression) {
			FieldExpression column = (FieldExpression) obj;
			EntityCache[] entityCaches = session.getEntityCacheManager().getEntitiesBySuperClassIncluding(resultClass);
			if (entityCaches==null) {
				throw new SQLSessionException("Não foi encontrada entidade sendo gerenciada da classe "+resultClass.getSimpleName());
			}
			DescriptionField descriptionField=null;
			for (EntityCache entityCache : entityCaches) {
				descriptionField = entityCache.getDescriptionField(column.getName());
				if (descriptionField!=null){
					break;
				}
			}
			
			if (descriptionField==null) {
				throw new SQLSessionException("Não foi encontrada campo "+column.getName()+" na entidade gerenciada "+resultClass.getSimpleName());
			}
			return descriptionField.getSimpleColumn().getColumnName();
		} else {
			return defaultName;
		}
	}

	private void acceptOrVisitValue(final Object value, final String baseName) throws FilterException {
		if (value instanceof Visitable)
			((Visitable) value).accept(this);
		else {
			final String variableName = addVariable(value, baseName);
			result.append(":").append(variableName);
		}
	}

	public void visitValue(final Object value) {
		final String variableName = addVariable(value, DEFAULT_BASE_VARIABLE_NAME);
		result.append(":").append(variableName);
	}

	protected void buildFilter(final FilterExpression filter) throws FilterException {
		result = new StringBuffer();
		variableIndex = 1;
		params = new ArrayList<NamedParameter>();
		filter.accept(this);
	}

	public void visit(final FieldExpression column) {
		EntityCache[] entityCaches = session.getEntityCacheManager().getEntitiesBySuperClassIncluding(resultClass);
		if (entityCaches==null) {
			throw new SQLSessionException("Não foi encontrada entidade sendo gerenciada da classe "+resultClass.getSimpleName());
		}
		DescriptionField descriptionField=null;
		for (EntityCache entityCache : entityCaches) {
			descriptionField = entityCache.getDescriptionField(column.getName());
			if (descriptionField!=null){
				break;
			}
		}
		
		if (descriptionField==null) {
			throw new FilterException("Não foi encontrada campo "+column.getName()+" na entidade gerenciada "+resultClass.getSimpleName());
		}
		if (StringUtils.isNotEmpty(alias))
			result.append(alias+"."+descriptionField.getSimpleColumn().getColumnName());
		else 
			result.append(descriptionField.getSimpleColumn().getColumnName());
	}

	public void visit(final OperationExpression exp) throws FilterException {
		String baseVariableName = getVariableName(exp.getLhsValue(), null);
		if (baseVariableName == null)
			baseVariableName = getVariableName(exp.getRhsValue(), "var");

		result.append(OPEN_BRACKET);
		acceptOrVisitValue(exp.getLhsValue(), baseVariableName);
		if ((exp.getRhsValue() == null) || (exp.getRhsValue().equals(Constant.NULL.name()))
				|| (exp.getRhsValue() instanceof Nullable)) {
			if (!Operator.IS.equals(exp.getOperator()) && !Operator.IS_NOT.equals(exp.getOperator()))
				throw new FilterException(
						"Não é possível usar valor NULO com operador." + exp.getOperator().getValue());
			if (Operator.IS.equals(exp.getOperator())) {
				result.append(" ").append(Operator.IS.getValue());
			} else if (Operator.IS_NOT.equals(exp.getOperator())) {
				result.append(" ").append(Operator.IS_NOT.getValue());
			}
			result.append(" ").append(Constant.NULL);

		} else {
			result.append(" ").append(exp.getOperator().getValue()).append(" ");
			acceptOrVisitValue(exp.getRhsValue(), baseVariableName);
		}
		result.append(CLOSE_BRACKET);
	}

	public void visit(final BetweenExpression betweenExp) throws FilterException {
		final FieldExpression column = betweenExp.getField();
		if ((betweenExp.getValueStart() == null) || (betweenExp.getValueEnd() == null))
			throw new FilterException("Não é possível aplicar ENTRE com um valor nulo.");

		result.append(OPEN_BRACKET);
		visit(column);
		result.append(" ").append(BETWEEN).append(" ");
		acceptOrVisitValue(betweenExp.getValueStart(), column.getName());
		result.append(" ").append(Operator.AND).append(" ");
		acceptOrVisitValue(betweenExp.getValueEnd(), column.getName());
		result.append(CLOSE_BRACKET);
	}

	public void visit(final GroupExpression groupExpression) throws FilterException {
		result.append(OPEN_BRACKET);
		final Operator operator = groupExpression.getOperator();
		boolean firstExpression = true;
		for (final FilterExpression expression : groupExpression.getExpressions()) {
			if (firstExpression)
				firstExpression = false;
			else
				result.append(" ").append(operator.getValue()).append(" ");
			expression.accept(this);
		}
		result.append(CLOSE_BRACKET);
	}

	public void visit(final InExpression inExp) throws FilterException {
		if (StringUtils.isEmpty(inExp.getValues()))
			throw new FilterException("Valores CONTIDOS não podem ser vazios ou nulos.");

		result.append(OPEN_BRACKET);
		inExp.getField().accept(this);
		result.append(" ");
		if (inExp.isNegative())
			result.append(Operator.NOT.getValue()).append(" ");
		result.append(Operator.IN.getValue()).append(" ").append(OPEN_BRACKET);
		boolean firstValue = true;
		for (final Object value : inExp.getValues()) {
			if (firstValue)
				firstValue = false;
			else
				result.append(",");
			result.append(value);
		}
		result.append(CLOSE_BRACKET);
		result.append(CLOSE_BRACKET);
	}

	public void visit(final Constant constant) {
		result.append(constant.getValue());
	}

	public void visit(final Filter filter) throws FilterException {

		final FilterExpression filterCondition = filter.getFilterExpression();
		if ((filterCondition != null))
			buildFilter(filterCondition);
	}

	public void visit(final Nullable nullable) throws FilterException {
		acceptOrVisitValue(nullable.getValue());
	}
	
	public String toSql(Filter filter, SQLSession session, Class<?> resultClass) throws FilterException {
		return toSql("TBL",filter,session,resultClass);
	}

	public String toSql(String alias, Filter filter, SQLSession session, Class<?> resultClass) throws FilterException {
		this.alias = alias;
		this.session = session;
		this.resultClass = resultClass;
		filter.runVisitors();
		filter.accept(this);
		final String sqlQuery = this.getResult().toString();
		return sqlQuery;
	}
	
	public String toSortSql(Filter filter, SQLSession session, Class<?> resultClass) throws FilterException {
		return toSortSql("TBL", filter, session, resultClass);
	}

	public String toSortSql(String alias, Filter filter, SQLSession session, Class<?> resultClass) throws FilterException {
		this.alias = alias;
		this.session = session;
		this.resultClass = resultClass;
		String result = "";
		boolean appendDelimiter = false;
		for (FieldSort field : filter.getFieldsToSort()) {
			if (appendDelimiter)
				result += ", ";
			appendDelimiter = true;


			EntityCache[] entityCaches = session.getEntityCacheManager().getEntitiesBySuperClassIncluding(resultClass);
			if (entityCaches==null) {
				throw new SQLSessionException("Não foi encontrada entidade sendo gerenciada da classe "+resultClass.getSimpleName());
			}
			DescriptionField descriptionField=null;
			for (EntityCache entityCache : entityCaches) {
				descriptionField = entityCache.getDescriptionField(field.getField());
				if (descriptionField!=null){
					break;
				}
			}

			if (descriptionField==null) {
				throw new FilterException("Não foi encontrada campo "+field.getField()+" na entidade gerenciada "+resultClass.getSimpleName());
			}

			result += descriptionField.getSimpleColumn().getColumnName()+" "+field.getOrder();
		}
		return result;
	}

}
