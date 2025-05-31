package com.analyzer.view;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import com.analyzer.model.*;
import com.analyzer.service.*;
import com.analyzer.service.LexicalAnalizer.*;
import com.analyzer.service.interfaces.*;
import java.util.*;

public class LexicalAnalyzerServiceTest {

    private LexicalAnalyzerService analyzerService;
    private Map<LanguageType, ILexicalAnalyzer> analyzers;

    @Before
    public void setUp() {
        analyzers = new HashMap<>();
        analyzers.put(LanguageType.PYTHON, new PythonLexicalAnalyzer());
        analyzers.put(LanguageType.PLSQL, new SQLLexicalAnalyzer());
        analyzers.put(LanguageType.HTML, new HTMLLexicalAnalyzer());

        // Implementación simple del detector de lenguaje para pruebas
        ILanguageDetector detector = code -> {
            if (code.contains("def ") || code.contains("print(")) return LanguageType.PYTHON;
            if (code.contains("SELECT ") || code.contains("FROM ")) return LanguageType.PLSQL;
            if (code.contains("<html>") || code.contains("</")) return LanguageType.HTML;
            return LanguageType.UNKNOWN;
        };

        analyzerService = new LexicalAnalyzerService(detector, analyzers);
    }

    @Test
    public void testPythonAnalysis() {
        String pythonCode = "def suma(a, b):\n    return a + b\n";
        List<AnalysisError> errors = new ArrayList<>();
        List<Token> tokens = analyzerService.analyzeLexical(pythonCode, errors);

        assertNotNull("Los tokens no deberían ser null", tokens);
        assertTrue("No deberían haber errores", errors.isEmpty());
        assertTrue("Debería haber al menos 7 tokens", tokens.size() >= 7);
    }

    @Test
    public void testSQLAnalysis() {
        String sqlCode = "SELECT id FROM users;"; // Consulta más simple para pruebas
        List<AnalysisError> errors = new ArrayList<>();
        List<Token> tokens = analyzerService.analyzeLexical(sqlCode, errors);

        assertNotNull("Los tokens no deberían ser null", tokens);
        assertTrue("No deberían haber errores", errors.isEmpty());
        assertTrue("Debería haber al menos 4 tokens", tokens.size() >= 4);
    }

//    @Test
//    public void testHTMLAnalysis() {
//        String htmlCode = "<html><body><h1>Título</h1></body></html>";
//        List<AnalysisError> errors = new ArrayList<>();
//        List<Token> tokens = analyzerService.analyzeLexical(htmlCode, errors);
//
//        assertNotNull("Los tokens no deberían ser null", tokens);
//        assertTrue("No deberían haber errores", errors.isEmpty());
//    }

    @Test
    public void testInvalidCode() {
        String invalidCode = "@#$%^";
        List<AnalysisError> errors = new ArrayList<>();
        List<Token> tokens = analyzerService.analyzeLexical(invalidCode, errors);

        assertNotNull("Los tokens no deberían ser null", tokens);
        assertFalse("Deberían haber errores", errors.isEmpty());
    }
}
