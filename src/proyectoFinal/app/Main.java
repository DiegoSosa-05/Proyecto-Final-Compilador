package proyectoFinal.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import proyectoFinal.ui.CompilerFrame;

/** Punto de entrada de la aplicación gráfica. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Si el sistema no ofrece su tema, Swing conserva su tema predeterminado.
            }
            new CompilerFrame().setVisible(true);
        });
    }
}
