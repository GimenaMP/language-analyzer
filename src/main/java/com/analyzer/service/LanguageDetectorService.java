
// --- LanguageDetectorService.java ---
package com.analyzer.service;

import com.analyzer.service.interfaces.ILanguageDetector;
import com.analyzer.model.LanguageType;
import java.util.regex.Pattern;

public class LanguageDetectorService implements ILanguageDetector {

    // Patrones para HTML
    private static final Pattern HTML_PATTERN = Pattern.compile(
            ".*(<html|<head|<body|<div|<p>|<span|<!DOCTYPE|<meta|<link|<script>).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Patrones para Python
    private static final Pattern PYTHON_PATTERN = Pattern.compile(
            ".*(def\\s+\\w+|import\\s+\\w+|from\\s+\\w+|if\\s+\\w+:|for\\s+\\w+\\s+in|class\\s+\\w+:|print\\s*\\().*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Patrones para PL/SQL
    private static final Pattern PLSQL_PATTERN = Pattern.compile(
            ".*(CREATE\\s+TABLE|SELECT\\s+\\*|INSERT\\s+INTO|UPDATE\\s+\\w+|DELETE\\s+FROM|BEGIN|END;|DECLARE).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    public LanguageType detectLanguage(String code) {
        if (code == null || code.trim().isEmpty()) {
            return LanguageType.UNKNOWN;
        }

        // Limpiar código para análisis
        String cleanCode = code.trim();

        // Verificar HTML primero (más específico)
        if (HTML_PATTERN.matcher(cleanCode).matches()) {
            return LanguageType.HTML;
        }

        // Verificar PL/SQL
        if (PLSQL_PATTERN.matcher(cleanCode).matches()) {
            return LanguageType.PLSQL;
        }

        // Verificar Python
        if (PYTHON_PATTERN.matcher(cleanCode).matches()) {
            return LanguageType.PYTHON;
        }

        return LanguageType.UNKNOWN;
    }
}
