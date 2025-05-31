package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Symbol;
import com.analyzer.model.Symbol.SymbolType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class SQLSyntacticAnalyzer implements ISyntacticAnalyzer {

    private int currentPosition;
    private List<Token> tokens;
    private List<AnalysisError> errors;
    private Map<String, Symbol> symbolTable;
    private Stack<String> clausulaStack;
    private String currentScope;

    // Context tracking para tabla de símbolos
    private String currentTableName;
    private Set<String> declaredTables;
    private Map<String, Set<String>> tableColumns; // tabla -> set de columnas
    private Map<String, String> aliases; // alias -> nombre real

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        initializeAnalysis(tokens, language);

        if (!isValidInput()) {
            return errors;
        }

        try {
            parseSQL();
            validateFinalState();
        } catch (Exception e) {
            addError("Error crítico durante análisis sintáctico: " + e.getMessage(), null);
        }

        return errors;
    }

    private void initializeAnalysis(List<Token> tokens, LanguageType language) {
        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.symbolTable = new HashMap<>();
        this.clausulaStack = new Stack<>();
        this.currentPosition = 0;
        this.currentScope = "global";
        this.currentTableName = null;
        this.declaredTables = new HashSet<>();
        this.tableColumns = new HashMap<>();
        this.aliases = new HashMap<>();
    }

    private boolean isValidInput() {
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        return LanguageType.PLSQL.equals(LanguageType.PLSQL); // Verificar si es SQL
    }

    private void parseSQL() {
        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null) break;

            // Solo procesar palabras clave principales
            if ("PALABRA_CLAVE".equals(token.getType())) {
                parseStatement();
            } else {
                advance(); // Avanzar para evitar bucle infinito
            }
        }
    }

    private void parseStatement() {
        Token keyword = getCurrentToken();
        if (keyword == null) return;

        String keywordValue = keyword.getValue().toUpperCase();

        switch (keywordValue) {
            case "CREATE":
                parseCreateStatement();
                break;
            case "DROP":
                parseDropStatement();
                break;
            case "ALTER":
                parseAlterStatement();
                break;
            case "SELECT":
                parseSelectStatement();
                break;
            case "INSERT":
                parseInsertStatement();
                break;
            case "UPDATE":
                parseUpdateStatement();
                break;
            case "DELETE":
                parseDeleteStatement();
                break;
            default:
                advance(); // Palabra clave no reconocida, continuar
        }
    }

    private void parseCreateStatement() {
        expectToken("CREATE");

        Token nextToken = getCurrentToken();
        if (nextToken != null && "PALABRA_CLAVE".equals(nextToken.getType())) {
            String objectType = nextToken.getValue().toUpperCase();

            switch (objectType) {
                case "TABLE":
                    parseCreateTable();
                    break;
                case "INDEX":
                    parseCreateIndex();
                    break;
                case "DATABASE":
                case "SCHEMA":
                    parseCreateDatabase();
                    break;
                default:
                    addError("Tipo de objeto CREATE no soportado: " + objectType, nextToken);
                    advance();
            }
        } else {
            addError("Se esperaba tipo de objeto después de CREATE", getCurrentToken());
        }
    }

    private void parseCreateTable() {
        expectToken("TABLE");

        Token tableName = getCurrentToken();
        if (!expectTokenType("IDENTIFICADOR")) {
            addError("Se esperaba nombre de tabla después de CREATE TABLE", tableName);
            return;
        }

        // Agregar tabla a tabla de símbolos
        currentTableName = tableName.getValue();
        addTableToSymbolTable(currentTableName, tableName.getLine(), tableName.getColumn());

        if (!expectTokenType("SEPARADOR", "(")) {
            addError("Se esperaba '(' después de nombre de tabla", getCurrentToken());
            return;
        }

        parseColumnDefinitions();

        if (!expectTokenType("SEPARADOR", ")")) {
            addError("Se esperaba ')' para cerrar definición de tabla", getCurrentToken());
        }

        currentTableName = null;
    }

    private void parseColumnDefinitions() {
        Set<String> currentTableCols = new HashSet<>();

        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null) break;

            // Verificar si llegamos al cierre de paréntesis
            if ("SEPARADOR".equals(token.getType()) && ")".equals(token.getValue())) {
                break;
            }

            // Parsear definición de columna
            if ("IDENTIFICADOR".equals(token.getType())) {
                String columnName = token.getValue();
                int line = token.getLine();
                int column = token.getColumn();

                advance(); // consumir nombre de columna

                // Esperar tipo de dato
                Token dataTypeToken = getCurrentToken();
                if (isDataType(dataTypeToken)) {
                    String dataType = dataTypeToken.getValue();
                    advance(); // consumir tipo de dato

                    // Agregar columna a tabla de símbolos
                    addColumnToSymbolTable(columnName, dataType, currentTableName, line, column);
                    currentTableCols.add(columnName);

                    // Parsear modificadores opcionales (PRIMARY KEY, NOT NULL, etc.)
                    parseColumnModifiers();

                } else {
                    addError("Se esperaba tipo de dato después de nombre de columna: " + columnName, dataTypeToken);
                }
            }

            // Verificar si hay más columnas (coma)
            if (checkAndConsumeTokenType("SEPARADOR", ",")) {
                continue; // Hay más columnas
            } else {
                break; // No hay más columnas
            }
        }

        // Guardar columnas de esta tabla
        if (currentTableName != null) {
            tableColumns.put(currentTableName, currentTableCols);
        }
    }

    private void parseColumnModifiers() {
        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null) break;

            if ("PALABRA_CLAVE".equals(token.getType())) {
                String keyword = token.getValue().toUpperCase();

                switch (keyword) {
                    case "PRIMARY":
                        advance();
                        if (expectToken("KEY")) {
                            // PRIMARY KEY procesado
                        }
                        break;
                    case "FOREIGN":
                        advance();
                        if (expectToken("KEY")) {
                            parseReferencesClause();
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
                        parseDefaultValue();
                        break;
                    default:
                        return; // No es un modificador de columna
                }
            } else {
                break; // No es palabra clave, salir
            }
        }
    }

    private void parseReferencesClause() {
        if (expectToken("REFERENCES")) {
            Token refTableToken = getCurrentToken();
            if (expectTokenType("IDENTIFICADOR")) {
                String refTableName = refTableToken.getValue();

                // Verificar que la tabla referenciada existe
                if (!declaredTables.contains(refTableName)) {
                    addError("Tabla referenciada no existe: " + refTableName, refTableToken);
                }

                if (checkAndConsumeTokenType("SEPARADOR", "(")) {
                    if (expectTokenType("IDENTIFICADOR")) {
                        // Aquí podrías verificar que la columna existe en la tabla referenciada
                        expectTokenType("SEPARADOR", ")");
                    }
                }
            }
        }
    }

    private void parseDefaultValue() {
        Token defaultValue = getCurrentToken();
        if (defaultValue != null) {
            String type = defaultValue.getType();
            if ("NUMERO_ENTERO".equals(type) || "NUMERO_DECIMAL".equals(type) ||
                    "CADENA_SIMPLE".equals(type) || "CADENA_DOBLE".equals(type) ||
                    "VALOR_BOOLEANO".equals(type)) {
                advance();
            } else {
                addError("Valor por defecto inválido", defaultValue);
            }
        }
    }

    private void parseSelectStatement() {
        expectToken("SELECT");
        clausulaStack.push("SELECT");

        // Parsear lista de columnas/expresiones
        parseSelectList();

        // FROM es obligatorio
        if (!expectToken("FROM")) {
            addError("Se esperaba cláusula FROM después de SELECT", getCurrentToken());
            return;
        }

        // Parsear tablas en FROM
        parseFromClause();

        // Parsear cláusulas opcionales
        parseOptionalClauses();

        if (!clausulaStack.isEmpty()) {
            clausulaStack.pop();
        }
    }

    private void parseSelectList() {
        do {
            parseSelectExpression();
        } while (checkAndConsumeTokenType("SEPARADOR", ","));
    }

    private void parseSelectExpression() {
        Token token = getCurrentToken();
        if (token == null) return;

        if ("OPERADOR_ARITMETICO".equals(token.getType()) && "*".equals(token.getValue())) {
            advance(); // SELECT *
        } else if ("IDENTIFICADOR".equals(token.getType())) {
            String identifier = token.getValue();
            advance();

            // Verificar si tiene alias
            parseAlias(identifier, SymbolType.COLUMN);

            // Verificar que la columna existe (si conocemos las tablas)
            validateColumnReference(identifier, token);

        } else {
            // Podría ser una expresión más compleja
            parseExpression();
        }
    }

    private void parseFromClause() {
        do {
            parseTableReference();
        } while (checkAndConsumeTokenType("SEPARADOR", ","));
    }

    private void parseTableReference() {
        Token tableToken = getCurrentToken();
        if (!expectTokenType("IDENTIFICADOR")) {
            addError("Se esperaba nombre de tabla en FROM", tableToken);
            return;
        }

        String tableName = tableToken.getValue();

        // Verificar que la tabla existe
        if (!declaredTables.contains(tableName)) {
            addError("Tabla no definida: " + tableName, tableToken);
        }

        // Verificar si hay alias
        parseAlias(tableName, SymbolType.TABLE);
    }

    private void parseAlias(String originalName, SymbolType symbolType) {
        Token nextToken = getCurrentToken();

        // Verificar si hay palabra clave AS
        boolean hasAs = false;
        if (nextToken != null && "PALABRA_CLAVE".equals(nextToken.getType()) &&
                "AS".equalsIgnoreCase(nextToken.getValue())) {
            hasAs = true;
            advance();
            nextToken = getCurrentToken();
        }

        // Verificar si hay un identificador (alias)
        if (nextToken != null && "IDENTIFICADOR".equals(nextToken.getType()) &&
                !isKeyword(nextToken.getValue())) {

            String alias = nextToken.getValue();
            advance();

            // Agregar alias a tabla de símbolos
            addAliasToSymbolTable(alias, originalName, symbolType, nextToken.getLine(), nextToken.getColumn());
            aliases.put(alias, originalName);
        } else if (hasAs) {
            addError("Se esperaba alias después de AS", nextToken);
        }
    }

    private void parseOptionalClauses() {
        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null || !"PALABRA_CLAVE".equals(token.getType())) break;

            String keyword = token.getValue().toUpperCase();

            switch (keyword) {
                case "WHERE":
                    parseWhereClause();
                    break;
                case "GROUP":
                    parseGroupByClause();
                    break;
                case "HAVING":
                    parseHavingClause();
                    break;
                case "ORDER":
                    parseOrderByClause();
                    break;
                default:
                    if (isStatementStart(keyword)) {
                        return; // Nueva declaración
                    }
                    advance();
            }
        }
    }

    private void parseWhereClause() {
        expectToken("WHERE");
        parseCondition();
    }

    private void parseGroupByClause() {
        expectToken("GROUP");
        expectToken("BY");

        do {
            parseExpression();
        } while (checkAndConsumeTokenType("SEPARADOR", ","));
    }

    private void parseHavingClause() {
        expectToken("HAVING");
        parseCondition();
    }

    private void parseOrderByClause() {
        expectToken("ORDER");
        expectToken("BY");

        do {
            parseExpression();

            // ASC o DESC opcional
            Token orderToken = getCurrentToken();
            if (orderToken != null && "PALABRA_CLAVE".equals(orderToken.getType())) {
                String order = orderToken.getValue().toUpperCase();
                if ("ASC".equals(order) || "DESC".equals(order)) {
                    advance();
                }
            }
        } while (checkAndConsumeTokenType("SEPARADOR", ","));
    }

    private void parseCondition() {
        int parenthesesLevel = 0;

        while (currentPosition < tokens.size()) {
            Token token = getCurrentToken();
            if (token == null) break;

            if ("SEPARADOR".equals(token.getType())) {
                if ("(".equals(token.getValue())) {
                    parenthesesLevel++;
                } else if (")".equals(token.getValue())) {
                    parenthesesLevel--;
                    if (parenthesesLevel < 0) {
                        addError("Paréntesis desbalanceados en condición", token);
                    }
                }
            }

            // Si estamos en nivel 0 y encontramos nueva cláusula, salir
            if (parenthesesLevel == 0 && "PALABRA_CLAVE".equals(token.getType())) {
                String keyword = token.getValue().toUpperCase();
                if (isClauseStart(keyword) || isStatementStart(keyword)) {
                    return;
                }
            }

            advance();
        }
    }

    private void parseExpression() {
        Token token = getCurrentToken();
        if (token == null) return;

        String type = token.getType();

        if ("IDENTIFICADOR".equals(type)) {
            validateColumnReference(token.getValue(), token);
            advance();
        } else if ("NUMERO_ENTERO".equals(type) || "NUMERO_DECIMAL".equals(type) ||
                "CADENA_SIMPLE".equals(type) || "CADENA_DOBLE".equals(type) ||
                "VALOR_BOOLEANO".equals(type)) {
            advance();
        } else {
            addError("Expresión inválida", token);
            advance();
        }
    }

    // Métodos para INSERT, UPDATE, DELETE (implementación básica)
    private void parseInsertStatement() {
        expectToken("INSERT");
        expectToken("INTO");

        Token tableToken = getCurrentToken();
        if (expectTokenType("IDENTIFICADOR")) {
            String tableName = tableToken.getValue();
            validateTableExists(tableName, tableToken);
        }

        // Parsear columnas opcionales
        if (checkAndConsumeTokenType("SEPARADOR", "(")) {
            parseColumnList();
            expectTokenType("SEPARADOR", ")");
        }

        expectToken("VALUES");
        parseValuesList();
    }

    private void parseUpdateStatement() {
        expectToken("UPDATE");

        Token tableToken = getCurrentToken();
        if (expectTokenType("IDENTIFICADOR")) {
            String tableName = tableToken.getValue();
            validateTableExists(tableName, tableToken);
        }

        expectToken("SET");
        parseAssignmentList();

        // WHERE opcional
        if (checkToken("WHERE")) {
            parseWhereClause();
        }
    }

    private void parseDeleteStatement() {
        expectToken("DELETE");
        expectToken("FROM");

        Token tableToken = getCurrentToken();
        if (expectTokenType("IDENTIFICADOR")) {
            String tableName = tableToken.getValue();
            validateTableExists(tableName, tableToken);
        }

        // WHERE opcional
        if (checkToken("WHERE")) {
            parseWhereClause();
        }
    }

    private void parseDropStatement() {
        expectToken("DROP");

        Token objectTypeToken = getCurrentToken();
        if (objectTypeToken != null && "PALABRA_CLAVE".equals(objectTypeToken.getType())) {
            String objectType = objectTypeToken.getValue().toUpperCase();
            advance();

            if ("TABLE".equals(objectType)) {
                Token tableToken = getCurrentToken();
                if (expectTokenType("IDENTIFICADOR")) {
                    String tableName = tableToken.getValue();
                    validateTableExists(tableName, tableToken);

                    // Remover tabla de símbolos declarados
                    declaredTables.remove(tableName);
                    tableColumns.remove(tableName);
                }
            }
        }
    }

    private void parseAlterStatement() {
        expectToken("ALTER");
        expectToken("TABLE");

        Token tableToken = getCurrentToken();
        if (expectTokenType("IDENTIFICADOR")) {
            String tableName = tableToken.getValue();
            validateTableExists(tableName, tableToken);
            currentTableName = tableName;

            Token actionToken = getCurrentToken();
            if (actionToken != null && "PALABRA_CLAVE".equals(actionToken.getType())) {
                String action = actionToken.getValue().toUpperCase();
                advance();

                switch (action) {
                    case "ADD":
                        parseAddColumn();
                        break;
                    case "DROP":
                        parseDropColumn();
                        break;
                    case "MODIFY":
                    case "ALTER":
                        parseModifyColumn();
                        break;
                }
            }

            currentTableName = null;
        }
    }

    // Métodos auxiliares para validación y tabla de símbolos
    private void addTableToSymbolTable(String tableName, int line, int column) {
        Symbol tableSymbol = new Symbol(tableName, SymbolType.TABLE, currentScope, line, column);
        tableSymbol.setDataType("TABLE");
        symbolTable.put(tableName, tableSymbol);
        declaredTables.add(tableName);
    }

    private void addColumnToSymbolTable(String columnName, String dataType, String tableName, int line, int column) {
        String fullColumnName = tableName != null ? tableName + "." + columnName : columnName;
        Symbol columnSymbol = new Symbol(fullColumnName, SymbolType.COLUMN, currentScope, line, column);
        columnSymbol.setDataType(dataType);
        symbolTable.put(fullColumnName, columnSymbol);
    }

    private void addAliasToSymbolTable(String alias, String originalName, SymbolType symbolType, int line, int column) {
        Symbol aliasSymbol = new Symbol(alias, symbolType, currentScope, line, column);
        aliasSymbol.setDataType("ALIAS");
        symbolTable.put(alias, aliasSymbol);
    }

    private void validateTableExists(String tableName, Token token) {
        if (!declaredTables.contains(tableName)) {
            addError("Tabla no definida: " + tableName, token);
        }
    }

    private void validateColumnReference(String columnName, Token token) {
        // Verificar si es un alias conocido
        if (aliases.containsKey(columnName)) {
            return;
        }

        // Verificar si existe en alguna tabla conocida
        boolean columnExists = false;
        for (Set<String> columns : tableColumns.values()) {
            if (columns.contains(columnName)) {
                columnExists = true;
                break;
            }
        }

        if (!columnExists && !tableColumns.isEmpty()) {
            addError("Columna no definida: " + columnName, token);
        }
    }

    // Métodos utilitarios
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
        addError("Se esperaba '" + expectedValue + "'", token);
        return false;
    }

    private boolean expectTokenType(String expectedType) {
        Token token = getCurrentToken();
        if (token != null && expectedType.equals(token.getType())) {
            advance();
            return true;
        }
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

    private boolean checkAndConsumeTokenType(String type, String value) {
        Token token = getCurrentToken();
        if (token != null && type.equals(token.getType()) && value.equals(token.getValue())) {
            advance();
            return true;
        }
        return false;
    }

    private void addError(String message, Token token) {
        if (token != null) {
            errors.add(new AnalysisError(message, AnalysisError.ErrorType.SYNTACTIC,
                    token.getLine(), token.getColumn()));
        } else {
            errors.add(new AnalysisError(message, AnalysisError.ErrorType.SYNTACTIC));
        }
    }

    private void validateFinalState() {
        if (!clausulaStack.isEmpty()) {
            addError("Sentencia SQL incompleta", null);
        }
    }

    // Métodos auxiliares para parsing específico
    private void parseColumnList() {
        do {
            if (!expectTokenType("IDENTIFICADOR")) {
                addError("Se esperaba nombre de columna", getCurrentToken());
                return;
            }
        } while (checkAndConsumeTokenType("SEPARADOR", ","));
    }

    private void parseValuesList() {
        if (!expectTokenType("SEPARADOR", "(")) {
            addError("Se esperaba '(' después de VALUES", getCurrentToken());
            return;
        }

        do {
            parseExpression();
        } while (checkAndConsumeTokenType("SEPARADOR", ","));

        expectTokenType("SEPARADOR", ")");
    }

    private void parseAssignmentList() {
        do {
            if (!expectTokenType("IDENTIFICADOR")) {
                addError("Se esperaba nombre de columna en SET", getCurrentToken());
                return;
            }

            if (!expectTokenType("OPERADOR_COMP_SIMPLE", "=")) {
                addError("Se esperaba '=' después de nombre de columna", getCurrentToken());
                return;
            }

            parseExpression();
        } while (checkAndConsumeTokenType("SEPARADOR", ","));
    }

    private void parseAddColumn() {
        if (expectTokenType("IDENTIFICADOR")) {
            Token columnToken = tokens.get(currentPosition - 1);

            if (isDataType(getCurrentToken())) {
                Token dataTypeToken = getCurrentToken();
                advance();

                // Agregar nueva columna a la tabla
                if (currentTableName != null) {
                    addColumnToSymbolTable(columnToken.getValue(), dataTypeToken.getValue(),
                            currentTableName, columnToken.getLine(), columnToken.getColumn());

                    Set<String> columns = tableColumns.computeIfAbsent(currentTableName, k -> new HashSet<>());
                    columns.add(columnToken.getValue());
                }

                parseColumnModifiers();
            } else {
                addError("Se esperaba tipo de dato después de nombre de columna", getCurrentToken());
            }
        }
    }

    private void parseDropColumn() {
        if (expectTokenType("IDENTIFICADOR")) {
            Token columnToken = tokens.get(currentPosition - 1);
            String columnName = columnToken.getValue();

            // Verificar que la columna existe
            if (currentTableName != null) {
                Set<String> columns = tableColumns.get(currentTableName);
                if (columns == null || !columns.contains(columnName)) {
                    addError("Columna no existe en tabla: " + columnName, columnToken);
                } else {
                    // Remover columna
                    columns.remove(columnName);
                    symbolTable.remove(currentTableName + "." + columnName);
                }
            }
        }
    }

    private void parseModifyColumn() {
        if (expectTokenType("IDENTIFICADOR")) {
            Token columnToken = tokens.get(currentPosition - 1);
            String columnName = columnToken.getValue();

            // Verificar que la columna existe
            if (currentTableName != null) {
                Set<String> columns = tableColumns.get(currentTableName);
                if (columns == null || !columns.contains(columnName)) {
                    addError("Columna no existe en tabla: " + columnName, columnToken);
                }
            }

            if (isDataType(getCurrentToken())) {
                advance();
                parseColumnModifiers();
            } else {
                addError("Se esperaba tipo de dato", getCurrentToken());
            }
        }
    }

    private void parseCreateIndex() {
        expectToken("INDEX");

        if (expectTokenType("IDENTIFICADOR")) {
            Token indexToken = tokens.get(currentPosition - 1);
            String indexName = indexToken.getValue();

            // Agregar índice a tabla de símbolos
            Symbol indexSymbol = new Symbol(indexName, SymbolType.UNKNOWN, currentScope,
                    indexToken.getLine(), indexToken.getColumn());
            indexSymbol.setDataType("INDEX");
            symbolTable.put(indexName, indexSymbol);
        }

        expectToken("ON");

        if (expectTokenType("IDENTIFICADOR")) {
            Token tableToken = tokens.get(currentPosition - 1);
            validateTableExists(tableToken.getValue(), tableToken);
        }

        if (expectTokenType("SEPARADOR", "(")) {
            parseColumnList();
            expectTokenType("SEPARADOR", ")");
        }
    }

    private void parseCreateDatabase() {
        advance(); // consumir DATABASE o SCHEMA

        if (expectTokenType("IDENTIFICADOR")) {
            Token dbToken = tokens.get(currentPosition - 1);
            String dbName = dbToken.getValue();

            // Agregar base de datos a tabla de símbolos
            Symbol dbSymbol = new Symbol(dbName, SymbolType.UNKNOWN, currentScope,
                    dbToken.getLine(), dbToken.getColumn());
            dbSymbol.setDataType("DATABASE");
            symbolTable.put(dbName, dbSymbol);
        }
    }

    // Getter para tabla de símbolos
    public Map<String, Symbol> getSymbolTable() {
        return Collections.unmodifiableMap(symbolTable);
    }

    public List<Symbol> getSymbolList() {
        return new ArrayList<>(symbolTable.values());
    }
}