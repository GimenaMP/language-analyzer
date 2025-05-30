package com.analyzer.service;

import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.model.AnalysisError;

import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.service.interfaces.ILanguageDetector;

import java.util.*;

/**
 * Servicio orquestador para análisis léxico.
 * Delega a implementaciones específicas según el lenguaje detectado.
 */
public class LexicalAnalyzerService implements ILexicalAnalyzer {

    private final Map<LanguageType, ILexicalAnalyzer> analizadores;
    private final ILanguageDetector detector;
    private List<AnalysisError> ultimosErrores = new ArrayList<>();

    public LexicalAnalyzerService(ILanguageDetector detector, Map<LanguageType, ILexicalAnalyzer> analizadores) {
        this.detector = detector;
        this.analizadores = analizadores;
    }

    private List<Token> analizarGenerico(String fuente) {
        List<Token> tokens = new ArrayList<>();
        String[] lineas = fuente.split("\n");

        for (int numeroLinea = 0; numeroLinea < lineas.length; numeroLinea++) {
            String linea = lineas[numeroLinea].trim();
            if (!linea.isEmpty()) {
                // Tokenización básica por palabras
                String[] palabras = linea.split("\\s+");
                for (int i = 0; i < palabras.length; i++) {
                    if (!palabras[i].isEmpty()) {
                        tokens.add(new Token(palabras[i], "GENERICO", numeroLinea + 1, i));
                    }
                }
            }
        }

        return tokens;
    }

    @Override
    public List<Token> tokenize(String code) {
        LanguageType language = detector.detectLanguage(code);
        ILexicalAnalyzer analyzer = analizadores.get(language);
        
        if (analyzer == null) {
            return analizarGenerico(code);
        }
        
        return analyzer.tokenize(code);
    }

    @Override
    public List<Token> analyze(String code, LanguageType language) {
        ultimosErrores.clear();
        ILexicalAnalyzer analizador = analizadores.get(language);
        if (analizador == null) {
            throw new IllegalStateException("No hay analizador disponible para el lenguaje: " + language);
        }
        return analizador.analyze(code, language);
    }

    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        if (errores == null) {
            errores = new ArrayList<>();
        }
        ultimosErrores = errores;
        
        LanguageType lenguaje = detector.detectLanguage(fuente);
        ILexicalAnalyzer analizador = analizadores.get(lenguaje);
        
        if (analizador == null) {
            errores.add(new AnalysisError(
                "No se encontró analizador para el lenguaje detectado",
                AnalysisError.ErrorType.LEXICAL,
                1,
                0
            ));
            return analizarGenerico(fuente);
        }
        
        return analizador.analyzeLexical(fuente, errores);
    }

}
