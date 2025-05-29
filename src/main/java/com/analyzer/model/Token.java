

// --- Token.java ---
package com.analyzer.model;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Token {
    private String value;
    private String type;
    private int line;
    private int column;
    private String subType;
    private Map<String, String> attributes = new HashMap<>();

    public Token(String value, String type, int line, int column) {
        this.value = value != null ? value : "";
        this.type = type != null ? type : "UNKNOWN";
        this.line = Math.max(0, line);
        this.column = Math.max(0, column);
    }

    public Token(String value, String type) {
        this(value, type, 0, 0);
    }

    // Getters y Setters
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value != null ? value : ""; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type != null ? type : "UNKNOWN"; }
    public int getLine() { return line; }
    public void setLine(int line) { this.line = Math.max(0, line); }
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = Math.max(0, column); }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public String getPosition() { return line + ":" + column; }
    public boolean isOfType(String tokenType) { return this.type.equalsIgnoreCase(tokenType); }
    public boolean hasValue(String tokenValue) { return this.value.equals(tokenValue); }

    @Override
    public String toString() {
        return "Token{value='" + value + "', type='" + type + "', pos=" + getPosition() + "}";
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return line == token.line && column == token.column &&
                Objects.equals(value, token.value) && Objects.equals(type, token.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type, line, column);
    }


}
