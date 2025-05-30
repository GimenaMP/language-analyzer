package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;

public class PythonSemanticAnalyzer implements ISemanticAnalyzer {


    @Override
    public Map<String, Symbol> getSymbolTable() {
        Map<String, Symbol> symbolTable = Map.of();
        return symbolTable;
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {


        List<AnalysisError> errors = List.of();
        return errors;
    }


}