package proyectoFinal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import proyectoFinal.lexer.LexerResult;
import proyectoFinal.lexer.Token;
import proyectoFinal.model.AnalysisResult;
import proyectoFinal.model.Diagnostic;
import proyectoFinal.parser.AstNode;
import proyectoFinal.semantic.ExpressionTreeFormatter;
import proyectoFinal.semantic.SemanticResult;
import proyectoFinal.semantic.SymbolEntry;

/** Vistas de salida en pestañas; no contiene reglas de análisis del compilador. */
final class ResultsPanel extends JPanel {
    private static final Color TEAL = new Color(12, 148, 145);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextArea summary = createTextArea();
    private final JTable tokenTable = createTable();
    private final JTable errorTable = createTable();
    private final JTextArea syntaxTree = createTextArea();
    private final JTextArea expressionTrees = createTextArea();
    private final JTable symbolTable = createTable();
    private final JTable addressTable = createTable();

    ResultsPanel() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(220, 228, 237)));
        addTab("Resumen", new JScrollPane(summary));
        addTab("Tokens", new JScrollPane(tokenTable));
        addTab("Errores", new JScrollPane(errorTable));
        addTab("Árbol sintáctico", new JScrollPane(syntaxTree));
        addTab("Expresiones", new JScrollPane(expressionTrees));
        addTab("Símbolos", new JScrollPane(symbolTable));
        addTab("Direcciones", new JScrollPane(addressTable));
        add(tabs, BorderLayout.CENTER);
    }

    void showAnalysis(AnalysisResult result) {
        showTokens(result.getLexer());
        showDiagnostics(allDiagnostics(result));
        AstNode root = result.getParser().getRoot();
        syntaxTree.setText(root == null ? "No se pudo construir el árbol por errores en la estructura principal."
                : root.toTreeString());
        expressionTrees.setText(ExpressionTreeFormatter.format(root));
        showSymbols(result.getSemantic());
        summary.setText(createSummary(result));
        tabs.setSelectedIndex(0);
    }

    void showTokens(LexerResult result) {
        String[] columns = { "Lexema", "Categoría", "Línea", "Columna" };
        DefaultTableModel model = model(columns);
        for (Token token : result.getTokens()) {
            if (!"EOF".equals(token.getType().name())) {
                model.addRow(new Object[] { token.getLexeme(), token.getType(),
                        token.getLine(), token.getColumn() });
            }
        }
        tokenTable.setModel(model);
        showDiagnostics(result.getDiagnostics());
    }

    void selectTokensTab() { tabs.setSelectedIndex(1); }
    void selectErrorsIfPresent(boolean hasErrors) { if (hasErrors) tabs.setSelectedIndex(2); }

    private void showDiagnostics(List<Diagnostic> diagnostics) {
        DefaultTableModel model = model(new String[] { "Clasificación", "Línea", "Columna", "Descripción", "Sugerencia" });
        for (Diagnostic diagnostic : diagnostics) {
            model.addRow(new Object[] { diagnostic.getCategory(), diagnostic.getLine(),
                    diagnostic.getColumn(), diagnostic.getDescription(), diagnostic.getSuggestion() });
        }
        errorTable.setModel(model);
    }

    private void showSymbols(SemanticResult result) {
        DefaultTableModel symbols = model(new String[] { "Nombre", "Tipo", "Valor", "Ámbito" });
        DefaultTableModel addresses = model(new String[] { "Dirección", "Nombre", "Tipo", "Ámbito" });
        for (SymbolEntry entry : result.getSymbols()) {
            symbols.addRow(new Object[] { entry.getName(), entry.getType(), entry.getValue(), entry.getScope() });
            addresses.addRow(new Object[] { entry.getAddress(), entry.getName(), entry.getType(), entry.getScope() });
        }
        symbolTable.setModel(symbols);
        addressTable.setModel(addresses);
    }

    private String createSummary(AnalysisResult result) {
        List<Diagnostic> errors = allDiagnostics(result);
        int tokens = Math.max(0, result.getLexer().getTokens().size() - 1);
        int lexical = count(errors, Diagnostic.Category.LEXICO);
        int syntactic = count(errors, Diagnostic.Category.SINTACTICO);
        int semantic = count(errors, Diagnostic.Category.SEMANTICO);
        String state = errors.isEmpty() ? "ANÁLISIS CORRECTO" : "ANÁLISIS CON OBSERVACIONES";
        return state + "\n\n"
                + "Se reconocieron " + tokens + " tokens.\n"
                + "Errores léxicos: " + lexical + "\n"
                + "Errores sintácticos: " + syntactic + "\n"
                + "Errores semánticos: " + semantic + "\n"
                + "Variables registradas: " + result.getSemantic().getSymbols().size() + "\n\n"
                + (errors.isEmpty()
                    ? "El programa cumple las reglas de la gramática implementada y no se encontraron errores semánticos."
                    : "Consulta la pestaña Errores para ver la ubicación, explicación y sugerencia de cada diagnóstico.");
    }

    private List<Diagnostic> allDiagnostics(AnalysisResult result) {
        List<Diagnostic> all = new ArrayList<>();
        all.addAll(result.getLexer().getDiagnostics());
        all.addAll(result.getParser().getDiagnostics());
        all.addAll(result.getSemantic().getDiagnostics());
        all.sort((a, b) -> {
            int byLine = Integer.compare(a.getLine(), b.getLine());
            if (byLine != 0) return byLine;
            return Integer.compare(a.getColumn(), b.getColumn());
        });
        return all;
    }

    private int count(List<Diagnostic> diagnostics, Diagnostic.Category category) {
        int count = 0;
        for (Diagnostic diagnostic : diagnostics) if (diagnostic.getCategory() == category) count++;
        return count;
    }

    private DefaultTableModel model(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private JTable createTable() {
        JTable table = new JTable(model(new String[0]));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.setForeground(new Color(38, 55, 76));
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(231, 237, 243));
        table.setSelectionBackground(new Color(217, 241, 238));
        table.setSelectionForeground(new Color(25, 55, 70));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(239, 245, 249));
        table.getTableHeader().setForeground(new Color(45, 66, 88));
        return table;
    }

    private JTextArea createTextArea() {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setLineWrap(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setForeground(new Color(38, 55, 76));
        text.setBackground(new Color(252, 253, 255));
        text.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return text;
    }

    private void addTab(String title, JComponent content) {
        if (content instanceof JScrollPane) {
            ((JScrollPane) content).setBorder(BorderFactory.createEmptyBorder());
            Component view = ((JScrollPane) content).getViewport().getView();
            if (view instanceof JTable) ((JTable) view).setFillsViewportHeight(true);
        }
        tabs.addTab(title, content);
        tabs.setForegroundAt(tabs.getTabCount() - 1, TEAL);
    }
}
