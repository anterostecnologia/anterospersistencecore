package br.com.anteros.persistence.session.query.filter;


import java.util.HashMap;
import java.util.Map;

import br.com.anteros.core.utils.StringUtils;

public class DefaultFilterBuilder extends BaseVisitor {
  public static final String DEFAULT_BASE_VARIABLE_NAME = "param";
  private static final String OPEN_BRACKET = "(";
  private static final String BETWEEN = "BETWEEN";
  private static final String CLOSE_BRACKET = ")";

  protected StringBuilder result = new StringBuilder();
  private int variableIndex = 1;
  private Map<String, Object> params = new HashMap<String, Object>();

  public StringBuilder getResult() {
    return result;
  }
  
  public Map<String, Object> getParams() {
    return params;
  }

  protected String addVariable(final Object value, final String baseColumnName) {
    final String columnName = "P"+baseColumnName + variableIndex++;
    params.put(columnName, value);
    return columnName;
  }

  private String getVariableName(final Object obj, final String defaultName) {
    if (obj instanceof FieldExpression){
      return ((FieldExpression) obj).getName();
    }
    else {
      return defaultName;
    }
  }

  private void acceptOrVisitValue(final Object value, final String baseName)
      throws FilterException {
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
	  result = new StringBuilder();
	  variableIndex = 1;
	  params = new HashMap<String, Object>();
      filter.accept(this);
  }
  
  public void visit(final FieldExpression column) {
    result.append(column.getName());
  }

  public void visit(final OperationExpression exp) throws FilterException {
    String baseVariableName = getVariableName(exp.getLhsValue(), null);
    if (baseVariableName == null)
      baseVariableName = getVariableName(exp.getRhsValue(), "var");

    result.append(OPEN_BRACKET);
    acceptOrVisitValue(exp.getLhsValue(), baseVariableName);
    if ((exp.getRhsValue() == null) //
        || ((exp.getRhsValue() instanceof Nullable) //
        && ((Nullable) exp.getRhsValue()).isNull())) {
      if (!Operator.EQ.equals(exp.getOperator()))
        throw new FilterException("Não é possível usar valor NULO com operador."
            + exp.getOperator().getValue());
      result.append(" ").append(Operator.IS.getValue());
      result.append(" ").append(Constant.NULL);
    } else {
      result.append(" ").append(exp.getOperator().getValue()).append(" ");
      acceptOrVisitValue(exp.getRhsValue(), baseVariableName);
    }
    result.append(CLOSE_BRACKET);
  }

  public void visit(final BetweenExpression betweenExp) throws FilterException {
    final FieldExpression column = betweenExp.getField();
    if ((betweenExp.getValueStart() == null)
        || (betweenExp.getValueEnd() == null))
      throw new FilterException(
          "Não é possível aplicar ENTRE com um valor nulo.");

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
      acceptOrVisitValue(value, inExp.getField().getName());
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
}