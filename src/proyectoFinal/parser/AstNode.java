package proyectoFinal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Nodo sencillo del árbol; kind distingue la regla y value guarda lexema/nombre opcional. */
public final class AstNode {
    private final String kind;
    private final String value;
    private final int line;
    private final int column;
    private final List<AstNode> children;

    public AstNode(String kind, String value, int line, int column, List<AstNode> children) {
        this.kind = kind;
        this.value = value == null ? "" : value;
        this.line = line;
        this.column = column;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public String getKind() { return kind; }
    public String getValue() { return value; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public List<AstNode> getChildren() { return children; }

    /** Representación de texto con indentación para mostrar el árbol en un panel Swing. */
    public String toTreeString() {
        StringBuilder result = new StringBuilder();
        appendTree(result, "", true);
        return result.toString();
    }

    private void appendTree(StringBuilder output, String prefix, boolean last) {
        output.append(prefix).append(last ? "└── " : "├── ").append(kind);
        if (!value.isEmpty()) output.append(": ").append(value);
        output.append(" [").append(line).append(':').append(column).append("]\n");
        String childPrefix = prefix + (last ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).appendTree(output, childPrefix, i == children.size() - 1);
        }
    }
}
