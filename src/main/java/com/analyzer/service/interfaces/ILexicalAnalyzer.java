// --- ILexicalAnalyzer.java ---
package com.analyzer.service.interfaces;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.Token;
import com.analyzer.model.LanguageType;
import java.util.List;

public interface ILexicalAnalyzer {
    List<Token> analyze(String code, LanguageType language);

    List<Token> analyzeLexical(String fuente, List<AnalysisError> errores);

    List<Token> tokenize(String code);


}
