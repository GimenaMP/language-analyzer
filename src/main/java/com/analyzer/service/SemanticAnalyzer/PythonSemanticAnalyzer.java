package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ISemanticAnalyzer;
import com.analyzer.model.Token; // Asegúrate de que esta importación sea válida

import java.util.*;
import java.util.stream.Collectors;

public class PythonSemanticAnalyzer implements ISemanticAnalyzer {

    private Map<String, Symbol> symbolTable;
    private List<AnalysisError> semanticErrors;
    private Set<String> usedVariables;
    private Set<String> initializedVariables;
    private Map<String, Integer> functionParameterCounts;
    private String currentScope;
    private int currentLine;

    public PythonSemanticAnalyzer() {
        this.semanticErrors = new ArrayList<>();
        this.usedVariables = new HashSet<>();
        this.initializedVariables = new HashSet<>();
        this.functionParameterCounts = new HashMap<>();
        this.currentScope = "global";
    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return symbolTable != null ? new HashMap<>(symbolTable) : new HashMap<>();
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        this.symbolTable = symbolTable != null ? new HashMap<>(symbolTable) : new HashMap<>();
        this.semanticErrors = new ArrayList<>();
        this.usedVariables = new HashSet<>();
        this.initializedVariables = new HashSet<>();
        this.functionParameterCounts = new HashMap<>();
        this.currentScope = "global";

        System.out.println("DEBUG - Iniciando análisis semántico de Python...");
        System.out.println("DEBUG - Tokens a analizar: " + tokens.size());
        System.out.println("DEBUG - Símbolos en tabla: " + symbolTable.size());

        // 1. Preparar información de símbolos
        prepareSymbolInformation();

        // 2. Analizar uso de variables y funciones
        analyzeTokenSemantics(tokens);

        // 3. Detectar variables no utilizadas
        detectUnusedVariables();

        // 4. Detectar variables no inicializadas
        detectUninitializedVariables();

        System.out.println("DEBUG - Análisis semántico completado. Errores encontrados: " + semanticErrors.size());
        return new ArrayList<>(semanticErrors);
    }

    /**
     * Prepara información sobre funciones y parámetros para validaciones posteriores
     */
    private void prepareSymbolInformation() {
        for (Symbol symbol : symbolTable.values()) {
            if (symbol.getSymbolType() == Symbol.SymbolType.FUNCTION) {
                // Contar parámetros de la función
                String functionName = symbol.getName();
                long parameterCount = symbolTable.values().stream()
                        .filter(s -> s.getSymbolType() == Symbol.SymbolType.PARAMETER)
                        .filter(s -> s.getScope().equals(functionName))
                        .count();

                functionParameterCounts.put(functionName, (int) parameterCount);
                System.out.println("DEBUG - Función '" + functionName + "' tiene " + parameterCount + " parámetros");
            }

            // Marcar variables que tienen valor inicial como inicializadas
            if (symbol.getSymbolType() == Symbol.SymbolType.VARIABLE && symbol.isInitialized()) {
                initializedVariables.add(symbol.getName());
            }
        }
    }

    /**
     * Analiza la semántica de los tokens de Python
     */
    private void analyzeTokenSemantics(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i); // Asegúrate de que 'Token' esté correctamente importado
            currentLine = token.getLine();

            // Actualizar scope actual si estamos en una función
            updateCurrentScope(token, tokens, i);

