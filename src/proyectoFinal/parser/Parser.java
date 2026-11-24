package proyectoFinal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proyectoFinal.lexer.Token;
import proyectoFinal.lexer.TokenType;
import proyectoFinal.model.Diagnostic;
import proyectoFinal.model.Diagnostic.Category;

/** Parser descendente recursivo para la gramática documentada del subconjunto de Java. */
public final class Parser {
    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens == null ? Collections.<Token>emptyList() : tokens;
    }

    public ParserResult parse() {
        AstNode root = null;
        try {
            root = parseClass();
            consumeEnd();
        } catch (ParseFailure failure) {
            // El error de estructura principal impide construir un árbol completo.
        }
        return new ParserResult(root, diagnostics);
    }

    private AstNode parseClass() {
        Token start = peek();
        if (matchText("public")) start = previous();
        consumeText("class", "Se esperaba 'class' al inicio de la clase.", "Agrega 'class' después del modificador public.");
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la clase.", "Escribe un identificador después de 'class'.");
        consumeText("{", "Se esperaba '{' para iniciar la clase.", "Agrega '{' después del nombre de la clase.");
        AstNode main = parseMainMethod();
        consumeText("}", "Se esperaba '}' para cerrar la clase.", "Agrega '}' al final de la clase.");
        return node("Class", name.getLexeme(), start, main);
    }

    private AstNode parseMainMethod() {
        Token start = peek();
        consumeText("public", "Se esperaba 'public' en el método principal.", "Declara el punto de entrada como 'public static void main'.");
        consumeText("static", "Se esperaba 'static' en el método principal.", "Agrega 'static' después de 'public'.");
        consumeText("void", "Se esperaba 'void' en el método principal.", "Usa 'void' como tipo de retorno de main.");
        consumeText("main", "Se esperaba el método 'main'.", "Escribe 'main' como nombre del método principal.");
        consumeText("(", "Se esperaba '(' después de main.", "Agrega '(' antes de los parámetros de main.");
        consumeText("String", "Se esperaba el tipo String del parámetro de main.", "Usa 'String[] nombre' como parámetro de main.");
        consumeText("[", "Se esperaba '[' en el parámetro String[].", "Agrega '[]' después de String.");
        consumeText("]", "Se esperaba ']' en el parámetro String[].", "Cierra los corchetes del arreglo String[].");
        consume(TokenType.IDENTIFIER, "Se esperaba el nombre del parámetro de main.", "Agrega un identificador después de String[].");
        consumeText(")", "Se esperaba ')' después del parámetro de main.", "Agrega ')' para cerrar los parámetros.");
        AstNode body = parseBlock();
        return node("MainMethod", "main", start, body);
    }

    private AstNode parseBlock() {
        Token start = consumeText("{", "Se esperaba '{' para iniciar un bloque.", "Agrega '{' antes de las sentencias del bloque.");
        List<AstNode> statements = new ArrayList<>();
        while (!checkText("}") && !isAtEnd()) {
            int before = current;
            try {
                statements.add(parseStatement());
            } catch (ParseFailure failure) {
                synchronizeStatement();
            }
            // Garantiza progreso si un error aparece justo en un token de sincronización.
            if (current == before && !isAtEnd()) advance();
        }
        consumeText("}", "Se esperaba '}' para cerrar el bloque.", "Agrega '}' al final del bloque.");
        return new AstNode("Block", "", start.getLine(), start.getColumn(), statements);
    }

    private AstNode parseStatement() {
        if (checkText("{")) return parseBlock();
        if (matchText("if")) return parseIf(previous());
        if (matchText("while")) return parseWhile(previous());
        if (isType(peek())) return parseDeclaration();
        if (checkText("System")) return parsePrint();
        if (check(TokenType.IDENTIFIER)) return parseAssignment();
        Token found = peek();
        report(found, "Se esperaba una declaración, asignación, impresión, if, while o bloque.",
                "Inicia la línea con un tipo, variable, System.out.println, if, while o '{'.");
        throw new ParseFailure();
    }

    private AstNode parseDeclaration() {
        Token type = advance();
        Token name = consume(TokenType.IDENTIFIER, "Se esperaba un nombre de variable después del tipo.", "Agrega un identificador después de '" + type.getLexeme() + "'.");
        List<AstNode> children = new ArrayList<>();
        if (matchText("=")) children.add(parseExpression());
        consumeText(";", "Se esperaba ';' al final de la declaración.", "Agrega ';' al final de la declaración.");
        return new AstNode("Declaration", type.getLexeme() + " " + name.getLexeme(), type.getLine(), type.getColumn(), children);
    }

    private AstNode parseAssignment() {
        Token name = advance();
        consumeText("=", "Se esperaba '=' después del nombre de variable.", "Usa '=' para asignar un valor a la variable.");
        AstNode value = parseExpression();
        consumeText(";", "Se esperaba ';' al final de la asignación.", "Agrega ';' al final de la asignación.");
        return node("Assignment", name.getLexeme(), name, value);
    }

    private AstNode parsePrint() {
        Token start = advance();
        consumeText(".", "Se esperaba '.' después de System.", "Escribe System.out.println(...).");
        consumeText("out", "Se esperaba 'out' después de System.", "Escribe 'out' en System.out.println(...).");
        consumeText(".", "Se esperaba '.' después de System.out.", "Agrega '.' antes de println.");
        consumeText("println", "Se esperaba println.", "Usa println para imprimir en esta versión.");
        consumeText("(", "Se esperaba '(' después de println.", "Agrega '(' antes del valor a imprimir.");
        List<AstNode> children = new ArrayList<>();
        if (!checkText(")")) children.add(parseExpression());
        consumeText(")", "Se esperaba ')' para cerrar println.", "Agrega ')' después del valor a imprimir.");
        consumeText(";", "Se esperaba ';' después de println.", "Agrega ';' al final de la instrucción.");
        return new AstNode("Print", "println", start.getLine(), start.getColumn(), children);
    }

    private AstNode parseIf(Token start) {
        consumeText("(", "Se esperaba '(' después de if.", "Escribe la condición entre paréntesis.");
        AstNode condition = parseExpression();
        consumeText(")", "Se esperaba ')' después de la condición de if.", "Cierra la condición con ')'.");
        AstNode thenBlock = parseBlock();
        List<AstNode> children = new ArrayList<>();
        children.add(condition);
        children.add(thenBlock);
        if (matchText("else")) {
            if (matchText("if")) children.add(parseIf(previous()));
            else children.add(parseBlock());
        }
        return new AstNode("If", "if", start.getLine(), start.getColumn(), children);
    }

    private AstNode parseWhile(Token start) {
        consumeText("(", "Se esperaba '(' después de while.", "Escribe la condición entre paréntesis.");
        AstNode condition = parseExpression();
        consumeText(")", "Se esperaba ')' después de la condición de while.", "Cierra la condición con ')'.");
        AstNode body = parseBlock();
        return node("While", "while", start, condition, body);
    }

    private AstNode parseExpression() { return parseOr(); }
    private AstNode parseOr() { return parseBinary(this::parseAnd, "||"); }
    private AstNode parseAnd() { return parseBinary(this::parseEquality, "&&"); }
    private AstNode parseEquality() { return parseBinary(this::parseRelational, "==", "!="); }
    private AstNode parseRelational() { return parseBinary(this::parseAdditive, "<", "<=", ">", ">="); }
    private AstNode parseAdditive() { return parseBinary(this::parseMultiplicative, "+", "-"); }
    private AstNode parseMultiplicative() { return parseBinary(this::parseUnary, "*", "/", "%"); }

    /** Construye operadores binarios asociativos a la izquierda respetando niveles de precedencia. */
    private AstNode parseBinary(ParseStep nextLevel, String... operators) {
        AstNode expression = nextLevel.parse();
        while (matchOperator(operators)) {
            Token operator = previous();
            AstNode right = nextLevel.parse();
            expression = node("Binary", operator.getLexeme(), operator, expression, right);
        }
        return expression;
    }

    private AstNode parseUnary() {
        if (matchOperator("!", "-", "+")) {
            Token operator = previous();
            return node("Unary", operator.getLexeme(), operator, parseUnary());
        }
        return parsePrimary();
    }

    private AstNode parsePrimary() {
        if (match(TokenType.INTEGER_LITERAL, TokenType.DECIMAL_LITERAL,
                TokenType.STRING_LITERAL, TokenType.BOOLEAN_LITERAL)) {
            Token literal = previous();
            return leaf("Literal", literal.getLexeme(), literal);
        }
        if (match(TokenType.IDENTIFIER)) {
            Token identifier = previous();
            return leaf("Identifier", identifier.getLexeme(), identifier);
        }
        if (matchText("(")) {
            Token open = previous();
            AstNode expression = parseExpression();
            consumeText(")", "Se esperaba ')' para cerrar la expresión.", "Agrega ')' al final de la expresión entre paréntesis.");
            return node("Grouping", "", open, expression);
        }
        Token found = peek();
        report(found, "Se esperaba un literal, identificador o expresión entre paréntesis.",
                "Agrega un valor válido en esta posición de la expresión.");
        throw new ParseFailure();
    }

    private void synchronizeStatement() {
        // Avanza hasta un límite de sentencia sin consumir la llave que cierra el bloque.
        while (!isAtEnd() && !checkText("}")) {
            if (previousOrCurrentIs(";")) return;
            if (checkText("if") || checkText("while") || isType(peek())
                    || checkText("System") || check(TokenType.IDENTIFIER) || checkText("{")) return;
            advance();
        }
    }

    private boolean previousOrCurrentIs(String text) {
        if (current > 0 && previous().getLexeme().equals(text)) return true;
        if (checkText(text)) { advance(); return true; }
        return false;
    }

    private void consumeEnd() {
        if (!isAtEnd()) {
            Token extra = peek();
            report(extra, "Hay contenido después del cierre de la clase.",
                    "Elimina el contenido extra o inclúyelo dentro de la clase.");
        }
    }

    private boolean isType(Token token) {
        return token.getType() == TokenType.KEYWORD
                && (token.getLexeme().equals("int") || token.getLexeme().equals("double")
                || token.getLexeme().equals("float") || token.getLexeme().equals("boolean")
                || token.getLexeme().equals("String"));
    }

    private Token consumeText(String text, String description, String suggestion) {
        if (checkText(text)) return advance();
        report(peek(), description, suggestion);
        throw new ParseFailure();
    }

    private Token consume(TokenType type, String description, String suggestion) {
        if (check(type)) return advance();
        report(peek(), description, suggestion);
        throw new ParseFailure();
    }

    private boolean matchText(String... texts) {
        for (String text : texts) if (checkText(text)) { advance(); return true; }
        return false;
    }

    private boolean matchOperator(String... operators) {
        if (peek().getType() != TokenType.OPERATOR) return false;
        return matchText(operators);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) if (check(type)) { advance(); return true; }
        return false;
    }

    private boolean checkText(String text) { return !isAtEnd() && peek().getLexeme().equals(text); }
    private boolean check(TokenType type) { return !isAtEnd() && peek().getType() == type; }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().getType() == TokenType.EOF; }
    private Token peek() {
        if (tokens.isEmpty()) return new Token(TokenType.EOF, "", 1, 1);
        return tokens.get(Math.min(current, tokens.size() - 1));
    }
    private Token previous() { return tokens.get(Math.max(0, Math.min(current - 1, tokens.size() - 1))); }

    private AstNode leaf(String kind, String value, Token token) {
        return new AstNode(kind, value, token.getLine(), token.getColumn(), Collections.<AstNode>emptyList());
    }
    private AstNode node(String kind, String value, Token token, AstNode... children) {
        List<AstNode> list = new ArrayList<>();
        Collections.addAll(list, children);
        return new AstNode(kind, value, token.getLine(), token.getColumn(), list);
    }
    private void report(Token token, String description, String suggestion) {
        diagnostics.add(new Diagnostic(Category.SINTACTICO, token.getLine(), token.getColumn(), description, suggestion));
    }

    private interface ParseStep { AstNode parse(); }
    private static final class ParseFailure extends RuntimeException { private static final long serialVersionUID = 1L; }
}
