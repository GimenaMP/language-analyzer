

package com.analyzer.service.LexicalAnalizer;
import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Analizador léxico especializado para HTML.
 * Tokeniza etiquetas, atributos, texto y comentarios HTML.
 */
public class HTMLLexicalAnalyzer implements ILexicalAnalyzer {

    // Patrones regex para HTML
    private static final Pattern PATRON_ETIQUETA = Pattern.compile(
            "</?\\s*([a-zA-Z][a-zA-Z0-9]*)(\\s+[^>]*)?>", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATRON_COMENTARIO = Pattern.compile(
            "<!--[\\s\\S]*?-->", Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern PATRON_DOCTYPE = Pattern.compile(
            "<!DOCTYPE[^>]*>", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATRON_ATRIBUTO = Pattern.compile(
            "(\\w+)\\s*=\\s*([\"']?)([^\"'\\s>]*?)\\2"
    );

    private static final Pattern PATRON_TEXTO = Pattern.compile(
            "[^<>]+(?=<|$)"
    );

    // Conjunto de etiquetas HTML válidas
    private static final Set<String> ETIQUETAS_VALIDAS = Set.of(
            "html", "head", "title", "body", "h1", "h2", "h3", "h4", "h5", "h6",
            "p", "div", "span", "a", "img", "ul", "ol", "li", "table", "tr", "td", "th",
            "form", "input", "button", "textarea", "select", "option", "meta", "link",
            "script", "style", "br", "hr", "strong", "em", "b", "i", "section", "article",
            "nav", "aside", "header", "footer", "main"
    );
/// verificar
    @Override
    public List<Token> analyze(String code, LanguageType language) {
        List<AnalysisError> errores = new ArrayList<>();
        return analyzeLexical(code, errores);
    }


    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        List<Token> tokens = new ArrayList<>();

        if (fuente == null || fuente.trim().isEmpty()) {
            return tokens;
        }

        // Dividir en líneas para análisis línea por línea
        String[] lineas = fuente.split("\n", -1);

        for (int numeroLinea = 0; numeroLinea < lineas.length; numeroLinea++) {
            String linea = lineas[numeroLinea];
            tokenizarLineaHTML(linea, numeroLinea + 1, tokens, errores);
        }

        // Validaciones finales
        validarEstructuraFinal(tokens, errores);

        return tokens;
    }

    /**
     * Tokeniza una línea HTML completa.
     */
    private void tokenizarLineaHTML(String linea, int numeroLinea, List<Token> tokens, List<AnalysisError> errores) {
        if (linea.trim().isEmpty()) {
            return;
        }

        int posicion = 0;

        while (posicion < linea.length()) {
            // Saltar espacios en blanco
            if (Character.isWhitespace(linea.charAt(posicion))) {
                posicion++;
                continue;
            }

            // Intentar tokenización en orden de prioridad
            ResultadoToken resultado = intentarTokenizar(linea, posicion, numeroLinea, errores);

            if (resultado.exito) {
                if (resultado.token != null) {
                    tokens.add(resultado.token);
                }
                posicion = resultado.siguientePosicion;
            } else {
                // Carácter no reconocido
                registrarCaracterInvalido(linea.charAt(posicion), numeroLinea, posicion, errores);
                posicion++;
            }
        }
    }

    /**
     * Intenta tokenizar en la posición actual según prioridades.
     */
    private ResultadoToken intentarTokenizar(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        // 1. Comentarios HTML (prioridad alta)
        ResultadoToken resultado = reconocerComentario(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 2. DOCTYPE
        resultado = reconocerDoctype(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 3. Etiquetas HTML
        resultado = reconocerEtiqueta(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 4. Texto plano
        resultado = reconocerTexto(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        return new ResultadoToken(false, null, posicion);
    }

    /**
     * Reconoce comentarios HTML <!-- -->
     */
    private ResultadoToken reconocerComentario(String linea, int posicion, int numeroLinea) {
        Matcher matcher = PATRON_COMENTARIO.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String comentario = matcher.group();
            Token token = new Token(comentario, "COMENTARIO_HTML", numeroLinea, posicion);
            return new ResultadoToken(true, token, matcher.end());
        }

        return new ResultadoToken(false, null, posicion);
    }

    /**
     * Reconoce declaraciones DOCTYPE
     */
    private ResultadoToken reconocerDoctype(String linea, int posicion, int numeroLinea) {
        Matcher matcher = PATRON_DOCTYPE.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String doctype = matcher.group();
            Token token = new Token(doctype, "DOCTYPE", numeroLinea, posicion);
            return new ResultadoToken(true, token, matcher.end());
        }

        return new ResultadoToken(false, null, posicion);
    }

    /**
     * Reconoce etiquetas HTML <tag> </tag>
     */
    private ResultadoToken reconocerEtiqueta(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_ETIQUETA.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String etiquetaCompleta = matcher.group();
            String nombreEtiqueta = matcher.group(1);
            String atributos = matcher.group(2);

            // Validar etiqueta
            validarEtiqueta(nombreEtiqueta, numeroLinea, posicion, errores);

            // Determinar tipo de etiqueta
            String tipoToken = determinarTipoEtiqueta(etiquetaCompleta);
            Token token = new Token(etiquetaCompleta, tipoToken, numeroLinea, posicion);

            // Si tiene atributos, tokenizarlos también
            if (atributos != null && !atributos.trim().isEmpty()) {
                // Los atributos se manejan como parte de la etiqueta por simplicidad
                token.getAttributes().put("atributos", atributos.trim());
            }

            return new ResultadoToken(true, token, matcher.end());
        }

        return new ResultadoToken(false, null, posicion);
    }

    /**
     * Reconoce texto plano entre etiquetas
     */
    private ResultadoToken reconocerTexto(String linea, int posicion, int numeroLinea) {
        // Buscar texto hasta la siguiente etiqueta o final de línea
        int inicioTexto = posicion;
        int finTexto = encontrarSiguienteEtiqueta(linea, posicion);

        if (finTexto > inicioTexto) {
            String texto = linea.substring(inicioTexto, finTexto).trim();
            if (!texto.isEmpty()) {
                Token token = new Token(texto, "TEXTO_HTML", numeroLinea, inicioTexto);
                return new ResultadoToken(true, token, finTexto);
            }
        }

        return new ResultadoToken(false, null, posicion);
    }

    /**
     * Valida si una etiqueta HTML es reconocida
     */
    private void validarEtiqueta(String nombreEtiqueta, int numeroLinea, int posicion, List<AnalysisError> errores) {
        if (!ETIQUETAS_VALIDAS.contains(nombreEtiqueta.toLowerCase())) {
            errores.add(new AnalysisError(
                    "Etiqueta HTML no reconocida: " + nombreEtiqueta,
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }

        // Verificar etiquetas mal escritas comunes
        Map<String, String> erroresComunes = Map.of(
                "bol", "b",
                "imagen", "img",
                "enlase", "a",
                "parrafo", "p"
        );

        String sugerencia = erroresComunes.get(nombreEtiqueta.toLowerCase());
        if (sugerencia != null) {
            errores.add(new AnalysisError(
                    "Posible error de escritura: '" + nombreEtiqueta + "', ¿quiso decir '" + sugerencia + "'?",
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }
    }

    /**
     * Determina el tipo específico de etiqueta
     */
    private String determinarTipoEtiqueta(String etiqueta) {
        if (etiqueta.startsWith("</")) {
            return "ETIQUETA_CIERRE";
        } else if (etiqueta.endsWith("/>")) {
            return "ETIQUETA_AUTOCERRANTE";
        } else {
            return "ETIQUETA_APERTURA";
        }
    }

    /**
     * Encuentra la posición de la siguiente etiqueta
     */
    private int encontrarSiguienteEtiqueta(String linea, int inicio) {
        for (int i = inicio; i < linea.length(); i++) {
            if (linea.charAt(i) == '<') {
                return i;
            }
        }
        return linea.length();
    }

    /**
     * Registra un carácter inválido
     */
    private void registrarCaracterInvalido(char caracter, int numeroLinea, int posicion, List<AnalysisError> errores) {
        errores.add(new AnalysisError(
                "Carácter no reconocido en HTML: '" + caracter + "'",
                AnalysisError.ErrorType.LEXICAL,
                numeroLinea, posicion
        ));
    }

    /**
     * Validaciones finales de estructura HTML
     */
    private void validarEstructuraFinal(List<Token> tokens, List<AnalysisError> errores) {
        // Verificar balance de etiquetas
        Stack<String> pilaEtiquetas = new Stack<>();

        for (Token token : tokens) {
            if ("ETIQUETA_APERTURA".equals(token.getType())) {
                String nombreEtiqueta = extraerNombreEtiqueta(token.getValue());
                if (!esEtiquetaAutocerrante(nombreEtiqueta)) {
                    pilaEtiquetas.push(nombreEtiqueta);
                }
            } else if ("ETIQUETA_CIERRE".equals(token.getType())) {
                String nombreEtiqueta = extraerNombreEtiqueta(token.getValue());

                if (pilaEtiquetas.isEmpty()) {
                    errores.add(new AnalysisError(
                            "Etiqueta de cierre sin apertura: " + token.getValue(),
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                } else {
                    String ultimaAbierta = pilaEtiquetas.pop();
                    if (!ultimaAbierta.equals(nombreEtiqueta)) {
                        errores.add(new AnalysisError(
                                "Etiquetas mal anidadas: esperaba </" + ultimaAbierta + "> pero encontró " + token.getValue(),
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        // Etiquetas sin cerrar
        while (!pilaEtiquetas.isEmpty()) {
            String etiquetaSinCerrar = pilaEtiquetas.pop();
            errores.add(new AnalysisError(
                    "Etiqueta sin cerrar: <" + etiquetaSinCerrar + ">",
                    AnalysisError.ErrorType.SYNTACTIC,
                    1, 0
            ));
        }
    }

    /**
     * Extrae el nombre de la etiqueta del token
     */
    private String extraerNombreEtiqueta(String etiqueta) {
        Pattern patron = Pattern.compile("</?\\s*([a-zA-Z][a-zA-Z0-9]*)");
        Matcher matcher = patron.matcher(etiqueta);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return "";
    }

    /**
     * Verifica si es una etiqueta autocerrante
     */
    private boolean esEtiquetaAutocerrante(String nombreEtiqueta) {
        Set<String> autocerrantes = Set.of("br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "source", "track", "wbr");
        return autocerrantes.contains(nombreEtiqueta.toLowerCase());
    }

    /**
     * Clase auxiliar para resultados de tokenización
     */
    private static class ResultadoToken {
        final boolean exito;
        final Token token;
        final int siguientePosicion;

        ResultadoToken(boolean exito, Token token, int siguientePosicion) {
            this.exito = exito;
            this.token = token;
            this.siguientePosicion = siguientePosicion;
        }
    }
}