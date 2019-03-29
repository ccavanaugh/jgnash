package jgnash.util;

/**
 * A simple recursive decent math parser taken from public domain code:
 * <p>
 * https://stackoverflow.com/questions/3422673/how-to-evaluate-a-math-expression-given-in-string-form/26227947#26227947
 */
public class MathEval {

    private final String str;

    private int pos = -1;
    private int ch;

    private MathEval(final String str) {
        this.str = str;
    }

    public static double eval(final String str) throws ArithmeticException {
        return new MathEval(str).parse();
    }

    private void nextChar() {
        ch = (++pos < str.length()) ? str.charAt(pos) : -1;
    }

    private boolean eat(final int charToEat) {
        while (ch == ' ') {
            nextChar();
        }

        if (ch == charToEat) {
            nextChar();

            return true;
        }
        return false;
    }

    private double parse() throws ArithmeticException {
        nextChar();

        double x = parseExpression();

        if (pos < str.length()) {
            throw new ArithmeticException("Unexpected: " + (char) ch);
        }
        return x;
    }

    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)`
    //        | number | functionName factor | factor `^` factor

    private double parseExpression() {
        double x = parseTerm();

        for (; ; ) {
            if (eat('+')) {
                x += parseTerm(); // addition
            } else if (eat('-')) {
                x -= parseTerm(); // subtraction
            } else {
                return x;
            }
        }
    }

    private double parseTerm() {
        double x = parseFactor();

        for (; ; ) {
            if (eat('*')) {
                x *= parseFactor(); // multiplication
            } else if (eat('/')) {
                x /= parseFactor(); // division
            } else {
                return x;
            }
        }
    }

    private double parseFactor() {
        if (eat('+')) { // unary plus
            return parseFactor();
        }

        if (eat('-')) {  // unary minus
            return -parseFactor();
        }

        double x;

        int startPos = this.pos;

        if (eat('(')) { // parentheses
            x = parseExpression();
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                nextChar();
            }
            try {
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } catch (final NumberFormatException nfe) {
                return Double.NaN;  // return NaN if the parser failed
            }
        } else {
            throw new ArithmeticException("Unexpected: " + (char) ch);
        }

        return x;
    }

}
