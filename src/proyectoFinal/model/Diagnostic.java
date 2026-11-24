package proyectoFinal.model;

/** Diagnóstico común para que lexer, parser y analizador semántico reporten igual. */
public final class Diagnostic {
    public enum Category { LEXICO, SINTACTICO, SEMANTICO }

    private final Category category;
    private final int line;
    private final int column;
    private final String description;
    private final String suggestion;

    public Diagnostic(Category category, int line, int column,
                      String description, String suggestion) {
        this.category = category;
        this.line = line;
        this.column = column;
        this.description = description;
        this.suggestion = suggestion;
    }

    public Category getCategory() { return category; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getDescription() { return description; }
    public String getSuggestion() { return suggestion; }

    @Override
    public String toString() {
        return category + " | línea " + line + ", columna " + column + ": "
                + description + " Sugerencia: " + suggestion;
    }
}
