package proyectoFinal.model;

import proyectoFinal.lexer.LexerResult;
import proyectoFinal.parser.ParserResult;
import proyectoFinal.semantic.SemanticResult;

/** Agrupa las salidas de las etapas para que la interfaz solo se encargue de presentarlas. */
public final class AnalysisResult {
    private final LexerResult lexer;
    private final ParserResult parser;
    private final SemanticResult semantic;

    public AnalysisResult(LexerResult lexer, ParserResult parser, SemanticResult semantic) {
        this.lexer = lexer;
        this.parser = parser;
        this.semantic = semantic;
    }

    public LexerResult getLexer() { return lexer; }
    public ParserResult getParser() { return parser; }
    public SemanticResult getSemantic() { return semantic; }
}
