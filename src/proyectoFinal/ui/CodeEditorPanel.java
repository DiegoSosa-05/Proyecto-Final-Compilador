package proyectoFinal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Editor de código con números de línea y tipografía monoespaciada. */
final class CodeEditorPanel extends JPanel {
    private static final Color EDITOR_BACKGROUND = new Color(249, 251, 254);
    private final JTextArea sourceArea = new JTextArea();
    private final JTextArea lineNumbers = new JTextArea("1");

    CodeEditorPanel() {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        Font codeFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        sourceArea.setFont(codeFont);
        sourceArea.setTabSize(4);
        sourceArea.setForeground(new Color(31, 49, 72));
        sourceArea.setBackground(EDITOR_BACKGROUND);
        sourceArea.setCaretColor(new Color(12, 148, 145));
        sourceArea.setSelectionColor(new Color(196, 235, 232));
        sourceArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        sourceArea.setText(defaultExample());

        lineNumbers.setFont(codeFont);
        lineNumbers.setForeground(new Color(126, 143, 163));
        lineNumbers.setBackground(new Color(239, 244, 249));
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setAlignmentX(RIGHT_ALIGNMENT);
        lineNumbers.setMinimumSize(new Dimension(44, 0));

        JScrollPane scroll = new JScrollPane(sourceArea);
        scroll.setRowHeaderView(lineNumbers);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 228, 237)));
        scroll.getViewport().setBackground(EDITOR_BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        sourceArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateLineNumbers(); }
            @Override public void removeUpdate(DocumentEvent event) { updateLineNumbers(); }
            @Override public void changedUpdate(DocumentEvent event) { updateLineNumbers(); }
        });
    }

    String getSource() { return sourceArea.getText(); }

    void setSource(String source) {
        sourceArea.setText(source);
        sourceArea.setCaretPosition(0);
    }

    private void updateLineNumbers() {
        SwingUtilities.invokeLater(() -> {
            int lines = Math.max(1, sourceArea.getLineCount());
            StringBuilder numbers = new StringBuilder();
            for (int i = 1; i <= lines; i++) numbers.append(i).append('\n');
            lineNumbers.setText(numbers.toString());
        });
    }

    private String defaultExample() {
        return "public class Ejemplo {\n"
                + "    public static void main(String[] args) {\n"
                + "        int edad = 20;\n"
                + "        double promedio = 9.5;\n"
                + "        String mensaje = \"Hola \" + edad;\n"
                + "        boolean aprobado = promedio >= 6.0;\n"
                + "\n"
                + "        if (aprobado) {\n"
                + "            System.out.println(mensaje);\n"
                + "        } else {\n"
                + "            System.out.println(\"Sigue intentando\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }
}
