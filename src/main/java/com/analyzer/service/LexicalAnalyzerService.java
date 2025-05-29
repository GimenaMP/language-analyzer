
// ---------- LexicalAnalyzerService.java ----------
package com.analyzer.service;

import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.model.AnalysisError;
import com.analyzer.service.LexicalAnalizer.HTMLLexicalAnalyzer;
import com.analyzer.service.LexicalAnalizer.PythonLexicalAnalyzer;
import com.analyzer.service.LexicalAnalizer.SQLLexicalAnalyzer;
import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.service.interfaces.ILanguageDetector;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

/**
 * Servicio orquestador para análisis léxico.
 * Delega a implementaciones específicas según el lenguaje detectado.
 */
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


    /**
     * Analiza léxicamente la fuente y devuelve la lista de tokens.
     * @param fuente texto a analizar
     * @return lista de tokens encontrados
     */
    public List<Token> analyzeLexical(String fuente) {
        ultimosErrores.clear();

        if (fuente == null || fuente.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Detectar lenguaje
        LanguageType lenguaje = detector.detectLanguage(fuente);

        // Obtener analizador específico
        ILexicalAnalyzer analizador = analizadores.get(lenguaje);
        if (analizador == null) {
            // Lenguaje no soportado, usar analizador genérico
            return analizarGenerico(fuente);
        }

        // Realizar análisis léxico
        List<Token> tokens = analizador.analyzeLexical(fuente, ultimosErrores);

        return tokens;
    }

    /**
     * Obtiene los errores del último análisis realizado.
     * @return lista de errores léxicos encontrados
     */
    public List<AnalysisError> obtenerUltimosErrores() {
        return new ArrayList<>(ultimosErrores);
    }

    /**
     * Análisis genérico para lenguajes no soportados.
     */
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

    /**
     * Obtiene información sobre los analizadores disponibles.
     * @return conjunto de tipos de lenguaje soportados
     */
    public Set<LanguageType> obtenerLenguajesSoportados() {
        return analizadores.keySet();
    }

    @Override
    public List<Token> analyze(String code, LanguageType language) {
        ultimosErrores.clear();
        ILexicalAnalyzer analizador = analizadores.get(language);
        if (analizador == null) {
            return analizarGenerico(code);
        }
        return analizador.analyze(code, language);
    }

    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        ultimosErrores = errores;
        LanguageType lenguaje = detector.detectLanguage(fuente);
        ILexicalAnalyzer analizador = analizadores.get(lenguaje);
        if (analizador == null) {
            return analizarGenerico(fuente);
        }
        return analizador.analyzeLexical(fuente, errores);
    }

}
