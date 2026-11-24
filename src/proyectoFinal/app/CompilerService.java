package proyectoFinal.app;

import proyectoFinal.lexer.Lexer;
import proyectoFinal.lexer.LexerResult;
import proyectoFinal.model.AnalysisResult;
import proyectoFinal.parser.Parser;
import proyectoFinal.parser.ParserResult;
import proyectoFinal.semantic.SemanticAnalyzer;
import proyectoFinal.semantic.SemanticResult;

/** Ejecuta las etapas en orden y centraliza la coordinación del compilador. */
public final class CompilerService {
    public AnalysisResult analyze(String source) {
        LexerResult lexical = new Lexer(source).scan();
        ParserResult syntactic = new Parser(lexical.getTokens()).parse();
        // Una raíz válida permite analizar semánticamente las partes sintácticas recuperadas.
        SemanticResult semantic = new SemanticAnalyzer().analyze(
                syntactic.hasTree() ? syntactic.getRoot() : null);
        return new AnalysisResult(lexical, syntactic, semantic);
    }

    public LexerResult tokenize(String source) {
        return new Lexer(source).scan();
    }
}
