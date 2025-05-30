package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Symbol;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class PythonSyntacticAnalyzer implements ISyntacticAnalyzer {

    // Tabla de símbolos como Map para fácil acceso
    private Map<String, Symbol> symbolTable = new HashMap<>();

    // Stack para manejar niveles de indentación
    private Stack<Integer> indentationStack = new Stack<>();

    // Conjunto de variables declaradas en el scope actual
    private Set<String> declaredVariables = new HashSet<>();

    // Conjunto de funciones declaradas
    private Set<String> declaredFunctions = new HashSet<>();

    // Variables temporales de bucles for
    private Set<String> loopVariables = new HashSet<>();

    // Scope actual
    private String currentScope = "global";

    // Palabras clave de Python
    private final Set<String> PYTHON_KEYWORDS = Set.of(
            "def", "class", "if", "elif", "else", "for", "while", "try", "except",
            "finally", "with", "as", "import", "from", "return", "yield", "break",
            "continue", "pass", "lambda", "and", "or", "not", "in", "is", "True",
            "False", "None"
    );

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        List<AnalysisError> errors = new ArrayList<>();

        // Limpiar estado anterior
        symbolTable.clear();
        indentationStack.clear();
        declaredVariables.clear();
        declaredFunctions.clear();
        loopVariables.clear();
        currentScope = "global";

        // Inicializar stack de indentación
        indentationStack.push(0);

        System.out.println("DEBUG - Iniciando análisis sintáctico de Python con " + tokens.size() + " tokens");

        for (int i = 0; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);
            String tokenType = currentToken.getType();
            String tokenValue = currentToken.getValue();

            try {
                // Verificar indentación al inicio de línea
                if (isStartOfLine(tokens, i)) {
                    checkIndentation(currentToken, errors);
                }

                // Análisis según tipo de token
                switch (tokenType) {
                    case "KEYWORD":
                        i = handleKeyword(tokens, i, errors);
                        break;

                    case "IDENTIFICADOR":
                    case "IDENTIFIER":
                        i = handleIdentifier(tokens, i, errors);
                        break;

                    case "OPERATOR":
                        handleOperator(currentToken, errors);
                        break;

                    case "DELIMITER":
                        handleDelimiter(currentToken, errors);
                        break;

                    default:
                        // Otros tipos de tokens (números, strings, etc.)
                        break;
                }

            } catch (Exception e) {
                errors.add(new AnalysisError(
                        "Error en análisis sintáctico: " + e.getMessage(),
                        AnalysisError.ErrorType.SYNTACTIC,
                        currentToken.getLine(),
                        currentToken.getColumn()
                ));
            }
        }

        // Verificar indentación final
        checkFinalIndentation(errors);

        System.out.println("DEBUG - Análisis sintáctico completado:");
        System.out.println("  - Símbolos encontrados: " + symbolTable.size());
        System.out.println("  - Errores sintácticos: " + errors.size());

        return errors;
    }

    private boolean isStartOfLine(List<Token> tokens, int index) {
        if (index == 0) return true;

        // Buscar hacia atrás hasta encontrar un salto de línea o el inicio
        for (int i = index - 1; i >= 0; i--) {
            String type = tokens.get(i).getType();
            if (type.equals("SALTO_LINEA") || type.equals("NEWLINE")) {
                return true;
            }
            if (!type.equals("ESPACIO") && !type.equals("WHITESPACE")) {
                return false;
            }
        }
        return true;
    }

    private void checkIndentation(Token token, List<AnalysisError> errors) {
        String tokenType = token.getType();

        if (tokenType.equals("ESPACIO") || tokenType.equals("WHITESPACE")) {
            int indentLevel = token.getValue().length();

            // Verificar que la indentación sea múltiplo de 4
            if (indentLevel % 4 != 0) {
                errors.add(new AnalysisError(
                        "La indentación debe ser múltiplo de 4 espacios (encontrado: " + indentLevel + ")",
                        AnalysisError.ErrorType.SYNTACTIC,
                        token.getLine(),
                        token.getColumn()
                ));
            }

            // Verificar consistencia con el stack de indentación
            int currentIndent = indentationStack.peek();

            if (indentLevel > currentIndent) {
                // Nueva indentación (debe ser exactamente +4)
                if (indentLevel != currentIndent + 4) {
                    errors.add(new AnalysisError(
                            "Incremento de indentación inválido. Esperado: " + (currentIndent + 4) + ", encontrado: " + indentLevel,
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(),
                            token.getColumn()
                    ));
                }
                indentationStack.push(indentLevel);
            } else if (indentLevel < currentIndent) {
                // Reducción de indentación
                while (!indentationStack.isEmpty() && indentationStack.peek() > indentLevel) {
                    indentationStack.pop();
                }

                if (indentationStack.isEmpty() || indentationStack.peek() != indentLevel) {
                    errors.add(new AnalysisError(
                            "Nivel de indentación inconsistente",
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(),
                            token.getColumn()
                    ));
                }
            }
        }
    }

    private int handleKeyword(List<Token> tokens, int index, List<AnalysisError> errors) {
        Token keyword = tokens.get(index);
        String keywordValue = keyword.getValue();

        switch (keywordValue) {
            case "def":
                return handleFunctionDeclaration(tokens, index, errors);

            case "class":
                return handleClassDeclaration(tokens, index, errors);

            case "if":
            case "elif":
            case "else":
                return handleIfStatement(tokens, index, errors);

            case "for":
                return handleForLoop(tokens, index, errors);

            case "while":
                return handleWhileLoop(tokens, index, errors);

            case "try":
                return handleTryStatement(tokens, index, errors);

            case "import":
            case "from":
                return handleImportStatement(tokens, index, errors);

            default:
                // Otras palabras clave
                break;
        }

        return index;
    }

    private int handleFunctionDeclaration(List<Token> tokens, int index, List<AnalysisError> errors) {
        // def función_name(parámetros):
        int currentIndex = index + 1;

        // Buscar nombre de función
        while (currentIndex < tokens.size() &&
                (tokens.get(currentIndex).getType().equals("ESPACIO") ||
                        tokens.get(currentIndex).getType().equals("WHITESPACE"))) {
            currentIndex++;
        }

        if (currentIndex >= tokens.size()) {
            errors.add(new AnalysisError(
                    "Definición de función incompleta: falta nombre",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(index).getLine(),
                    tokens.get(index).getColumn()
            ));
            return index;
        }

        Token functionNameToken = tokens.get(currentIndex);
        if (!functionNameToken.getType().equals("IDENTIFICADOR") &&
                !functionNameToken.getType().equals("IDENTIFIER")) {
            errors.add(new AnalysisError(
                    "Nombre de función inválido",
                    AnalysisError.ErrorType.SYNTACTIC,
                    functionNameToken.getLine(),
                    functionNameToken.getColumn()
            ));
            return index;
        }

        String functionName = functionNameToken.getValue();

        // Verificar si ya existe
        if (declaredFunctions.contains(functionName)) {
            errors.add(new AnalysisError(
                    "Función '" + functionName + "' ya está definida",
                    AnalysisError.ErrorType.SYNTACTIC,
                    functionNameToken.getLine(),
                    functionNameToken.getColumn()
            ));
        }

        // Agregar función a tabla de símbolos
        Symbol functionSymbol = new Symbol(
                functionName,
                Symbol.SymbolType.FUNCTION,
                currentScope,
                functionNameToken.getLine(),
                functionNameToken.getColumn()
        );
        symbolTable.put(functionName, functionSymbol);
        declaredFunctions.add(functionName);

        System.out.println("DEBUG - Función declarada: " + functionName);

        // Buscar paréntesis de apertura
        currentIndex++;
        while (currentIndex < tokens.size() &&
                !tokens.get(currentIndex).getValue().equals("(")) {
            currentIndex++;
        }

        if (currentIndex >= tokens.size()) {
            errors.add(new AnalysisError(
                    "Definición de función: falta paréntesis de apertura",
                    AnalysisError.ErrorType.SYNTACTIC,
                    functionNameToken.getLine(),
                    functionNameToken.getColumn()
            ));
            return index;
        }

        // Procesar parámetros
        currentIndex = handleFunctionParameters(tokens, currentIndex, errors, functionName);

        // Buscar dos puntos
        while (currentIndex < tokens.size() &&
                !tokens.get(currentIndex).getValue().equals(":")) {
            currentIndex++;
        }

        if (currentIndex >= tokens.size()) {
            errors.add(new AnalysisError(
                    "Definición de función: falta ':' al final",
                    AnalysisError.ErrorType.SYNTACTIC,
                    functionNameToken.getLine(),
                    functionNameToken.getColumn()
            ));
        }

        return currentIndex;
    }

    private int handleFunctionParameters(List<Token> tokens, int startIndex, List<AnalysisError> errors, String functionName) {
        int currentIndex = startIndex + 1; // Saltar '('

        while (currentIndex < tokens.size() &&
                !tokens.get(currentIndex).getValue().equals(")")) {

            Token token = tokens.get(currentIndex);
            if (token.getType().equals("IDENTIFICADOR") || token.getType().equals("IDENTIFIER")) {
                // Es un parámetro
                String paramName = token.getValue();

                Symbol paramSymbol = new Symbol(
                        paramName,
                        Symbol.SymbolType.PARAMETER,
                        functionName,
                        token.getLine(),
                        token.getColumn()
                );
                symbolTable.put(functionName + "." + paramName, paramSymbol);

                System.out.println("DEBUG - Parámetro declarado: " + paramName + " en función " + functionName);
            }

            currentIndex++;
        }

        return currentIndex;
    }

    private int handleClassDeclaration(List<Token> tokens, int index, List<AnalysisError> errors) {
        // Implementación básica para clases
        int currentIndex = index + 1;

        // Buscar nombre de clase
        while (currentIndex < tokens.size() &&
                (tokens.get(currentIndex).getType().equals("ESPACIO") ||
                        tokens.get(currentIndex).getType().equals("WHITESPACE"))) {
            currentIndex++;
        }

        if (currentIndex < tokens.size() &&
                (tokens.get(currentIndex).getType().equals("IDENTIFICADOR") ||
                        tokens.get(currentIndex).getType().equals("IDENTIFIER"))) {

            String className = tokens.get(currentIndex).getValue();
            Symbol classSymbol = new Symbol(
                    className,
                    Symbol.SymbolType.CLASS,
                    currentScope,
                    tokens.get(currentIndex).getLine(),
                    tokens.get(currentIndex).getColumn()
            );
            symbolTable.put(className, classSymbol);

            System.out.println("DEBUG - Clase declarada: " + className);
        }

        return currentIndex;
    }

    private int handleIdentifier(List<Token> tokens, int index, List<AnalysisError> errors) {
        Token identifier = tokens.get(index);
        String varName = identifier.getValue();

        // Verificar si es una asignación (declaración de variable)
        if (index + 1 < tokens.size()) {
            Token nextToken = tokens.get(index + 1);

            // Saltar espacios
            int nextIndex = index + 1;
            while (nextIndex < tokens.size() &&
                    (tokens.get(nextIndex).getType().equals("ESPACIO") ||
                            tokens.get(nextIndex).getType().equals("WHITESPACE"))) {
                nextIndex++;
            }

            if (nextIndex < tokens.size()) {
                String nextValue = tokens.get(nextIndex).getValue();

                // Asignaciones simples y compuestas
                if (nextValue.equals("=") || nextValue.matches("\\+=|\\-=|\\*=|/=|//=|%=|\\*\\*=")) {
                    // Es una declaración/asignación de variable
                    Symbol variableSymbol = symbolTable.get(varName);

                    if (variableSymbol == null) {
                        // Nueva variable
                        variableSymbol = new Symbol(
                                varName,
                                Symbol.SymbolType.VARIABLE,
                                currentScope,
                                identifier.getLine(),
                                identifier.getColumn()
                        );
                        symbolTable.put(varName, variableSymbol);
                        declaredVariables.add(varName);

                        System.out.println("DEBUG - Variable declarada: " + varName);
                    }

                    // Marcar como inicializada
                    variableSymbol.setInitialized(true);

                    return nextIndex;
                }
            }
        }

        // Si no es asignación, verificar si la variable está declarada
        if (!declaredVariables.contains(varName) &&
                !declaredFunctions.contains(varName) &&
                !loopVariables.contains(varName) &&
                !PYTHON_KEYWORDS.contains(varName) &&
                !isPredefinedFunction(varName)) {

            errors.add(new AnalysisError(
                    "Variable '" + varName + "' no está definida",
                    AnalysisError.ErrorType.SYNTACTIC,
                    identifier.getLine(),
                    identifier.getColumn()
            ));
        }

        return index;
    }

    private int handleForLoop(List<Token> tokens, int index, List<AnalysisError> errors) {
        // for variable in iterable:
        int currentIndex = index + 1;

        // Buscar variable del bucle
        while (currentIndex < tokens.size() &&
                (tokens.get(currentIndex).getType().equals("ESPACIO") ||
                        tokens.get(currentIndex).getType().equals("WHITESPACE"))) {
            currentIndex++;
        }

        if (currentIndex < tokens.size() &&
                (tokens.get(currentIndex).getType().equals("IDENTIFICADOR") ||
                        tokens.get(currentIndex).getType().equals("IDENTIFIER"))) {

            String loopVar = tokens.get(currentIndex).getValue();
            loopVariables.add(loopVar);

            // Agregar variable del bucle a tabla de símbolos
            Symbol loopSymbol = new Symbol(
                    loopVar,
                    Symbol.SymbolType.VARIABLE,
                    currentScope,
                    tokens.get(currentIndex).getLine(),
                    tokens.get(currentIndex).getColumn()
            );
            symbolTable.put(loopVar, loopSymbol);
            declaredVariables.add(loopVar);

            System.out.println("DEBUG - Variable de bucle for declarada: " + loopVar);
        }

        return currentIndex;
    }

    private int handleWhileLoop(List<Token> tokens, int index, List<AnalysisError> errors) {
        // while condición:
        // Buscar dos puntos
        int currentIndex = index + 1;
        while (currentIndex < tokens.size() &&
                !tokens.get(currentIndex).getValue().equals(":")) {
            currentIndex++;
        }

        if (currentIndex >= tokens.size()) {
            errors.add(new AnalysisError(
                    "Declaración while: falta ':' al final",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(index).getLine(),
                    tokens.get(index).getColumn()
            ));
        }

        return currentIndex;
    }

    private int handleIfStatement(List<Token> tokens, int index, List<AnalysisError> errors) {
        // if condición:
        // Buscar dos puntos
        int currentIndex = index + 1;
        while (currentIndex < tokens.size() &&
                !tokens.get(currentIndex).getValue().equals(":")) {
            currentIndex++;
        }

        if (currentIndex >= tokens.size()) {
            errors.add(new AnalysisError(
                    "Declaración if/elif/else: falta ':' al final",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(index).getLine(),
                    tokens.get(index).getColumn()
            ));
        }

        return currentIndex;
    }

    private int handleTryStatement(List<Token> tokens, int index, List<AnalysisError> errors) {
        // try:
        int currentIndex = index + 1;
        while (currentIndex < tokens.size() &&
                !tokens.get(currentIndex).getValue().equals(":")) {
            currentIndex++;
        }

        if (currentIndex >= tokens.size()) {
            errors.add(new AnalysisError(
                    "Declaración try: falta ':' al final",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(index).getLine(),
                    tokens.get(index).getColumn()
            ));
        }

        return currentIndex;
    }

    private int handleImportStatement(List<Token> tokens, int index, List<AnalysisError> errors) {
        // import module o from module import name
        // Por simplicidad, no validamos imports por ahora
        return index;
    }

    private void handleOperator(Token token, List<AnalysisError> errors) {
        // Validaciones básicas de operadores
        String operator = token.getValue();

        // Verificar operadores válidos
        if (!isValidOperator(operator)) {
            errors.add(new AnalysisError(
                    "Operador inválido: '" + operator + "'",
                    AnalysisError.ErrorType.SYNTACTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void handleDelimiter(Token token, List<AnalysisError> errors) {
        // Validaciones básicas de delimitadores
        String delimiter = token.getValue();

        // Verificar delimitadores válidos
        if (!isValidDelimiter(delimiter)) {
            errors.add(new AnalysisError(
                    "Delimitador inválido: '" + delimiter + "'",
                    AnalysisError.ErrorType.SYNTACTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void checkFinalIndentation(List<AnalysisError> errors) {
        // Al final del archivo, la indentación debe volver a 0
        if (indentationStack.size() > 1) {
            // Hay bloques sin cerrar
            System.out.println("DEBUG - Bloques de indentación sin cerrar: " + (indentationStack.size() - 1));
        }
    }

    private boolean isValidOperator(String operator) {
        Set<String> validOperators = Set.of(
                "+", "-", "*", "/", "//", "%", "**",
                "=", "+=", "-=", "*=", "/=", "//=", "%=", "**=",
                "==", "!=", "<", ">", "<=", ">=",
                "and", "or", "not", "in", "is",
                "&", "|", "^", "~", "<<", ">>"
        );
        return validOperators.contains(operator);
    }

    private boolean isValidDelimiter(String delimiter) {
        Set<String> validDelimiters = Set.of(
                "(", ")", "[", "]", "{", "}", ",", ":", ";", ".", "->"
        );
        return validDelimiters.contains(delimiter);
    }

    private boolean isPredefinedFunction(String name) {
        Set<String> predefined = Set.of(
                "print", "input", "len", "range", "str", "int", "float", "bool",
                "list", "dict", "tuple", "set", "abs", "max", "min", "sum",
                "open", "type", "isinstance", "hasattr", "getattr", "setattr"
        );
        return predefined.contains(name);
    }

    // Método para obtener la tabla de símbolos
    public Map<String, Symbol> getSymbolTable() {
        return new HashMap<>(symbolTable);
    }

    // Método para obtener solo la lista de símbolos (para compatibilidad)
    public List<Symbol> getSymbolList() {
        return new ArrayList<>(symbolTable.values());
    }
}