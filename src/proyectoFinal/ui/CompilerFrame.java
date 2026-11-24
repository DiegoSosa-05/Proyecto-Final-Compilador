package proyectoFinal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import proyectoFinal.app.CompilerService;
import proyectoFinal.lexer.LexerResult;
import proyectoFinal.model.AnalysisResult;

/** Ventana principal del compilador con editor y vistas independientes de resultados. */
public final class CompilerFrame extends JFrame {
    private static final Color NAVY = new Color(20, 39, 61);
    private static final Color TEAL = new Color(12, 148, 145);
    private static final Color LIGHT_BACKGROUND = new Color(244, 247, 251);
    private final CompilerService compiler = new CompilerService();
    private final CodeEditorPanel editor = new CodeEditorPanel();
    private final ResultsPanel results = new ResultsPanel();
    private final JLabel status = new JLabel("Listo para analizar código Java.");

    public CompilerFrame() {
        super("Compilador Java Educativo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1420, 870);
        setLocationRelativeTo(null);
        getContentPane().setBackground(LIGHT_BACKGROUND);
        setLayout(new BorderLayout(0, 0));
        add(createHeader(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel titleBox = new JPanel(new BorderLayout(0, 4));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Compilador Java");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        JLabel subtitle = new JLabel("Análisis léxico · sintáctico · semántico");
        subtitle.setForeground(new Color(190, 207, 224));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton analyze = button("▶  Analizar código", TEAL, Color.WHITE);
        JButton tokens = button("▦  Generar tabla de tokens", new Color(231, 239, 246), NAVY);
        analyze.addActionListener(event -> analyzeSource());
        tokens.addActionListener(event -> showTokenTable());
        actions.add(analyze);
        actions.add(tokens);

        header.add(titleBox, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JSplitPane createWorkspace() {
        JPanel editorCard = createCard("Código fuente", "Escribe o pega el programa que quieres analizar.", editor);
        JPanel resultCard = createCard("Resultados del análisis", "Explora cada salida en las pestañas.", results);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorCard, resultCard);
        split.setResizeWeight(0.43);
        split.setDividerSize(9);
        split.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        split.setBackground(LIGHT_BACKGROUND);
        split.setContinuousLayout(true);
        return split;
    }

    private JPanel createCard(String titleText, String subtitleText, java.awt.Component content) {
        JPanel card = new JPanel(new BorderLayout(0, 9));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 231, 239)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JPanel heading = new JPanel(new BorderLayout(0, 3));
        heading.setOpaque(false);
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(111, 128, 148));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 230, 238)),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setForeground(new Color(76, 96, 118));
        JLabel language = new JLabel("Java educativo  •  Gramática definida en el proyecto");
        language.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        language.setForeground(new Color(125, 141, 159));
        bar.add(status, BorderLayout.WEST);
        bar.add(language, BorderLayout.EAST);
        return bar;
    }

    private JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    private void analyzeSource() {
        AnalysisResult result = compiler.analyze(editor.getSource());
        results.showAnalysis(result);
        int errorCount = result.getLexer().getDiagnostics().size()
                + result.getParser().getDiagnostics().size()
                + result.getSemantic().getDiagnostics().size();
        results.selectErrorsIfPresent(errorCount > 0);
        status.setText(errorCount == 0
                ? "Análisis terminado: no se encontraron errores."
                : "Análisis terminado: " + errorCount + " error(es). Revisa la pestaña Errores.");
    }

    private void showTokenTable() {
        LexerResult result = compiler.tokenize(editor.getSource());
        results.showTokens(result);
        results.selectTokensTab();
        status.setText(result.getDiagnostics().isEmpty()
                ? "Tabla de tokens generada: " + Math.max(0, result.getTokens().size() - 1) + " tokens."
                : "Tabla generada con " + result.getDiagnostics().size() + " error(es) léxico(s); usa Analizar código para ver sugerencias.");
    }
}
