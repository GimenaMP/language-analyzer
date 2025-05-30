
// --- Symbol.java ---
package com.analyzer.model;

import com.analyzer.model.Token;

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

//    String MODULE = Token.getValue();
//    // Asegúrate de usar un tipo existente en la enumeración o usa VARIABLE como alternativa temporal
//    Symbol symbol = new Symbol(MODULE, Symbol.SymbolType.VARIABLE);

    private String name;// Nombre del símbolo, no puede ser nulo
    private SymbolType symbolType;// Tipo de símbolo (ej. Variable, Función, Clase, etc.)
    private String dataType;// Tipo de dato del símbolo (ej. int, String, etc.)
    private String scope;
    private Object value;// Valor del símbolo, puede ser cualquier tipo de dato
    private int declarationLine;// Línea donde se declaró el símbolo
    private int declarationColumn;// Columna donde se declaró el símbolo
    private boolean isInitialized;// Indica si el símbolo ha sido inicializado

    public int getDeclarationColumn() { return declarationColumn; }
    public void setDeclarationColumn(int declarationColumn) { this.declarationColumn = declarationColumn; }


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


    public void setDeclarationLine(int declarationLine) { this.declarationLine = declarationLine; }

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
