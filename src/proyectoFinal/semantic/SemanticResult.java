package proyectoFinal.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proyectoFinal.model.Diagnostic;

/** Resultado semántico: diagnósticos y filas listas para las tablas de la interfaz. */
public final class SemanticResult {
    private final List<SymbolEntry> symbols;
    private final List<Diagnostic> diagnostics;

    public SemanticResult(List<SymbolEntry> symbols, List<Diagnostic> diagnostics) {
        this.symbols = Collections.unmodifiableList(new ArrayList<>(symbols));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public List<SymbolEntry> getSymbols() { return symbols; }
    public List<Diagnostic> getDiagnostics() { return diagnostics; }
}
