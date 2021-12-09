package Backend;

import Backend.Exceptions.InvalidTermException;
import Backend.ExpressionBuilders.BooleanValuedExpressionFactory;
import Backend.ExpressionBuilders.RealValuedExpressionFactory;
import Backend.Expressions.Expression;
import Backend.Expressions.FunctionExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RealBooleanCreator implements ExpressionCreator{
    protected final ExpressionPropertyReporter propertyReporter;
    protected final Map<String, FunctionExpression> funcMap = new HashMap<>();
    protected final RealValuedExpressionFactory realValuedExpressionFactory = new RealValuedExpressionFactory();
    protected final BooleanValuedExpressionFactory booleanValuedExpressionFactory = new
            BooleanValuedExpressionFactory();

    public RealBooleanCreator(Map<String, FunctionExpression> funcMap, ExpressionPropertyReporter vc) {
        for (String funcName : funcMap.keySet()) {
            this.funcMap.put(funcName, funcMap.get(funcName));
        }
        this.propertyReporter = vc;
    }

    public abstract Expression<?> create(List<String> terms) throws InvalidTermException;

}