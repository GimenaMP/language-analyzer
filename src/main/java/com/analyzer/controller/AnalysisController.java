// --- AnalysisController.java ---
package com.analyzer.controller;

import com.analyzer.model.*;
import com.analyzer.service.*;
import com.analyzer.service.LexicalAnalizer.HTMLLexicalAnalyzer;
import com.analyzer.service.LexicalAnalizer.PythonLexicalAnalyzer;
import com.analyzer.service.LexicalAnalizer.SQLLexicalAnalyzer;
import com.analyzer.service.interfaces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador principal que orquesta todo el análisis
 * Principio de Responsabilidad Única: Solo coordina los servicios
 */
public class AnalysisController {

    private final ILanguageDetector languageDetector;
    private final ILexicalAnalyzer lexicalAnalyzer;
    private final ISyntacticAnalyzer syntacticAnalyzer;
    private final ISemanticAnalyzer semanticAnalyzer;
    private final ExecutionSimulator executionSimulator;

    public AnalysisController() {
        this.languageDetector = new LanguageDetectorService();
        this.lexicalAnalyzer = new LexicalAnalyzerService(
                languageDetector,
                Map.of(
                        LanguageType.PYTHON, new PythonLexicalAnalyzer(),
                        LanguageType.HTML, new HTMLLexicalAnalyzer(),
                        LanguageType.PLSQL, new SQLLexicalAnalyzer()
                )
        );
        this.syntacticAnalyzer = new SyntacticAnalyzerService();
        this.semanticAnalyzer = new SemanticAnalyzerService();
        this.executionSimulator = new ExecutionSimulator();
    }

    /**
     * Realiza el análisis completo del código
     */
    public AnalysisResult performCompleteAnalysis(String code) {
        AnalysisResult result = new AnalysisResult();

        try {
            // 1. Detectar lenguaje
            LanguageType language = languageDetector.detectLanguage(code);
            result.setLanguage(language);

            // 2. Análisis léxico
            List<AnalysisError> lexicalErrors = new ArrayList<>();
            List<Token> tokens = lexicalAnalyzer.analyzeLexical(code, lexicalErrors);
            result.setTokens(tokens);
            result.setLexicalErrors(lexicalErrors);

            // 3. Análisis sintáctico
            List<AnalysisError> syntacticErrors = syntacticAnalyzer.analyze(tokens, language);
            result.setSyntacticErrors(syntacticErrors);

            // 4. Análisis semántico
            List<AnalysisError> semanticErrors = semanticAnalyzer.analyze(tokens, language, null);
            result.setSemanticErrors(semanticErrors);

            // 5. Obtener tabla de símbolos
            Map<String, Symbol> symbolTable = semanticAnalyzer.getSymbolTable();
            result.setSymbolTable(symbolTable);

            // 6. Simulación de ejecución
            List<String> executionOutput = executionSimulator.simulateExecution(
                    tokens, language, List.copyOf(symbolTable.values())
            );
            result.setExecutionOutput(executionOutput);

            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.addError(new AnalysisError(
                    "Error interno: " + e.getMessage(),
                    AnalysisError.ErrorType.LEXICAL
            ));
        }

        return result;
    }

    /**
     * Clase interna para encapsular los resultados del análisis
     */
    public static class AnalysisResult {
        private LanguageType language;
        private List<Token> tokens;
        private List<AnalysisError> lexicalErrors;
        private List<AnalysisError> syntacticErrors;
        private List<AnalysisError> semanticErrors;
        private Map<String, Symbol> symbolTable;
        private List<String> executionOutput;
        private boolean success;

        public AnalysisResult() {
            this.lexicalErrors = new ArrayList<>();
            this.syntacticErrors = new ArrayList<>();
            this.semanticErrors = new ArrayList<>();
            this.executionOutput = new ArrayList<>();
            this.success = false;
        }

        // Getters y setters
        public LanguageType getLanguage() { return language; }
        public void setLanguage(LanguageType language) { this.language = language; }

        public List<Token> getTokens() { return tokens; }
        public void setTokens(List<Token> tokens) { this.tokens = tokens; }

        public List<AnalysisError> getLexicalErrors() { return lexicalErrors; }
        public void setLexicalErrors(List<AnalysisError> lexicalErrors) {
            this.lexicalErrors = lexicalErrors;
        }

        public List<AnalysisError> getSyntacticErrors() { return syntacticErrors; }
        public void setSyntacticErrors(List<AnalysisError> syntacticErrors) {
            this.syntacticErrors = syntacticErrors;
        }

        public List<AnalysisError> getSemanticErrors() { return semanticErrors; }
        public void setSemanticErrors(List<AnalysisError> semanticErrors) {
            this.semanticErrors = semanticErrors;
        }

        public Map<String, Symbol> getSymbolTable() { return symbolTable; }
        public void setSymbolTable(Map<String, Symbol> symbolTable) {
            this.symbolTable = symbolTable;
        }

        public List<String> getExecutionOutput() { return executionOutput; }
        public void setExecutionOutput(List<String> executionOutput) {
            this.executionOutput = executionOutput;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public void addError(AnalysisError error) {
            switch (error.getErrorType()) {
                case LEXICAL:
                    lexicalErrors.add(error);
                    break;
                case SYNTACTIC:
                    syntacticErrors.add(error);
                    break;
                case SEMANTIC:
                    semanticErrors.add(error);
                    break;
                default:
                    syntacticErrors.add(error);
            }
        }

        public List<AnalysisError> getAllErrors() {
            List<AnalysisError> allErrors = new ArrayList<>();
            allErrors.addAll(lexicalErrors);
            allErrors.addAll(syntacticErrors);
            allErrors.addAll(semanticErrors);
            return allErrors;
        }
    }
}