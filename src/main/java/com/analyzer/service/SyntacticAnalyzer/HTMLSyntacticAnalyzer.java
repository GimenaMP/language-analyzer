package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class HTMLSyntacticAnalyzer implements ISyntacticAnalyzer {


    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {


        List<AnalysisError> errors = List.of();
        return errors;
    }







}
