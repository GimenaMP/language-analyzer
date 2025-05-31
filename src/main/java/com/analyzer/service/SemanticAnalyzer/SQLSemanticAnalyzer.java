package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Symbol;
import com.analyzer.model.Symbol.SymbolType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;
import java.util.regex.Pattern;

public class SQLSemanticAnalyzer implements ISemanticAnalyzer {

    private Map<String, Symbol> symbolTable;
    private List<AnalysisError> errors;
    private List<Token> tokens;
    private int currentPosition;

    // Context tracking para análisis semántico
    private Map<String, TableInfo> tableDefinitions;
    private Map<String, String> aliases; // alias -> tabla real
    private Set<String> currentSelectColumns; // columnas en SELECT actual
    private boolean inAggregateContext; // si estamos en contexto de función agregada
    private Set<String> groupByColumns; // columnas en GROUP BY
    private String currentTableContext; // tabla actual en INSERT/UPDATE
    private Map<String, String> columnDataTypes; // tabla.columna -> tipo

    // Patrones para validación
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Set<String> SQL_FUNCTIONS = Set.of(
            "COUNT", "SUM", "AVG", "MIN", "MAX", "UPPER", "LOWER", "LENGTH", "SUBSTRING"
    );
    private static final Set<String> AGGREGATE_FUNCTIONS = Set.of(
            "COUNT", "SUM", "AVG", "MIN", "MAX"
    );

    // Inner class para información de tabla
    private static class TableInfo {
        Map<String, String> columns; // columna -> tipo
        Set<String> primaryKeys;
        Map<String, String> foreignKeys; // columna -> tabla.columna referenciada

        TableInfo() {
            this.columns = new HashMap<>();
            this.primaryKeys = new HashSet<>();
            this.foreignKeys = new HashMap<>();
        }
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        initializeAnalysis(tokens, symbolTable);

        if (!isValidInput(language)) {
            return errors;
        }

        try {
            // 1. Construir información detallada de tablas desde tabla de símbolos
            buildTableInformation();

            // 2. Análisis semántico principal
            performSemanticAnalysis();

            // 3. Validaciones finales
            performFinalValidations();

        } catch (Exception e) {
            addError("Error crítico durante análisis semántico: " + e.getMessage(), null, "INTERNAL");
        }

        return errors;
    }

    private void initializeAnalysis(List<Token> tokens, Map<String, Symbol> symbolTable) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
        this.symbolTable = symbolTable != null ? new HashMap<>(symbolTable) : new HashMap<>();
        this.errors = new ArrayList<>();
        this.currentPosition = 0;

