package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Symbol;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;

public class SQLSemanticAnalyzer implements ISemanticAnalyzer {
    private Map<String, Symbol> symbolTable;
    private Stack<String> scopeStack;
    private String currentScope;

    public SQLSemanticAnalyzer() {
        this.symbolTable = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.currentScope = "global";
    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return symbolTable;
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        List<AnalysisError> errors = new ArrayList<>();
        this.symbolTable = symbolTable != null ? symbolTable : new HashMap<>();

        // Inicializar análisis
        scopeStack.clear();
        scopeStack.push("global");
        currentScope = "global";

        // Primera pasada: recolectar declaraciones de tablas y columnas
        recolectarDeclaracionesTablas(tokens, errors);
        // Segunda pasada: validar referencias y tipos
        validarReferencias(tokens, errors);

        return errors;
    }

    private void recolectarDeclaracionesTablas(List<Token> tokens, List<AnalysisError> errors) {
        boolean inCreateTable = false;
        String currentTable = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (esSentenciaCrearTabla(tokens, i)) {
                inCreateTable = true;
                if (i + 2 < tokens.size()) {
                    currentTable = tokens.get(i + 2).getValue();
                    manejarDeclaracionTabla(currentTable, tokens.get(i + 2), errors);
                }
            }

            if (inCreateTable && currentTable != null) {
                if (esDefinicionColumna(tokens, i)) {
                    manejarDeclaracionColumna(currentTable, tokens, i, errors);
                }
            }

            if (token.getValue().equals(";")) {
                inCreateTable = false;
                currentTable = null;
            }
        }
    }

    private void manejarDeclaracionTabla(String tableName, Token token, List<AnalysisError> errors) {
        if (symbolTable.containsKey(obtenerClaveSimbolo(tableName))) {
            errors.add(new AnalysisError(
                    "Tabla ya declarada: " + tableName,
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        } else {
            Symbol symbol = new Symbol(tableName, Symbol.SymbolType.TABLE);
            symbol.setScope(currentScope);
            symbol.setDeclarationLine(token.getLine());
            symbolTable.put(obtenerClaveSimbolo(tableName), symbol);
        }
    }

    private void manejarDeclaracionColumna(String tableName, List<Token> tokens, int position, List<AnalysisError> errors) {
        Token columnToken = tokens.get(position);
        Token typeToken = tokens.get(position + 1);

        String columnName = columnToken.getValue();
        String dataType = typeToken.getValue().toUpperCase();

        String columnKey = tableName + "." + columnName;
        Symbol symbol = new Symbol(columnName, Symbol.SymbolType.COLUMN);
        symbol.setScope(tableName);
        symbol.setDataType(dataType);
        symbol.setDeclarationLine(columnToken.getLine());

        symbolTable.put(obtenerClaveSimbolo(columnKey), symbol);
    }

    private void validarReferencias(List<Token> tokens, List<AnalysisError> errors) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (esSentenciaSelect(tokens, i)) {
                validarSentenciaSelect(tokens, i, errors);
            } else if (esSentenciaInsert(tokens, i)) {
                validarSentenciaInsert(tokens, i, errors);
            } else if (esRestriccionClaveForanea(tokens, i)) {
                validarRestriccionClaveForanea(tokens, i, errors);
            }
        }
    }

    private void validarRestriccionClaveForanea(List<Token> tokens, int position, List<AnalysisError> errors) {
        if (position + 3 < tokens.size() && tokens.get(position + 1).getValue().equalsIgnoreCase("REFERENCES")) {
            String referencedTable = tokens.get(position + 2).getValue();
            if (!symbolTable.containsKey(obtenerClaveSimbolo(referencedTable))) {
                errors.add(new AnalysisError(
                        "Tabla referenciada no declarada: " + referencedTable,
                        AnalysisError.ErrorType.SEMANTIC,
                        tokens.get(position).getLine(),
                        tokens.get(position).getColumn()
                ));
            }
        } else {
            errors.add(new AnalysisError(
                    "Declaración de clave foránea incompleta.",
                    AnalysisError.ErrorType.SEMANTIC,
                    tokens.get(position).getLine(),
                    tokens.get(position).getColumn()
            ));
        }
    }

    private void validarSentenciaSelect(List<Token> tokens, int position, List<AnalysisError> errors) {
        List<String> referencedTables = new ArrayList<>();
        List<String> referencedColumns = new ArrayList<>();

        // Recolectar tablas y columnas referenciadas
        for (int i = position; i < tokens.size() && !tokens.get(i).getValue().equals(";"); i++) {
            Token token = tokens.get(i);

            if (token.getType().equals("IDENTIFICADOR")) {
                if (esTokenPrevioFrom(tokens, i)) {
                    referencedTables.add(token.getValue());
                } else if (!token.getValue().equals("*")) {
                    referencedColumns.add(token.getValue());
                }
            }
        }

        // Validar existencia de tablas y columnas
        for (String table : referencedTables) {
            if (!symbolTable.containsKey(obtenerClaveSimbolo(table))) {
                errors.add(new AnalysisError(
                        "Tabla no declarada: " + table,
                        AnalysisError.ErrorType.SEMANTIC,
                        tokens.get(position).getLine(),
                        tokens.get(position).getColumn()
                ));
            }
        }

        for (String column : referencedColumns) {
            boolean columnExists = false;
            for (String table : referencedTables) {
                if (symbolTable.containsKey(obtenerClaveSimbolo(table + "." + column))) {
                    columnExists = true;
                    break;
                }
            }
            if (!columnExists) {
                errors.add(new AnalysisError(
                        "Columna no encontrada en ninguna tabla referenciada: " + column,
                        AnalysisError.ErrorType.SEMANTIC,
                        tokens.get(position).getLine(),
                        tokens.get(position).getColumn()
                ));
            }
        }
    }

    private void validarSentenciaInsert(List<Token> tokens, int position, List<AnalysisError> errors) {
        String tableName = null;
        List<String> columns = new ArrayList<>();
        List<Token> values = new ArrayList<>();

        // Obtener tabla objetivo y columnas
        for (int i = position; i < tokens.size() && !tokens.get(i).getValue().equals(";"); i++) {
            Token token = tokens.get(i);
            if (token.getValue().equalsIgnoreCase("INTO") && i + 1 < tokens.size()) {
                tableName = tokens.get(i + 1).getValue();
            }
            // Recolectar columnas y valores
            if (token.getType().equals("IDENTIFICADOR") && !token.getValue().equalsIgnoreCase("INTO")) {
                columns.add(token.getValue());
            }
            if (token.getValue().equalsIgnoreCase("VALUES")) {
                i++; while (i < tokens.size() && !tokens.get(i).getValue().equals(";")) {
                    if (!tokens.get(i).getValue().equals("(") && !tokens.get(i).getValue().equals(")") &&
                            !tokens.get(i).getValue().equals(",")) {
                        values.add(tokens.get(i));
                    }
                    i++;
                }
            }
        }

        // Validar tabla
        if (tableName != null && !symbolTable.containsKey(obtenerClaveSimbolo(tableName))) {
            errors.add(new AnalysisError(
                    "Tabla no declarada para INSERT: " + tableName,
                    AnalysisError.ErrorType.SEMANTIC,
                    tokens.get(position).getLine(),
                    tokens.get(position).getColumn()
            ));
        }

        // Validar columnas y tipos de datos
        if (tableName != null) {
            for (int i = 0; i < columns.size(); i++) {
                String columnKey = tableName + "." + columns.get(i);
                Symbol columnSymbol = symbolTable.get(obtenerClaveSimbolo(columnKey));

                if (columnSymbol == null) {
                    errors.add(new AnalysisError(
                            "Columna no existente en tabla " + tableName + ": " + columns.get(i),
                            AnalysisError.ErrorType.SEMANTIC,
                            tokens.get(position).getLine(),
                            tokens.get(position).getColumn()
                    ));
                } else if (i < values.size()) {
                    validarTipoDeDato(columnSymbol.getDataType(), values.get(i), errors);
                }
            }
        }
    }

    private void validarTipoDeDato(String expectedType, Token valueToken, List<AnalysisError> errors) {
        String value = valueToken.getValue();

        switch (expectedType) {
            case "INTEGER":
            case "INT":
                if (!value.matches("-?\\d+")) {
                    agregarErrorDeTipo(expectedType, value, valueToken, errors);
                }
                break;

            case "DECIMAL":
            case "NUMERIC":
                if (!value.matches("-?\\d*\\.?\\d+")) {
                    agregarErrorDeTipo(expectedType, value, valueToken, errors);
                }
                break;

            case "VARCHAR":
            case "CHAR":
            case "TEXT":
                if (!value.startsWith("'") || !value.endsWith("'")) {
                    agregarErrorDeTipo(expectedType, value, valueToken, errors);
                }
                break;

            case "DATE":
                if (!value.matches("'\\d{4}-\\d{2}-\\d{2}'")) {
                    agregarErrorDeTipo(expectedType, value, valueToken, errors);
                }
                break;
        }
    }

    private void agregarErrorDeTipo(String expectedType, String value, Token token, List<AnalysisError> errors) {
        errors.add(new AnalysisError(
                "Tipo de dato incorrecto. Se esperaba " + expectedType + " pero se encontró: " + value,
                AnalysisError.ErrorType.SEMANTIC,
                token.getLine(),
                token.getColumn()
        ));
    }

    private String obtenerClaveSimbolo(String identifier) {
        return currentScope + "." + identifier;
    }

    private boolean esSentenciaCrearTabla(List<Token> tokens, int position) {
        return position + 2 < tokens.size() &&
                tokens.get(position).getValue().equalsIgnoreCase("CREATE") &&
                tokens.get(position + 1).getValue().equalsIgnoreCase("TABLE");
    }

    private boolean esDefinicionColumna(List<Token> tokens, int position) {
        return tokens.get(position).getType().equals("IDENTIFICADOR") &&
                position + 1 < tokens.size() &&
                esTipoDeDato(tokens.get(position + 1).getValue());
    }

    private boolean esTipoDeDato(String value) {
        String upperValue = value.toUpperCase();
        return upperValue.equals("INT") || upperValue.equals("VARCHAR") ||
                upperValue.equals("DATE") || upperValue.equals("DECIMAL") ||
                upperValue.equals("TEXT") || upperValue.equals("CHAR") ||
                upperValue.equals("NUMERIC");
    }

    private boolean esSentenciaSelect(List<Token> tokens, int position) {
        return tokens.get(position).getValue().equalsIgnoreCase("SELECT");
    }

    private boolean esSentenciaInsert(List<Token> tokens, int position) {
        return tokens.get(position).getValue().equalsIgnoreCase("INSERT");
    }

    private boolean esRestriccionClaveForanea(List<Token> tokens, int position) {
        return position + 1 < tokens.size() &&
                tokens.get(position).getValue().equalsIgnoreCase("FOREIGN") &&
                tokens.get(position + 1).getValue().equalsIgnoreCase("KEY");
    }

    private boolean esTokenPrevioFrom(List<Token> tokens, int position) {
        return position > 0 && tokens.get(position - 1).getValue().equalsIgnoreCase("FROM");
    }
}
