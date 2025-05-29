// --- AnalysisError.java ---
package com.analyzer.model;

import java.util.Objects;

public class AnalysisError {

    public enum ErrorType {
        LEXICAL("Léxico"), SYNTACTIC("Sintáctico"),
        SEMANTIC("Semántico"), WARNING("Advertencia");

        private final String displayName;
        ErrorType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override
        public String toString() { return displayName; }
    }

    private String message;
    private int line;
    private int column;
    private ErrorType errorType;
    private String suggestion;

    public AnalysisError(String message, ErrorType errorType, int line, int column) {
        this.message = message != null ? message : "Error desconocido";
        this.errorType = errorType != null ? errorType : ErrorType.LEXICAL;
        this.line = Math.max(0, line);
        this.column = Math.max(0, column);
    }

    public AnalysisError(String message, ErrorType errorType) {
        this(message, errorType, 0, 0);
    }

    // Getters y Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getLine() { return line; }
    public void setLine(int line) { this.line = Math.max(0, line); }
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = Math.max(0, column); }
    public ErrorType getErrorType() { return errorType; }
    public void setErrorType(ErrorType errorType) { this.errorType = errorType; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getPosition() { return line + ":" + column; }

    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorType.getDisplayName()).append("] ");
        if (line > 0 || column > 0) {
            sb.append("(").append(getPosition()).append(") ");
        }
        sb.append(message);
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            sb.append(" - Sugerencia: ").append(suggestion);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "AnalysisError{message='" + message + "', line=" + line +
                ", column=" + column + ", errorType=" + errorType + "}";
    }
}
