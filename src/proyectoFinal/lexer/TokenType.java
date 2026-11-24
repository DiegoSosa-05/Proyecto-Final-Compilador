package proyectoFinal.lexer;

/** Categorías de tokens reconocidas por el analizador léxico. */
public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    INTEGER_LITERAL,
    DECIMAL_LITERAL,
    STRING_LITERAL,
    BOOLEAN_LITERAL,
    OPERATOR,
    DELIMITER,
    EOF
}
