// SemanticAnalyzerService.java
package com.analyzer.service;

import com.analyzer.model.*;
import com.analyzer.service.SemanticAnalyzer.HTMLSemanticAnalyzer;
import com.analyzer.service.SemanticAnalyzer.PythonSemanticAnalyzer;
import com.analyzer.service.SemanticAnalyzer.SQLSemanticAnalyzer;
import com.analyzer.service.SyntacticAnalyzer.HTMLSyntacticAnalyzer;
import com.analyzer.service.SyntacticAnalyzer.PythonSyntacticAnalyzer;
import com.analyzer.service.SyntacticAnalyzer.SQLSyntacticAnalyzer;
import com.analyzer.service.interfaces.ISemanticAnalyzer;
import java.util.*;

public class SemanticAnalyzerService implements ISemanticAnalyzer {
    private final Map<LanguageType, ISemanticAnalyzer> analyzers;
    private ISemanticAnalyzer currentAnalyzer;

    public SemanticAnalyzerService() {
        this.analyzers = new HashMap<>();
        // Registrar analizadores semánticos por lenguaje
        analyzers.put(LanguageType.PYTHON, new PythonSemanticAnalyzer());
        analyzers.put(LanguageType.PLSQL, new SQLSemanticAnalyzer());
        analyzers.put(LanguageType.HTML, new HTMLSemanticAnalyzer());

        // Añadir más analizadores según se implementen

        this.currentAnalyzer = analyzers.get(LanguageType.PYTHON); // Default

    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return currentAnalyzer != null ? currentAnalyzer.getSymbolTable() : new HashMap<>();
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        if (currentAnalyzer == null) {
            List<AnalysisError> errors = new ArrayList<>();
            errors.add(new AnalysisError(
                    "No hay analizador semántico disponible para el lenguaje",
                    AnalysisError.ErrorType.SEMANTIC
            ));
            return errors;
        }

        return currentAnalyzer.analyze(tokens, language, symbolTable);
    }

    public void setLanguage(LanguageType language) {
        currentAnalyzer = analyzers.getOrDefault(language, analyzers.get(LanguageType.PYTHON));
    }
}