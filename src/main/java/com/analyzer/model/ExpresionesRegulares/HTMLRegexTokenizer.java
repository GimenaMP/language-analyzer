package com.analyzer.model.ExpresionesRegulares;

import java.util.*;
import java.util.regex.*;
import com.analyzer.model.Token;

/**
 * Tokenizador basado en expresiones regulares para HTML.
 * Incluye tokens válidos, palabras reservadas y categorías de error para diagnóstico.
 */
public class HTMLRegexTokenizer {

    private static class TokenPattern {
        final Pattern pattern;
        final String type;
        TokenPattern(String type, String regex) {
            this.type = type;
            this.pattern = Pattern.compile("^(" + regex + ")", Pattern.CASE_INSENSITIVE);
        }
    }

    private static final List<TokenPattern> tokenPatterns;
    static {
        List<TokenPattern> patterns = new ArrayList<>();
        // 1. Comentarios: errores primero, luego válidos
        patterns.add(new TokenPattern("ERROR_COMENTARIO", "<!--(?:[^-]|-(?!->))*$") );
        patterns.add(new TokenPattern("COMENTARIO", "<!--(?:[^-]|-(?!->))*-->") );
        // 2. Doctype: errores primero, luego válido
        patterns.add(new TokenPattern("ERROR_DOCTYPE", "<!DOCKTYPE\\s+html>") );
        patterns.add(new TokenPattern("DOCTYPE", "<!DOCTYPE\\s+html>") );
        // 3. Etiquetas reservadas de HTML: apertura y cierre
        String reserved = "html|head|body|title|div|span|p|a|ul|ol|li|table|tr|td|th|script|style|link|meta|header|footer|nav|section|article|aside|main";
        patterns.add(new TokenPattern("TAG_RESERVADA_CIERRE", "</(?:" + reserved + ")>") );
        patterns.add(new TokenPattern("TAG_RESERVADA_ABIERTA", "<(?:" + reserved + ")\\b[^>]*>") );
        // 4. Etiquetas genéricas de apertura y cierre
        patterns.add(new TokenPattern("TAG_CIERRE", "</([A-Za-z][A-Za-z0-9]*)>") );
        patterns.add(new TokenPattern("TAG_ABIERTA", "<([A-Za-z][A-Za-z0-9]*)\\b[^>]*>") );
        // 5. Atributos: errores primero, luego válidos
   patterns.add(new TokenPattern("ERROR_ATRIBUTO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*=[^\"\s>]+") );
   patterns.add(new TokenPattern("ATRIBUTO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*=\"[^\"]*\"") );
        // 6. Entidades: errores primero, luego válidas
        patterns.add(new TokenPattern("ERROR_ENTIDAD", "&[A-Za-z]+[^;\\s]*") );
        patterns.add(new TokenPattern("ENTIDAD", "&[A-Za-z]+;") );
        // 7. Espacios y tabulaciones
        patterns.add(new TokenPattern("ESPACIO", "[ \t]+") );
        // 8. Saltos de línea
        patterns.add(new TokenPattern("SALTO_LINEA", "\\r?\\n") );
        // 9. Texto entre etiquetas (excluye signos de '<', '>', '&')
        patterns.add(new TokenPattern("TEXTO", "[^<>&]+") );
        // 10. Símbolos sueltos: '<', '>' y '/'
        patterns.add(new TokenPattern("SIMBOLO", "[<>/]") );
        // 11. Cualquier otro carácter inválido
        patterns.add(new TokenPattern("INVALIDO", ".") );
        tokenPatterns = Collections.unmodifiableList(patterns);
    }

    /**
     * Tokeniza el código HTML en una lista de Tokens.
     * Omite espacios y saltos de línea para centrarse en tokens relevantes.
     */
    public List<Token> tokenize(String html) {
        List<Token> tokens = new ArrayList<>();
        int length = html.length();
        int pos = 0;
        int line = 1, column = 1;

        while (pos < length) {
            CharSequence sub = html.subSequence(pos, length);
            boolean matched = false;
            for (TokenPattern tp : tokenPatterns) {
                Matcher m = tp.pattern.matcher(sub);
                if (m.lookingAt()) {
                    String lexeme = m.group(1);

                    if (lexeme.length() == 0) {
                        throw new RuntimeException("Expresión regular inválida: detectada coincidencia vacía con patrón " + tp.type);
                    }

                    if (!tp.type.equals("ESPACIO") && !tp.type.equals("SALTO_LINEA")) {
                        tokens.add(new Token(lexeme, tp.type, line, column));
                    }
                    pos += lexeme.length();
                    column += lexeme.length();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (html.charAt(pos) == '\n') {
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
