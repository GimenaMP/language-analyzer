package com.analyzer.model.ExpresionesRegulares;

import java.util.*;
import java.util.regex.*;
import com.analyzer.model.Token;

/**
 * Tokenizador basado en expresiones regulares para Python.
 * Incluye tokens de error y válidos, con patrones agrupados: error primero, luego válido.
 */
public class PythonRegexTokenizer {

    private static class PatronToken {
        final Pattern patron;
        final String tipo;
        PatronToken(String tipo, String regex) {
            this.tipo = tipo;
            this.patron = Pattern.compile("^(" + regex + ")");
        }
    }

    private static final List<PatronToken> patrones;
    static {
        List<PatronToken> lista = new ArrayList<>();
        // --- Comentarios ---
        lista.add(new PatronToken("ERROR_COMMENT", "\\/\\#[^\\r\\n]*"));
        lista.add(new PatronToken("COMMENT", "#[^\\r\\n]*"));
        // --- Cadenas ---
        lista.add(new PatronToken("ERROR_CADENA_DOUBLE", "\"(\\\\.|[^\"\\\\])*$"));
        lista.add(new PatronToken("ERROR_CADENA_SINGLE", "'(\\\\.|[^'\\\\])*$"));
        lista.add(new PatronToken("STRING", "\"(\\\\.|[^\"\\\\])*\""));
        lista.add(new PatronToken("STRING", "'(\\\\.|[^'\\\\])*'"));
        // --- Palabras clave ---
    lista.add(new PatronToken("ERROR_IDENTIFICADOR", "[0-9]+([A-Za-z][A-Za-z0-9_]*|=)"));
        lista.add(new PatronToken("KEYWORD", "\\b(def|print|return|if|else|elif|for|while|class|import|from|as|pass|break|continue|in|and|or|not|is|None|True|False|with|yield|try|except|finally|raise|lambda)\\b"));
        lista.add(new PatronToken("ERROR_NUMERO_MULT_PUNTOS", "\\d+\\.\\d+\\.\\d+"));
        lista.add(new PatronToken("ERROR_LIT_BINARIO", "0[bB][0-1]*[2-9][0-9]*"));
        lista.add(new PatronToken("NUMBER", "\\d+\\.\\d+"));
        lista.add(new PatronToken("NUMBER", "\\d+"));
        lista.add(new PatronToken("IDENTIFICADOR", "[A-Za-z_][A-Za-z0-9_]*"));
        // --- Números ---

        // --- Identificadores ---

        // --- Operadores ---
        lista.add(new PatronToken("ERROR_OPERADOR_SEQ", "(\\+{2,}|\\-{2,}|\\={2,}|\\*{2,}|\\/{2,}|\\^{2,}|\\&{2,}|\\|{2,}|\\>{2,}|\\<{2,})"));
        lista.add(new PatronToken("OPERATOR", "==|!=|<=|>=|\\*\\*|//|\\+=|-=|\\*=|/=|%=|//=|\\*\\*=|<<|>>"));
        lista.add(new PatronToken("OPERATOR", "\\+|\\-|\\*|\\/|%|<|>|=|:|\\.|,|@"));
        // --- Separadores ---
        lista.add(new PatronToken("SEPARATOR", "\\(|\\)|\\[|\\]|\\{|\\}"));
        // --- Espacios y saltos ---
        lista.add(new PatronToken("ESPACIO", "[ \\t]+"));
        lista.add(new PatronToken("SALTO_LINEA", "\\r?\\n"));
        // --- Token genérico inválido ---
        lista.add(new PatronToken("INVALIDO", "."));
        patrones = Collections.unmodifiableList(lista);
    }

    private static final Map<String, String> ERROR_PATTERNS = new HashMap<>() {{
        put("ERROR_COMMENT", "#[^\\n]*(?!\\n)");
        put("INVALID_COMMENT", "#[^\\n]*\\\\(?!\\n)");
        put("ERROR_CADENA_DOUBLE", "\"[^\"\\n]*(?!\"\\n)");
        put("ERROR_CADENA_SINGLE", "'[^'\\n]*(?!'\\n)");
        put("ERROR_NUMERO_MULT_PUNTOS", "\\d+\\.\\d+\\.\\d+");
        put("ERROR_LIT_BINARIO", "0[bB][^01]+");
        put("ERROR_IDENTIFICADOR", "\\d+[a-zA-Z_][a-zA-Z0-9_]*");
        put("ERROR_OPERADOR_SEQ", "[+\\-*/<>=!&|^~%]+(?![-=<>!&|])");
    }};

    public List<Token> tokenize(String code) {
        List<Token> tokens = new ArrayList<>();
        int line = 1;
        int column = 0;

        String[] lines = code.split("\n");
        for (String currentLine : lines) {
            column = 0;
            String remainingLine = currentLine;

            while (!remainingLine.trim().isEmpty()) {
                Token token = null;
                
                // Primero buscar errores
                for (Map.Entry<String, String> error : ERROR_PATTERNS.entrySet()) {
                    Pattern errorPattern = Pattern.compile("^(" + error.getValue() + ")");
                    Matcher matcher = errorPattern.matcher(remainingLine);
                    
                    if (matcher.find()) {
                        String value = matcher.group(1);
                        token = new Token(value, error.getKey(), line, column);
                        column += value.length();
                        remainingLine = remainingLine.substring(value.length()).trim();
                        break;
                    }
                }

                // Si no se encontró un error, continuar con el tokenizado normal
                if (token == null) {
                    // ...existing tokenization code...
                }

                if (token != null) {
                    tokens.add(token);
                }
            }
            line++;
        }
        return tokens;
    }
}
