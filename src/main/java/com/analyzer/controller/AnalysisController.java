package com.analyzer.controller;

import com.analyzer.model.*;
import com.analyzer.service.*;
import com.analyzer.service.LexicalAnalizer.HTMLLexicalAnalyzer;
import com.analyzer.service.LexicalAnalizer.PythonLexicalAnalyzer;
import com.analyzer.service.LexicalAnalizer.SQLLexicalAnalyzer;
import com.analyzer.service.SemanticAnalyzer.*;
import com.analyzer.service.SyntacticAnalyzer.SQLSyntacticAnalyzer;
import com.analyzer.service.interfaces.*;
import com.analyzer.service.SyntacticAnalyzer.HTMLSyntacticAnalyzer;

import java.util.*;

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
        try {
            System.out.println("🚀 DEBUG - Inicializando AnalysisController...");

            this.languageDetector = new LanguageDetectorService();
            System.out.println("✅ DEBUG - LanguageDetector inicializado");

            this.lexicalAnalyzer = new LexicalAnalyzerService(
                    languageDetector,
                    Map.of(
                            LanguageType.PYTHON, new PythonLexicalAnalyzer(),
                            LanguageType.HTML, new HTMLLexicalAnalyzer(),
                            LanguageType.PLSQL, new SQLLexicalAnalyzer()
                    )
            );
            System.out.println("✅ DEBUG - LexicalAnalyzer inicializado");

            this.syntacticAnalyzer = new SyntacticAnalyzerService();
            System.out.println("✅ DEBUG - SyntacticAnalyzer inicializado");

            this.semanticAnalyzer = new SemanticAnalyzerService();
            System.out.println("✅ DEBUG - SemanticAnalyzer inicializado");

            this.executionSimulator = new ExecutionSimulator();
            System.out.println("✅ DEBUG - ExecutionSimulator inicializado");

            System.out.println("🎉 DEBUG - AnalysisController completamente inicializado");

        } catch (Exception e) {
            System.err.println("❌ ERROR fatal inicializando AnalysisController: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("No se pudo inicializar el controlador de análisis", e);
        }
    }

    /**
     * Realiza el análisis completo del código
     */
    public AnalysisResult performCompleteAnalysis(String code) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🔍 INICIANDO ANÁLISIS COMPLETO");
        System.out.println("📝 Código a analizar: " + (code != null ? code.length() + " caracteres" : "null"));
        System.out.println("=".repeat(50));

        AnalysisResult result = new AnalysisResult();

        // Validación de entrada
        if (code == null || code.trim().isEmpty()) {
            System.out.println("⚠️ WARNING - Código vacío o null");
            result.setSuccess(false);
            result.addError(new AnalysisError(
                    "Código vacío o inválido",
                    AnalysisError.ErrorType.LEXICAL,
                    1, 0
            ));
            return result;
        }

        try {
            // 1. Detectar lenguaje
            System.out.println("\n🏷️ PASO 1: Detectando lenguaje...");
            LanguageType language = languageDetector.detectLanguage(code);
            result.setLanguage(language);
            System.out.println("✅ Lenguaje detectado: " + (language != null ? language.getDisplayName() : "UNKNOWN"));

            if (language == null) {
                throw new RuntimeException("No se pudo detectar el lenguaje del código");
            }

            // 2. Análisis léxico
            System.out.println("\n📚 PASO 2: Análisis léxico...");
            List<AnalysisError> lexicalErrors = new ArrayList<>();

            List<Token> tokens = lexicalAnalyzer.analyzeLexical(code, lexicalErrors);
            result.setTokens(tokens);
            result.setLexicalErrors(new ArrayList<>(lexicalErrors));

            System.out.println("✅ Tokens generados: " + (tokens != null ? tokens.size() : 0));
            System.out.println("⚠️ Errores léxicos: " + lexicalErrors.size());

            // Log de primeros tokens para debug
            if (tokens != null && !tokens.isEmpty()) {
                System.out.println("🔍 Primeros 5 tokens:");
                tokens.stream().limit(5).forEach(token ->
                        System.out.println("   " + token.getType() + ": '" + token.getValue() + "' [" + token.getLine() + "," + token.getColumn() + "]")
                );
            }

            // 3. Análisis sintáctico
            System.out.println("\n🔧 PASO 3: Análisis sintáctico...");
            List<AnalysisError> syntacticErrors = new ArrayList<>();

            if (tokens != null && !tokens.isEmpty()) {
                syntacticErrors = syntacticAnalyzer.analyze(tokens, language);
            }

            result.setSyntacticErrors(syntacticErrors);
            System.out.println("✅ Análisis sintáctico completado");
            System.out.println("⚠️ Errores sintácticos: " + syntacticErrors.size());

            // 4. Obtener tabla de símbolos
            System.out.println("\n📋 PASO 4: Obteniendo tabla de símbolos...");
            Map<String, Symbol> symbolTable = getSymbolTableFromAnalyzer(syntacticAnalyzer);
            result.setSymbolTable(symbolTable);
            System.out.println("✅ Símbolos en tabla: " + symbolTable.size());

            // 5. Análisis semántico
            System.out.println("\n🧠 PASO 5: Análisis semántico...");
            Map<String, Symbol> currentSymbolTable = result.getSymbolTable() != null ?
                    new HashMap<>(result.getSymbolTable()) :
                    new HashMap<>();

            List<AnalysisError> semanticErrors = new ArrayList<>();
            if (tokens != null && !tokens.isEmpty()) {
                semanticErrors = semanticAnalyzer.analyze(tokens, language, currentSymbolTable);
            }

            result.setSemanticErrors(semanticErrors);
            result.setSymbolTable(currentSymbolTable);
            System.out.println("✅ Análisis semántico completado");
            System.out.println("⚠️ Errores semánticos: " + semanticErrors.size());

            // 6. Simulación de ejecución
            System.out.println("\n⚡ PASO 6: Simulación de ejecución...");
            List<String> executionOutput = new ArrayList<>();

            if (tokens != null && !tokens.isEmpty()) {
                try {
                    executionOutput = executionSimulator.simulateExecution(
                            tokens, language, List.copyOf(currentSymbolTable.values())
                    );
                } catch (Exception e) {
                    System.out.println("⚠️ Error en simulación: " + e.getMessage());
                    executionOutput.add("Error en simulación: " + e.getMessage());
                }
            }

            result.setExecutionOutput(executionOutput);
            System.out.println("✅ Simulación completada. Líneas de salida: " + executionOutput.size());

            // Determinar éxito
            boolean hasBlockingErrors = !lexicalErrors.isEmpty() ||
                    syntacticErrors.stream().anyMatch(e -> e.getErrorType() == AnalysisError.ErrorType.SYNTACTIC);
            result.setSuccess(!hasBlockingErrors);

            System.out.println("\n🎯 RESUMEN FINAL:");
            System.out.println("   📊 Tokens: " + (tokens != null ? tokens.size() : 0));
            System.out.println("   🏷️ Símbolos: " + symbolTable.size());
            System.out.println("   ❌ Errores léxicos: " + lexicalErrors.size());
            System.out.println("   ❌ Errores sintácticos: " + syntacticErrors.size());
            System.out.println("   ❌ Errores semánticos: " + semanticErrors.size());
            System.out.println("   🎉 Éxito: " + result.isSuccess());

        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO en análisis: " + e.getMessage());
            e.printStackTrace();

            result.setSuccess(false);
            AnalysisError error = new AnalysisError(
                    "Error crítico en el análisis: " + e.getMessage(),
                    AnalysisError.ErrorType.LEXICAL,
                    0, 0
            );
            result.addError(error);
        }

        System.out.println("=".repeat(50));
        System.out.println("🏁 ANÁLISIS COMPLETO FINALIZADO");
        System.out.println("=".repeat(50) + "\n");

        return result;
    }

    /**
     * Obtiene la tabla de símbolos del analizador sintáctico de forma genérica.
     */
    private Map<String, Symbol> getSymbolTableFromAnalyzer(ISyntacticAnalyzer analyzer) {
        Map<String, Symbol> symbolTable = new HashMap<>();

        try {
            if (analyzer instanceof SyntacticAnalyzerService) {
                Map<String, Symbol> serviceTable = ((SyntacticAnalyzerService) analyzer).getSymbolTable();
                if (serviceTable != null) {
                    symbolTable.putAll(serviceTable);
                }

                System.out.println("🔍 DEBUG - Símbolos obtenidos del SyntacticAnalyzerService:");
                for (Map.Entry<String, Symbol> entry : symbolTable.entrySet()) {
                    Symbol symbol = entry.getValue();
                    System.out.println("   📌 " + symbol.getName() + " (" +
                            symbol.getSymbolType().getDisplayName() +
                            ") en línea " + symbol.getDeclarationLine());
                }
            } else {
                // Fallback con reflexión
                try {
                    java.lang.reflect.Method getSymbolTableMethod = analyzer.getClass().getMethod("getSymbolTable");
                    Object result = getSymbolTableMethod.invoke(analyzer);

                    if (result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Symbol> mapResult = (Map<String, Symbol>) result;
                        symbolTable.putAll(mapResult);
                    } else if (result instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Symbol> listResult = (List<Symbol>) result;
                        for (Symbol symbol : listResult) {
                            if (symbol != null && symbol.getName() != null) {
                                symbolTable.put(symbol.getName(), symbol);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ DEBUG - Analizador no tiene tabla de símbolos disponible: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ WARNING - Error obteniendo tabla de símbolos: " + e.getMessage());
            e.printStackTrace();
        }

        return symbolTable;
    }

    // ... resto de la clase AnalysisResult igual ...
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
            this.lexicalErrors = lexicalErrors != null ? lexicalErrors : new ArrayList<>();
        }

        public List<AnalysisError> getSyntacticErrors() { return syntacticErrors; }
        public void setSyntacticErrors(List<AnalysisError> syntacticErrors) {
            this.syntacticErrors = syntacticErrors != null ? syntacticErrors : new ArrayList<>();
        }

        public List<AnalysisError> getSemanticErrors() { return semanticErrors; }
        public void setSemanticErrors(List<AnalysisError> semanticErrors) {
            this.semanticErrors = semanticErrors != null ? semanticErrors : new ArrayList<>();
        }

        public Map<String, Symbol> getSymbolTable() { return symbolTable; }
        public void setSymbolTable(Map<String, Symbol> symbolTable) {
            this.symbolTable = symbolTable != null ? symbolTable : new HashMap<>();
        }

        public List<String> getExecutionOutput() { return executionOutput; }
        public void setExecutionOutput(List<String> executionOutput) {
            this.executionOutput = executionOutput != null ? executionOutput : new ArrayList<>();
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public void addError(AnalysisError error) {
            if (error == null) return;

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