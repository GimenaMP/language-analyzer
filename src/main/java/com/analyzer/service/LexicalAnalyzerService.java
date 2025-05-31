package com.analyzer.service;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ILanguageDetector;
import com.analyzer.service.interfaces.ILexicalAnalyzer;

import java.util.*;

public class LexicalAnalyzerService implements ILexicalAnalyzer {

    private final Map<LanguageType, ILexicalAnalyzer> analizadores;
    private final ILanguageDetector detector;

    public LexicalAnalyzerService(ILanguageDetector detector, Map<LanguageType, ILexicalAnalyzer> analizadores) {
        this.detector = detector;
        this.analizadores = analizadores;

        // Debug de inicialización
        System.out.println("LexicalAnalyzerService inicializado con " + analizadores.size() + " analizadores:");
        for (Map.Entry<LanguageType, ILexicalAnalyzer> entry : analizadores.entrySet()) {
            System.out.println("   " + entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName());
        }
    }

    @Override
    public List<Token> tokenize(String code) {
        if (code == null || code.trim().isEmpty()) {
            System.out.println("LexicalAnalyzerService.tokenize: Código vacío");
            return new ArrayList<>();
        }

        LanguageType language = detector.detectLanguage(code);
        System.out.println("🔍 LexicalAnalyzerService.tokenize: Lenguaje detectado = " + language);

        ILexicalAnalyzer analyzer = analizadores.get(language);

        if (analyzer == null) {
            System.out.println("No hay analizador para " + language + ", usando genérico");
            return analizarGenerico(code);
        }

        System.out.println("Usando analizador: " + analyzer.getClass().getSimpleName());
        return analyzer.tokenize(code);
    }

    @Override
    public List<Token> analyze(String code, LanguageType language) {
        if (code == null || code.trim().isEmpty()) {
            System.out.println("LexicalAnalyzerService.analyze: Código vacío");
            return new ArrayList<>();
        }

        System.out.println("LexicalAnalyzerService.analyze: Lenguaje = " + language);

        ILexicalAnalyzer analizador = analizadores.get(language);
        if (analizador == null) {
            System.err.println("No hay analizador disponible para el lenguaje: " + language);
            throw new IllegalStateException("No hay analizador disponible para el lenguaje: " + language);
        }

        System.out.println("Delegando a: " + analizador.getClass().getSimpleName());
        return analizador.analyze(code, language);
    }

    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        System.out.println("🔍 LexicalAnalyzerService.analyzeLexical iniciado");

        // VALIDAR PARÁMETROS
        if (fuente == null || fuente.trim().isEmpty()) {
            System.out.println("Fuente vacía o null");
            return new ArrayList<>();
        }

        // CREAR LISTA LOCAL DE ERRORES SI ES NULL
        List<AnalysisError> localErrors = errores != null ? errores : new ArrayList<>();

        try {
            // DETECTAR LENGUAJE
            LanguageType lenguaje = detector.detectLanguage(fuente);
            System.out.println("Lenguaje detectado: " + lenguaje);

            // OBTENER ANALIZADOR ESPECÍFICO
            ILexicalAnalyzer analizador = analizadores.get(lenguaje);

            if (analizador == null) {
                System.err.println("No se encontró analizador para: " + lenguaje);
                AnalysisError error = new AnalysisError(
                        "No se encontró analizador para el lenguaje detectado: " + lenguaje,
                        AnalysisError.ErrorType.LEXICAL,
                        1, 0
                );
                localErrors.add(error);
                return analizarGenerico(fuente);
            }

            System.out.println("Usando analizador específico: " + analizador.getClass().getSimpleName());

            // LLAMAR AL ANALIZADOR ESPECÍFICO
            List<Token> tokens = analizador.analyzeLexical(fuente, localErrors);

            System.out.println("Análisis completado:");
            System.out.println("Tokens: " + (tokens != null ? tokens.size() : 0));
            System.out.println("Errores: " + localErrors.size());

            return tokens != null ? new ArrayList<>(tokens) : new ArrayList<>();

        } catch (Exception e) {
            System.err.println(" ERROR CRÍTICO en LexicalAnalyzerService: " + e.getMessage());
            e.printStackTrace();

            AnalysisError error = new AnalysisError(
                    "Error en el análisis léxico: " + e.getMessage(),
                    AnalysisError.ErrorType.LEXICAL,
                    1, 0
            );
            localErrors.add(error);
            return new ArrayList<>();
        }
    }

    private List<Token> analizarGenerico(String fuente) {
        System.out.println("🔧 Usando análisis genérico...");
        List<Token> tokens = new ArrayList<>();
        String[] lineas = fuente.split("\n");

        for (int numeroLinea = 0; numeroLinea < lineas.length; numeroLinea++) {
            String linea = lineas[numeroLinea].trim();
            if (!linea.isEmpty()) {
                String[] palabras = linea.split("\\s+");
                for (int i = 0; i < palabras.length; i++) {
                    if (!palabras[i].isEmpty()) {
                        tokens.add(new Token(palabras[i], "GENERICO", numeroLinea + 1, i));
                    }
                }
            }
        }

        System.out.println("✅ Análisis genérico completado: " + tokens.size() + " tokens");
        return tokens;
    }
}