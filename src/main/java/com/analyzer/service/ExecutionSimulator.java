
// --- ExecutionSimulator.java ---
package com.analyzer.service;

import com.analyzer.model.*;
import java.util.List;
import java.util.ArrayList;

public class ExecutionSimulator {

    public List<String> simulateExecution(List<Token> tokens, LanguageType language,
                                          List<Symbol> symbols) {
        List<String> outputs = new ArrayList<>();

        switch (language) {
            case HTML:
                outputs.addAll(simulateHtmlExecution(tokens, symbols));
                break;
            case PYTHON:
                outputs.addAll(simulatePythonExecution(tokens, symbols));
                break;
            case PLSQL:
                outputs.addAll(simulatePlsqlExecution(tokens, symbols));
                break;
            default:
               // outputs.add("Simulación no disponible para el lenguaje: " + language.getDisplayName());
                break;
        }

        return outputs;
    }

    private List<String> simulateHtmlExecution(List<Token> tokens, List<Symbol> symbols) {
        List<String> outputs = new ArrayList<>();
        outputs.add("=== SIMULACIÓN DE RENDERIZADO HTML ===");

        for (Token token : tokens) {
            if (token.isOfType("TAG")) {
                String tagValue = token.getValue();
                if (!tagValue.startsWith("</")) {
                    String tagName = tagValue.substring(1,
                            tagValue.contains(" ") ? tagValue.indexOf(" ") : tagValue.length() - 1);
                    outputs.add("Renderizando elemento: <" + tagName + ">");
                }
            }
        }

        outputs.add("Página HTML renderizada correctamente");
        return outputs;
    }

    private List<String> simulatePythonExecution(List<Token> tokens, List<Symbol> symbols) {
        List<String> outputs = new ArrayList<>();
        outputs.add("=== SIMULACIÓN DE EJECUCIÓN PYTHON ===");

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD")) {
                switch (token.getValue()) {
                    case "def":
                        if (i + 1 < tokens.size()) {
                            outputs.add("Definiendo función: " + tokens.get(i + 1).getValue());
                        }
                        break;
                    case "class":
                        if (i + 1 < tokens.size()) {
                            outputs.add("Definiendo clase: " + tokens.get(i + 1).getValue());
                        }
                        break;
                    case "print":
                        outputs.add("Ejecutando print()...");
                        break;
                }
            }
        }

        outputs.add("Script Python ejecutado correctamente");
        return outputs;
    }

    private List<String> simulatePlsqlExecution(List<Token> tokens, List<Symbol> symbols) {
        List<String> outputs = new ArrayList<>();
        outputs.add("=== SIMULACIÓN DE EJECUCIÓN PL/SQL ===");

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD")) {
                String keyword = token.getValue().toUpperCase();
                switch (keyword) {
                    case "CREATE":
                        if (i + 1 < tokens.size() &&
                                tokens.get(i + 1).getValue().toUpperCase().equals("TABLE")) {
                            if (i + 2 < tokens.size()) {
                                outputs.add("Creando tabla: " + tokens.get(i + 2).getValue());
                            }
                        }
                        break;
                    case "SELECT":
                        outputs.add("Ejecutando consulta SELECT...");
                        break;
                    case "INSERT":
                        outputs.add("Insertando datos...");
                        break;
                    case "UPDATE":
                        outputs.add("Actualizando registros...");
                        break;
                    case "DELETE":
                        outputs.add("Eliminando registros...");
                        break;
                }
            }
        }

        outputs.add("Comandos SQL ejecutados correctamente");
        return outputs;
    }
}