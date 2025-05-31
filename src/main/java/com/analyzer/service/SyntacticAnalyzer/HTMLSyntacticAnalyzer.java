package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.model.Symbol;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class HTMLSyntacticAnalyzer implements ISyntacticAnalyzer {

    private List<AnalysisError> errors;
    private Stack<String> tagStack;
    private List<Symbol> symbolTable;
    private int currentIndex;
    private List<Token> tokens;

    // Etiquetas que no requieren cierre
    private static final Set<String> VOID_ELEMENTS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    );

    // Etiquetas que solo pueden ir en head
    private static final Set<String> HEAD_ONLY_ELEMENTS = Set.of(
            "title", "meta", "link", "style", "script", "base"
    );

    // Etiquetas que solo pueden ir en body
    private static final Set<String> BODY_ONLY_ELEMENTS = Set.of(
            "header", "main", "footer", "article", "section", "nav", "aside",
            "h1", "h2", "h3", "h4", "h5", "h6", "p", "div", "span", "a", "ul", "ol", "li"
    );

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        inicializar(tokens);
        analizarDocumento();
        return errors;
    }

    private void inicializar(List<Token> tokens) {
        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.tagStack = new Stack<>();
        this.symbolTable = new ArrayList<>();
        this.currentIndex = 0;
    }

    private void analizarDocumento() {
        // Estructura esperada: DOCTYPE? HTML_ELEMENT
        consumirEspaciosYComentarios();

        // DOCTYPE opcional
        if (currentTokenIs("DOCTYPE")) {
            consumirToken();
            agregarASimbolos("DOCTYPE", "Declaración", "documento", getCurrentToken());
        }

        consumirEspaciosYComentarios();

        // Elemento HTML obligatorio
        if (!analizarElementoHTML()) {
            agregarError("Se esperaba elemento <html>", getCurrentToken());
        }

        consumirEspaciosYComentarios();

        // Verificar que no queden tokens sin procesar (excepto espacios/comentarios)
        if (currentIndex < tokens.size()) {
            agregarError("Contenido inesperado después del elemento html", getCurrentToken());
        }

        // Verificar etiquetas sin cerrar
        verificarEtiquetasSinCerrar();
    }

    private boolean analizarElementoHTML() {
        if (!currentTokenIs("TAG_RESERVADA_ABIERTA") && !currentTokenIs("TAG_ABIERTA")) {
            return false;
        }

        Token htmlToken = getCurrentToken();
        String tagName = extraerNombreEtiqueta(htmlToken.getValue());

        if (!"html".equalsIgnoreCase(tagName)) {
            agregarError("El documento debe comenzar con <html>", htmlToken);
            return false;
        }

        // Consumir <html
        consumirToken();
        tagStack.push("html");
        agregarASimbolos("html", "Etiqueta apertura", "estructura", htmlToken);

        // Atributos opcionales
        analizarAtributos();

        // Consumir >
        if (!consumirSimbolo(">")) {
            agregarError("Se esperaba '>' después de <html", getCurrentToken());
            return false;
        }

        consumirEspaciosYComentarios();

        // HEAD obligatorio
        if (!analizarElementoHead()) {
            agregarError("Se esperaba elemento <head>", getCurrentToken());
        }

        consumirEspaciosYComentarios();

        // BODY obligatorio
        if (!analizarElementoBody()) {
            agregarError("Se esperaba elemento <body>", getCurrentToken());
        }

        consumirEspaciosYComentarios();

        // Cierre </html>
        if (!analizarCierreEtiqueta("html")) {
            agregarError("Se esperaba </html>", getCurrentToken());
            return false;
        }

        return true;
    }

    private boolean analizarElementoHead() {
        if (!esAperturaEtiqueta("head")) {
            return false;
        }

        Token headToken = getCurrentToken();
        consumirToken(); // <head
        tagStack.push("head");
        agregarASimbolos("head", "Etiqueta apertura", "estructura", headToken);

        analizarAtributos();

        if (!consumirSimbolo(">")) {
            agregarError("Se esperaba '>' después de <head", getCurrentToken());
            return false;
        }

        // Contenido de head
        while (currentIndex < tokens.size() && !esCierreEtiqueta("head")) {
            consumirEspaciosYComentarios();

            if (esCierreEtiqueta("head")) break;

            if (esAperturaEtiqueta()) {
                String tagName = extraerNombreEtiqueta(getCurrentToken().getValue());
                if (!HEAD_ONLY_ELEMENTS.contains(tagName) && !tagName.equals("script")) {
                    agregarError("La etiqueta <" + tagName + "> no puede estar en <head>", getCurrentToken());
                }
                analizarElemento();
            } else if (currentTokenIs("TEXTO")) {
                // Texto directo en head generalmente no es válido
                agregarError("Texto directo no permitido en <head>", getCurrentToken());
                consumirToken();
            } else {
                consumirToken();
            }
        }

        return analizarCierreEtiqueta("head");
    }

    private boolean analizarElementoBody() {
        if (!esAperturaEtiqueta("body")) {
            return false;
        }

        Token bodyToken = getCurrentToken();
        consumirToken(); // <body
        tagStack.push("body");
        agregarASimbolos("body", "Etiqueta apertura", "estructura", bodyToken);

        analizarAtributos();

        if (!consumirSimbolo(">")) {
            agregarError("Se esperaba '>' después de <body", getCurrentToken());
            return false;
        }

        // Contenido de body
        while (currentIndex < tokens.size() && !esCierreEtiqueta("body")) {
            consumirEspaciosYComentarios();

            if (esCierreEtiqueta("body")) break;

            if (esAperturaEtiqueta()) {
                analizarElemento();
            } else if (currentTokenIs("TEXTO")) {
                consumirToken(); // Texto válido en body
            } else if (currentTokenIs("ENTIDAD")) {
                consumirToken(); // Entidades válidas
            } else {
                consumirToken(); // Otros tokens
            }
        }

        return analizarCierreEtiqueta("body");
    }

    private void analizarElemento() {
        if (!esAperturaEtiqueta()) {
            return;
        }

        Token aperturaToken = getCurrentToken();
        String tagName = extraerNombreEtiqueta(aperturaToken.getValue());

        consumirToken(); // Consumir etiqueta de apertura
        agregarASimbolos(tagName, "Etiqueta apertura", "contenido", aperturaToken);

        // Si es void element, no debe tener contenido ni cierre
        if (VOID_ELEMENTS.contains(tagName.toLowerCase())) {
            analizarAtributos();
            if (consumirSimbolo("/")) {
                if (!consumirSimbolo(">")) {
                    agregarError("Se esperaba '>' después de '/'", getCurrentToken());
                }
            } else if (!consumirSimbolo(">")) {
                agregarError("Se esperaba '>' o '/>' para elemento vacío", getCurrentToken());
            }
            return;
        }

        // Elemento normal con posible contenido
        tagStack.push(tagName);
        analizarAtributos();

        if (!consumirSimbolo(">")) {
            agregarError("Se esperaba '>' después de la etiqueta", getCurrentToken());
            return;
        }

        // Contenido del elemento
        while (currentIndex < tokens.size() && !esCierreEtiqueta(tagName)) {
            consumirEspaciosYComentarios();

            if (esCierreEtiqueta(tagName)) break;

            if (esAperturaEtiqueta()) {
                analizarElemento();
            } else if (currentTokenIs("TEXTO")) {
                consumirToken();
            } else if (currentTokenIs("ENTIDAD")) {
                consumirToken();
            } else {
                consumirToken();
            }
        }

        // Cierre del elemento
        analizarCierreEtiqueta(tagName);
    }

    private void analizarAtributos() {
        while (currentTokenIs("ATRIBUTO")) {
            Token atributoToken = getCurrentToken();
            String[] partes = atributoToken.getValue().split("=", 2);
            if (partes.length > 0) {
                agregarASimbolos(partes[0], "Atributo", "local", atributoToken);
            }
            consumirToken();
        }
    }

    private boolean analizarCierreEtiqueta(String expectedTag) {
        if (!esCierreEtiqueta(expectedTag)) {
            return false;
        }

        Token cierreToken = getCurrentToken();
        String tagName = extraerNombreEtiqueta(cierreToken.getValue());

        if (!tagStack.isEmpty() && tagStack.peek().equalsIgnoreCase(expectedTag)) {
            tagStack.pop();
        } else {
            agregarError("Cierre inesperado: " + cierreToken.getValue(), cierreToken);
        }

        agregarASimbolos(tagName, "Etiqueta cierre", "estructura", cierreToken);
        consumirToken();
        return true;
    }

    // Métodos auxiliares
    private boolean currentTokenIs(String type) {
        return currentIndex < tokens.size() &&
                tokens.get(currentIndex).getType().equals(type);
    }

    private Token getCurrentToken() {
        return currentIndex < tokens.size() ? tokens.get(currentIndex) : null;
    }

    private void consumirToken() {
        if (currentIndex < tokens.size()) {
            currentIndex++;
        }
    }

    private boolean consumirSimbolo(String simbolo) {
        if (currentTokenIs("SIMBOLO") &&
                getCurrentToken().getValue().equals(simbolo)) {
            consumirToken();
            return true;
        }
        return false;
    }

    private void consumirEspaciosYComentarios() {
        while (currentIndex < tokens.size()) {
            String type = getCurrentToken().getType();
            if (type.equals("ESPACIO") || type.equals("SALTO_LINEA") ||
                    type.equals("COMENTARIO")) {
                if (type.equals("COMENTARIO")) {
                    agregarASimbolos("comentario", "Comentario", "global", getCurrentToken());
                }
                consumirToken();
            } else {
                break;
            }
        }
    }

    private boolean esAperturaEtiqueta() {
        return currentTokenIs("TAG_RESERVADA_ABIERTA") || currentTokenIs("TAG_ABIERTA");
    }

    private boolean esAperturaEtiqueta(String tagName) {
        if (!esAperturaEtiqueta()) return false;
        String currentTag = extraerNombreEtiqueta(getCurrentToken().getValue());
        return currentTag.equalsIgnoreCase(tagName);
    }

    private boolean esCierreEtiqueta(String tagName) {
        if (!currentTokenIs("TAG_RESERVADA_CIERRE") && !currentTokenIs("TAG_CIERRE")) {
            return false;
        }
        String currentTag = extraerNombreEtiqueta(getCurrentToken().getValue());
        return currentTag.equalsIgnoreCase(tagName);
    }

    private void verificarEtiquetasSinCerrar() {
        while (!tagStack.isEmpty()) {
            String unclosedTag = tagStack.pop();
            agregarError("Etiqueta sin cerrar: <" + unclosedTag + ">", null);
        }
    }

    private void agregarError(String mensaje, Token token) {
        if (token != null) {
            errors.add(new AnalysisError(mensaje, AnalysisError.ErrorType.SYNTACTIC,
                    token.getLine(), token.getColumn()));
        } else {
            errors.add(new AnalysisError(mensaje, AnalysisError.ErrorType.SYNTACTIC));
        }
    }

    private void agregarASimbolos(String nombre, String tipoDisplay, String contexto, Token token) {
        if (token == null) return;

        Symbol.SymbolType tipo = Symbol.SymbolType.fromDisplayName(tipoDisplay);

        boolean exists = symbolTable.stream()
                .anyMatch(symbol ->
                        symbol.getName() != null &&
                                symbol.getSymbolType() != null &&
                                symbol.getName().equals(nombre) &&
                                symbol.getSymbolType() == tipo
                );

        if (!exists) {
            symbolTable.add(new Symbol(nombre, tipoDisplay, contexto,
                    token.getLine(), token.getColumn()));
        }
    }

    private String extraerNombreEtiqueta(String tag) {
        String limpio = tag.replaceAll("[<>/]+", "").trim();
        String[] partes = limpio.split("\\s+");
        return partes.length > 0 && !partes[0].isEmpty() ? partes[0] : "desconocido";
    }

    public List<Symbol> getSymbolTable() {
        System.out.println("DEBUG HTML - Total símbolos: " + symbolTable.size());
        return Collections.unmodifiableList(new ArrayList<>(symbolTable));
    }
}
