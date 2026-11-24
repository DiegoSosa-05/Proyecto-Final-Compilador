package proyectoFinal.semantic;

import java.util.List;

/** Formatea las tablas semánticas para las futuras pestañas de la interfaz. */
public final class SymbolTableFormatter {
    private SymbolTableFormatter() { }

    public static String formatSymbols(List<SymbolEntry> symbols) {
        StringBuilder text = new StringBuilder(String.format("%-20s %-12s %-22s %-18s%n",
                "NOMBRE", "TIPO", "VALOR", "ÁMBITO"));
        text.append("------------------------------------------------------------------------\n");
        for (SymbolEntry symbol : symbols) {
            text.append(String.format("%-20s %-12s %-22s %-18s%n", symbol.getName(),
                    symbol.getType(), symbol.getValue(), symbol.getScope()));
        }
        return text.toString();
    }

    public static String formatAddresses(List<SymbolEntry> symbols) {
        StringBuilder text = new StringBuilder(String.format("%-14s %-20s %-12s %-18s%n",
                "DIRECCIÓN", "NOMBRE", "TIPO", "ÁMBITO"));
        text.append("----------------------------------------------------------------\n");
        for (SymbolEntry symbol : symbols) {
            text.append(String.format("%-14s %-20s %-12s %-18s%n", symbol.getAddress(),
                    symbol.getName(), symbol.getType(), symbol.getScope()));
        }
        return text.toString();
    }
}
