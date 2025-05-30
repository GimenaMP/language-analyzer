package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;
import java.util.regex.Pattern;

public class HTMLSemanticAnalyzer implements ISemanticAnalyzer {

    private Map<String, Symbol> symbolTable;
    private List<AnalysisError> errors;
    private List<Token> tokens;

// Atributos válidos por etiqueta
private static final Map<String, Set<String>> VALID_ATTRIBUTES = Map.ofEntries(
        Map.entry("a", Set.of("href", "target", "rel", "title", "download", "id", "class", "style")),
        Map.entry("img", Set.of("src", "alt", "width", "height", "title", "id", "class", "style")),
        Map.entry("div", Set.of("id", "class", "style", "title", "data-*")),
        Map.entry("span", Set.of("id", "class", "style", "title")),
        Map.entry("p", Set.of("id", "class", "style", "title")),
        Map.entry("input", Set.of("type", "name", "value", "placeholder", "required", "id", "class", "style")),
        Map.entry("meta", Set.of("charset", "name", "content", "http-equiv")),
        Map.entry("link", Set.of("rel", "href", "type", "media")),
        Map.entry("script", Set.of("src", "type", "async", "defer")),
        Map.entry("style", Set.of("type", "media")),
        Map.entry("title", Set.of()),
        Map.entry("html", Set.of("lang", "dir")),
        Map.entry("head", Set.of()),
        Map.entry("body", Set.of("id", "class", "style", "onload"))
);

    // Atributos obligatorios por etiqueta
    private static final Map<String, Set<String>> REQUIRED_ATTRIBUTES = Map.of(
            "img", Set.of("src", "alt"),
            "a", Set.of("href"),
            "meta", Set.of("charset")  // Para charset meta tag
    );

    // Valores válidos para ciertos atributos
    private static final Map<String, Set<String>> VALID_ATTRIBUTE_VALUES = Map.of(
            "target", Set.of("_blank", "_self", "_parent", "_top"),
            "rel", Set.of("stylesheet", "icon", "canonical", "nofollow", "noopener", "noreferrer"),
            "type", Set.of("text/css", "text/javascript", "application/javascript", "text", "email", "password", "submit", "button"),
            "input-type", Set.of("text", "email", "password", "number", "tel", "url", "search", "submit", "button", "checkbox", "radio", "file", "hidden")
    );

    // Patrones de validación
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://|mailto:|tel:|#|/|\\./|\\.\\./).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> symbolTable) {
        inicializar(tokens, symbolTable);

        // Análisis semántico principal
        validarEstructuraSemantica();
        validarAtributos();
        validarAccesibilidad();
        validarUnicidadElementos();
        validarReferenciasSemánticas();
        validarJerarquiaHeadings();

