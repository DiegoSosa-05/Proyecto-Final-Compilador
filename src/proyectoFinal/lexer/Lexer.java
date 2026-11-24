package proyectoFinal.lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import proyectoFinal.model.Diagnostic;
import proyectoFinal.model.Diagnostic.Category;

/** Convierte texto fuente en tokens sin depender de un compilador externo. */
public final class Lexer {
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "public", "class", "static", "void", "main", "String",
            "int", "double", "float", "boolean", "if", "else", "while"));
    private static final Set<Character> DELIMITERS = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', '[', ']', ';', ',', '.'));

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int current;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        // Unificar finales de línea para contar CRLF como un solo salto.
        this.source = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** Analiza el texto completo y siempre termina la secuencia con EOF. */
    public LexerResult scan() {
        while (!isAtEnd()) scanNext();
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return new LexerResult(tokens, diagnostics);
    }

    private void scanNext() {
        int start = current;
        char c = advance();
        int startLine = line;
        int startColumn = column - 1;

        if (Character.isWhitespace(c)) return;
        if (isIdentifierStart(c)) { scanIdentifier(start, startLine, startColumn); return; }
        if (Character.isDigit(c)) { scanNumber(startLine, startColumn); return; }

        if (c == '"') { scanString(startLine, startColumn); return; }
        if (c == '/') {
            if (match('/')) { skipLineComment(); return; }
            if (match('*')) { skipBlockComment(startLine, startColumn); return; }
            add(TokenType.OPERATOR, "/", startLine, startColumn);
            return;
        }

        if (isDelimiter(c)) { add(TokenType.DELIMITER, String.valueOf(c), startLine, startColumn); return; }
        if (isOperatorStart(c)) { scanOperator(c, startLine, startColumn); return; }

        report(startLine, startColumn, "Carácter no reconocido: '" + c + "'.",
                "Elimina el carácter o reemplázalo por un símbolo válido del lenguaje.");
    }

    private void scanIdentifier(int start, int tokenLine, int tokenColumn) {
        while (isIdentifierPart(peek())) advance();
        String text = source.substring(start, current);
        add(classifyWord(text), text, tokenLine, tokenColumn);
    }

    private TokenType classifyWord(String text) {
        if ("true".equals(text) || "false".equals(text)) return TokenType.BOOLEAN_LITERAL;
        return KEYWORDS.contains(text) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
    }

    private void scanNumber(int tokenLine, int tokenColumn) {
        int start = current - 1;
        while (Character.isDigit(peek())) advance();
        boolean decimal = false;
        if (peek() == '.' && Character.isDigit(peekNext())) {
            decimal = true;
            advance();
            while (Character.isDigit(peek())) advance();
        }
        String number = source.substring(start, current);
        add(decimal ? TokenType.DECIMAL_LITERAL : TokenType.INTEGER_LITERAL,
                number, tokenLine, tokenColumn);
    }

    private void scanString(int tokenLine, int tokenColumn) {
        StringBuilder value = new StringBuilder("\"");
        boolean closed = false;
        while (!isAtEnd() && peek() != '\n') {
            char c = advance();
            value.append(c);
            if (c == '"') { closed = true; break; }
            if (c == '\\') {
                if (isAtEnd() || peek() == '\n') break;
                char escaped = advance();
                value.append(escaped);
                if ("\\\"nrt".indexOf(escaped) < 0) {
                    report(line, column - 2, "Secuencia de escape no válida en cadena.",
                            "Usa una secuencia válida: \\\\, \\\" , \\n, \\r o \\t.");
                }
            }
        }
        if (!closed) {
            report(tokenLine, tokenColumn, "Cadena sin comilla de cierre.",
                    "Agrega una comilla doble antes del fin de línea.");
        } else {
            add(TokenType.STRING_LITERAL, value.toString(), tokenLine, tokenColumn);
        }
    }

    private void skipLineComment() {
        while (!isAtEnd() && peek() != '\n') advance();
    }

    private void skipBlockComment(int tokenLine, int tokenColumn) {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') { advance(); advance(); return; }
            advance();
        }
        report(tokenLine, tokenColumn, "Comentario de bloque sin cierre.",
                "Agrega */ para cerrar el comentario.");
    }

    private void scanOperator(char first, int tokenLine, int tokenColumn) {
        String op = String.valueOf(first);
        if (first == '&' || first == '|') {
            if (match(first)) op += first;
            else {
                report(tokenLine, tokenColumn, "Operador incompleto: '" + first + "'.",
                        "Usa " + first + first + " para formar el operador lógico.");
                return;
            }
        } else if ((first == '=' || first == '!' || first == '<' || first == '>') && match('=')) {
            op += '=';
        }
        add(TokenType.OPERATOR, op, tokenLine, tokenColumn);
    }

    private boolean isOperatorStart(char c) { return "+-*%!=<>|&".indexOf(c) >= 0; }
    private boolean isDelimiter(char c) { return DELIMITERS.contains(c); }
    private boolean isIdentifierStart(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isIdentifierPart(char c) { return Character.isLetterOrDigit(c) || c == '_'; }
    private boolean isAtEnd() { return current >= source.length(); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
    private char peekNext() { return current + 1 >= source.length() ? '\0' : source.charAt(current + 1); }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') { line++; column = 1; }
        else column++;
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        advance();
        return true;
    }

    private void add(TokenType type, String lexeme, int tokenLine, int tokenColumn) {
        tokens.add(new Token(type, lexeme, tokenLine, tokenColumn));
    }

    private void report(int errorLine, int errorColumn, String description, String suggestion) {
        diagnostics.add(new Diagnostic(Category.LEXICO, errorLine, Math.max(1, errorColumn),
                description, suggestion));
    }
}
