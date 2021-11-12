package Backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO: IGNORE THIS FILE OMAR!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

class BaseCaseCreatorException extends Exception {
    public BaseCaseCreatorException(String message) {
        super(message);
    }
}

class CompoundCaseCreatorException extends Exception {
    public CompoundCaseCreatorException(String message) {
        super(message);
    }
}

class NonCreatorException extends Exception {
    public NonCreatorException(String message) {
        super(message);
    }
}

public class ExpressionCreatorFromOldMain {

    private final Constants constants = new Constants();

    public void precheck(List<String> terms) throws CompoundCaseCreatorException, NonCreatorException {
        if (!checkMatchingBrackets(terms)) {
            throw new CompoundCaseCreatorException("UnmatchedBracketsException!");
        }
        else if (!checkAllTermsValid(terms)) {
            throw new CompoundCaseCreatorException("InvalidTermException");
        }
        else if (!checkFunctionBrackets(terms)) { // TODO: Perhaps move FunctionBracketsException to somewhere else, or not!
            throw new CompoundCaseCreatorException("FunctionBracketsException");
        }
    }

    /** Converts a (valid) expression (represented as a list) into an Backend.Expression // Ted: TODO: No longer have to be totally valid, but must be checked by "precheck" before.
     * @param terms A list of terms in the expression (see below for how they should be broken up
     * @return An Backend.Expression (AST) representation of the expression
     */
    // e.g. x ^ 2 + 5 -> ["x", "^", "2", "+", "5"]
    // e.g. (2) + 3 or 3 + (2) -> ["(", "2", ")", "+", "3"]
    // e.g. cos(x) -> ["cos", "(", "x", ")"]
    public Expression create(List<String> terms) throws BaseCaseCreatorException, CompoundCaseCreatorException, NonCreatorException {
        Expression returnExpression = null;
        // Below minimalTerms is redundant-outer-brackets-reduced.
        List<String> minimalTerms = bracketsReduction(terms);

        // Below base cases.
        if (minimalTerms.size() == 0) {
            throw new BaseCaseCreatorException("NullExpressionException");
        }
        else if (minimalTerms.size() == 1) {
            // Only five cases:
            // 1. Number(Float, multi-digit integer).
            // 2. Operator.
            // 3. Special Character (like just brackets for now).
            // 4. Special Variable.
            // 5. Function.
            String term = minimalTerms.get(0);
            if (constants.getVariables().contains(term)){
                returnExpression = new VariableExpression(term);
            }
            else if (checkNumber(term)) {
                returnExpression = new NumberExpression(term);
            }
            else {
                throw new BaseCaseCreatorException("InvalidSingleExpression");
            }
        }

        // Somewhat recursive and base case (input is an expression, but meanwhile the expression must be entirely within the function).
        else if(constants.getFunctions().contains(minimalTerms.get(0)) &&
                containsOuterBrackets(minimalTerms.subList(1, minimalTerms.size()))) {
            returnExpression = buildBuiltInFunctionExpression(minimalTerms);
        }

        // Recursive case. Wow, so elegant :)
        else {
            returnExpression = createOnOperators(minimalTerms);
        }

        return returnExpression;
    }

    ////////////////// BELOW HELPERS ///////////////////

    private Expression createOnOperators(List<String> terms) throws CompoundCaseCreatorException, NonCreatorException {
        Map<String, Integer> operatorAndIndices = getOuterOperators(terms);
        Expression lExpression, rExpression;

        // We first find what operators are not inside any brackets
        // (operators inside brackets are dealt with deeper in the recursion)
        // Then we sort them by (reverse) order of precedence to ensure
        // that order of operations is maintained.
        // Don't need to get all the necessary operators, only one with the lowest precedence.
        // Ted: Clever, since the op with lowest precedence (e.g. '-', '+' among all supported ops) should be
        // evaluated lastly. //
        // Ted: Handling exception idea: since we have checked everything else is correct, except for the operands of
        // each operator, the only invalid case will be two operators together or either operand of an operator is empty.
        // Since we parse the left and right terms with respect to an operator, the "two-operators-together" case will
        // eventually fall in to the "either-operand-is-empty" case. Fuck yea. // TODO: Delete "Fuck yea".

        for (String op : constants.getOperators()) {
            if (operatorAndIndices.containsKey(op)) {
                int opIndex = operatorAndIndices.get(op);
                List<String> leftTerms = terms.subList(0, opIndex);
                List<String> rightTerms = terms.subList(opIndex + 1, terms.size());
                try {
                    lExpression = create(leftTerms);
                } catch (BaseCaseCreatorException e) {
                    throw new CompoundCaseCreatorException("LeftOperandException");
                }
                try {
                    rExpression = create(rightTerms);
                } catch (BaseCaseCreatorException e) {
                    throw new CompoundCaseCreatorException("RightOperandException");
                }
                return new OperatorExpression(op, lExpression, rExpression);
            }
        }
        throw new NonCreatorException("ExpressionReaderWrongException"); // TODO: Just a check for validity of ExpressionReader, should be deleted in future.
    }

