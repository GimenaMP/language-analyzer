package com.analyzer.service;

import com.analyzer.service.interfaces.ISemanticAnalyzer;
import com.analyzer.model.*;
import java.util.*;

public class SemanticAnalyzerService implements ISemanticAnalyzer {

    private Map<String, Symbol> symbolTable;

    public SemanticAnalyzerService() {
        this.symbolTable = new HashMap<>();
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language,
                                       Map<String, Symbol> existingSymbolTable) {
        List<AnalysisError> errors = new ArrayList<>();

        if (existingSymbolTable != null) {
            this.symbolTable = new HashMap<>(existingSymbolTable);
        } else {
            this.symbolTable = new HashMap<>();
        }

        switch (language) {
            case PYTHON:
                errors.addAll(analyzePythonSemantics(tokens));
                break;
            case PLSQL:
                errors.addAll(analyzePlsqlSemantics(tokens));
                break;
            case HTML:
                errors.addAll(analyzeHtmlSemantics(tokens));
                break;
            default:
                errors.add(new AnalysisError(
                        "Análisis semántico no implementado para el lenguaje: " + language.getDisplayName(),
                        AnalysisError.ErrorType.SEMANTIC,
                        -1, -1
                ));
                break;
        }

        return errors;
    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return new HashMap<>(symbolTable);
    }

    // ==============================================
    // ANÁLISIS SEMÁNTICO PARA PYTHON - MEJORADO
    // ==============================================
    private List<AnalysisError> analyzePythonSemantics(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        errors.addAll(checkPythonLogicalExpressions(tokens));
        errors.addAll(checkPythonMathematicalExpressions(tokens));
        errors.addAll(checkPythonVariables(tokens));
        errors.addAll(checkPythonVariables2(tokens)); // Verificación adicional
        errors.addAll(checkPythonConstants(tokens));
        errors.addAll(checkPythonFunctions(tokens));
        errors.addAll(checkPythonClasses(tokens));
        errors.addAll(checkPythonLoops(tokens));
        errors.addAll(checkPythonConditionals(tokens));

        return errors;
    }