            switch (token.getType().toUpperCase()) {
                case "IDENTIFIER":
                case "VARIABLE":
                    analyzeIdentifierUsage(token, tokens, i);
                    break;

                case "FUNCTION_CALL":
                    analyzeFunctionCall(token, tokens, i);
                    break;

                case "ASSIGNMENT":
                case "OPERATOR":
                    if ("=".equals(token.getValue())) {
                        analyzeAssignment(tokens, i);
                    }
                    break;

                case "KEYWORD":
                    analyzeKeywordUsage(token, tokens, i);
                    break;

                default:
                    // Otros tipos de token no requieren análisis semántico especial
                    break;
            }
        }
    }

    /**
     * Actualiza el scope actual basado en el contexto
     */
    private void updateCurrentScope(Token token, List<Token> tokens, int index) {
        if ("def".equals(token.getValue()) && index + 1 < tokens.size()) {
            Token nextToken = tokens.get(index + 1);
            if ("IDENTIFIER".equals(nextToken.getType()) || "FUNCTION".equals(nextToken.getType())) {
                currentScope = nextToken.getValue();
            }
        }

        // Detectar fin de función (return o desindentación)
        if ("return".equals(token.getValue()) ||
                (token.getType().equals("NEWLINE") && isAtGlobalLevel(tokens, index))) {
            currentScope = "global";
        }
    }

    /**
     * Verifica si estamos a nivel global basado en la indentación
     */
    private boolean isAtGlobalLevel(List<Token> tokens, int index) {
        // Simplificado: asumir que estamos en global después de newlines sin indentación
        return true; // Esta lógica podría ser más sofisticada según tu implementación léxica
    }

    /**
     * Analiza el uso de identificadores (variables)
     */
    private void analyzeIdentifierUsage(Token token, List<Token> tokens, int index) {
        String varName = token.getValue();
        usedVariables.add(varName);

        // Verificar si es una asignación (variable = valor)
        boolean isAssignment = false;
        if (index + 1 < tokens.size()) {
            Token nextToken = tokens.get(index + 1);
            if ("=".equals(nextToken.getValue()) || "ASSIGNMENT".equals(nextToken.getType())) {
                isAssignment = true;
                initializedVariables.add(varName);
            }
        }

        // Si no es asignación, verificar que la variable esté declarada
        if (!isAssignment) {
            if (!isVariableDeclared(varName)) {
                addSemanticError(
                        "Variable '" + varName + "' no está declarada",
                        AnalysisError.ErrorType.SEMANTIC,
                        token.getLine(),
                        token.getColumn()
                );
            } else if (!isVariableInitialized(varName)) {
                addSemanticError(
                        "Variable '" + varName + "' se usa antes de ser inicializada",
                        AnalysisError.ErrorType.SEMANTIC,
                        token.getLine(),
                        token.getColumn()
                );
            }
        }
    }

    /**
     * Analiza llamadas a funciones
     */
    private void analyzeFunctionCall(Token token, List<Token> tokens, int index) {
        String functionName = token.getValue();

        // Verificar que la función esté declarada
        if (!isFunctionDeclared(functionName)) {
            addSemanticError(
                    "Función '" + functionName + "' no está declarada",
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            );
            return;
        }

        // Contar argumentos en la llamada
        int argumentCount = countFunctionCallArguments(tokens, index);
        int expectedParameters = functionParameterCounts.getOrDefault(functionName, 0);

        if (argumentCount != expectedParameters) {
            addSemanticError(
                    "Función '" + functionName + "' espera " + expectedParameters +
                            " argumentos, pero se llamó con " + argumentCount,
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            );
        }
    }

    /**
     * Cuenta los argumentos en una llamada a función
     */
    private int countFunctionCallArguments(List<Token> tokens, int functionIndex) {
        int argumentCount = 0;
        boolean insideParentheses = false;
        int parenthesesDepth = 0;

        for (int i = functionIndex + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            String value = token.getValue();

            if ("(".equals(value)) {
                insideParentheses = true;
                parenthesesDepth++;
            } else if (")".equals(value)) {
                parenthesesDepth--;
                if (parenthesesDepth == 0) {
                    break;
                }
            } else if (",".equals(value) && parenthesesDepth == 1) {
                argumentCount++;
            } else if (insideParentheses && parenthesesDepth == 1 &&
                    ("IDENTIFIER".equals(token.getType()) || "NUMBER".equals(token.getType()) ||
                            "STRING".equals(token.getType()))) {
                if (argumentCount == 0) argumentCount = 1; // Primer argumento
            }
        }

        return argumentCount;
    }

    /**
     * Analiza asignaciones
     */
    private void analyzeAssignment(List<Token> tokens, int operatorIndex) {
        if (operatorIndex > 0) {
            Token leftSide = tokens.get(operatorIndex - 1);
            if ("IDENTIFIER".equals(leftSide.getType()) || "VARIABLE".equals(leftSide.getType())) {
                String varName = leftSide.getValue();

                // Marcar como inicializada
                initializedVariables.add(varName);

                // Verificar que la variable del lado derecho esté declarada
                if (operatorIndex + 1 < tokens.size()) {
                    Token rightSide = tokens.get(operatorIndex + 1);
                    if (("IDENTIFIER".equals(rightSide.getType()) || "VARIABLE".equals(rightSide.getType()))
                            && !rightSide.getValue().equals(varName)) {

                        String rightVarName = rightSide.getValue();
                        if (!isVariableDeclared(rightVarName) && !isBuiltinFunction(rightVarName)) {
                            addSemanticError(
                                    "Variable '" + rightVarName + "' no está declarada",
                                    AnalysisError.ErrorType.SEMANTIC,
                                    rightSide.getLine(),
                                    rightSide.getColumn()
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * Analiza el uso de palabras clave
     */
    private void analyzeKeywordUsage(Token token, List<Token> tokens, int index) {
        String keyword = token.getValue();

        switch (keyword) {
            case "return":
                analyzeReturnStatement(tokens, index);
                break;
            case "if":
            case "elif":
            case "while":
                analyzeConditionalExpression(tokens, index);
                break;
            case "for":
                analyzeForLoop(tokens, index);
                break;
        }
    }

    /**
     * Analiza declaraciones return
     */
    private void analyzeReturnStatement(List<Token> tokens, int index) {
        if (!currentScope.equals("global")) {
            // Return está dentro de una función, verificar expresión si existe
            if (index + 1 < tokens.size()) {
                Token nextToken = tokens.get(index + 1);
                if ("IDENTIFIER".equals(nextToken.getType()) || "VARIABLE".equals(nextToken.getType())) {
                    String varName = nextToken.getValue();
                    if (!isVariableDeclared(varName) && !isBuiltinFunction(varName)) {
                        addSemanticError(
                                "Variable '" + varName + "' en return no está declarada",
                                AnalysisError.ErrorType.SEMANTIC,
                                nextToken.getLine(),
                                nextToken.getColumn()
                        );
                    }
                }
            }
        } else {
            addSemanticError(
                    "Return fuera de función",
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
                    
            );
        }
    }

    /**
     * Analiza expresiones condicionales
     */
    private void analyzeConditionalExpression(List<Token> tokens, int index) {
        // Verificar variables en la condición
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (":".equals(token.getValue())) break; // Fin de la condición

            if ("IDENTIFIER".equals(token.getType()) || "VARIABLE".equals(token.getType())) {
                String varName = token.getValue();
                if (!isVariableDeclared(varName) && !isBuiltinFunction(varName)) {
                    addSemanticError(
                            "Variable '" + varName + "' en condición no está declarada",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(),
                            token.getColumn()
                    );
                }
            }
        }
    }

    /**
     * Analiza bucles for
     */
    private void analyzeForLoop(List<Token> tokens, int index) {
        // Verificar que la variable del iterable esté declarada
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("in".equals(token.getValue()) && i + 1 < tokens.size()) {
                Token iterableToken = tokens.get(i + 1);
                if ("IDENTIFIER".equals(iterableToken.getType())) {
                    String iterableName = iterableToken.getValue();
                    if (!isVariableDeclared(iterableName) && !isBuiltinFunction(iterableName)) {
                        addSemanticError(
                                "Iterable '" + iterableName + "' no está declarado",
                                AnalysisError.ErrorType.SEMANTIC,
                                iterableToken.getLine(),
                                iterableToken.getColumn()
                        );
                    }
                }
                break;
            }
        }
    }

    /**
     * Detecta variables declaradas pero no utilizadas
     */
    private void detectUnusedVariables() {
        for (Symbol symbol : symbolTable.values()) {
            if (symbol.getSymbolType() == Symbol.SymbolType.VARIABLE) {
                String varName = symbol.getName();
                if (!usedVariables.contains(varName)) {
                    addSemanticError(
                            "Variable '" + varName + "' declarada pero no utilizada",
                            AnalysisError.ErrorType.SEMANTIC,
                            symbol.getDeclarationLine(),
                            symbol.getDeclarationColumn()
                    );
                }
            }
        }
    }

    /**
     * Detecta variables utilizadas pero no inicializadas
     */
    private void detectUninitializedVariables() {
        for (String usedVar : usedVariables) {
            if (isVariableDeclared(usedVar) && !initializedVariables.contains(usedVar)) {
                Symbol symbol = symbolTable.get(usedVar);
                if (symbol != null && !symbol.isInitialized()) {
                    addSemanticError(
                            "Variable '" + usedVar + "' puede ser utilizada sin inicializar",
                            AnalysisError.ErrorType.SEMANTIC,
                            symbol.getDeclarationLine(),
                            symbol.getDeclarationColumn()
                    );
                }
            }
        }
    }

    /**
     * Verifica si una variable está declarada
     */
    private boolean isVariableDeclared(String varName) {
        return symbolTable.containsKey(varName) &&
                (symbolTable.get(varName).getSymbolType() == Symbol.SymbolType.VARIABLE ||
                        symbolTable.get(varName).getSymbolType() == Symbol.SymbolType.PARAMETER);
    }

    /**
     * Verifica si una función está declarada
     */
    private boolean isFunctionDeclared(String funcName) {
        return symbolTable.containsKey(funcName) &&
                symbolTable.get(funcName).getSymbolType() == Symbol.SymbolType.FUNCTION;
    }

    /**
     * Verifica si una variable está inicializada
     */
    private boolean isVariableInitialized(String varName) {
        return initializedVariables.contains(varName) ||
                (symbolTable.containsKey(varName) && symbolTable.get(varName).isInitialized());
    }

    /**
     * Verifica si es una función built-in de Python
     */
    private boolean isBuiltinFunction(String name) {
        Set<String> builtins = Set.of(
                "print", "input", "len", "range", "str", "int", "float", "bool",
                "list", "dict", "set", "tuple", "type", "isinstance", "hasattr",
                "getattr", "setattr", "open", "read", "write", "close", "enumerate",
                "zip", "map", "filter", "sorted", "reversed", "sum", "min", "max"
        );
        return builtins.contains(name);
    }

    /**
     * Agrega un error semántico a la lista
     */
    private void addSemanticError(String message, AnalysisError.ErrorType type, int line, int column) {
        AnalysisError error = new AnalysisError(message, type, line, column);
        semanticErrors.add(error);
        System.out.println("DEBUG - Error semántico: " + message + " en línea " + line);
    }
}
