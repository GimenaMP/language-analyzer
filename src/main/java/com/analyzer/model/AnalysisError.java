package com.analyzer.model;

public class AnalysisError {
    public String getFullMessage() {
        return String.format("[%s] Línea %d, Columna %d: %s",
            errorType.getDisplayName(), line, column, message);
    }

    public enum ErrorType {
        LEXICAL("Léxico"),
        SYNTACTIC("Sintáctico"),
        SEMANTIC("Semántico");

        private final String displayName;
        ErrorType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private final String message;
    private final ErrorType errorType;
    private final int line;
    private final int column;

    public AnalysisError(String message, ErrorType errorType, int line, int column) {
        this.message = message;
        this.errorType = errorType;
        this.line = line;
        this.column = column;
    }

    public AnalysisError(String message, ErrorType errorType) {
        this(message, errorType, 0, 0);
    }

    public String getMessage() { return message; }
    public ErrorType getErrorType() { return errorType; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    @Override
    public String toString() {
        return String.format("[%s] Línea %d, Columna %d: %s", 
            errorType.getDisplayName(), line, column, message);
    }
}
