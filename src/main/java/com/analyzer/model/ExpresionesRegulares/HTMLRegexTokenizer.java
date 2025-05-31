package com.analyzer.model.ExpresionesRegulares;

import java.util.*;
import java.util.regex.*;
import com.analyzer.model.Token;

/**
 * Tokenizador basado en expresiones regulares para HTML.
 * Versión mejorada con tokens adicionales para análisis más preciso.
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

        // CDATA (debe ir antes que comentarios)
        patterns.add(new TokenPattern("CDATA", "<!\\[CDATA\\[.*?\\]\\]>"));

        // Instrucciones de procesamiento XML
        patterns.add(new TokenPattern("PROCESAMIENTO_XML", "<\\?xml\\s[^>]*\\?>"));

        // Comentarios
        patterns.add(new TokenPattern("ERROR_COMENTARIO", "<!--.*[^\r\n]*$"));
        patterns.add(new TokenPattern("COMENTARIO", "<!--(?:[^-]|-(?!->))*-->"));

        // Doctype
        patterns.add(new TokenPattern("ERROR_DOCTYPE", "<!DOCKTYPE\\s+html>"));
        patterns.add(new TokenPattern("DOCTYPE", "<!DOCTYPE\\s+html>"));

        // Etiquetas auto-cerradas (debe ir antes que etiquetas normales)
        String voidElements = "area|base|br|col|embed|hr|img|input|link|meta|param|source|track|wbr";
        patterns.add(new TokenPattern("TAG_VOID_SELF_CLOSED", "<(" + voidElements + ")\\b[^>]*/\\s*>"));
        patterns.add(new TokenPattern("TAG_VOID_OPEN", "<(" + voidElements + ")\\b"));

        // Etiquetas reservadas (apertura y cierre)
        String reserved = "html|head|body|title|div|span|p|a|ul|ol|li|table|tr|td|th|script|style|link|meta|header|footer|nav|section|article|aside|main|h1|h2|h3|h4|h5|h6";
        patterns.add(new TokenPattern("TAG_RESERVADA_CIERRE", "</(" + reserved + ")>"));
        patterns.add(new TokenPattern("TAG_RESERVADA_ABIERTA", "<(" + reserved + ")\\b"));

        // Etiquetas genéricas auto-cerradas
        patterns.add(new TokenPattern("TAG_SELF_CLOSED", "<[A-Za-z][A-Za-z0-9]*[^>]*/\\s*>"));

        // Etiquetas genéricas
        patterns.add(new TokenPattern("TAG_CIERRE", "</[A-Za-z][A-Za-z0-9]*>"));
        patterns.add(new TokenPattern("TAG_ABIERTA", "<[A-Za-z][A-Za-z0-9]*"));

        // Etiquetas malformadas (sin cierre >)
        patterns.add(new TokenPattern("ERROR_TAG_ABIERTA", "<[A-Za-z][A-Za-z0-9]*[^>]*$"));

        // Atributos específicos con validación mejorada

        // URLs en atributos (href, src)
        patterns.add(new TokenPattern("ATRIBUTO_URL",
                "(href|src)\\s*=\\s*\"(https?://[^\"]*|mailto:[^\"]*|tel:[^\"]*|#[^\"]*|/[^\"]*|\\./[^\"]*|\\.\\./ [^\"]*|[^\"]*\\.[a-zA-Z]{2,}[^\"]*)\""
        ));

        // IDs válidos
        patterns.add(new TokenPattern("ATRIBUTO_ID",
                "id\\s*=\\s*\"[a-zA-Z][a-zA-Z0-9_-]*\""
        ));

        // Clases CSS
        patterns.add(new TokenPattern("ATRIBUTO_CLASS",
                "class\\s*=\\s*\"[a-zA-Z0-9\\s_-]+\""
        ));

        // Números (para width, height, etc.)
        patterns.add(new TokenPattern("ATRIBUTO_NUMERICO",
                "(width|height|size|colspan|rowspan|tabindex)\\s*=\\s*\"\\d+\""
        ));

        // Atributos booleanos
        patterns.add(new TokenPattern("ATRIBUTO_BOOLEANO",
                "(required|disabled|checked|selected|multiple|readonly|autofocus|autoplay|controls|defer|hidden|loop|muted)\\s*=\\s*\"(true|false|)\""
        ));

        // Atributos de tipo input
        patterns.add(new TokenPattern("ATRIBUTO_INPUT_TYPE",
                "type\\s*=\\s*\"(text|email|password|number|tel|url|search|submit|button|checkbox|radio|file|hidden|date|time|datetime-local|month|week|color|range)\""
        ));

        // Atributos target
        patterns.add(new TokenPattern("ATRIBUTO_TARGET",
                "target\\s*=\\s*\"(_blank|_self|_parent|_top)\""
        ));

        // Atributos rel
        patterns.add(new TokenPattern("ATRIBUTO_REL",
                "rel\\s*=\\s*\"(stylesheet|icon|canonical|nofollow|noopener|noreferrer|alternate|author|bookmark|help|license|next|prev|search|tag)\""
        ));

        // Errores específicos de atributos
        patterns.add(new TokenPattern("ERROR_ATRIBUTO_ID", "id\\s*=\\s*\"[^a-zA-Z][^\"]*\""));
        patterns.add(new TokenPattern("ERROR_ATRIBUTO_URL", "(href|src)\\s*=\\s*\"[^\"]*\""));
        patterns.add(new TokenPattern("ERROR_ATRIBUTO_VACIO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*\\s*=\\s*\"\""));

        // Atributos válidos generales (con comillas)
        patterns.add(new TokenPattern("ATRIBUTO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*\\s*=\\s*\"[^\"]*\""));

        // Atributos sin comillas (error)
        patterns.add(new TokenPattern("ERROR_ATRIBUTO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*\\s*=\\s*[^\"\\s>]+"));

        // Entidades HTML específicas
        patterns.add(new TokenPattern("ENTIDAD_COMUN", "&(amp|lt|gt|quot|apos|nbsp|copy|reg|trade|euro|hellip|mdash|ndash|lsquo|rsquo|ldquo|rdquo);"));
        patterns.add(new TokenPattern("ENTIDAD_NUMERICA", "&#[0-9]+;"));
        patterns.add(new TokenPattern("ENTIDAD_HEX", "&#x[0-9a-fA-F]+;"));

        // Entidades malformadas
        patterns.add(new TokenPattern("ERROR_ENTIDAD", "&[A-Za-z0-9]+[^;\\s]*"));
        patterns.add(new TokenPattern("ENTIDAD", "&[A-Za-z]+;"));

        // Valores literales
        patterns.add(new TokenPattern("NUMERO", "\\d+(\\.\\d+)?"));
        patterns.add(new TokenPattern("STRING_LITERAL", "\"[^\"]*\""));
        patterns.add(new TokenPattern("STRING_LITERAL_SIMPLE", "'[^']*'"));

        // Símbolos específicos
        patterns.add(new TokenPattern("CIERRE_AUTO", "/>"));
        patterns.add(new TokenPattern("SIMBOLO", "[<>/=]"));

        // URLs independientes en texto
        patterns.add(new TokenPattern("URL_TEXTO", "https?://[^\\s<>\"']+"));

        // Emails independientes en texto
        patterns.add(new TokenPattern("EMAIL_TEXTO", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));

        // Texto visible (mejorado para no capturar URLs/emails)
        patterns.add(new TokenPattern("TEXTO", "[^<>&=\"\n\r\\s]+"));

        // Espacios y saltos de línea
        patterns.add(new TokenPattern("ESPACIO", "[ \t]+"));
        patterns.add(new TokenPattern("SALTO_LINEA", "\\r?\\n"));

        // Token inválido
        patterns.add(new TokenPattern("INVALIDO", "."));

        tokenPatterns = Collections.unmodifiableList(patterns);
    }

    /**
     * Tokeniza el código HTML en una lista de Tokens.
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
                        throw new RuntimeException("Expresión regular inválida: coincidencia vacía en patrón " + tp.type);
                    }

                    // Solo agregar tokens significativos (filtrar espacios y saltos de línea)
                    if (!tp.type.equals("ESPACIO") && !tp.type.equals("SALTO_LINEA")) {
                        tokens.add(new Token(lexeme, tp.type, line, column));
                    }

                    // Actualizar posición
                    for (char c : lexeme.toCharArray()) {
                        if (c == '\n') {
                            line++;
                            column = 1;
                        } else {
                            column++;
                        }
                    }

                    pos += lexeme.length();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                // Manejar caracteres no reconocidos
                char currentChar = html.charAt(pos);
                if (currentChar == '\n') {
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

    /**
     * Obtiene información sobre los patrones de tokens disponibles.
     * Útil para debugging y documentación.
     */
    public static List<String> getAvailableTokenTypes() {
        return tokenPatterns.stream()
                .map(tp -> tp.type)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Verifica si un tipo de token específico existe.
     */
    public static boolean hasTokenType(String tokenType) {
        return tokenPatterns.stream()
                .anyMatch(tp -> tp.type.equals(tokenType));
    }
}//package com.analyzer.model.ExpresionesRegulares;
//
//import java.util.*;
//import java.util.regex.*;
//import com.analyzer.model.Token;
//
///**
// * Tokenizador basado en expresiones regulares para HTML.
// * Mejorado para tokenizar etiquetas, atributos y símbolos por separado.
// */
//public class HTMLRegexTokenizer {
//
//    private static class TokenPattern {
//        final Pattern pattern;
//        final String type;
//        TokenPattern(String type, String regex) {
//            this.type = type;
//            this.pattern = Pattern.compile("^(" + regex + ")", Pattern.CASE_INSENSITIVE);
//        }
//    }
//
//    private static final List<TokenPattern> tokenPatterns;
//    static {
//        List<TokenPattern> patterns = new ArrayList<>();
//
//        // Comentarios
//        patterns.add(new TokenPattern("ERROR_COMENTARIO", "<!--.*[^\r\n]*$"));
//        patterns.add(new TokenPattern("COMENTARIO", "<!--(?:[^-]|-(?!->))*-->"));
//
//        // Doctype
//        patterns.add(new TokenPattern("ERROR_DOCTYPE", "<!DOCKTYPE\\s+html>"));
//        patterns.add(new TokenPattern("DOCTYPE", "<!DOCTYPE\\s+html>"));
//
//        // Etiquetas reservadas (apertura y cierre)
//        String reserved = "html|head|body|title|div|span|p|a|ul|ol|li|table|tr|td|th|script|style|link|meta|header|footer|nav|section|article|aside|main";
//        patterns.add(new TokenPattern("TAG_RESERVADA_CIERRE", "</(" + reserved + ")>"));
//        patterns.add(new TokenPattern("TAG_RESERVADA_ABIERTA", "<(" + reserved + ")\\b"));
//
//        // Etiquetas genéricas
//        patterns.add(new TokenPattern("TAG_CIERRE", "</[A-Za-z][A-Za-z0-9]*>"));
//        patterns.add(new TokenPattern("TAG_ABIERTA", "<[A-Za-z][A-Za-z0-9]*"));
//
//        // Atributos válidos e inválidos
//        patterns.add(new TokenPattern("ERROR_ATRIBUTO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*=[^\"\s>]+"));
//        patterns.add(new TokenPattern("ATRIBUTO", "[A-Za-z_:][A-Za-z0-9_:.\\-]*=\"[^\"]*\""));
//
//
//        // Entidades HTML
//        patterns.add(new TokenPattern("ERROR_ENTIDAD", "&[A-Za-z]+[^;\\s]*"));
//        patterns.add(new TokenPattern("ENTIDAD", "&[A-Za-z]+;"));
//
//
//        // Símbolos individuales
//        patterns.add(new TokenPattern("SIMBOLO", "[<>/=]"));
//
//        // Texto visible
//        patterns.add(new TokenPattern("TEXTO", "[^<>&=\"\n\r]+"));
//
//        // Espacios y saltos de línea
//        patterns.add(new TokenPattern("ESPACIO", "[ \t]+"));
//        patterns.add(new TokenPattern("SALTO_LINEA", "\\r?\\n"));
//
//        // Token inválido
//        patterns.add(new TokenPattern("INVALIDO", "."));
//
//        tokenPatterns = Collections.unmodifiableList(patterns);
//    }
//
//    /**
//     * Tokeniza el código HTML en una lista de Tokens.
//     */
//    public List<Token> tokenize(String html) {
//        List<Token> tokens = new ArrayList<>();
//        int length = html.length();
//        int pos = 0;
//        int line = 1, column = 1;
//
//        while (pos < length) {
//            CharSequence sub = html.subSequence(pos, length);
//            boolean matched = false;
//            for (TokenPattern tp : tokenPatterns) {
//                Matcher m = tp.pattern.matcher(sub);
//                if (m.lookingAt()) {
//                    String lexeme = m.group(1);
//
//                    if (lexeme.length() == 0) {
//                        throw new RuntimeException("Expresión regular inválida: coincidencia vacía en patrón " + tp.type);
//                    }
//
//                    if (!tp.type.equals("ESPACIO") && !tp.type.equals("SALTO_LINEA")) {
//                        tokens.add(new Token(lexeme, tp.type, line, column));
//                    }
//
//                    for (char c : lexeme.toCharArray()) {
//                        if (c == '\n') {
//                            line++;
//                            column = 1;
//                        } else {
//                            column++;
//                        }
//                    }
//
//                    pos += lexeme.length();
//                    matched = true;
//                    break;
//                }
//            }
//            if (!matched) {
//                if (html.charAt(pos) == '\n') {
//                    line++;
//                    column = 1;
//                } else {
//                    column++;
//                }
//                pos++;
//            }
//        }
//        return tokens;
//    }
//}