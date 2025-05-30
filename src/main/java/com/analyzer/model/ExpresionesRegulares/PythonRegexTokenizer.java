package com.analyzer.model.ExpresionesRegulares;

import java.util.*;
import java.util.regex.*;
import com.analyzer.model.Token;

/**
 * Tokenizador basado en expresiones regulares para Python.
 * VERSIÓN CORREGIDA con patrones y constructor de Token arreglados.
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
        lista.add(new PatronToken("ERROR_COMMENT", "/\\#[^\\r\\n]*"));
        lista.add(new PatronToken("COMMENT", "#[^\\r\\n]*"));

        // --- Cadenas - PATRONES CORREGIDOS ---
        // Error: cadena doble sin cerrar (hasta fin de línea, NO hasta fin de archivo)
        lista.add(new PatronToken("ERROR_CADENA_DOUBLE", "\"[^\"\\n]*(?=\\n|$)"));
        // Error: cadena simple sin cerrar (hasta fin de línea, NO hasta fin de archivo)
        lista.add(new PatronToken("ERROR_CADENA_SINGLE", "'[^'\\n]*(?=\\n|$)"));
        // Cadenas válidas
        lista.add(new PatronToken("STRING", "\"(?:\\\\.|[^\"\\\\])*\""));
        lista.add(new PatronToken("STRING", "'(?:\\\\.|[^'\\\\])*'"));

        // --- Números - ORDEN Y PATRONES CORREGIDOS ---
        // ERRORES PRIMERO
        // ERROR PRIMERO: identificador que empieza con número
        lista.add(new PatronToken("ERROR_IDENTIFICADOR", "\\d+[A-Za-z_][A-Za-z0-9_]*"));
        lista.add(new PatronToken("ERROR_NUMERO_MULT_PUNTOS", "\\d+(?:\\.\\d+){2,}"));
        lista.add(new PatronToken("ERROR_LIT_BINARIO", "0[bB][01]*[2-9]+[0-9]*"));
        // NÚMEROS VÁLIDOS DESPUÉS
        lista.add(new PatronToken("NUMBER", "\\d+\\.\\d+"));
        lista.add(new PatronToken("NUMBER", "\\d+"));

        // --- Identificadores - ORDEN CORREGIDO ---

        // Palabras clave
        lista.add(new PatronToken("KEYWORD", "\\b(?:def|print|return|if|else|elif|for|while|class|import|from|as|pass|break|continue|in|and|or|not|is|None|True|False|with|yield|try|except|finally|raise|lambda)\\b"));
        // Identificadores válidos
        lista.add(new PatronToken("IDENTIFICADOR", "[A-Za-z_][A-Za-z0-9_]*"));

        // --- Operadores - ORDEN CORREGIDO ---
        // ERRORES PRIMERO
        lista.add(new PatronToken("ERROR_OPERADOR_SEQ", "(?:\\+{3,}|\\-{3,}|\\={3,}|\\*{3,}|\\/{3,}|\\^{2,}|\\&{2,}|\\|{2,}|\\>{2,}|\\<{2,})"));
        // Operadores compuestos
        lista.add(new PatronToken("OPERATOR", "==|!=|<=|>=|\\*\\*|//|\\+=|-=|\\*=|/=|%=|//=|\\*\\*=|<<|>>"));
        // Operadores simples
        lista.add(new PatronToken("OPERATOR", "[+\\-*/%<>=:.,@]"));

        // --- Separadores ---
        lista.add(new PatronToken("SEPARATOR", "[()\\[\\]{}]"));

        // --- Espacios y saltos ---
        lista.add(new PatronToken("ESPACIO", "[ \\t]+"));
        lista.add(new PatronToken("SALTO_LINEA", "\\r?\\n"));

        // --- Token genérico inválido ---
        lista.add(new PatronToken("INVALIDO", "."));

        patrones = Collections.unmodifiableList(lista);
    }

    /**
     * Tokeniza el código Python en una lista de Tokens.
     */
    public List<Token> tokenize(String codigo) {
        List<Token> tokens = new ArrayList<>();
        int longitud = codigo.length();
        int pos = 0;
        int line = 1;
        int column = 1;

        System.out.println("=== INICIANDO TOKENIZACIÓN ===");
        System.out.println("Código a tokenizar: '" + codigo + "'");

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

                    // Agregar token (omitir espacios y saltos) - CONSTRUCTOR CORRECTO
                    if (!pt.tipo.equals("ESPACIO") && !pt.tipo.equals("SALTO_LINEA")) {
                        // Usar el constructor: Token(valor, tipo, línea, columna)
                        Token token = new Token(lexema, pt.tipo, line, column);
                        tokens.add(token);
                        System.out.println("Token creado: valor='" + lexema + "', tipo=" + pt.tipo + ", posición=" + line + ":" + column);
                    }

                    // Actualizar posición
                    for (char c : lexema.toCharArray()) {
                        if (c == '\n') {
                            line++;
                            column = 1;
                        } else {
                            column++;
                        }
                    }

                    pos += lexema.length();
                    coincidio = true;
                    break;
                }
            }

            if (!coincidio) {
                // Si no hay coincidencia, avanzar un carácter
                char c = codigo.charAt(pos);
                System.out.println("Carácter sin coincidencia: '" + c + "' en posición " + pos);
                if (c == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                pos++;
            }
        }



        return tokens;
    }
}