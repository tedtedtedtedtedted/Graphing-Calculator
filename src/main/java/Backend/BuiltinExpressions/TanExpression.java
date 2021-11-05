package Backend.BuiltinExpressions;

import Backend.Expression;
import Backend.FunctionExpression;

import java.util.Map;

public class TanExpression extends FunctionExpression {

    // Only one input for cos of course
    // But the input can be an expression of any kind
    public TanExpression(Expression[] inputs){
        super("tan", inputs);
    }

    @Override
    public double evaluate(Map<String, Double> arguments) {
        return Math.tan(getInputs()[0].evaluate(arguments));
    }
}