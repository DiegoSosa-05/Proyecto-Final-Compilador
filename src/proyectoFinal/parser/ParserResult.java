package proyectoFinal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proyectoFinal.model.Diagnostic;

/** Árbol y diagnósticos sintácticos producidos en una ejecución del parser. */
public final class ParserResult {
    private final AstNode root;
    private final List<Diagnostic> diagnostics;

    public ParserResult(AstNode root, List<Diagnostic> diagnostics) {
        this.root = root;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public AstNode getRoot() { return root; }
    public List<Diagnostic> getDiagnostics() { return diagnostics; }
    public boolean hasTree() { return root != null; }
}
