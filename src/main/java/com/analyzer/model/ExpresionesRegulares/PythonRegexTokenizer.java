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

    /**
     * Tokeniza el código Python en una lista de Tokens.
     * Omite espacios y saltos de línea.
     */
    public List<Token> tokenize(String codigo) {
        List<Token> tokens = new ArrayList<>();
        int longitud = codigo.length();
        int pos = 0;
        while (pos < longitud) {
            CharSequence sub = codigo.subSequence(pos, longitud);
            boolean coincidio = false;
            for (PatronToken pt : patrones) {
                Matcher matcher = pt.patron.matcher(sub);
                if (matcher.lookingAt()) {
                    String lexema = matcher.group(1);

                    if (lexema.length() == 0) {
                        throw new RuntimeException("Expresión regular inválida: detectada coincidencia vacía con patrón " + pt.tipo);
                    }

                    if (!pt.tipo.equals("ESPACIO") && !pt.tipo.equals("SALTO_LINEA")) {
                        tokens.add(new Token(pt.tipo, lexema));
                    }
                    pos += lexema.length();
                    coincidio = true;
                    break;
                }
            }
            if (!coincidio) pos++;
        }
        return tokens;
    }
}
