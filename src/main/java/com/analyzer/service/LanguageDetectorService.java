package com.analyzer.service;

import com.analyzer.model.LanguageType;
import com.analyzer.service.interfaces.ILanguageDetector;
import java.util.regex.Pattern;

public class LanguageDetectorService implements ILanguageDetector {
    
    private static final Pattern PYTHON_PATTERN = Pattern.compile(
        "\\b(def|class|import|from|if|elif|else|while|for|in|return|print)\\b|" +
        "#.*$|" +
        "\\b(True|False|None)\\b",
        Pattern.MULTILINE
    );

    private static final Pattern SQL_PATTERN = Pattern.compile(
        "\\b(SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|JOIN|GROUP BY|ORDER BY|HAVING|CREATE|TABLE|ALTER|DROP|INDEX|VIEW)\\b|" +
        "\\b(VARCHAR|INTEGER|NUMBER|DATE|CHAR|BOOLEAN)\\b|" +
        "--.*$|/\\*(?s).*?\\*/",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HTML_PATTERN = Pattern.compile(
        "<[^>]+>|<!DOCTYPE|<html|<head|<body|<div|<p|<script|<style",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public LanguageType detectLanguage(String code) {
        if (code == null || code.trim().isEmpty()) {
            return LanguageType.UNKNOWN;
        }

        // Calcular puntuación para cada lenguaje
        int pythonScore = countMatches(PYTHON_PATTERN, code);
        int sqlScore = countMatches(SQL_PATTERN, code);
        int htmlScore = countMatches(HTML_PATTERN, code);

        // Determinar el lenguaje con mayor puntuación
        if (pythonScore > sqlScore && pythonScore > htmlScore) {
            return LanguageType.PYTHON;
        } else if (sqlScore > pythonScore && sqlScore > htmlScore) {
            return LanguageType.PLSQL; // Cambiado de SQL a PLSQL
        } else if (htmlScore > pythonScore && htmlScore > sqlScore) {
            return LanguageType.HTML;
        }

        // Si no hay un claro ganador o no hay coincidencias
        return LanguageType.UNKNOWN;
    }

    private int countMatches(Pattern pattern, String text) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