    private List<String> bracketsReduction(List<String> terms) {
        List<String> terms_copy = terms;
        while (containsOuterBrackets(terms_copy)) {
            terms_copy = terms_copy.subList(1, terms_copy.size() - 1);
        }
        return terms_copy;
    }

    private boolean checkNumber(String term) {
        try {
            Double num = Double.parseDouble(term); // Just check for whether "term" represents a number.
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // TODO: Seems static, and we thought to use a static checker before, but actually we are just ignorant (e.g.
    //  what if each check is very time consuming? as somewhat shown in "checkAllTermsValid", there could be many
    //  many many builf-in functions!). The conclusion is that, although in "checkMatchingBracktets, we do as below
    //  (looks like it can be static and unnecessary to be recursive), we actually shouldn't do any checking when
    //  traversing inside of bracktes!
    private boolean checkMatchingBrackets(List<String> terms) {
        int counter = 0;
        int index = 0;
        while (counter >= 0 && index <= terms.size() - 1) {
            String curr = terms.get(index);
            if (curr.equals("(")) {
                counter++;
            }
            else if (curr.equals(")")) {
                counter--;
            }
            index++;
        }
        return counter == 0;
    }

    // TODO: Below change to DON'T CHECK if traversing inside of brackets!
    private boolean checkAllTermsValid(List<String> terms) {
        for (int i = 0; i <= terms.size() - 1; i++) {
            String term = terms.get(i);

            if (!(checkNumber(term) |
                    constants.getVariables().contains(term) |
                    constants.getOperators().contains(term) |
                    constants.getFunctions().contains(term) |
                    constants.getSPECIAL_CHARS().contains(term))
            ) {
                return false;
            }
        }
        return true;
    }

    private boolean checkFunctionBrackets(List<String> terms) throws NonCreatorException {
        Map<Integer, String> indexAndFunction = getOuterFunctions(terms);
        for (int index : indexAndFunction.keySet()) {
            // Below only checks for whether it's posible to have two brackets after the function, but doesn't care
            // whether function inputs are correct.
            if (index >= terms.size() - 2) {
                return false;
            }
            else if (terms.get(index + 1).equals("(")) {
                return findCorrespondingBracket(terms, index + 1) != -1;
            }
            else { return false; }
        }
        return true;
    }

//    private boolean checkFunctionInputs(List<String> terms) {}; // TODO: Check for multiple inputs.

    private Map<Integer, String> getOuterFunctions(List<String> terms) {
        // Original thought: throw exception "InvalidTermException" if it detects any unacceptable term!
        // Current thought: NO. The rule may change in the future, so let it just do what it should do, and have "checkOuterInvalidTerm" do what it does.
        // Idea similar to "getOuterOperators"!
        Map<Integer, String> indexAndFunction = new HashMap<>();
        int bracketCounter = 0;
        for (int i = 0; i <= terms.size() - 1; i++){
            String term = terms.get(i);

            if (term.equals("(")){
                bracketCounter += 1;
            } else if (term.equals(")")){
                bracketCounter -= 1;
            }

            if (bracketCounter == 0){
                if (constants.getFunctions().contains(term)) {
                    indexAndFunction.put(i, term);
                }
            }
        }
        return indexAndFunction;
    }

//    /**
//     * Note that exponentiation is not associative. This is a sub-case of "checkRestOperators".
//     * @param terms
//     * @return
//     */
//    private boolean checkExponentiation(List<String> terms) {}; // TODO: If have time.

    // TODO: Delete below or no? Does this appear in anywhere else?
    private Expression buildBuiltInFunctionExpression(List<String> terms) throws BaseCaseCreatorException,
            CompoundCaseCreatorException, NonCreatorException {
        Expression innerExpression = create(terms.subList(2, terms.size() - 1));
        return switch (terms.get(0)) {
            case "cos" -> new CosExpression(innerExpression);
            case "sin" -> new SinExpression(innerExpression);
            case "tan" -> new TanExpression(innerExpression);
            case "sqrt" -> new SqrtExpression(innerExpression);
            default -> throw new IllegalArgumentException("Unrecognised function");
        };
    }

    /** Returns a map of operators that are not in any brackets (in the order that they appear)
     *  along with the indices that they appear at.
     *  If multiple instances of the same operator are present (outside any brackets),
     *  then only the first appearance is noted
     * @param terms The list of terms as accepted by the create method
     *              e.g. ["2", "*", "(", "5", "+", "6", ")", "-", "9"]
     * @return The list of operators that are not in any brackets. For the example above, we get
     *              {"*": 1, "-": 7}.
     */

    private Map<Integer, String> getOuterOperators(List<String> terms){

        Map<Integer, String> indexAndOperator = new HashMap<>();

        // We use the bracketCounter to track whether we are inside
        // a pair of brackets or not
        int bracketCounter = 0;

        // We iterate over the terms, if we encounter ')', we increment counter
        // by 1 and if we encounter '(' we decrement it by 1
        // Thus we know we are outside every pair of brackets when counter is 0
        // We need to go in reverse order as the operators at the end
        // have lower precedence and those up ahead.
        // e.g. 2 - 1 - 3 == (2 - 1) - 3 != 2 - (1 - 3)
        for (int i = terms.size() - 1; i > -1; i--){
            String term = terms.get(i);

            if (term.equals(")")){
                bracketCounter += 1;
            } else if (term.equals("(")){
                bracketCounter -= 1;
            }

            if (bracketCounter == 0){
                if (constants.getOperators().contains(term) &&
                        !indexAndOperator.containsValue(term)) {
                    indexAndOperator.put(i, term);
                }
            }
        }
        return indexAndOperator;
    }

    /**
     * @param terms The list of terms as accepted by the create method
     *              e.g. ["2", "*", "(", "5", "+", "6", ")", "-", "9"]
     * @return Returns True if and only if the terms are entirely within a pair of brackets
     *         ["(", "4", "+", "3", ")"] -> true (this list represents "(4 + 3)")
     *         ["(", "4", "), "+", "(", "3", ")"] -> false (this list represents "(4) + (3)") Since between
     *         4 and 3, there are brackets inside that encloses either of the two outermost brackets.
     */
    private boolean containsOuterBrackets(List<String> terms){

        if (terms.size() <= 1){
            return false;
        }

        // Same bracketCounter idea as in getOuterOperators
        // However in this case we want to ensure that we only reach 0
        // when we get to the end of the expression
        // Reaching 0 before then means that the corresponding ')'
        // is in the middle of the expression
        int counter = 0;

        for (int i = 0; i < terms.size() - 1; i++){
            String c = terms.get(i);
            if (c.equals("(")){
                counter += 1;
            } else if (c.equals(")")){
                counter -= 1;
            }

            if (counter == 0){
                return false;
            }
        }
        return true;
    }

    private int findCorrespondingBracket(List<String> terms, int index) throws NonCreatorException {
        int Counter = 0;
        boolean initialState = true;
        boolean rightTraverse = true;

        while (0 <= index && index <= terms.size()){
            // Below section initializes the traversal direction.
            if (initialState) {
                if (terms.get(index).equals(")")) {
                    rightTraverse = false;
                }
                else if (!terms.get(index).equals("(")) {
                    throw new NonCreatorException("TermNotBracketException");
                }
                initialState = false;
            }

            // Below section tries finding the corresponding bracket.
            if (terms.get(index).equals("(")){
                Counter += 1;
            }
            else if (terms.get(index).equals(")")){
                Counter -= 1;
            }
            if (Counter == 0){
                return index;
            }

            // Below section decides the traversal direction.
            if (rightTraverse) { index++; }
            else { index--; }
        }
        return -1;
    }
}




///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Below From Old Main!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!





//package Backend;
//
//import Backend.Expressions.Expression;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
//public class ExpressionCreator {
//
//    private final Constants constants = new Constants();
//
//    /** Converts a (valid) expression (represented as a list) into an Backend.Expressions.Expression
//     * @param terms A list of terms in the expression (see below for how they should be broken up
//     * @return An Backend.Expressions.Expression (AST) representation of the expression
//     */
//    // e.g. x ^ 2 + 5 -> ["x", "^", "2", "+", "5"]
//    // e.g. (2) + 3 or 3 + (2) -> ["(", "2", ")", "+", "3"]
//    // e.g. cos(x) -> ["cos", "(", "x", ")"]
//    public Expression create(List<String> terms) {
//        Expression returnExpression = null;
//
//        // Base case for the recursion
//        // One term means it's a variable, number or a function that takes in some input
//        if (terms.size() == 1) {
//            String term = terms.get(0);
//            ExpressionBuilder eb = new ExpressionBuilder();
//            returnExpression = eb.constructExpression(term);
//        }
//
//        // check that we have only one expression that we are composing with our built-in functions
//        else if (constants.getBuildInFunctions().contains(terms.get(0)) &&
//                containsOuterBrackets(terms.subList(1, terms.size()))) {
//            Expression[] inputs = findFunctionInputs(terms);
//            ExpressionBuilder eb = new ExpressionBuilder();
//            returnExpression = eb.constructExpression(terms.get(0), inputs);
//        }
//
//        // Recursive step
//        else {
//            // We first find what operators are not inside any brackets
//            // (operators inside brackets are dealt with deeper in the recursion)
//            // Then we sort them by (reverse) order of precedence to ensure
//            // that order of operations is maintained
//
//            // TODO: Don't need to get all the necessary operators, only one with the lowest precedence
//            Map<String, Integer> operatorAndIndices = getOuterOperators(terms);
//
//            // Go through the different types of operators in reverse order of precedence.
//            // TODO: use a better empty expression representation than null
//            returnExpression = createExpressionRecursiveHelper("Logical", operatorAndIndices, terms);
//            if (returnExpression == null) {
//                returnExpression = createExpressionRecursiveHelper("Comparator", operatorAndIndices, terms);
//            }
//            if (returnExpression == null) {
//                returnExpression = createExpressionRecursiveHelper("Operator", operatorAndIndices, terms);
//            }
//        }
//
//        return returnExpression;
//    }
//
//    /** Returns a map of operators that are not in any brackets (in the order that they appear)
//     *  along with the indices that they appear at.
//     *  If multiple instances of the same operator are present (outside any brackets),
//     *  then only the first appearance is noted
//     * @param terms The list of terms as accepted by the create method
//     *              e.g. ["2", "*", "(", "5", "+", "6", ")", "-", "9"]
//     * @return The list of operators that are not in any brackets. For the example above, we get
//     *              {"*": 1, "-": 7}
//     */
//    private Map<String, Integer> getOuterOperators(List<String> terms){
//
//        Map<String, Integer> operatorAndIndex= new HashMap<>();
//
//        // We use the bracketCounter to track whether we are inside
//        // a pair of brackets or not
//        int bracketCounter = 0;
//
//        // We iterate over the terms, if we encounter ')', we increment counter
//        // by 1 and if we encounter '(' we decrement it by 1
//        // Thus we know we are outside every pair of brackets when counter is 0
//        // We need to go in reverse order as the operators at the end
//        // have lower precedence and those up ahead.
//        // e.g. 2 - 1 - 3 == (2 - 1) - 3 != 2 - (1 - 3)
//        for (int i = terms.size() - 1; i > -1; i--){
//            String term = terms.get(i);
//
//            if (term.equals(")")){
//                bracketCounter += 1;
//            } else if (term.equals("(")){
//                bracketCounter -= 1;
//            }
//
//            if (bracketCounter == 0){
//                boolean bOperatorFound = constants.getOperators().contains(term) ||
//                        constants.getComparators().contains(term) ||
//                        constants.getLogicalOperators().contains(term);
//
//                if (bOperatorFound && !operatorAndIndex.containsKey(term)){
//                    operatorAndIndex.put(term, i);
//                }
//            }
//        }
//
//        return operatorAndIndex;
//    }
//
//
//    /**
//     * @param terms The list of terms as accepted by the create method
//     *              e.g. ["2", "*", "(", "5", "+", "6", ")", "-", "9"]
//     * @return Returns True if and only if the terms are entirely within a pair of brackets
//     *         ["(", "4", "+", "3", ")"] -> true (this list represents "(4 + 3)")
//     *         ["(", "4", "), "+", "(", "3", ")"] -> false (this list represents "(4) + (3)")
//     */
//    private boolean containsOuterBrackets(List<String> terms){
//
//        if (terms.size() == 1){ return false; }
//
//        // Same bracketCounter idea as in getOuterOperators
//        // However in this case we want to ensure that we only reach 0
//        // when we get to the end of the expression
//        // Reaching 0 before then means that the corresponding ')'
//        // is in the middle of the expression
//        int counter = 0;
//
//        for (int i = 0; i < terms.size() - 1; i++){
//            String c = terms.get(i);
//            if (c.equals("(")){
//                counter += 1;
//            } else if (c.equals(")")){
//                counter -= 1;
//            }
//
//            if (counter == 0){
//                return false;
//            }
//
//        }
//        return true;
//    }
//
//    private Expression createExpressionRecursiveHelper(String expressionType, Map<String, Integer> operatorAndIndices,
//                                                       List<String> terms) {
//        // TODO: javadoc
//        List<String> candidateOperators = new ArrayList<>();
//        switch (expressionType) {
//            case "Operator":
//                candidateOperators = constants.getOperators();
//                break;
//            case "Comparator":
//                candidateOperators = constants.getComparators();
//                break;
//            case "Logical":
//                candidateOperators = constants.getLogicalOperators();
//                break;
//        }
//
//        for (String op : candidateOperators) {
//            if (operatorAndIndices.containsKey(op)) {
//                int opIndex = operatorAndIndices.get(op);
//
//                List<String> leftTerms = terms.subList(0, opIndex);
//                List<String> rightTerms = terms.subList(opIndex + 1, terms.size());
//
//                // Via induction, we only need to deal with cases
//                // where the left or right expressions are contained in a pair of brackets
//                // e.g. (2 + 3) * 5
//                // If this is not the case, we will get to such a case recursively
//                if (containsOuterBrackets(leftTerms)) {
//                    // we remove the first and last term which we know are '(' and ')' respectively
//                    leftTerms = leftTerms.subList(1, leftTerms.size() - 1);
//                }
//                if (containsOuterBrackets(rightTerms)) {
//                    rightTerms = rightTerms.subList(1, rightTerms.size() - 1);
//                }
//
//                // Recursively create expressions for the left and right terms
//                Expression lExpression = create(leftTerms);
//                Expression rExpression = create(rightTerms);
//
//                ExpressionBuilder eb = new ExpressionBuilder();
//                return eb.constructExpression(lExpression, op, rExpression);
//            }
//        }
//
//        return null;
//    }
//
//
//    /**
//     * @param terms List of terms as accepted by create, assumed to be of the form [func, (, ..., )]
//     * @return A list of Expressions where each expression is an input to some function
//     */
//    private Expression[] findFunctionInputs (List<String> terms){
//        List<Integer> commaIndices = findCommaIndices(terms);
//        // we add the final index (corresponding to ')' )
//        // this ensures that between every pair of indices in commaIndices
//        // we have an input expression
//        commaIndices.add(terms.size() - 1);
//
//        Expression[] inputs = new Expression[commaIndices.size()];
//        // start at 2 because first item if function name and second item is '('
//        int startInd = 2;
//
//        for (int i = 0; i < inputs.length; i++){
//            inputs[i] = create(terms.subList(startInd, commaIndices.get(i)));
//            startInd = commaIndices.get(i) + 1;
//        }
//
//        return inputs;
//
//    }
//
//
//    /**
//     * Assumed to be for inputs like [min, (, x, y, )] but could be used for anything
//     * @param terms List of terms as accepted by create
//     * @return List of indices corresponding to where the "," character appears
//     */
//    private List<Integer> findCommaIndices(List<String> terms){
//        List<Integer> commaIndices = new ArrayList<>();
//
//        int startInd = 0;
//
//        while (terms.subList(startInd, terms.size()).contains(",")){
//            commaIndices.add(terms.indexOf(","));
//            startInd = terms.indexOf(",") + 1;
//        }
//
//        return commaIndices;
//    }
//}
