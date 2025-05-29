// --- ISemanticAnalyzer.java ---
package com.analyzer.service.interfaces;

import com.analyzer.model.Token;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.Symbol;
import com.analyzer.model.LanguageType;
import java.util.List;
import java.util.Map;

public interface ISemanticAnalyzer {
    List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable);
    Map<String, Symbol> getSymbolTable();
}