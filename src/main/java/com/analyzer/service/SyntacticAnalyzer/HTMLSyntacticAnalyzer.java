package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class HTMLSyntacticAnalyzer implements ISyntacticAnalyzer {


    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        inicializar();

        // Validar estructura general del documento
        validarEstructuraDocumento(tokens);

        // Validar anidamiento de etiquetas
        validarAnidamientoEtiquetas(tokens);

        // Validar reglas específicas
        validarReglasEspecificas(tokens);

        return errors;
    }

    private List<AnalysisError> errors;
    private Stack<Token> tagStack;
    private boolean hasDoctype;
    private boolean hasHtmlTag;
    private boolean hasHeadTag;
    private boolean hasBodyTag;



    private void inicializar() {
        errors = new ArrayList<>();
        tagStack = new Stack<>();
        hasDoctype = false;
        hasHtmlTag = false;
        hasHeadTag = false;
        hasBodyTag = false;
    }

    private void validarEstructuraDocumento(List<Token> tokens) {
        for (Token token : tokens) {
            switch (token.getType()) {
                case "DOCTYPE":
                    if (hasDoctype) {
                        agregarError("DOCTYPE duplicado", token);
                    }
                    hasDoctype = true;
                    break;

                case "ETIQUETA_APERTURA":
                    String tagName = extraerNombreEtiqueta(token.getValue());
                    switch (tagName.toLowerCase()) {
                        case "html":
                            if (hasHtmlTag) {
                                agregarError("Etiqueta <html> duplicada", token);
                            }
                            hasHtmlTag = true;
                            break;
                        case "head":
                            if (hasHeadTag) {
                                agregarError("Etiqueta <head> duplicada", token);
                            }
                            hasHeadTag = true;
                            break;
                        case "body":
                            if (hasBodyTag) {
                                agregarError("Etiqueta <body> duplicada", token);
                            }
                            hasBodyTag = true;
                            break;
                    }
                    break;
            }
        }

        // Validar estructura básica
        if (!hasDoctype) {
            agregarError("Falta declaración DOCTYPE", null);
        }
        if (!hasHtmlTag) {
            agregarError("Falta etiqueta <html>", null);
        }
        if (!hasHeadTag) {
            agregarError("Falta etiqueta <head>", null);
        }
        if (!hasBodyTag) {
            agregarError("Falta etiqueta <body>", null);
        }
    }

    private void validarAnidamientoEtiquetas(List<Token> tokens) {
        for (Token token : tokens) {
            switch (token.getType()) {
                case "ETIQUETA_APERTURA":
                    String tagName = extraerNombreEtiqueta(token.getValue());
                    if (!esElementoVacio(tagName)) {
                        tagStack.push(token);
                    }
                    break;

                case "ETIQUETA_CIERRE":
                    String closingTag = extraerNombreEtiqueta(token.getValue());
                    if (tagStack.isEmpty()) {
                        agregarError("Etiqueta de cierre sin apertura correspondiente: " + token.getValue(), token);
                    } else {
                        Token openingToken = tagStack.pop();
                        String openingTag = extraerNombreEtiqueta(openingToken.getValue());
                        if (!closingTag.equalsIgnoreCase(openingTag)) {
                            agregarError("Etiqueta mal cerrada: esperaba </" + openingTag + "> pero encontró </" + closingTag + ">", token);
                        }
                    }
                    break;
            }
        }

        // Verificar etiquetas sin cerrar
        while (!tagStack.isEmpty()) {
            Token unclosedTag = tagStack.pop();
            agregarError("Etiqueta sin cerrar: " + unclosedTag.getValue(), unclosedTag);
        }
    }

    private void validarReglasEspecificas(List<Token> tokens) {
        boolean inHead = false;
        boolean inBody = false;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            String tagName = extraerNombreEtiqueta(token.getValue());

            switch (token.getType()) {
                case "ETIQUETA_APERTURA":
                    if (tagName.equalsIgnoreCase("head")) {
                        inHead = true;
                    } else if (tagName.equalsIgnoreCase("body")) {
                        inBody = true;
                    } else if (esElementoSoloEnHead(tagName) && !inHead) {
                        agregarError("La etiqueta <" + tagName + "> solo debe aparecer dentro de <head>", token);
                    } else if (esElementoSoloEnBody(tagName) && !inBody) {
                        agregarError("La etiqueta <" + tagName + "> solo debe aparecer dentro de <body>", token);
                    }
                    break;

                case "ETIQUETA_CIERRE":
                    if (tagName.equalsIgnoreCase("head")) {
                        inHead = false;
                    } else if (tagName.equalsIgnoreCase("body")) {
                        inBody = false;
                    }
                    break;
            }
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

    private String extraerNombreEtiqueta(String tag) {
        // Extrae el nombre de la etiqueta sin < > /
        return tag.replaceAll("[<>/]", "").trim().split("\\s+")[0];
    }

    private boolean esElementoVacio(String tagName) {
        // Elementos que no necesitan etiqueta de cierre
        Set<String> voidElements = Set.of(
                "area", "base", "br", "col", "embed", "hr", "img", "input",
                "link", "meta", "param", "source", "track", "wbr"
        );
        return voidElements.contains(tagName.toLowerCase());
    }

    private boolean esElementoSoloEnHead(String tagName) {
        // Elementos que solo deben aparecer en <head>
        Set<String> headElements = Set.of(
                "title", "meta", "link", "style", "script", "base"
        );
        return headElements.contains(tagName.toLowerCase());
    }

    private boolean esElementoSoloEnBody(String tagName) {
        // Elementos que solo deben aparecer en <body>
        Set<String> bodyElements = Set.of(
                "header", "main", "footer", "article", "section", "nav",
                "aside", "h1", "h2", "h3", "h4", "h5", "h6", "p", "div"
        );
        return bodyElements.contains(tagName.toLowerCase());
    }


}
