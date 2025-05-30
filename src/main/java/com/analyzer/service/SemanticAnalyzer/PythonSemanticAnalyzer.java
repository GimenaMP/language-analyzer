package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;

public class PythonSemanticAnalyzer implements ISemanticAnalyzer {

    private Map<String, Symbol> symbolTable;
    private Stack<String> scopeStack;
    private List<AnalysisError> errors;
    private int currentLine;

    public PythonSemanticAnalyzer() {
        this.symbolTable = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.errors = new ArrayList<>();
        this.currentLine = 1;
    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return symbolTable;
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        this.symbolTable = symbolTable != null ? symbolTable : new HashMap<>();
        this.errors.clear();
        this.scopeStack.clear();
        this.scopeStack.push("global");

        analizarDeclaraciones(tokens);
        tokens_noAceptados(tokens);
        analizarUsos(tokens);
        validarSimbolos();

        return errors;
    }

    private void analizarDeclaraciones(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            currentLine = token.getLine();

            if (token.getType().equals("IDENTIFICADOR")) {
                // Analizar asignaciones
                if (i + 1 < tokens.size() && tokens.get(i + 1).getValue().equals("=")) {
                    registrarVariable(token.getValue(), inferirTipo(tokens, i + 2));
                }
            } else if (token.getType().equals("KEYWORD")) {
                switch (token.getValue().toUpperCase()) {
                    case "DEF":
                        analizarFuncion(tokens, i);
                        break;
                    case "CLASS":
                        analizarClase(tokens, i);
                        break;
                }
            }
        }
    }

   public void tokens_noAceptados(List<Token> tokens) {
       for (Token token : tokens) {
           validarOperadores(token);
              validarDivisionPorCero(token);
              validarRango(token);

       }
   }
    private void validarOperadores(Token token) {
        if (token.getType().equals("OPERADOR")) {
            String operador = token.getValue();

            // Detectar operadores repetidos inválidos
            if (operador.matches("[+\\-*/]{2,}")) {
                errors.add(new AnalysisError(
                        "Operador inválido '" + operador + "' - uso múltiple no permitido en Python",
                        AnalysisError.ErrorType.LEXICAL,
                        token.getLine(),
                        token.getColumn()
                ));
            }

            // Detectar operadores compuestos inválidos
            if (!operador.matches("\\*\\*|//|==|!=|=>|&&|<<|>>")) {
                if (operador.length() > 1) {
                    errors.add(new AnalysisError(
                            "Operador compuesto inválido '" + operador + "'",
                            AnalysisError.ErrorType.LEXICAL,
                            token.getLine(),
                            token.getColumn()
                    ));
                }
            }
        }
    }

    //validar divicion en 0
    private void validarDivisionPorCero(Token token) {
        if (token.getValue().equals("/") || token.getValue().equals("//")) {
            if (token.getType().equals("NUMERO_ENTERO") || token.getType().equals("NUMERO_DECIMAL")) {
                int valor = Integer.parseInt(token.getValue());
                if (valor == 0) {
                    errors.add(new AnalysisError(
                            "División por cero no permitida",
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(),
                            token.getColumn()
                    ));
                }
            }
        }
    }


    //validar rangos
    private void validarRango(Token token) {
        if (token.getType().equals("RANGO")) {
            String rango = token.getValue();
            String[] partes = rango.split(":");
            if (partes.length != 2) {
                errors.add(new AnalysisError(
                        "Rango inválido: " + rango,
                        AnalysisError.ErrorType.SEMANTIC,
                        token.getLine(),
                        token.getColumn()
                ));
            } else {
                try {
                    int inicio = Integer.parseInt(partes[0]);
                    int fin = Integer.parseInt(partes[1]);
                    if (inicio >= fin) {
                        errors.add(new AnalysisError(
                                "El inicio del rango debe ser menor que el fin: " + rango,
                                AnalysisError.ErrorType.SEMANTIC,
                                token.getLine(),
                                token.getColumn()
                        ));
                    }
                } catch (NumberFormatException e) {
                    errors.add(new AnalysisError(
                            "Rango debe contener números enteros: " + rango,
                            AnalysisError.ErrorType.SEMANTIC,
                            token.getLine(),
                            token.getColumn()
                    ));
                }
            }
        }
    }





    private void registrarVariable(String nombre, String tipo) {
        String scope = scopeStack.peek();
        String symbolKey = scope + "." + nombre;

        if (!symbolTable.containsKey(symbolKey)) {
            Symbol symbol = new Symbol(nombre, Symbol.SymbolType.VARIABLE, tipo, scope);
            symbol.setDeclarationLine(currentLine);
            symbolTable.put(symbolKey, symbol);
        }
    }

    private String inferirTipo(List<Token> tokens, int inicio) {
        if (inicio >= tokens.size()) return "unknown";

        Token token = tokens.get(inicio);
        switch (token.getType()) {
            case "STRING":
                return "str";
            case "NUMERO_ENTERO":
                return "int";
            case "NUMERO_DECIMAL":
                return "float";
            case "KEYWORD":
                if (token.getValue().equals("True") || token.getValue().equals("False")) {
                    return "bool";
                }
                break;
        }
        return "unknown";
    }

    private void analizarFuncion(List<Token> tokens, int inicio) {
        if (inicio + 1 >= tokens.size()) return;

        Token nombreToken = tokens.get(inicio + 1);
        if (nombreToken.getType().equals("IDENTIFICADOR")) {
            String nombre = nombreToken.getValue();
            String scope = scopeStack.peek();
            String symbolKey = scope + "." + nombre;

            Symbol symbol = new Symbol(nombre, Symbol.SymbolType.FUNCTION, "function", scope);
            symbol.setDeclarationLine(currentLine);
            symbolTable.put(symbolKey, symbol);

            scopeStack.push(scope + "." + nombre);
            // Analizar parámetros
            analizarParametrosFuncion(tokens, inicio + 2);
        }
    }

    private void analizarClase(List<Token> tokens, int inicio) {
        if (inicio + 1 >= tokens.size()) return;

        Token nombreToken = tokens.get(inicio + 1);
        if (nombreToken.getType().equals("IDENTIFICADOR")) {
            String nombre = nombreToken.getValue();
            String scope = scopeStack.peek();
            String symbolKey = scope + "." + nombre;

            Symbol symbol = new Symbol(nombre, Symbol.SymbolType.CLASS, "class", scope);
            symbol.setDeclarationLine(currentLine);
            symbolTable.put(symbolKey, symbol);

            scopeStack.push(scope + "." + nombre);
        }
    }

    private void analizarParametrosFuncion(List<Token> tokens, int inicio) {
        String scope = scopeStack.peek();

        for (int i = inicio; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType().equals("IDENTIFICADOR")) {
                String nombre = token.getValue();
                String symbolKey = scope + "." + nombre;

                Symbol symbol = new Symbol(nombre, Symbol.SymbolType.PARAMETER, "unknown", scope);
                symbol.setDeclarationLine(currentLine);
                symbolTable.put(symbolKey, symbol);
            } else if (token.getValue().equals(":")) {
                break;
            }
        }
    }

    private void analizarUsos(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.getType().equals("IDENTIFICADOR")) {
                verificarUsoVariable(token);
            }
        }
    }

    private void verificarUsoVariable(Token token) {
        String nombre = token.getValue();
        String scope = scopeStack.peek();

        // Buscar en el scope actual y scopes superiores
        boolean encontrado = false;
        for (String currentScope = scope; currentScope != null; currentScope = obtenerScopePadre(currentScope)) {
            String symbolKey = currentScope + "." + nombre;
            if (symbolTable.containsKey(symbolKey)) {
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            errors.add(new AnalysisError(
                    "Variable no declarada: " + nombre,
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private String obtenerScopePadre(String scope) {
        int ultimoPunto = scope.lastIndexOf(".");
        return ultimoPunto > 0 ? scope.substring(0, ultimoPunto) : null;
    }

    private void validarSimbolos() {
        for (Symbol symbol : symbolTable.values()) {
            if (!symbol.isInitialized() && symbol.getSymbolType() == Symbol.SymbolType.VARIABLE) {
                errors.add(new AnalysisError(
                        "Variable no inicializada: " + symbol.getName(),
                        AnalysisError.ErrorType.WARNING,
                        symbol.getDeclarationLine(),
                        0
                ));
            }
        }
    }
}