        this.tableDefinitions = new HashMap<>();
        this.aliases = new HashMap<>();
        this.currentSelectColumns = new HashSet<>();
        this.inAggregateContext = false;
        this.groupByColumns = new HashSet<>();
        this.currentTableContext = null;
        this.columnDataTypes = new HashMap<>();
    }

    private boolean isValidInput(LanguageType language) {
        if (!LanguageType.PLSQL.equals(language)) {
            return false;
        }

        if (tokens.isEmpty()) {
            return false;
        }

        return true;
    }

    private void buildTableInformation() {
        for (Symbol symbol : symbolTable.values()) {
            if (SymbolType.TABLE.equals(symbol.getSymbolType())) {
                String tableName = symbol.getName();
                tableDefinitions.put(tableName, new TableInfo());
            } else if (SymbolType.COLUMN.equals(symbol.getSymbolType())) {
                String fullName = symbol.getName();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.", 2);
                    String tableName = parts[0];
                    String columnName = parts[1];
                    String dataType = symbol.getDataType();

                    TableInfo tableInfo = tableDefinitions.computeIfAbsent(tableName, k -> new TableInfo());
                    tableInfo.columns.put(columnName, dataType);
                    columnDataTypes.put(fullName, dataType);
                }
            }
        }
    }

    private void performSemanticAnalysis() {
        currentPosition = 0;

        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null) break;

            if ("PALABRA_CLAVE".equals(token.getType())) {
                analyzeStatement();
            } else {
                advance();
            }
        }
    }

    private void analyzeStatement() {
        Token keyword = getCurrentToken();
        if (keyword == null) return;

        String keywordValue = keyword.getValue().toUpperCase();

        switch (keywordValue) {
            case "CREATE":
                analyzeCreateStatement();
                break;
            case "DROP":
                analyzeDropStatement();
                break;
            case "ALTER":
                analyzeAlterStatement();
                break;
            case "SELECT":
                analyzeSelectStatement();
                break;
            case "INSERT":
                analyzeInsertStatement();
                break;
            case "UPDATE":
                analyzeUpdateStatement();
                break;
            case "DELETE":
                analyzeDeleteStatement();
                break;
            default:
                advance();
        }
    }

    private void analyzeCreateStatement() {
        advance(); // consumir CREATE

        Token nextToken = getCurrentToken();
        if (nextToken != null && "PALABRA_CLAVE".equals(nextToken.getType())) {
            String objectType = nextToken.getValue().toUpperCase();

            switch (objectType) {
                case "TABLE":
                    analyzeCreateTable();
                    break;
                case "INDEX":
                    analyzeCreateIndex();
                    break;
                default:
                    advance();
            }
        }
    }

    private void analyzeCreateTable() {
        advance(); // consumir TABLE

        Token tableNameToken = getCurrentToken();
        if (!"IDENTIFICADOR".equals(tableNameToken.getType())) {
            addError("Se esperaba nombre de tabla", tableNameToken, "SYNTAX");
            return;
        }

        String tableName = tableNameToken.getValue();
        advance();

        // Verificar que el nombre de tabla sea válido
        if (!isValidIdentifier(tableName)) {
            addError("Nombre de tabla inválido: " + tableName, tableNameToken, "NAMING");
        }

        // Verificar que la tabla no exista ya
        if (tableDefinitions.containsKey(tableName)) {
            addError("La tabla '" + tableName + "' ya existe", tableNameToken, "DUPLICATE");
        }

        currentTableContext = tableName;

        // Analizar definición de columnas
        if (consumeTokenIfMatches("SEPARADOR", "(")) {
            analyzeTableDefinition(tableName);

            if (!consumeTokenIfMatches("SEPARADOR", ")")) {
                addError("Se esperaba ')' para cerrar definición de tabla", getCurrentToken(), "SYNTAX");
            }
        }

        currentTableContext = null;
    }

    private void analyzeTableDefinition(String tableName) {
        TableInfo tableInfo = tableDefinitions.computeIfAbsent(tableName, k -> new TableInfo());
        Set<String> declaredColumns = new HashSet<>();

        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null ||
                    ("SEPARADOR".equals(token.getType()) && ")".equals(token.getValue()))) {
                break;
            }

            if ("IDENTIFICADOR".equals(token.getType())) {
                String columnName = token.getValue();
                Token columnToken = token;
                advance();

                // Verificar nombre de columna válido
                if (!isValidIdentifier(columnName)) {
                    addError("Nombre de columna inválido: " + columnName, columnToken, "NAMING");
                }

                // Verificar columna duplicada
                if (declaredColumns.contains(columnName)) {
                    addError("Columna duplicada: " + columnName, columnToken, "DUPLICATE");
                } else {
                    declaredColumns.add(columnName);
                }

                // Analizar tipo de dato
                Token dataTypeToken = getCurrentToken();
                if (isDataType(dataTypeToken)) {
                    String dataType = dataTypeToken.getValue();
                    tableInfo.columns.put(columnName, dataType);
                    advance();

                    // Analizar restricciones de columna
                    analyzeColumnConstraints(tableName, columnName, tableInfo);

                } else {
                    addError("Se esperaba tipo de dato para columna: " + columnName, dataTypeToken, "SYNTAX");
                }
            }

            // Consumir coma si existe
            if (!consumeTokenIfMatches("SEPARADOR", ",")) {
                break;
            }
        }

        // Validar que la tabla tenga al menos una columna
        if (declaredColumns.isEmpty()) {
            addError("La tabla debe tener al menos una columna", null, "STRUCTURE");
        }
    }

    private void analyzeColumnConstraints(String tableName, String columnName, TableInfo tableInfo) {
        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null || !"PALABRA_CLAVE".equals(token.getType())) {
                break;
            }

            String constraint = token.getValue().toUpperCase();

            switch (constraint) {
                case "PRIMARY":
                    advance();
                    if (expectToken("KEY")) {
                        tableInfo.primaryKeys.add(columnName);
                    }
                    break;

                case "FOREIGN":
                    advance();
                    if (expectToken("KEY")) {
                        analyzeForeignKeyConstraint(tableName, columnName, tableInfo);
                    }
                    break;

                case "NOT":
                    advance();
                    expectToken("NULL");
                    break;

                case "NULL":
                case "UNIQUE":
                case "AUTO_INCREMENT":
                    advance();
                    break;

                case "DEFAULT":
                    advance();
                    analyzeDefaultValue(tableName, columnName);
                    break;

                default:
                    return; // No es una restricción de columna
            }
        }
    }

    private void analyzeForeignKeyConstraint(String tableName, String columnName, TableInfo tableInfo) {
        if (expectToken("REFERENCES")) {
            Token refTableToken = getCurrentToken();
            if ("IDENTIFICADOR".equals(refTableToken.getType())) {
                String refTableName = refTableToken.getValue();
                advance();

                // Verificar que la tabla referenciada existe
                if (!tableDefinitions.containsKey(refTableName)) {
                    addError("Tabla referenciada no existe: " + refTableName, refTableToken, "REFERENCE");
                }

                if (consumeTokenIfMatches("SEPARADOR", "(")) {
                    Token refColumnToken = getCurrentToken();
                    if ("IDENTIFICADOR".equals(refColumnToken.getType())) {
                        String refColumnName = refColumnToken.getValue();
                        advance();

                        // Verificar que la columna referenciada existe
                        TableInfo refTableInfo = tableDefinitions.get(refTableName);
                        if (refTableInfo != null && !refTableInfo.columns.containsKey(refColumnName)) {
                            addError("Columna referenciada no existe: " + refTableName + "." + refColumnName,
                                    refColumnToken, "REFERENCE");
                        }

                        // Verificar compatibilidad de tipos
                        if (refTableInfo != null) {
                            String sourceType = tableInfo.columns.get(columnName);
                            String targetType = refTableInfo.columns.get(refColumnName);
                            if (!areTypesCompatible(sourceType, targetType)) {
                                addError("Tipos incompatibles en clave foránea: " + sourceType + " -> " + targetType,
                                        refColumnToken, "TYPE_MISMATCH");
                            }
                        }

                        // Registrar clave foránea
                        tableInfo.foreignKeys.put(columnName, refTableName + "." + refColumnName);

                        consumeTokenIfMatches("SEPARADOR", ")");
                    }
                }
            }
        }
    }

    private void analyzeDefaultValue(String tableName, String columnName) {
        Token defaultToken = getCurrentToken();
        if (defaultToken != null) {
            String tokenType = defaultToken.getType();
            String defaultValue = defaultToken.getValue();

            // Obtener tipo de la columna
            TableInfo tableInfo = tableDefinitions.get(tableName);
            if (tableInfo != null) {
                String columnType = tableInfo.columns.get(columnName);

                // Verificar compatibilidad del valor por defecto con el tipo de columna
                if (!isDefaultValueCompatible(defaultValue, tokenType, columnType)) {
                    addError("Valor por defecto incompatible con tipo de columna: " + defaultValue +
                            " para tipo " + columnType, defaultToken, "TYPE_MISMATCH");
                }
            }

            advance();
        }
    }

    private void analyzeSelectStatement() {
        advance(); // consumir SELECT

        currentSelectColumns.clear();
        inAggregateContext = false;
        groupByColumns.clear();
        aliases.clear();

        // Analizar lista de columnas
        analyzeSelectList();

        // FROM es obligatorio
        if (!expectToken("FROM")) {
            addError("Se esperaba cláusula FROM después de SELECT", getCurrentToken(), "SYNTAX");
            return;
        }

        // Analizar tablas en FROM
        analyzeFromClause();

        // Analizar cláusulas opcionales
        analyzeOptionalClauses();

        // Validaciones post-SELECT
        validateSelectSemantics();
    }

    private void analyzeSelectList() {
        do {
            analyzeSelectExpression();
        } while (consumeTokenIfMatches("SEPARADOR", ","));
    }

    private void analyzeSelectExpression() {
        Token token = getCurrentToken();
        if (token == null) return;

        if ("OPERADOR_ARITMETICO".equals(token.getType()) && "*".equals(token.getValue())) {
            // SELECT *
            advance();
            currentSelectColumns.add("*");
        } else if ("IDENTIFICADOR".equals(token.getType())) {
            String identifier = token.getValue();
            advance();

            // Verificar si es una función
            if (consumeTokenIfMatches("SEPARADOR", "(")) {
                analyzeFunctionCall(identifier, token);
                consumeTokenIfMatches("SEPARADOR", ")");
            } else {
                // Es una referencia de columna
                validateColumnReference(identifier, token);
                currentSelectColumns.add(identifier);
            }

            // Verificar alias
            analyzeAlias();

        } else {
            // Expresión más compleja
            analyzeExpression();
        }
    }

    private void analyzeFunctionCall(String functionName, Token functionToken) {
        String upperFunctionName = functionName.toUpperCase();

        // Verificar que la función existe
        if (!SQL_FUNCTIONS.contains(upperFunctionName)) {
            addError("Función desconocida: " + functionName, functionToken, "FUNCTION");
        }

        // Verificar contexto de función agregada
        if (AGGREGATE_FUNCTIONS.contains(upperFunctionName)) {
            inAggregateContext = true;

            // Analizar argumentos de función agregada
            Token argToken = getCurrentToken();
            if ("OPERADOR_ARITMETICO".equals(argToken.getType()) && "*".equals(argToken.getValue())) {
                // COUNT(*)
                advance();
            } else if ("IDENTIFICADOR".equals(argToken.getType())) {
                validateColumnReference(argToken.getValue(), argToken);
                advance();
            }
        }
    }

    private void analyzeFromClause() {
        do {
            analyzeTableReference();
        } while (consumeTokenIfMatches("SEPARADOR", ","));
    }

    private void analyzeTableReference() {
        Token tableToken = getCurrentToken();
        if (!"IDENTIFICADOR".equals(tableToken.getType())) {
            addError("Se esperaba nombre de tabla en FROM", tableToken, "SYNTAX");
            return;
        }

        String tableName = tableToken.getValue();
        advance();

        // Verificar que la tabla existe
        if (!tableDefinitions.containsKey(tableName)) {
            addError("Tabla no definida: " + tableName, tableToken, "REFERENCE");
        }

        // Verificar alias
        analyzeTableAlias(tableName);
    }

    private void analyzeTableAlias(String tableName) {
        Token nextToken = getCurrentToken();

        // Verificar palabra clave AS opcional
        boolean hasAs = false;
        if (nextToken != null && "PALABRA_CLAVE".equals(nextToken.getType()) &&
                "AS".equalsIgnoreCase(nextToken.getValue())) {
            hasAs = true;
            advance();
            nextToken = getCurrentToken();
        }

        // Verificar identificador de alias
        if (nextToken != null && "IDENTIFICADOR".equals(nextToken.getType()) &&
                !isKeyword(nextToken.getValue())) {

            String alias = nextToken.getValue();

            // Verificar que el alias no esté ya usado
            if (aliases.containsKey(alias)) {
                addError("Alias duplicado: " + alias, nextToken, "DUPLICATE");
            } else {
                aliases.put(alias, tableName);
            }

            advance();
        } else if (hasAs) {
            addError("Se esperaba alias después de AS", nextToken, "SYNTAX");
        }
    }

    private void analyzeOptionalClauses() {
        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null || !"PALABRA_CLAVE".equals(token.getType())) break;

            String keyword = token.getValue().toUpperCase();

            switch (keyword) {
                case "WHERE":
                    analyzeWhereClause();
                    break;
                case "GROUP":
                    analyzeGroupByClause();
                    break;
                case "HAVING":
                    analyzeHavingClause();
                    break;
                case "ORDER":
                    analyzeOrderByClause();
                    break;
                default:
                    if (isStatementStart(keyword)) {
                        return;
                    }
                    advance();
            }
        }
    }

    private void analyzeGroupByClause() {
        advance(); // consumir GROUP
        expectToken("BY");

        groupByColumns.clear();

        do {
            Token columnToken = getCurrentToken();
            if ("IDENTIFICADOR".equals(columnToken.getType())) {
                String columnName = columnToken.getValue();
                validateColumnReference(columnName, columnToken);
                groupByColumns.add(columnName);
                advance();
            } else {
                addError("Se esperaba nombre de columna en GROUP BY", columnToken, "SYNTAX");
                advance();
            }
        } while (consumeTokenIfMatches("SEPARADOR", ","));
    }

    private void analyzeHavingClause() {
        advance(); // consumir HAVING

        if (groupByColumns.isEmpty()) {
            addError("HAVING requiere GROUP BY", getCurrentToken(), "STRUCTURE");
        }

        analyzeCondition();
    }

    private void analyzeCondition() {
        // Analizar condiciones en WHERE/HAVING
        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null) break;

            if ("IDENTIFICADOR".equals(token.getType())) {
                validateColumnReference(token.getValue(), token);
            } else if ("PALABRA_CLAVE".equals(token.getType())) {
                String keyword = token.getValue().toUpperCase();
                if (isClauseStart(keyword) || isStatementStart(keyword)) {
                    return;
                }
            }

            advance();
        }
    }

    private void analyzeInsertStatement() {
        advance(); // consumir INSERT
        expectToken("INTO");

        Token tableToken = getCurrentToken();
        if (!"IDENTIFICADOR".equals(tableToken.getType())) {
            addError("Se esperaba nombre de tabla después de INSERT INTO", tableToken, "SYNTAX");
            return;
        }

        String tableName = tableToken.getValue();
        currentTableContext = tableName;
        advance();

        // Verificar que la tabla existe
        if (!tableDefinitions.containsKey(tableName)) {
            addError("Tabla no definida: " + tableName, tableToken, "REFERENCE");
            return;
        }

        List<String> specifiedColumns = new ArrayList<>();

        // Analizar lista de columnas opcional
        if (consumeTokenIfMatches("SEPARADOR", "(")) {
            analyzeInsertColumnList(tableName, specifiedColumns);
            consumeTokenIfMatches("SEPARADOR", ")");
        }

        expectToken("VALUES");
        analyzeInsertValues(tableName, specifiedColumns);

        currentTableContext = null;
    }

    private void analyzeInsertColumnList(String tableName, List<String> specifiedColumns) {
        TableInfo tableInfo = tableDefinitions.get(tableName);

        do {
            Token columnToken = getCurrentToken();
            if ("IDENTIFICADOR".equals(columnToken.getType())) {
                String columnName = columnToken.getValue();

                // Verificar que la columna existe en la tabla
                if (tableInfo != null && !tableInfo.columns.containsKey(columnName)) {
                    addError("Columna no existe en tabla " + tableName + ": " + columnName,
                            columnToken, "REFERENCE");
                }

                // Verificar columna duplicada en la lista
                if (specifiedColumns.contains(columnName)) {
                    addError("Columna duplicada en INSERT: " + columnName, columnToken, "DUPLICATE");
                } else {
                    specifiedColumns.add(columnName);
                }

                advance();
            } else {
                addError("Se esperaba nombre de columna", columnToken, "SYNTAX");
                advance();
            }
        } while (consumeTokenIfMatches("SEPARADOR", ","));
    }

    private void analyzeInsertValues(String tableName, List<String> specifiedColumns) {
        if (!consumeTokenIfMatches("SEPARADOR", "(")) {
            addError("Se esperaba '(' después de VALUES", getCurrentToken(), "SYNTAX");
            return;
        }

        TableInfo tableInfo = tableDefinitions.get(tableName);
        if (tableInfo == null) return;

        List<String> targetColumns = specifiedColumns.isEmpty() ?
                new ArrayList<>(tableInfo.columns.keySet()) : specifiedColumns;

        int valueCount = 0;

        do {
            Token valueToken = getCurrentToken();
            if (valueToken != null) {
                // Verificar tipo de valor vs tipo de columna
                if (valueCount < targetColumns.size()) {
                    String columnName = targetColumns.get(valueCount);
                    String expectedType = tableInfo.columns.get(columnName);

                    if (!isValueCompatibleWithColumnType(valueToken, expectedType)) {
                        addError("Tipo de valor incompatible para columna " + columnName +
                                ": esperado " + expectedType, valueToken, "TYPE_MISMATCH");
                    }
                }

                valueCount++;
                advance();
            }
        } while (consumeTokenIfMatches("SEPARADOR", ","));

        // Verificar que el número de valores coincida con el número de columnas
        if (valueCount != targetColumns.size()) {
            addError("Número de valores (" + valueCount + ") no coincide con número de columnas (" +
                    targetColumns.size() + ")", null, "STRUCTURE");
        }

        consumeTokenIfMatches("SEPARADOR", ")");
    }

    private void analyzeUpdateStatement() {
        advance(); // consumir UPDATE

        Token tableToken = getCurrentToken();
        if (!"IDENTIFICADOR".equals(tableToken.getType())) {
            addError("Se esperaba nombre de tabla después de UPDATE", tableToken, "SYNTAX");
            return;
        }

        String tableName = tableToken.getValue();
        currentTableContext = tableName;
        advance();

        // Verificar que la tabla existe
        if (!tableDefinitions.containsKey(tableName)) {
            addError("Tabla no definida: " + tableName, tableToken, "REFERENCE");
            return;
        }

        expectToken("SET");
        analyzeUpdateAssignments(tableName);

        // WHERE opcional
        if (checkToken("WHERE")) {
            analyzeWhereClause();
        }

        currentTableContext = null;
    }

    private void analyzeUpdateAssignments(String tableName) {
        TableInfo tableInfo = tableDefinitions.get(tableName);

        do {
            Token columnToken = getCurrentToken();
            if (!"IDENTIFICADOR".equals(columnToken.getType())) {
                addError("Se esperaba nombre de columna en SET", columnToken, "SYNTAX");
                return;
            }

            String columnName = columnToken.getValue();
            advance();

            // Verificar que la columna existe
            if (tableInfo != null && !tableInfo.columns.containsKey(columnName)) {
                addError("Columna no existe en tabla " + tableName + ": " + columnName,
                        columnToken, "REFERENCE");
            }

            if (!expectTokenType("OPERADOR_COMP_SIMPLE", "=")) {
                addError("Se esperaba '=' después de nombre de columna", getCurrentToken(), "SYNTAX");
                return;
            }

            // Analizar valor asignado
            Token valueToken = getCurrentToken();
            if (valueToken != null && tableInfo != null) {
                String expectedType = tableInfo.columns.get(columnName);
                if (!isValueCompatibleWithColumnType(valueToken, expectedType)) {
                    addError("Tipo de valor incompatible para columna " + columnName +
                            ": esperado " + expectedType, valueToken, "TYPE_MISMATCH");
                }
                advance();
            }

        } while (consumeTokenIfMatches("SEPARADOR", ","));
    }

    private void analyzeDeleteStatement() {
        advance(); // consumir DELETE
        expectToken("FROM");

        Token tableToken = getCurrentToken();
        if (!"IDENTIFICADOR".equals(tableToken.getType())) {
            addError("Se esperaba nombre de tabla después de DELETE FROM", tableToken, "SYNTAX");
            return;
        }

        String tableName = tableToken.getValue();
        advance();

        // Verificar que la tabla existe
        if (!tableDefinitions.containsKey(tableName)) {
            addError("Tabla no definida: " + tableName, tableToken, "REFERENCE");
        }

        // WHERE opcional pero recomendado
        if (checkToken("WHERE")) {
            analyzeWhereClause();
        } else {
            addError("DELETE sin WHERE eliminará todos los registros de la tabla",
                    tableToken, "WARNING");
        }
    }

    private void validateSelectSemantics() {
        // Validar uso correcto de funciones agregadas con GROUP BY
        if (inAggregateContext && !groupByColumns.isEmpty()) {
            for (String selectColumn : currentSelectColumns) {
                if (!"*".equals(selectColumn) &&
                        !groupByColumns.contains(selectColumn) &&
                        !isAggregateFunction(selectColumn)) {

                    addError("Columna '" + selectColumn +
                                    "' debe estar en GROUP BY o ser una función agregada",
                            null, "GROUPBY_VIOLATION");
                }
            }
        }
    }

    private void performFinalValidations() {
        // Validar integridad referencial
        validateReferentialIntegrity();

        // Validar que no haya tablas sin usar
        validateUnusedTables();

        // Validar nombres de identificadores
        validateIdentifierNaming();
    }

    private void validateReferentialIntegrity() {
        for (Map.Entry<String, TableInfo> entry : tableDefinitions.entrySet()) {
            String tableName = entry.getKey();
            TableInfo tableInfo = entry.getValue();

            for (Map.Entry<String, String> fkEntry : tableInfo.foreignKeys.entrySet()) {
                String fkColumn = fkEntry.getKey();
                String reference = fkEntry.getValue();
                String[] refParts = reference.split("\\.");

                if (refParts.length == 2) {
                    String refTable = refParts[0];
                    String refColumn = refParts[1];

                    TableInfo refTableInfo = tableDefinitions.get(refTable);
                    if (refTableInfo != null && !refTableInfo.primaryKeys.contains(refColumn)) {
                        addError("Clave foránea " + tableName + "." + fkColumn +
                                " debe referenciar una clave primaria", null, "INTEGRITY");
                    }
                }
            }
        }
    }

    private void validateUnusedTables() {
        // Esta validación podría implementarse rastreando el uso de tablas
        // en consultas SELECT, INSERT, UPDATE, DELETE
    }

    private void validateIdentifierNaming() {
        for (Symbol symbol : symbolTable.values()) {
            String name = symbol.getName();
            if (name.contains(".")) {
                name = name.substring(name.lastIndexOf(".") + 1);
            }

            if (!isValidIdentifier(name)) {
                addError("Nombre de identificador inválido: " + name, null, "NAMING");
            }
        }
    }

    // Métodos auxiliares de validación
    private void validateColumnReference(String columnName, Token token) {
        // Verificar si es un alias de tabla
        if (aliases.containsKey(columnName)) {
            return;
        }

        // Verificar si la columna existe en alguna tabla conocida
        boolean found = false;
        for (TableInfo tableInfo : tableDefinitions.values()) {
            if (tableInfo.columns.containsKey(columnName)) {
                found = true;
                break;
            }
        }

        if (!found && !tableDefinitions.isEmpty()) {
            addError("Columna no definida: " + columnName, token, "REFERENCE");
        }
    }

    private boolean areTypesCompatible(String type1, String type2) {
        if (type1 == null || type2 == null) return false;
        if (type1.equals(type2)) return true;

        // Reglas de compatibilidad específicas para SQL
        String upper1 = type1.toUpperCase();
        String upper2 = type2.toUpperCase();

        // Tipos numéricos son compatibles entre sí
        Set<String> numericTypes = Set.of("INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT",
                "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE", "REAL");

        if (numericTypes.contains(upper1) && numericTypes.contains(upper2)) {
            return true;
        }

        // Tipos de texto son compatibles entre sí
        Set<String> textTypes = Set.of("VARCHAR", "CHAR", "TEXT", "NVARCHAR", "NCHAR");

        return textTypes.contains(upper1) && textTypes.contains(upper2);
    }

    private boolean isDefaultValueCompatible(String value, String tokenType, String columnType) {
        if (columnType == null) return true;

        String upperColumnType = columnType.toUpperCase();

        // Verificar compatibilidad según tipo de token
        switch (tokenType) {
            case "NUMERO_ENTERO":
            case "NUMERO_DECIMAL":
                return upperColumnType.contains("INT") || upperColumnType.contains("DECIMAL") ||
                        upperColumnType.contains("NUMERIC") || upperColumnType.contains("FLOAT") ||
                        upperColumnType.contains("DOUBLE") || upperColumnType.contains("REAL");

            case "CADENA_SIMPLE":
            case "CADENA_DOBLE":
                return upperColumnType.contains("VARCHAR") || upperColumnType.contains("CHAR") ||
                        upperColumnType.contains("TEXT");

            case "VALOR_BOOLEANO":
                return upperColumnType.contains("BOOL") || upperColumnType.contains("BIT");

            default:
                return true; // NULL u otros valores especiales
        }
    }

    private boolean isValueCompatibleWithColumnType(Token valueToken, String columnType) {
        if (valueToken == null || columnType == null) return true;

        return isDefaultValueCompatible(valueToken.getValue(), valueToken.getType(), columnType);
    }

    private boolean isValidIdentifier(String identifier) {
        return identifier != null && VALID_IDENTIFIER.matcher(identifier).matches();
    }

    private boolean isDataType(Token token) {
        if (token == null) return false;
        String type = token.getType();
        return "TIPO_DATO_ENTERO".equals(type) || "TIPO_DATO_NUMERICO".equals(type) ||
                "TIPO_DATO_TEXTO".equals(type) || "TIPO_DATO_FECH_HOR".equals(type) ||
                "TIPO_DATO_BOOL".equals(type) || "TIPO_DATO_BINARIO".equals(type);
    }

    private boolean isKeyword(String value) {
        String upper = value.toUpperCase();
        return Set.of("SELECT", "FROM", "WHERE", "GROUP", "BY", "ORDER", "HAVING",
                "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER",
                "TABLE", "INDEX", "PRIMARY", "KEY", "FOREIGN", "REFERENCES").contains(upper);
    }

    private boolean isStatementStart(String keyword) {
        return Set.of("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER").contains(keyword);
    }

    private boolean isClauseStart(String keyword) {
        return Set.of("WHERE", "GROUP", "HAVING", "ORDER").contains(keyword);
    }

    private boolean isAggregateFunction(String expression) {
        return AGGREGATE_FUNCTIONS.stream().anyMatch(func ->
                expression.toUpperCase().startsWith(func + "("));
    }

    // Métodos utilitarios para navegación de tokens
    private Token getCurrentToken() {
        return currentPosition < tokens.size() ? tokens.get(currentPosition) : null;
    }

    private void advance() {
        currentPosition++;
    }

    private boolean expectToken(String expectedValue) {
        Token token = getCurrentToken();
        if (token != null && expectedValue.equalsIgnoreCase(token.getValue())) {
            advance();
            return true;
        }
        addError("Se esperaba '" + expectedValue + "'", token, "SYNTAX");
        return false;
    }

    private boolean expectTokenType(String expectedType, String expectedValue) {
        Token token = getCurrentToken();
        if (token != null && expectedType.equals(token.getType()) &&
                expectedValue.equals(token.getValue())) {
            advance();
            return true;
        }
        return false;
    }

    private boolean checkToken(String value) {
        Token token = getCurrentToken();
        return token != null && value.equalsIgnoreCase(token.getValue());
    }

    private boolean consumeTokenIfMatches(String type, String value) {
        Token token = getCurrentToken();
        if (token != null && type.equals(token.getType()) && value.equals(token.getValue())) {
            advance();
            return true;
        }
        return false;
    }

    private void analyzeAlias() {
        Token nextToken = getCurrentToken();

        // Verificar palabra clave AS opcional
        if (nextToken != null && "PALABRA_CLAVE".equals(nextToken.getType()) &&
                "AS".equalsIgnoreCase(nextToken.getValue())) {
            advance();
            nextToken = getCurrentToken();
        }

        // Verificar identificador de alias
        if (nextToken != null && "IDENTIFICADOR".equals(nextToken.getType()) &&
                !isKeyword(nextToken.getValue())) {
            advance();
        }
    }

    private void analyzeWhereClause() {
        advance(); // consumir WHERE
        analyzeCondition();
    }

    private void analyzeOrderByClause() {
        advance(); // consumir ORDER
        expectToken("BY");

        do {
            Token columnToken = getCurrentToken();
            if ("IDENTIFICADOR".equals(columnToken.getType())) {
                validateColumnReference(columnToken.getValue(), columnToken);
                advance();

                // ASC o DESC opcional
                Token orderToken = getCurrentToken();
                if (orderToken != null && "PALABRA_CLAVE".equals(orderToken.getType())) {
                    String order = orderToken.getValue().toUpperCase();
                    if ("ASC".equals(order) || "DESC".equals(order)) {
                        advance();
                    }
                }
            } else {
                advance();
            }
        } while (consumeTokenIfMatches("SEPARADOR", ","));
    }

    private void analyzeExpression() {
        Token token = getCurrentToken();
        if (token != null) {
            advance();
        }
    }

    private void analyzeDropStatement() {
        advance(); // consumir DROP

        Token objectTypeToken = getCurrentToken();
        if (objectTypeToken != null && "PALABRA_CLAVE".equals(objectTypeToken.getType())) {
            String objectType = objectTypeToken.getValue().toUpperCase();
            advance();

            if ("TABLE".equals(objectType)) {
                Token tableToken = getCurrentToken();
                if ("IDENTIFICADOR".equals(tableToken.getType())) {
                    String tableName = tableToken.getValue();

                    // Verificar que la tabla existe antes de eliminarla
                    if (!tableDefinitions.containsKey(tableName)) {
                        addError("No se puede eliminar tabla inexistente: " + tableName,
                                tableToken, "REFERENCE");
                    }

                    advance();
                }
            }
        }
    }

    private void analyzeAlterStatement() {
        advance(); // consumir ALTER
        // Implementación básica para ALTER TABLE
        if (expectToken("TABLE")) {
            Token tableToken = getCurrentToken();
            if ("IDENTIFICADOR".equals(tableToken.getType())) {
                String tableName = tableToken.getValue();

                if (!tableDefinitions.containsKey(tableName)) {
                    addError("No se puede alterar tabla inexistente: " + tableName,
                            tableToken, "REFERENCE");
                }

                advance();
            }
        }
    }

    private void analyzeCreateIndex() {
        advance(); // consumir INDEX

        if ("IDENTIFICADOR".equals(getCurrentToken().getType())) {
            advance(); // nombre del índice

            if (expectToken("ON")) {
                Token tableToken = getCurrentToken();
                if ("IDENTIFICADOR".equals(tableToken.getType())) {
                    String tableName = tableToken.getValue();

                    if (!tableDefinitions.containsKey(tableName)) {
                        addError("No se puede crear índice en tabla inexistente: " + tableName,
                                tableToken, "REFERENCE");
                    }

                    advance();
                }
            }
        }
    }

    private void addError(String message, Token token, String errorSubtype) {
        AnalysisError.ErrorType errorType = AnalysisError.ErrorType.SEMANTIC;

        if (token != null) {
            errors.add(new AnalysisError(
                    message + " [" + errorSubtype + "]",
                    errorType,
                    token.getLine(),
                    token.getColumn()
            ));
        } else {
            errors.add(new AnalysisError(
                    message + " [" + errorSubtype + "]",
                    errorType
            ));
        }
    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return Collections.unmodifiableMap(symbolTable);
    }
}