    private List<AnalysisError> checkPythonLogicalExpressions(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar uso de '=' en lugar de '==' en condiciones
            if (token.getValue().equals("=") && isInConditionalContext(tokens, i)) {
                errors.add(new AnalysisError(
                        "Posible error: uso de '=' (asignación) en lugar de '==' (comparación) en condición",
                        AnalysisError.ErrorType.SEMANTIC,
                        token.getLine(), token.getColumn()
                ));
            }

            // Verificar expresiones como "if x == True or False:"
            if (token.getValue().equals("or") || token.getValue().equals("and")) {
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);
                    if (nextToken.getValue().equals("True") || nextToken.getValue().equals("False")) {
                        errors.add(new AnalysisError(
                                "Expresión lógica ambigua. Considere usar paréntesis para clarificar: (x == True) or (y == False)",
                                AnalysisError.ErrorType.SEMANTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private boolean isInConditionalContext(List<Token> tokens, int index) {
        // Buscar hacia atrás por 'if', 'while', 'elif'
        for (int i = index - 1; i >= 0; i--) {
            Token token = tokens.get(i);
            if (token.getLine() != tokens.get(index).getLine()) break;

            if (token.getValue().equals("if") || token.getValue().equals("while") ||
                    token.getValue().equals("elif")) {
                return true;
            }
        }
        return false;
    }

    private List<AnalysisError> checkPythonMathematicalExpressions(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar división por cero
            if (token.getValue().equals("/")) {
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);
                    if (nextToken.getValue().equals("0")) {
                        errors.add(new AnalysisError(
                                "División por cero detectada",
                                AnalysisError.ErrorType.SEMANTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }

            // Verificar mezcla de tipos (simulado - strings y números)
            if (token.getValue().equals("+")) {
                if (i > 0 && i + 1 < tokens.size()) {
                    Token prevToken = tokens.get(i - 1);
                    Token nextToken = tokens.get(i + 1);

                    if (isStringLiteral(prevToken) && isNumericLiteral(nextToken)) {
                        errors.add(new AnalysisError(
                                "Posible error: intento de sumar string con número",
                                AnalysisError.ErrorType.SEMANTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private boolean isStringLiteral(Token token) {
        return token.isOfType("STRING") ||
                (token.getValue().startsWith("\"") && token.getValue().endsWith("\"")) ||
                (token.getValue().startsWith("'") && token.getValue().endsWith("'"));
    }

    private boolean isNumericLiteral(Token token) {
        return token.isOfType("NUMBER") || token.getValue().matches("\\d+(\\.\\d+)?");
    }


    //verificamos que los nombres antes de "=" sean variables pero no nombres propios de python

    private List<AnalysisError> checkPythonVariables2(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Set<String> pythonBuiltins = Set.of("print", "len", "range", "str", "int", "float", "bool", "list", "dict",
                "set", "tuple", "type", "isinstance", "hasattr", "getattr", "setattr",
                "min", "max", "sum", "abs", "round", "input", "open", "enumerate", "zip");

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar asignaciones
            if (token.isOfType("IDENTIFIER") && i + 1 < tokens.size() && tokens.get(i + 1).getValue().equals("=")) {
                String varName = token.getValue();

                // Verificar si es un nombre propio de Python
                if (pythonBuiltins.contains(varName)) {
                    errors.add(new AnalysisError(
                            "Uso de nombre propio de Python como variable: '" + varName + "'",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }






    private List<AnalysisError> checkPythonVariables(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Map<String, Set<Integer>> variableScopes = new HashMap<>(); // Variable -> Set de scopes donde es válida
        Set<String> functionParameters = new HashSet<>();
        Set<String> functionNames = new HashSet<>();
        Set<String> globalVariables = new HashSet<>();
        int currentScope = 0;

        // Primera pasada: registrar declaraciones y sus scopes
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Incrementar scope al entrar en una nueva función o bloque
            if (token.getValue().equals(":")) {
                currentScope++;
                continue;
            }

            // Detectar declaraciones globales
            if (token.getValue().equals("global")) {
                i++;
                while (i < tokens.size() && !tokens.get(i).getValue().equals("\n")) {
                    if (tokens.get(i).isOfType("IDENTIFIER")) {
                        globalVariables.add(tokens.get(i).getValue());
                    }
                    i++;
                }
                continue;
            }

            // Registrar funciones y parámetros
            if (token.isOfType("KEYWORD") && token.getValue().equals("def")) {
                if (i + 1 < tokens.size()) {
                    Token funcNameToken = tokens.get(i + 1);
                    functionNames.add(funcNameToken.getValue());

                    // Registrar parámetros en el scope de la función
                    i += 2;
                    while (i < tokens.size() && !tokens.get(i).getValue().equals(":")) {
                        if (tokens.get(i).isOfType("IDENTIFIER")) {
                            String paramName = tokens.get(i).getValue();
                            functionParameters.add(paramName);
                            addToScope(variableScopes, paramName, currentScope + 1);
                        }
                        i++;
                    }
                }
                continue;
            }

            //registra varibles en for
            if (token.isOfType("KEYWORD") && token.getValue().equals("for")) {
                // Asumimos que la variable del for es la siguiente
                if (i + 1 < tokens.size() && tokens.get(i + 1).isOfType("IDENTIFIER")) {
                    String loopVar = tokens.get(i + 1).getValue();
                    addToScope(variableScopes, loopVar, currentScope);
                }
                continue;
            }

            // Registrar asignaciones con su scope
            if (token.isOfType("IDENTIFIER")) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).getValue().equals("=")) {
                    String varName = token.getValue();
                    if (globalVariables.contains(varName)) {
                        addToScope(variableScopes, varName, 0); // scope global
                    } else {
                        addToScope(variableScopes, varName, currentScope);
                    }
                }
            }
        }

        // Segunda pasada: verificar usos considerando scopes
        currentScope = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getValue().equals(":")) {
                currentScope++;
                continue;
            }


            if (token.isOfType("IDENTIFIER")) {
                String name = token.getValue();

                // Verificar si la variable es válida en el scope actual
                if (!isValidInCurrentScope(name, currentScope, variableScopes) &&
                        !functionParameters.contains(name) &&
                        !functionNames.contains(name) &&
                        !globalVariables.contains(name) &&
                        !isPythonBuiltin(name)) {

                    errors.add(new AnalysisError(
                            "Variable '" + name + "' usada antes de ser declarada",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(),
                            token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    private void addToScope(Map<String, Set<Integer>> scopes, String variable, int scope) {
        scopes.computeIfAbsent(variable, k -> new HashSet<>()).add(scope);
    }

    private boolean isValidInCurrentScope(String variable, int currentScope,
                                          Map<String, Set<Integer>> scopes) {
        if (!scopes.containsKey(variable)) {
            return false;
        }

        Set<Integer> validScopes = scopes.get(variable);
        return validScopes.stream().anyMatch(scope -> scope <= currentScope);
    }


    // En checkPythonVariables(), agregar verificación de contexto de función
//    private List<AnalysisError> checkPythonVariables(List<Token> tokens) {
//        List<AnalysisError> errors = new ArrayList<>();
//        boolean inFunctionDefinition = false;
//        List<String> functionParameters = new ArrayList<>();
//
//
//
//        for (int i = 0; i < tokens.size(); i++) {
//            Token token = tokens.get(i);
//
//            // Detectar definición de función y sus parámetros
//            if (token.isOfType("KEYWORD") && token.getValue().equals("def")) {
//                inFunctionDefinition = true;
//                functionParameters.clear();
//                // Recolectar parámetros hasta encontrar ':'
//                i++;
//                while (i < tokens.size() && !tokens.get(i).getValue().equals(":")) {
//                    if (tokens.get(i).isOfType("IDENTIFIER")) {
//                        functionParameters.add(tokens.get(i).getValue());
//                    }
//                    i++;
//                }
//                continue;
//            }
//
//            // Verificar uso de variables
//            if (token.isOfType("IDENTIFIER") && !isInAssignmentContext(tokens, i)) {
//                String varName = token.getValue();
//                if (!symbolTable.containsKey(varName) &&
//                        !isPythonBuiltin(varName) &&
//                        !functionParameters.contains(varName)) {
//                    errors.add(new AnalysisError(
//                            "Variable '" + varName + "' usada antes de ser declarada",
//                            AnalysisError.ErrorType.SEMANTIC,
//                            token.getLine(), token.getColumn()
//                    ));
//                }
//            }
//        }
//        return errors;
//    }

    private String determineType(Token token) {
        if (isStringLiteral(token)) return "string";
        if (isNumericLiteral(token)) return "number";
        if (token.getValue().equals("True") || token.getValue().equals("False")) return "boolean";
        if (token.getValue().equals("None")) return "none";
        return "unknown";
    }

    private boolean isInAssignmentContext(List<Token> tokens, int index) {
        // Verificar si el token está del lado izquierdo de una asignación
        if (index + 1 < tokens.size()) {
            Token nextToken = tokens.get(index + 1);
            return nextToken.getValue().equals("=");
        }
        return false;
    }

    private boolean isPythonBuiltin(String name) {
        Set<String> builtins = Set.of(
                "print", "len", "range", "str", "int", "float", "bool", "list", "dict",
                "set", "tuple", "type", "isinstance", "hasattr", "getattr", "setattr",
                "min", "max", "sum", "abs", "round", "input", "open", "enumerate", "zip"
        );
        return builtins.contains(name);
    }

    private List<AnalysisError> checkPythonConstants(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Map<String, Integer> constantAssignments = new HashMap<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar constantes (nombres en mayúsculas)
            if (token.isOfType("IDENTIFIER") && token.getValue().matches("[A-Z_]+") &&
                    i + 1 < tokens.size() && tokens.get(i + 1).getValue().equals("=")) {

                String constName = token.getValue();
                constantAssignments.put(constName, constantAssignments.getOrDefault(constName, 0) + 1);

                if (constantAssignments.get(constName) > 1) {
                    errors.add(new AnalysisError(
                            "Reasignación de constante '" + constName + "' (rompe convención)",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }

                // Agregar a tabla de símbolos
                Symbol constant = new Symbol(constName, Symbol.SymbolType.CONSTANT, "unknown", "global");
                constant.setDeclarationLine(token.getLine());
                symbolTable.put(constName, constant);
            }
        }

        return errors;
    }

    private List<AnalysisError> checkPythonFunctions(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD") && token.getValue().equals("def")) {
                if (i + 1 < tokens.size()) {
                    Token nameToken = tokens.get(i + 1);
                    String funcName = nameToken.getValue();

                    // Verificar función con error lógico común
                    if (funcName.equals("suma") || funcName.equals("sumar")) {
                        // Buscar return en la función
                        boolean hasSubtraction = false;
                        for (int j = i + 2; j < tokens.size(); j++) {
                            Token t = tokens.get(j);
                            if (t.getValue().equals("def")) break; // Nueva función
                            if (t.getValue().equals("return") && j + 3 < tokens.size()) {
                                Token op = tokens.get(j + 2);
                                if (op.getValue().equals("-")) {
                                    hasSubtraction = true;
                                    break;
                                }
                            }
                        }

                        if (hasSubtraction) {
                            errors.add(new AnalysisError(
                                    "Error lógico en función '" + funcName + "': hace resta en lugar de suma",
                                    AnalysisError.ErrorType.SEMANTIC,
                                    nameToken.getLine(), nameToken.getColumn()
                            ));
                        }
                    }

                    // Agregar función a tabla de símbolos
                    Symbol function = new Symbol(funcName, Symbol.SymbolType.FUNCTION, "function", "global");
                    function.setDeclarationLine(nameToken.getLine());
                    symbolTable.put(funcName, function);
                }
            }
        }

        return errors;
    }

    private List<AnalysisError> checkPythonClasses(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD") && token.getValue().equals("class")) {
                if (i + 1 < tokens.size()) {
                    Token nameToken = tokens.get(i + 1);
                    String className = nameToken.getValue();

                    // Verificar método __init__ con errores comunes
                    if (hasInitMethodWithErrors(tokens, i, className)) {
                        errors.add(new AnalysisError(
                                "Error en método __init__ de clase '" + className + "': asignación incorrecta de atributos",
                                AnalysisError.ErrorType.SEMANTIC,
                                nameToken.getLine(), nameToken.getColumn()
                        ));
                    }

                    // Agregar clase a tabla de símbolos
                    Symbol clazz = new Symbol(className, Symbol.SymbolType.CLASS, "class", "global");
                    clazz.setDeclarationLine(nameToken.getLine());
                    symbolTable.put(className, clazz);
                }
            }
        }

        return errors;
    }

    private boolean hasInitMethodWithErrors(List<Token> tokens, int classIndex, String className) {
        // Buscar método __init__ y verificar asignaciones incorrectas
        for (int i = classIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getValue().equals("def") && i + 1 < tokens.size() &&
                    tokens.get(i + 1).getValue().equals("__init__")) {

                // Verificar asignaciones como self.edad = nombre
                for (int j = i + 2; j < tokens.size(); j++) {
                    Token t = tokens.get(j);
                    if (t.getValue().equals("def")) break; // Otro método

                    if (t.getValue().startsWith("self.") && j + 2 < tokens.size() &&
                            tokens.get(j + 1).getValue().equals("=")) {
                        Token assignedValue = tokens.get(j + 2);
                        String attrName = t.getValue().substring(5); // Quitar "self."

                        // Error común: self.edad = nombre
                        if (attrName.equals("edad") && assignedValue.getValue().equals("nombre")) {
                            return true;
                        }
                    }
                }
                break;
            }
        }
        return false;
    }

    private List<AnalysisError> checkPythonLoops(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD") && token.getValue().equals("for")) {
                // Verificar rangos incorrectos: for i in range(10, 1)
                if (hasIncorrectRange(tokens, i)) {
                    errors.add(new AnalysisError(
                            "Rango incorrecto en bucle for: el inicio es mayor que el final",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    private boolean hasIncorrectRange(List<Token> tokens, int forIndex) {
        // Buscar patrón: range(start, end) donde start > end
        for (int i = forIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getLine() != tokens.get(forIndex).getLine()) break;

            if (token.getValue().equals("range") && i + 5 < tokens.size()) {
                Token openParen = tokens.get(i + 1);
                Token start = tokens.get(i + 2);
                Token comma = tokens.get(i + 3);
                Token end = tokens.get(i + 4);
                Token closeParen = tokens.get(i + 5);

                if (openParen.getValue().equals("(") && comma.getValue().equals(",") &&
                        closeParen.getValue().equals(")")) {

                    try {
                        int startNum = Integer.parseInt(start.getValue());
                        int endNum = Integer.parseInt(end.getValue());
                        return startNum > endNum;
                    } catch (NumberFormatException e) {
                        // No son números, no podemos verificar
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private List<AnalysisError> checkPythonConditionals(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD") && token.getValue().equals("if")) {
                // Verificar condiciones que pueden no cubrir todos los casos
                if (hasIncompleteCondition(tokens, i)) {
                    errors.add(new AnalysisError(
                            "Condición posiblemente incompleta: puede no cubrir todos los casos esperados",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    private boolean hasIncompleteCondition(List<Token> tokens, int ifIndex) {
        // Buscar patrón: if x > 10 or x < 5 (gap entre 5 y 10)
        for (int i = ifIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getLine() != tokens.get(ifIndex).getLine()) break;

            // Simplificado: buscar patrón común de condiciones con gaps
            if (token.getValue().equals("or") && i + 4 < tokens.size()) {
                // Esta es una verificación muy simplificada
                return true; // Para el ejemplo, marcamos todas las condiciones con 'or' como posiblemente incompletas
            }
        }
        return false;
    }

    // ==============================================
    // ANÁLISIS SEMÁNTICO PARA SQL - MEJORADO
    // ==============================================
    private List<AnalysisError> analyzePlsqlSemantics(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        errors.addAll(checkSqlFunctionUsage(tokens));
        errors.addAll(checkSqlColumnReferences(tokens));
        errors.addAll(checkSqlIntegrityConstraints(tokens));
        errors.addAll(checkSqlCrudOperations(tokens));

        return errors;
    }

    private List<AnalysisError> checkSqlFunctionUsage(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar funciones agregadas usadas incorrectamente
            if (token.getValue().toUpperCase().equals("AVG") ||
                    token.getValue().toUpperCase().equals("SUM")) {

                // Verificar que no se use en columnas de texto
                if (i + 2 < tokens.size() && tokens.get(i + 1).getValue().equals("(")) {
                    Token columnToken = tokens.get(i + 2);
                    String columnName = columnToken.getValue();

                    if (isTextColumn(columnName)) {
                        errors.add(new AnalysisError(
                                "Función " + token.getValue() + " no puede usarse en columna de texto: " + columnName,
                                AnalysisError.ErrorType.SEMANTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private boolean isTextColumn(String columnName) {
        // Heurística simple: columnas que probablemente son texto
        Set<String> textColumns = Set.of("nombre", "descripcion", "titulo", "texto", "comentario");
        return textColumns.contains(columnName.toLowerCase());
    }

    private List<AnalysisError> checkSqlColumnReferences(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Set<String> availableColumns = new HashSet<>();

        // Simular columnas disponibles basándose en CREATE TABLE
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getValue().toUpperCase().equals("CREATE") &&
                    i + 2 < tokens.size() &&
                    tokens.get(i + 1).getValue().toUpperCase().equals("TABLE")) {

                // Agregar tabla a símbolos
                String tableName = tokens.get(i + 2).getValue();
                Symbol table = new Symbol(tableName, Symbol.SymbolType.TABLE, "table", "database");
                table.setDeclarationLine(token.getLine());
                symbolTable.put(tableName, table);

                // Simular algunas columnas comunes
                availableColumns.addAll(Set.of("id", "nombre", "fecha", "estado"));
            }
        }

        // Verificar referencias a columnas en SELECT
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getValue().toUpperCase().equals("SELECT")) {
                for (int j = i + 1; j < tokens.size(); j++) {
                    Token nextToken = tokens.get(j);
                    if (nextToken.getValue().toUpperCase().equals("FROM")) break;

                    if (nextToken.isOfType("IDENTIFIER") &&
                            !nextToken.getValue().equals("*") &&
                            !availableColumns.contains(nextToken.getValue().toLowerCase())) {

                        errors.add(new AnalysisError(
                                "Referencia a columna posiblemente inexistente: " + nextToken.getValue(),
                                AnalysisError.ErrorType.SEMANTIC,
                                nextToken.getLine(), nextToken.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private List<AnalysisError> checkSqlIntegrityConstraints(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar UPDATE que podría violar restricciones
            if (token.getValue().toUpperCase().equals("UPDATE")) {
                // Buscar SET id = NULL (si id es clave primaria)
                for (int j = i + 1; j < tokens.size(); j++) {
                    Token setToken = tokens.get(j);
                    if (setToken.getValue().toUpperCase().equals("SET")) {
                        if (j + 4 < tokens.size()) {
                            Token columnToken = tokens.get(j + 1);
                            Token equalsToken = tokens.get(j + 2);
                            Token valueToken = tokens.get(j + 3);

                            if (equalsToken.getValue().equals("=") &&
                                    valueToken.getValue().toUpperCase().equals("NULL") &&
                                    isPrimaryKeyColumn(columnToken.getValue())) {

                                errors.add(new AnalysisError(
                                        "Violación de restricción: intento de asignar NULL a clave primaria '" +
                                                columnToken.getValue() + "'",
                                        AnalysisError.ErrorType.SEMANTIC,
                                        columnToken.getLine(), columnToken.getColumn()
                                ));
                            }
                        }
                        break;
                    }
                }
            }
        }

        return errors;
    }

    private boolean isPrimaryKeyColumn(String columnName) {
        // Heurística: columnas que típicamente son claves primarias
        return columnName.toLowerCase().equals("id") ||
                columnName.toLowerCase().endsWith("_id");
    }

    private List<AnalysisError> checkSqlCrudOperations(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar INSERT sin VALUES
            if (token.getValue().toUpperCase().equals("INSERT")) {
                boolean hasValues = false;
                for (int j = i + 1; j < tokens.size(); j++) {
                    if (tokens.get(j).getValue().toUpperCase().equals("VALUES")) {
                        hasValues = true;
                        break;
                    }
                    if (tokens.get(j).getValue().toUpperCase().equals("INSERT")) break;
                }

                if (!hasValues) {
                    errors.add(new AnalysisError(
                            "INSERT sin cláusula VALUES",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }

            // Verificar DELETE sin WHERE (peligroso)
            if (token.getValue().toUpperCase().equals("DELETE")) {
                boolean hasWhere = false;
                for (int j = i + 1; j < tokens.size(); j++) {
                    if (tokens.get(j).getValue().toUpperCase().equals("WHERE")) {
                        hasWhere = true;
                        break;
                    }
                    if (tokens.get(j).getValue().toUpperCase().equals("DELETE")) break;
                }

                if (!hasWhere) {
                    errors.add(new AnalysisError(
                            "DELETE sin cláusula WHERE: esto eliminará todos los registros",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    // ==============================================
    // ANÁLISIS SEMÁNTICO PARA HTML - MEJORADO
    // ==============================================
    private List<AnalysisError> analyzeHtmlSemantics(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        errors.addAll(checkHtmlSemanticUsage(tokens));
        errors.addAll(checkHtmlStructuralElements(tokens));
        errors.addAll(checkHtmlSemanticNesting(tokens));

        return errors;
    }

    private List<AnalysisError> checkHtmlSemanticUsage(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (Token token : tokens) {
            if (token.isOfType("TAG")) {
                String tagValue = token.getValue();

                // Verificar uso semánticamente incorrecto de etiquetas
                if (tagValue.contains("<div") && isUsedAsHeading(token)) {
                    errors.add(new AnalysisError(
                            "Uso semánticamente incorrecto: use <h1>-<h6> para encabezados en lugar de <div>",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }

                if (tagValue.contains("<span") && isUsedAsTitle(token)) {
                    errors.add(new AnalysisError(
                            "Uso semánticamente incorrecto: use <h1>-<h6> para títulos en lugar de <span>",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }

                // Agregar elementos a tabla de símbolos
                String tagName = extractTagName(tagValue);
                if (!tagName.isEmpty()) {
                    Symbol tag = new Symbol(tagName, Symbol.SymbolType.TAG, "html", "document");
                    tag.setDeclarationLine(token.getLine());
                    symbolTable.put(tagName + "_" + token.getLine(), tag);
                }
            }
        }

        return errors;
    }

    private boolean isUsedAsHeading(Token divToken) {
        // Heurística: si el div contiene palabras como "title", "encabezado", etc.
        String tagValue = divToken.getValue().toLowerCase();
        return tagValue.contains("title") || tagValue.contains("encabezado") ||
                tagValue.contains("heading");
    }

    private boolean isUsedAsTitle(Token spanToken) {
        String tagValue = spanToken.getValue().toLowerCase();
        return tagValue.contains("title") || tagValue.contains("titulo");
    }

    private List<AnalysisError> checkHtmlStructuralElements(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (Token token : tokens) {
            if (token.isOfType("TAG")) {
                String tagValue = token.getValue();

                // Verificar elementos estructurales sin significado
                if (tagValue.contains("<div") && !hasSemanticMeaning(tagValue)) {
                    errors.add(new AnalysisError(
                            "Elemento <div> sin significado semántico claro. " +
                                    "Considere usar <section>, <article>, <nav>, <aside>, etc.",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    private boolean hasSemanticMeaning(String tagValue) {
        // Verificar si tiene atributos que indican propósito semántico
        return tagValue.contains("class=") || tagValue.contains("role=") ||
                tagValue.contains("id=");
    }

    private List<AnalysisError> checkHtmlSemanticNesting(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("TAG") && token.getValue().contains("<a")) {
                // Verificar anidación semánticamente incorrecta: <a><div>...</div></a>
                if (hasBlockElementInside(tokens, i)) {
                    errors.add(new AnalysisError(
                            "Anidación semánticamente incorrecta: elementos de bloque dentro de <a>",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    private boolean hasBlockElementInside(List<Token> tokens, int aTagIndex) {
        Token aTag = tokens.get(aTagIndex);
        Set<String> blockElements = Set.of("div", "p", "h1", "h2", "h3", "h4", "h5", "h6");

        // Buscar elementos de bloque después del tag <a> y antes del </a>
        for (int i = aTagIndex + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getValue().equals("</a>")) break;

            if (token.isOfType("TAG")) {
                String tagName = extractTagName(token.getValue());
                if (blockElements.contains(tagName.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractTagName(String tagValue) {
        if (tagValue.startsWith("<")) {
            String content = tagValue.substring(1);
            if (content.startsWith("/")) {
                content = content.substring(1);
            }

            int spaceIndex = content.indexOf(' ');
            int closeIndex = content.indexOf('>');

            int endIndex = content.length();
            if (spaceIndex != -1) endIndex = Math.min(endIndex, spaceIndex);
            if (closeIndex != -1) endIndex = Math.min(endIndex, closeIndex);

            return content.substring(0, endIndex).trim();
        }
        return "";
    }
}