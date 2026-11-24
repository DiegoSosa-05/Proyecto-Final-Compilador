package proyectoFinal.semantic;

/** Fila de la tabla de símbolos junto con su dirección didáctica simulada. */
public final class SymbolEntry {
    private final String name;
    private final String type;
    private final String scope;
    private final String address;
    private String value;

    SymbolEntry(String name, String type, String value, String scope, String address) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.scope = scope;
        this.address = address;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getValue() { return value; }
    public String getScope() { return scope; }
    public String getAddress() { return address; }
    void setValue(String value) { this.value = value; }
}
