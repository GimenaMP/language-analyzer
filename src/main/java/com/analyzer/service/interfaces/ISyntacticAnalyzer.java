// --- ISyntacticAnalyzer.java ---
package com.analyzer.service.interfaces;

import com.analyzer.model.Token;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import java.util.List;

public interface ISyntacticAnalyzer {
    List<AnalysisError> analyze(List<Token> tokens, LanguageType language);
}