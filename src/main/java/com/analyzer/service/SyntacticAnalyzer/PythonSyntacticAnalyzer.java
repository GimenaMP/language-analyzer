package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class PythonSyntacticAnalyzer implements ISyntacticAnalyzer {
    private Set<String> declaredVariables = new HashSet<>();
    private Stack<Integer> indentationLevels = new Stack<>();
    private int currentIndentation = 0;
    private Set<String> forLoopVariables = new HashSet<>();
    private Set<String> functionParameters = new HashSet<>();

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        List<AnalysisError> errors = new ArrayList<>();
        declaredVariables.clear();
        indentationLevels.clear();
        forLoopVariables.clear();
        functionParameters.clear();
        indentationLevels.push(0);

        for (int i = 0; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);

            // Verificar indentación
            if (currentToken.getType().equals("ESPACIO") &&
                (i == 0 || tokens.get(i-1).getType().equals("SALTO_LINEA"))) {
                int spaces = currentToken.getValue().length();
                if (spaces % 4 != 0) {
                    errors.add(new AnalysisError(
                        "La indentación debe ser múltiplo de 4 espacios",
                        AnalysisError.ErrorType.SYNTACTIC,
                        currentToken.getLine(),
                        currentToken.getColumn()
                    ));
                }
                currentIndentation = spaces;
            }

            // Verificar declaración de funciones
            if (currentToken.getType().equals("KEYWORD") && currentToken.getValue().equals("def")) {
                handleFunctionDeclaration(tokens, i, errors);
            }

            // Verificar bucles for
            if (currentToken.getType().equals("KEYWORD") && currentToken.getValue().equals("for")) {
                handleForLoop(tokens, i, errors);
            }

            // Verificar uso de variables
            if (currentToken.getType().equals("IDENTIFICADOR")) {
                // Ignorar si es parte de una declaración de función o bucle for
                if (!functionParameters.contains(currentToken.getValue()) &&
                    !forLoopVariables.contains(currentToken.getValue())) {

                    // Verificar si la variable está siendo declarada
                    if (i + 1 < tokens.size() && tokens.get(i + 1).getValue().equals("=")) {
                        declaredVariables.add(currentToken.getValue());
                    }
                    // Verificar uso de variable no declarada
                    else if (!declaredVariables.contains(currentToken.getValue())) {
                        errors.add(new AnalysisError(
                            "Variable '" + currentToken.getValue() + "' usada antes de ser declarada",
                            AnalysisError.ErrorType.SYNTACTIC,
                            currentToken.getLine(),
                            currentToken.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private void handleFunctionDeclaration(List<Token> tokens, int index, List<AnalysisError> errors) {
        // Buscar paréntesis de apertura
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getValue().equals("(")) {
                // Recolectar parámetros hasta el paréntesis de cierre
                i++;
                while (i < tokens.size() && !tokens.get(i).getValue().equals(")")) {
                    if (tokens.get(i).getType().equals("IDENTIFICADOR")) {
                        functionParameters.add(tokens.get(i).getValue());
                    }
                    i++;
                }
                break;
            }
        }
    }

    private void handleForLoop(List<Token> tokens, int index, List<AnalysisError> errors) {
        // Buscar la variable del bucle for
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType().equals("IDENTIFICADOR")) {
                forLoopVariables.add(token.getValue());
                break;
            }
        }
    }
}
