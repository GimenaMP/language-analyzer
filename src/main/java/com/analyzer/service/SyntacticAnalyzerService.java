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

        return currentAnalyzer.analyze(tokens, language);
    }

    public List<Symbol> getSymbolTable() {
        if (currentAnalyzer instanceof HTMLSyntacticAnalyzer) {
            return ((HTMLSyntacticAnalyzer) currentAnalyzer).getSymbolTable();
        }
        // Añadir más casos según se implementen otros analizadores
        return new ArrayList<>();
    }
}
