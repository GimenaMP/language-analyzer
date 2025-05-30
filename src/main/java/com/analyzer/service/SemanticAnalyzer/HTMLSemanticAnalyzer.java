package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;

public class HTMLSemanticAnalyzer implements ISemanticAnalyzer {



    @Override
    public Map<String, Symbol> getSymbolTable() {
        Map<String, Symbol> tablaSimbolos = Map.of();
        return tablaSimbolos;
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> tablaSimbolosExistente) {


        List<AnalysisError> errores = List.of();
        return errores;
    }

}