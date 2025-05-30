package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Symbol;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;

public class SQLSemanticAnalyzer implements ISemanticAnalyzer {


    @Override
    public Map<String, Symbol> getSymbolTable() {
        Map<String, Symbol> symbolTable = Map.of();
        return symbolTable;
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        List<AnalysisError> errors = new ArrayList<>();


        return errors;
    }


}
