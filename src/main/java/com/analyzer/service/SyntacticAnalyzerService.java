// SyntacticAnalyzerService.java
package com.analyzer.service;

import com.analyzer.model.*;
import com.analyzer.service.SyntacticAnalyzer.HTMLSyntacticAnalyzer;
import com.analyzer.service.SyntacticAnalyzer.PythonSyntacticAnalyzer;
import com.analyzer.service.SyntacticAnalyzer.SQLSyntacticAnalyzer;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;
import java.util.*;

public class SyntacticAnalyzerService implements ISyntacticAnalyzer {
    private final Map<LanguageType, ISyntacticAnalyzer> analyzers;
    private ISyntacticAnalyzer currentAnalyzer;

    public SyntacticAnalyzerService() {
        this.analyzers = new HashMap<>();
        // Registrar analizadores sintácticos por lenguaje
        analyzers.put(LanguageType.PYTHON, new PythonSyntacticAnalyzer());
        analyzers.put(LanguageType.PLSQL, new SQLSyntacticAnalyzer());
        analyzers.put(LanguageType.HTML, new HTMLSyntacticAnalyzer());

        // Añadir más analizadores según se implementen
        this.currentAnalyzer = analyzers.get(LanguageType.PYTHON); // Default
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        // Seleccionar el analizador apropiado según el lenguaje
        currentAnalyzer = analyzers.get(language);

        if (currentAnalyzer == null) {
            List<AnalysisError> errors = new ArrayList<>();
            errors.add(new AnalysisError(
                    "No hay analizador sintáctico disponible para el lenguaje: " + language,
                    AnalysisError.ErrorType.SYNTACTIC,
                    0,
                    0
            ));
            return errors;
        }

        System.out.println("DEBUG - SyntacticAnalyzerService: Analizando con " + language + " analyzer");
        return currentAnalyzer.analyze(tokens, language);
    }

    /**
     * Obtiene la tabla de símbolos del analizador sintáctico actual.
     * @return Map con la tabla de símbolos, o vacío si no está disponible
     */
    public Map<String, Symbol> getSymbolTable() {
        if (currentAnalyzer instanceof PythonSyntacticAnalyzer) {
            PythonSyntacticAnalyzer pythonAnalyzer = (PythonSyntacticAnalyzer) currentAnalyzer;
            Map<String, Symbol> symbolTable = pythonAnalyzer.getSymbolTable();
            System.out.println("DEBUG - SyntacticAnalyzerService: Devolviendo " + symbolTable.size() + " símbolos de Python");
            return symbolTable;
        }
        else if (currentAnalyzer instanceof HTMLSyntacticAnalyzer) {
            // El código original ya tenía esto implementado
            return ((HTMLSyntacticAnalyzer) currentAnalyzer).getSymbolTable()
                    .stream()
                    .collect(HashMap::new,
                            (map, symbol) -> map.put(symbol.getName(), symbol),
                            HashMap::putAll);
        }
        else if (currentAnalyzer instanceof SQLSyntacticAnalyzer) {
            // Aquí puedes agregar soporte para SQL cuando lo implementes
            return new HashMap<>();
        }

        System.out.println("DEBUG - SyntacticAnalyzerService: No hay tabla de símbolos disponible para el analizador actual");
        return new HashMap<>();
    }

    /**
     * Obtiene la lista de símbolos del analizador sintáctico actual.
     * @return Lista de símbolos, o vacía si no está disponible
     */
    public List<Symbol> getSymbolList() {
        if (currentAnalyzer instanceof PythonSyntacticAnalyzer) {
            PythonSyntacticAnalyzer pythonAnalyzer = (PythonSyntacticAnalyzer) currentAnalyzer;
            return pythonAnalyzer.getSymbolList();
        }
        else if (currentAnalyzer instanceof HTMLSyntacticAnalyzer) {
            return ((HTMLSyntacticAnalyzer) currentAnalyzer).getSymbolTable();
        }

        return new ArrayList<>();
    }

    /**
     * Obtiene el analizador actual para verificaciones adicionales.
     * @return El analizador sintáctico actual
     */
    public ISyntacticAnalyzer getCurrentAnalyzer() {
        return currentAnalyzer;
    }

    /**
     * Obtiene el tipo de lenguaje del analizador actual.
     * @return LanguageType del analizador actual, o null si no está definido
     */
    public LanguageType getCurrentLanguageType() {
        for (Map.Entry<LanguageType, ISyntacticAnalyzer> entry : analyzers.entrySet()) {
            if (entry.getValue() == currentAnalyzer) {
                return entry.getKey();
            }
        }
        return null;
    }
}