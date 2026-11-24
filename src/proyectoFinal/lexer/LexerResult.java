package proyectoFinal.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proyectoFinal.model.Diagnostic;

/** Resultado inmutable del lexer; los tokens y errores se muestran por separado en la GUI. */
public final class LexerResult {
    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics;

    public LexerResult(List<Token> tokens, List<Diagnostic> diagnostics) {
        this.tokens = Collections.unmodifiableList(new ArrayList<>(tokens));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public List<Token> getTokens() { return tokens; }
    public List<Diagnostic> getDiagnostics() { return diagnostics; }
}
