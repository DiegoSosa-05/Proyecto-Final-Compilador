package proyectoFinal.lexer;

import java.util.List;

/** Presentación textual simple de tokens, reutilizable mientras se construye la interfaz. */
public final class TokenTableFormatter {
    private TokenTableFormatter() { }

    public static String format(List<Token> tokens) {
        StringBuilder table = new StringBuilder();
        table.append(String.format("%-24s %-18s %-28s %7s %8s%n",
                "LEXEMA", "TOKEN", "PATRÓN", "LÍNEA", "COLUMNA"));
        table.append("---------------------------------------------------------------------------------------------\n");
        for (Token token : tokens) {
            if (token.getType() == TokenType.EOF) continue;
            table.append(String.format("%-24s %-18s %-28s %7d %8d%n",
                    printable(token.getLexeme()), token.getType(), patternFor(token),
                    token.getLine(), token.getColumn()));
        }
        return table.toString();
    }

    private static String patternFor(Token token) {
        switch (token.getType()) {
            case KEYWORD: return "palabra reservada";
            case IDENTIFIER: return "letra/_ + letras/dígitos/_";
            case INTEGER_LITERAL: return "dígitos+";
            case DECIMAL_LITERAL: return "dígitos+.dígitos+";
            case STRING_LITERAL: return "\"...\" con escapes";
            case BOOLEAN_LITERAL: return "true | false";
            case OPERATOR: return "operador del lenguaje";
            case DELIMITER: return "delimitador";
            default: return "";
        }
    }

    private static String printable(String text) {
        return text.replace("\n", "\\n").replace("\t", "\\t");
    }
}
