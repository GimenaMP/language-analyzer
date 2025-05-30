package com.analyzer.service.SemanticAnalyzer;

import com.analyzer.model.*;
import com.analyzer.service.interfaces.ISemanticAnalyzer;

import java.util.*;

public class HTMLSemanticAnalyzer implements ISemanticAnalyzer {

    private Map<String, Symbol> tablaSimbolos = new HashMap<>();
    private List<AnalysisError> errores = new ArrayList<>();

    @Override
    public Map<String, Symbol> getSymbolTable() {
        return tablaSimbolos;
    }

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language, Map<String, Symbol> tablaSimbolosExistente) {
        errores.clear();
        tablaSimbolos.clear();
        if (tablaSimbolosExistente != null) {
            tablaSimbolos.putAll(tablaSimbolosExistente);
        }

        validarEstructuraDocumento(tokens);
        validarAccesibilidad(tokens);
        validarFormularios(tokens);
        validarEnlaces(tokens);

        return errores;
    }

    private void validarEstructuraDocumento(List<Token> tokens) {
        boolean tieneHtml = false;
        boolean tieneHead = false;
        boolean tieneBody = false;
        boolean tieneTitulo = false;
        int contadorH1 = 0;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType().equals("ETIQUETA_APERTURA")) {
                String etiqueta = extraerNombreEtiqueta(token.getValue());

                switch (etiqueta.toLowerCase()) {
                    case "html":
                        tieneHtml = true;
                        break;
                    case "head":
                        tieneHead = true;
                        break;
                    case "body":
                        tieneBody = true;
                        break;
                    case "title":
                        tieneTitulo = true;
                        break;
                    case "h1":
                        contadorH1++;
                        if (contadorH1 > 1) {
                            errores.add(new AnalysisError(
                                    "Múltiples elementos H1 encontrados - Se recomienda usar solo uno por página",
                                    AnalysisError.ErrorType.SEMANTIC,
                                    token.getLine(),
                                    token.getColumn()
                            ));
                        }
                        break;
                }
            }
        }

        validarElementosObligatorios(tieneHtml, tieneHead, tieneBody, tieneTitulo);
    }

    private void validarElementosObligatorios(boolean tieneHtml, boolean tieneHead, boolean tieneBody, boolean tieneTitulo) {
        if (!tieneHtml) {
            errores.add(new AnalysisError(
                    "Falta elemento raíz <html>",
                    AnalysisError.ErrorType.SEMANTIC,
                    1, 0
            ));
        }
        if (!tieneHead) {
            errores.add(new AnalysisError(
                    "Falta sección <head>",
                    AnalysisError.ErrorType.SEMANTIC,
                    1, 0
            ));
        }
        if (!tieneBody) {
            errores.add(new AnalysisError(
                    "Falta sección <body>",
                    AnalysisError.ErrorType.SEMANTIC,
                    1, 0
            ));
        }
        if (!tieneTitulo) {
            errores.add(new AnalysisError(
                    "Falta elemento <title> en <head>",
                    AnalysisError.ErrorType.SEMANTIC,
                    1, 0
            ));
        }
    }

    private void validarAccesibilidad(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.getType().equals("ETIQUETA_APERTURA")) {
                String etiqueta = extraerNombreEtiqueta(token.getValue());
                Map<String, String> atributos = analizarAtributos(token.getValue());

                switch (etiqueta.toLowerCase()) {
                    case "img":
                        validarAtributosImagen(atributos, token);
                        break;
                    case "a":
                        validarAtributosEnlace(atributos, token);
                        break;
                    case "input":
                        validarAtributosInput(atributos, token);
                        break;
                }
            }
        }
    }

    private void validarFormularios(List<Token> tokens) {
        boolean enFormulario = false;
        boolean tieneSubmit = false;

        for (Token token : tokens) {
            if (token.getType().equals("ETIQUETA_APERTURA")) {
                String etiqueta = extraerNombreEtiqueta(token.getValue());
                Map<String, String> atributos = analizarAtributos(token.getValue());

                if (etiqueta.equals("form")) {
                    enFormulario = true;
                    tieneSubmit = false;
                    validarAtributosFormulario(atributos, token);
                } else if (enFormulario && etiqueta.equals("input")) {
                    String tipo = atributos.getOrDefault("type", "text");
                    if (tipo.equals("submit")) {
                        tieneSubmit = true;
                    }
                }
            } else if (token.getType().equals("ETIQUETA_CIERRE") && token.getValue().contains("form")) {
                validarSubmitFormulario(tieneSubmit, token);
                enFormulario = false;
            }
        }
    }

    private void validarEnlaces(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.getType().equals("ETIQUETA_APERTURA") && extraerNombreEtiqueta(token.getValue()).equals("a")) {
                Map<String, String> atributos = analizarAtributos(token.getValue());
                validarHrefEnlace(atributos, token);
            }
        }
    }

    private String extraerNombreEtiqueta(String valorEtiqueta) {
        String etiqueta = valorEtiqueta.replaceAll("[<>/]", "").trim();
        int indiceEspacio = etiqueta.indexOf(" ");
        return indiceEspacio > -1 ? etiqueta.substring(0, indiceEspacio) : etiqueta;
    }

    private Map<String, String> analizarAtributos(String valorEtiqueta) {
        Map<String, String> atributos = new HashMap<>();
        int indiceEspacio = valorEtiqueta.indexOf(" ");

        if (indiceEspacio > -1) {
            String cadenaAtributos = valorEtiqueta.substring(indiceEspacio + 1, valorEtiqueta.length() - 1).trim();
            String[] pares = cadenaAtributos.split("\\s+");

            for (String par : pares) {
                String[] claveValor = par.split("=", 2);
                if (claveValor.length == 2) {
                    String valor = claveValor[1].replaceAll("[\"']", "");
                    atributos.put(claveValor[0].toLowerCase(), valor);
                } else if (claveValor.length == 1) {
                    atributos.put(claveValor[0].toLowerCase(), "");
                }
            }
        }

        return atributos;
    }

    private void validarAtributosImagen(Map<String, String> atributos, Token token) {
        if (!atributos.containsKey("alt")) {
            errores.add(new AnalysisError(
                    "Imagen sin atributo alt - Requerido para accesibilidad",
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void validarAtributosEnlace(Map<String, String> atributos, Token token) {
        if (!atributos.containsKey("href")) {
            errores.add(new AnalysisError(
                    "Enlace sin atributo href",
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void validarAtributosInput(Map<String, String> atributos, Token token) {
        if (!atributos.containsKey("label")) {
            errores.add(new AnalysisError(
                    "Campo de formulario sin etiqueta asociada",
                    AnalysisError.ErrorType.WARNING,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void validarAtributosFormulario(Map<String, String> atributos, Token token) {
        if (!atributos.containsKey("action")) {
            errores.add(new AnalysisError(
                    "Formulario sin atributo action",
                    AnalysisError.ErrorType.SEMANTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void validarSubmitFormulario(boolean tieneSubmit, Token token) {
        if (!tieneSubmit) {
            errores.add(new AnalysisError(
                    "Formulario sin botón de envío",
                    AnalysisError.ErrorType.WARNING,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private void validarHrefEnlace(Map<String, String> atributos, Token token) {
        String href = atributos.get("href");
        if (href != null) {
            if (href.trim().isEmpty()) {
                errores.add(new AnalysisError(
                        "Enlace con href vacío",
                        AnalysisError.ErrorType.WARNING,
                        token.getLine(),
                        token.getColumn()
                ));
            } else if (href.equals("#")) {
                errores.add(new AnalysisError(
                        "Enlace placeholder (#) detectado",
                        AnalysisError.ErrorType.WARNING,
                        token.getLine(),
                        token.getColumn()
                ));
            }
        }
    }
}