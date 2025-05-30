package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class PythonSyntacticAnalyzer implements ISyntacticAnalyzer {

    private int posicionActual;
    private List<Token> tokens;
    private List<AnalysisError> errores;
    private Stack<String> pilaDelimitadores;
    private int nivelIndentacion;
    private Stack<ContextoIndentacion> pilaIndentacion;
    private String tipoIndentacionActual; // "ESPACIOS" o "TABS"
    private boolean primeraIndentacion;



    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        this.tokens = tokens;
        this.errores = new ArrayList<>();
        this.posicionActual = 0;
        this.pilaDelimitadores = new Stack<>();
        this.pilaIndentacion = new Stack<>();
        this.primeraIndentacion = true;
        this.tipoIndentacionActual = null;
        this.nivelIndentacion = 0;

        while (posicionActual < tokens.size()) {
            analizarSentencia();
        }

        validarEstadoFinal();
        return errores;
    }

    private static class ContextoIndentacion {
        final int nivel;
        final String tipo; // "ESPACIOS" o "TABS"

        ContextoIndentacion(int nivel, String tipo) {
            this.nivel = nivel;
            this.tipo = tipo;
        }
    }




    private void analizarSentencia() {
        Token token = obtenerTokenActual();
        if (token == null) return;

        switch (token.getType()) {
            case "KEYWORD":
                analizarSentenciaPalabraClave();
                break;
            case "IDENTIFICADOR":
                analizarSentenciaIdentificador();
                break;
            case "COMENTARIO":
                avanzar(); // Ignorar comentarios
                break;
            default:
                manejarTokenInesperado();
        }
    }

    private void analizarSentenciaPalabraClave() {
        Token palabra = obtenerTokenActual();
        switch (palabra.getValue().toUpperCase()) {
            case "IF":
                analizarSentenciaIf();
                break;
            case "FOR":
                analizarSentenciaFor();
                break;
            case "WHILE":
                analizarSentenciaWhile();
                break;
            case "DEF":
                analizarDefinicionFuncion();
                break;
            case "CLASS":
                analizarDefinicionClase();
                break;
            default:
                avanzar();
        }
    }

    private void analizarSentenciaIf() {
        // Estructura: if condición:
        consumir("IF");
        analizarCondicion();
        esperarDosPuntos();
        verificarIndentacion();
    }

    private void analizarSentenciaFor() {
        // Estructura: for variable in iterable:
        consumir("FOR");
        esperarIdentificador();
        consumir("IN");
        analizarExpresion();
        esperarDosPuntos();
        verificarIndentacion();
    }

    private void analizarCondicion() {
        int nivelParentesis = 0;
        while (posicionActual < tokens.size()) {
            Token token = obtenerTokenActual();
            if (token.getType().equals("PAREN_IZQ")) {
                nivelParentesis++;
            } else if (token.getType().equals("PAREN_DER")) {
                nivelParentesis--;
                if (nivelParentesis < 0) {
                    agregarError("Paréntesis no balanceados en condición");
                }
            } else if (token.getType().equals("DOS_PUNTOS") && nivelParentesis == 0) {
                return;
            }
            avanzar();
        }
    }
    private void verificarIndentacion() {
        Token token = obtenerTokenActual();
        if (token == null) return;

        String contenido = token.getValue();
        int espaciosBlancos = contarEspaciosBlancos(contenido);

        // Determinar tipo de indentación
        if (primeraIndentacion) {
            tipoIndentacionActual = contenido.contains("\t") ? "TABS" : "ESPACIOS";
            primeraIndentacion = false;
        }

        // Verificar mezcla de tabs y espacios
        if (contenido.contains("\t") && contenido.contains(" ")) {
            agregarError("No se pueden mezclar tabs y espacios en la indentación", token);
            return;
        }

        // Verificar consistencia del tipo de indentación
        String tipoActual = contenido.contains("\t") ? "TABS" : "ESPACIOS";
        if (!tipoActual.equals(tipoIndentacionActual)) {
            agregarError("Inconsistencia en el tipo de indentación: se mezclan tabs y espacios", token);
            return;
        }

        // Calcular nivel de indentación
        int nivelActual = tipoActual.equals("TABS") ?
                (int) contenido.chars().filter(ch -> ch == '\t').count() :
                espaciosBlancos / 4;

        if (pilaIndentacion.isEmpty()) {
            if (nivelActual > 0) {
                pilaIndentacion.push(new ContextoIndentacion(nivelActual, tipoActual));
            }
        } else {
            int nivelPrevio = pilaIndentacion.peek().nivel;

            if (nivelActual > nivelPrevio) {
                // Incremento de indentación
                if (nivelActual != nivelPrevio + 1) {
                    agregarError("Indentación incorrecta: el incremento debe ser de exactamente un nivel", token);
                }
                pilaIndentacion.push(new ContextoIndentacion(nivelActual, tipoActual));
            } else if (nivelActual < nivelPrevio) {
                // Reducción de indentación
                while (!pilaIndentacion.isEmpty() && pilaIndentacion.peek().nivel > nivelActual) {
                    pilaIndentacion.pop();
                }
                if (pilaIndentacion.isEmpty() || pilaIndentacion.peek().nivel != nivelActual) {
                    agregarError("Nivel de indentación no coincide con ningún nivel anterior", token);
                }
            }
        }
    }

    private int contarEspaciosBlancos(String contenido) {
        int count = 0;
        for (char c : contenido.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 8; // Python considera cada tab como 8 espacios
        }
        return count;
    }


    private int obtenerNivelIndentacion(Token token) {
        String valor = token.getValue();
        int espacios = 0;
        for (char c : valor.toCharArray()) {
            if (c == ' ') espacios++;
            else if (c == '\t') espacios += 8;
            else break;
        }
        return espacios / 4; // 4 espacios por nivel
    }

    private void agregarError(String mensaje) {
        Token token = obtenerTokenActual();
        errores.add(new AnalysisError(
                mensaje,
                AnalysisError.ErrorType.SYNTACTIC,
                token.getLine(),
                token.getColumn()
        ));
    }

    private void agregarError(String mensaje, Token token) {
        errores.add(new AnalysisError(
                mensaje,
                AnalysisError.ErrorType.SYNTACTIC,
                token.getLine(),
                token.getColumn()
        ));
    }

    private Token obtenerTokenActual() {
        return posicionActual < tokens.size() ? tokens.get(posicionActual) : null;
    }

    private void avanzar() {
        posicionActual++;
    }

    private void consumir(String valorEsperado) {
        Token token = obtenerTokenActual();
        if (token != null && token.getValue().equalsIgnoreCase(valorEsperado)) {
            avanzar();
        } else {
            agregarError("Se esperaba '" + valorEsperado + "'");
        }
    }

    private void esperarIdentificador() {
        Token token = obtenerTokenActual();
        if (token == null || !token.getType().equals("IDENTIFICADOR")) {
            agregarError("Se esperaba un identificador");
        } else {
            avanzar();
        }
    }

    private void esperarDosPuntos() {
        Token token = obtenerTokenActual();
        if (token == null || !token.getValue().equals(":")) {
            agregarError("Se esperaba ':'");
        } else {
            avanzar();
        }
    }

    private void validarEstadoFinal() {
        if (!pilaDelimitadores.isEmpty()) {
            agregarError("Quedan " + pilaDelimitadores.size() + " delimitadores sin cerrar al final", obtenerTokenActual());
        }
    }

    private void analizarSentenciaWhile() {
        // Estructura: while condición:
        consumir("WHILE");
        analizarCondicion();
        esperarDosPuntos();
        verificarIndentacion();
    }

    private void analizarDefinicionFuncion() {
        // Estructura: def nombre(parametros):
        consumir("DEF");
        esperarIdentificador();
        esperarDosPuntos();
        verificarIndentacion();
    }

    private void analizarDefinicionClase() {
        // Estructura: class NombreClase:
        consumir("CLASS");
        esperarIdentificador();
        esperarDosPuntos();
        verificarIndentacion();
    }

    private void analizarSentenciaIdentificador() {
        Token identificador = obtenerTokenActual();
        if (identificador == null || !identificador.getType().equals("IDENTIFICADOR")) {
            agregarError("Se esperaba un identificador");
            return;
        }
        avanzar();

        if (posicionActual < tokens.size() && obtenerTokenActual().getValue().equals("=")) {
            avanzar();
            analizarExpresion();
        } else if (posicionActual < tokens.size() && obtenerTokenActual().getValue().equals("(")) {
            analizarLlamadaFuncion();
        } else {
            agregarError("Se esperaba '=' o '(' después de identificador");
        }
    }

    private void analizarExpresion() {
        while (posicionActual < tokens.size()) {
            Token token = obtenerTokenActual();
            if (token.getValue().equals(";") || token.getValue().equals("\n")) {
                avanzar();
                return;
            }
            avanzar();
        }
    }

    private void analizarLlamadaFuncion() {
        Token id = obtenerTokenActual();
        if (id == null || !id.getType().equals("IDENTIFICADOR")) {
            agregarError("Se esperaba un identificador para llamada a función");
            return;
        }
        avanzar();

        if (posicionActual < tokens.size() && obtenerTokenActual().getValue().equals("(")) {
            avanzar();
            if (posicionActual < tokens.size() && obtenerTokenActual().getValue().equals(")")) {
                avanzar();
            } else {
                agregarError("Se esperaba ')' en llamada a función");
            }
        } else {
            agregarError("Se esperaba '(' después de identificador");
        }
    }

    private void manejarTokenInesperado() {
        Token token = obtenerTokenActual();
        if (token != null) {
            agregarError("Token inesperado: " + token.getValue(), token);
        }
        avanzar();
    }


}
