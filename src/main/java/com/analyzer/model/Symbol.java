
// --- Symbol.java ---
package com.analyzer.model;

import java.util.Objects;

public class Symbol {

    public enum SymbolType {
        VARIABLE("Variable"), FUNCTION("Función"), CLASS("Clase"),
        TABLE("Tabla"), COLUMN("Columna"), PARAMETER("Parámetro"),
        CONSTANT("Constante"), TAG("Etiqueta HTML"), ATTRIBUTE("Atributo"),
        UNKNOWN("Desconocido");

        private final String displayName;
        SymbolType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override
        public String toString() { return displayName; }
    }

    private String name;
    private SymbolType symbolType;
    private String dataType;
    private String scope;
    private Object value;
    private int declarationLine;
    private boolean isInitialized;

    public Symbol(String name, SymbolType symbolType, String dataType, String scope) {
        this.name = name != null ? name : "";
        this.symbolType = symbolType != null ? symbolType : SymbolType.UNKNOWN;
        this.dataType = dataType != null ? dataType : "unknown";
        this.scope = scope != null ? scope : "global";
        this.isInitialized = false;
    }

    public Symbol(String name, SymbolType symbolType) {
        this(name, symbolType, "unknown", "global");
    }

    // Getters y Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SymbolType getSymbolType() { return symbolType; }
    public void setSymbolType(SymbolType symbolType) { this.symbolType = symbolType; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; this.isInitialized = (value != null); }
    public int getDeclarationLine() { return declarationLine; }
    public void setDeclarationLine(int declarationLine) { this.declarationLine = declarationLine; }
    public boolean isInitialized() { return isInitialized; }
    public void setInitialized(boolean initialized) { isInitialized = initialized; }

    public String getValueAsString() {
        if (value == null) return isInitialized ? "null" : "no inicializado";
        return value.toString();
    }

    @Override
    public String toString() {
        return "Symbol{name='" + name + "', type=" + symbolType +
                ", dataType='" + dataType + "', scope='" + scope + "'}";
    }
}