        return errors;
    }

    private void inicializar(List<Token> tokens, Map<String, Symbol> symbolTable) {
        this.tokens = tokens;
        this.symbolTable = new HashMap<>(symbolTable);
        this.errors = new ArrayList<>();
    }

    private void validarEstructuraSemantica() {
        boolean hasTitle = false;
        boolean hasLang = false;
        boolean hasCharset = false;

        for (Token token : tokens) {
            if (token.getType().equals("TAG_RESERVADA_ABIERTA") || token.getType().equals("TAG_ABIERTA")) {
                String tagName = extraerNombreEtiqueta(token.getValue());

                switch (tagName.toLowerCase()) {
                    case "title":
                        if (hasTitle) {
                            agregarError("Múltiples elementos <title> encontrados. Solo debe haber uno.",
                                    token, AnalysisError.ErrorType.SEMANTIC);
                        }
                        hasTitle = true;
                        break;
                    case "html":
                        // Verificar si tiene atributo lang
                        if (tieneAtributoLang(token)) {
                            hasLang = true;
                        }
                        break;
                }
            } else if (token.getType().equals("ATRIBUTO")) {
                String attr = token.getValue().toLowerCase();
                if (attr.startsWith("charset=")) {
                    hasCharset = true;
                }
            }
        }

        if (!hasTitle) {
            agregarError("Falta elemento <title> obligatorio en el documento",
                    null, AnalysisError.ErrorType.SEMANTIC);
        }

        if (!hasLang) {
            agregarError("Se recomienda especificar el atributo 'lang' en el elemento <html> para accesibilidad",
                    null, AnalysisError.ErrorType.SEMANTIC);
        }

        if (!hasCharset) {
            agregarError("Se recomienda especificar la codificación de caracteres con <meta charset=\"UTF-8\">",
                    null, AnalysisError.ErrorType.SEMANTIC);
        }
    }

    private void validarAtributos() {
        String currentTag = "";
        Set<String> currentAttributes = new HashSet<>();
        Token currentTagToken = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getType().equals("TAG_RESERVADA_ABIERTA") || token.getType().equals("TAG_ABIERTA")) {
                // Validar atributos de la etiqueta anterior si existe
                if (!currentTag.isEmpty()) {
                    validarAtributosDeEtiqueta(currentTag, currentAttributes, currentTagToken);
                }

                // Nueva etiqueta
                currentTag = extraerNombreEtiqueta(token.getValue());
                currentAttributes.clear();
                currentTagToken = token;

            } else if (token.getType().equals("ATRIBUTO") && !currentTag.isEmpty()) {
                String[] partes = token.getValue().split("=", 2);
                if (partes.length >= 1) {
                    String attrName = partes[0].trim().toLowerCase();
                    String attrValue = partes.length > 1 ? partes[1].replaceAll("\"", "").trim() : "";

                    currentAttributes.add(attrName);

                    // Validar atributo específico
                    validarAtributoEspecifico(currentTag, attrName, attrValue, token);
                }

            } else if (token.getType().equals("SIMBOLO") && token.getValue().equals(">")) {
                // Fin de etiqueta de apertura, validar atributos
                if (!currentTag.isEmpty()) {
                    validarAtributosDeEtiqueta(currentTag, currentAttributes, currentTagToken);
                    currentTag = "";
                    currentAttributes.clear();
                }
            }
        }
    }

    private void validarAtributosDeEtiqueta(String tagName, Set<String> attributes, Token tagToken) {
        tagName = tagName.toLowerCase();

        // Verificar atributos válidos
        Set<String> validAttrs = VALID_ATTRIBUTES.get(tagName);
        if (validAttrs != null) {
            for (String attr : attributes) {
                if (!validAttrs.contains(attr) && !attr.startsWith("data-") && !isGlobalAttribute(attr)) {
                    agregarError("Atributo '" + attr + "' no es válido para la etiqueta <" + tagName + ">",
                            tagToken, AnalysisError.ErrorType.SEMANTIC);
                }
            }
        }

        // Verificar atributos obligatorios
        Set<String> requiredAttrs = REQUIRED_ATTRIBUTES.get(tagName);
        if (requiredAttrs != null) {
            for (String required : requiredAttrs) {
                if (!attributes.contains(required)) {
                    agregarError("Atributo obligatorio '" + required + "' falta en la etiqueta <" + tagName + ">",
                            tagToken, AnalysisError.ErrorType.SEMANTIC);
                }
            }
        }
    }

    private void validarAtributoEspecifico(String tagName, String attrName, String attrValue, Token token) {
        // Validar URLs
        if (attrName.equals("href") || attrName.equals("src")) {
            if (!URL_PATTERN.matcher(attrValue).matches() && !attrValue.isEmpty()) {
                agregarError("URL mal formada en atributo '" + attrName + "': " + attrValue,
                        token, AnalysisError.ErrorType.SEMANTIC);
            }
        }

        // Validar emails
        if (attrName.equals("href") && attrValue.startsWith("mailto:")) {
            String email = attrValue.substring(7); // Remove "mailto:"
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                agregarError("Email mal formado en href: " + email,
                        token, AnalysisError.ErrorType.SEMANTIC);
            }
        }

        // Validar IDs
        if (attrName.equals("id")) {
            if (!ID_PATTERN.matcher(attrValue).matches()) {
                agregarError("ID mal formado: " + attrValue + ". Debe comenzar con letra y contener solo letras, números, guiones y guiones bajos",
                        token, AnalysisError.ErrorType.SEMANTIC);
            }
        }

        // Validar valores específicos
        if (attrName.equals("target") && VALID_ATTRIBUTE_VALUES.get("target") != null) {
            if (!VALID_ATTRIBUTE_VALUES.get("target").contains(attrValue)) {
                agregarError("Valor inválido para 'target': " + attrValue,
                        token, AnalysisError.ErrorType.SEMANTIC);
            }
        }

        // Validar type en input
        if (tagName.equals("input") && attrName.equals("type")) {
            if (!VALID_ATTRIBUTE_VALUES.get("input-type").contains(attrValue)) {
                agregarError("Tipo de input inválido: " + attrValue,
                        token, AnalysisError.ErrorType.SEMANTIC);
            }
        }
    }

    private void validarAccesibilidad() {
        for (Token token : tokens) {
            if (token.getType().equals("TAG_RESERVADA_ABIERTA") || token.getType().equals("TAG_ABIERTA")) {
                String tagName = extraerNombreEtiqueta(token.getValue());

                // Verificar alt en imágenes
                if (tagName.equalsIgnoreCase("img")) {
                    if (!tieneAtributo(token, "alt")) {
                        agregarError("Imagen sin atributo 'alt' afecta la accesibilidad",
                                token, AnalysisError.ErrorType.SEMANTIC);
                    }
                }

                // Verificar form labels
                if (tagName.equalsIgnoreCase("input")) {
                    String type = obtenerValorAtributo(token, "type");
                    if (type != null && (type.equals("text") || type.equals("email") || type.equals("password"))) {
                        // Debería estar asociado con un label (simplificado)
                        agregarAdvertencia("Input debería estar asociado con un elemento <label> para accesibilidad",
                                token);
                    }
                }
            }
        }
    }

    private void validarUnicidadElementos() {
        Set<String> ids = new HashSet<>();

        for (Token token : tokens) {
            if (token.getType().equals("ATRIBUTO")) {
                String attr = token.getValue();
                if (attr.toLowerCase().startsWith("id=")) {
                    String[] partes = attr.split("=", 2);
                    if (partes.length > 1) {
                        String id = partes[1].replaceAll("\"", "").trim();
                        if (ids.contains(id)) {
                            agregarError("ID duplicado: " + id + ". Los IDs deben ser únicos en el documento",
                                    token, AnalysisError.ErrorType.SEMANTIC);
                        }
                        ids.add(id);
                    }
                }
            }
        }
    }

    private void validarReferenciasSemánticas() {
        // Buscar enlaces internos y verificar que los IDs existan
        Set<String> ids = new HashSet<>();
        List<String> referencesInternas = new ArrayList<>();

        // Primera pasada: recolectar IDs
        for (Token token : tokens) {
            if (token.getType().equals("ATRIBUTO") && token.getValue().toLowerCase().startsWith("id=")) {
                String[] partes = token.getValue().split("=", 2);
                if (partes.length > 1) {
                    String id = partes[1].replaceAll("\"", "").trim();
                    ids.add(id);
                }
            }
        }

        // Segunda pasada: verificar referencias
        for (Token token : tokens) {
            if (token.getType().equals("ATRIBUTO") && token.getValue().toLowerCase().startsWith("href=")) {
                String[] partes = token.getValue().split("=", 2);
                if (partes.length > 1) {
                    String href = partes[1].replaceAll("\"", "").trim();
                    if (href.startsWith("#") && href.length() > 1) {
                        String targetId = href.substring(1);
                        if (!ids.contains(targetId)) {
                            agregarError("Referencia interna inválida: " + href + ". No existe elemento con ID '" + targetId + "'",
                                    token, AnalysisError.ErrorType.SEMANTIC);
                        }
                    }
                }
            }
        }
    }

    private void validarJerarquiaHeadings() {
        List<Integer> headingLevels = new ArrayList<>();

        for (Token token : tokens) {
            if (token.getType().equals("TAG_RESERVADA_ABIERTA") || token.getType().equals("TAG_ABIERTA")) {
                String tagName = extraerNombreEtiqueta(token.getValue());
                if (tagName.matches("h[1-6]")) {
                    int level = Integer.parseInt(tagName.substring(1));
                    headingLevels.add(level);
                }
            }
        }

        // Verificar jerarquía lógica
        for (int i = 1; i < headingLevels.size(); i++) {
            int previous = headingLevels.get(i - 1);
            int current = headingLevels.get(i);

            if (current > previous + 1) {
                agregarAdvertencia("Jerarquía de headings inconsistente: salto de h" + previous + " a h" + current +
                        ". Se recomienda una progresión secuencial", null);
            }
        }

        // Verificar que comience con h1
        if (!headingLevels.isEmpty() && headingLevels.get(0) != 1) {
            agregarAdvertencia("Se recomienda comenzar la jerarquía de headings con h1", null);
        }
    }

    // Métodos auxiliares
    private boolean tieneAtributo(Token token, String attributeName) {
        // Buscar en tokens siguientes hasta encontrar > o nueva etiqueta
        int index = findTokenIndex(token);
        if (index == -1) return false;

        for (int i = index + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType().equals("SIMBOLO") && t.getValue().equals(">")) {
                break;
            }
            if (t.getType().equals("ATRIBUTO")) {
                String attr = t.getValue().toLowerCase();
                if (attr.startsWith(attributeName.toLowerCase() + "=")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tieneAtributoLang(Token token) {
        return tieneAtributo(token, "lang");
    }

    private String obtenerValorAtributo(Token token, String attributeName) {
        int index = findTokenIndex(token);
        if (index == -1) return null;

        for (int i = index + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType().equals("SIMBOLO") && t.getValue().equals(">")) {
                break;
            }
            if (t.getType().equals("ATRIBUTO")) {
                String attr = t.getValue();
                if (attr.toLowerCase().startsWith(attributeName.toLowerCase() + "=")) {
                    String[] partes = attr.split("=", 2);
                    return partes.length > 1 ? partes[1].replaceAll("\"", "").trim() : null;
                }
            }
        }
        return null;
    }

    private int findTokenIndex(Token token) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) == token) {
                return i;
            }
        }
        return -1;
    }

    private boolean isGlobalAttribute(String attrName) {
        return Set.of("id", "class", "style", "title", "lang", "dir", "hidden", "tabindex").contains(attrName);
    }

    private String extraerNombreEtiqueta(String tag) {
        String limpio = tag.replaceAll("[<>/]+", "").trim();
        String[] partes = limpio.split("\\s+");
        return partes.length > 0 && !partes[0].isEmpty() ? partes[0] : "desconocido";
    }

    private void agregarError(String mensaje, Token token, AnalysisError.ErrorType tipo) {
        if (token != null) {
            errors.add(new AnalysisError(mensaje, tipo, token.getLine(), token.getColumn()));
        } else {
            errors.add(new AnalysisError(mensaje, tipo));
        }
    }

    private void agregarAdvertencia(String mensaje, Token token) {
        // Las advertencias se pueden manejar como errores semánticos de menor prioridad
        agregarError("ADVERTENCIA: " + mensaje, token, AnalysisError.ErrorType.SEMANTIC);
    }

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return Collections.unmodifiableMap(symbolTable);
    }
}