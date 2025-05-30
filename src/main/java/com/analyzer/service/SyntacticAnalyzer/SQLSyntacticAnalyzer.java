package com.analyzer.service.SyntacticAnalyzer;

import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.model.Token;
import com.analyzer.service.interfaces.ISyntacticAnalyzer;

import java.util.*;

public class SQLSyntacticAnalyzer implements ISyntacticAnalyzer {

    private int currentPosition;
    private List<Token> tokens;
    private List<AnalysisError> errors;
    private Stack<String> clausulaStack;

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        if (language != LanguageType.PLSQL) {
            return List.of();
        }

        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.currentPosition = 0;
        this.clausulaStack = new Stack<>();

        while (currentPosition < tokens.size()) {
            analizarSentenciaSQL();
        }

        validarEstadoFinal();
        return errors;
    }

    private void analizarSentenciaSQL() {
        Token token = obtenerTokenActual();
        if (token == null) return;

        switch (token.getType()) {
            case "KEYWORD":
                analizarClausulaSQL();
                break;
            case "IDENTIFICADOR":
                analizarIdentificadorSQL();
                break;
            case "COMENTARIO":
                // Ignorar comentarios
                avanzar();
                break;
            default:
                manejarTokenInesperado();
        }
    }

    private void manejarTokenInesperado() {

    }

    private void analizarIdentificadorSQL() {

    }

    private void analizarClausulaSQL() {
        Token keyword = obtenerTokenActual();
        switch (keyword.getValue().toUpperCase()) {
            case "SELECT":
                analizarSelect();
                break;
            case "INSERT":
                analizarInsert();
                break;
            case "UPDATE":
                analizarUpdate();
                break;
            case "DELETE":
                analizarDelete();
                break;
            case "CREATE":
                analizarCreate();
                break;
            case "DROP":
                analizarDrop();
                break;
            default:
                avanzar();
        }
    }

    private void analizarDrop() {

    }

    private void analizarCreate() {

    }

    private void analizarDelete() {

    }

    private void analizarUpdate() {

    }

    private void analizarSelect() {
        esperarToken("SELECT");
        clausulaStack.push("SELECT");

        // Analizar lista de columnas
        analizarListaColumnas();

        // Esperar FROM obligatorio
        if (!encontrarClausula("FROM")) {
            agregarError("Se esperaba cláusula FROM después de SELECT", obtenerTokenActual());
            return;
        }

        // Analizar cláusulas opcionales
        while (currentPosition < tokens.size()) {
            Token token = obtenerTokenActual();
            if (token == null || !token.getType().equals("KEYWORD")) break;

            switch (token.getValue().toUpperCase()) {
                case "WHERE":
                    analizarWhere();
                    break;
                case "GROUP":
                    analizarGroupBy();
                    break;
                case "HAVING":
                    analizarHaving();
                    break;
                case "ORDER":
                    analizarOrderBy();
                    break;
                default:
                    if (esInicioNuevaSentencia(token.getValue())) {
                        return;
                    }
                    avanzar();
            }
        }

        clausulaStack.pop();
    }

    private void analizarOrderBy() {

    }

    private void analizarHaving() {

    }

    private void analizarGroupBy() {
        esperarToken("GROUP");
        esperarToken("BY");
        if (!esperarTipo("IDENTIFICADOR")) {
            agregarError("Se esperaba identificador después de GROUP BY", obtenerTokenActual());
            return;
        }
        // Analizar lista de columnas
        analizarListaColumnas();

    }

    private void analizarListaColumnas() {
        do {
            if (!analizarExpresionColumna()) {
                agregarError("Se esperaba nombre de columna o expresión", obtenerTokenActual());
                return;
            }
        } while (consumirSiExiste("COMA"));
    }

    private boolean analizarExpresionColumna() {
        Token token = obtenerTokenActual();
        if (token == null) return false;

        if (token.getType().equals("IDENTIFICADOR") ||
                token.getType().equals("OPERADOR") ||
                token.getType().equals("NUMERO_ENTERO") ||
                token.getType().equals("NUMERO_DECIMAL") ||
                token.getValue().equals("*")) {

            avanzar();

            // Verificar alias opcional
            token = obtenerTokenActual();
            if (token != null && token.getValue().equalsIgnoreCase("AS")) {
                avanzar();
                if (!esperarTipo("IDENTIFICADOR")) {
                    agregarError("Se esperaba identificador después de AS", token);
                }
            }
            return true;
        }
        return false;
    }

    private void analizarWhere() {
        esperarToken("WHERE");
        analizarCondicion();
    }

    private void analizarCondicion() {
        int parentesisNivel = 0;

        while (currentPosition < tokens.size()) {
            Token token = obtenerTokenActual();

            if (token.getType().equals("PAREN_IZQ")) {
                parentesisNivel++;
            } else if (token.getType().equals("PAREN_DER")) {
                parentesisNivel--;
                if (parentesisNivel < 0) {
                    agregarError("Paréntesis desbalanceados en condición", token);
                }
            } else if (parentesisNivel == 0 &&
                    (esInicioNuevaClausula(token.getValue()) ||
                            esInicioNuevaSentencia(token.getValue()))) {
                return;
            }

            avanzar();
        }
    }

    private boolean esInicioNuevaClausula(String valor) {
        return valor.equalsIgnoreCase("GROUP") ||
                valor.equalsIgnoreCase("HAVING") ||
                valor.equalsIgnoreCase("ORDER");
    }

    private boolean esInicioNuevaSentencia(String valor) {
        return valor.equalsIgnoreCase("SELECT") ||
                valor.equalsIgnoreCase("INSERT") ||
                valor.equalsIgnoreCase("UPDATE") ||
                valor.equalsIgnoreCase("DELETE");
    }

    private Token obtenerTokenActual() {
        return currentPosition < tokens.size() ? tokens.get(currentPosition) : null;
    }

    private void avanzar() {
        currentPosition++;
    }

    private void agregarError(String mensaje, Token token) {
        if (token != null) {
            errors.add(new AnalysisError(
                    mensaje,
                    AnalysisError.ErrorType.SYNTACTIC,
                    token.getLine(),
                    token.getColumn()
            ));
        }
    }

    private boolean esperarToken(String valorEsperado) {
        Token token = obtenerTokenActual();
        if (token != null && token.getValue().equalsIgnoreCase(valorEsperado)) {
            avanzar();
            return true;
        }
        agregarError("Se esperaba '" + valorEsperado + "'", token);
        return false;
    }

    private boolean esperarTipo(String tipoEsperado) {
        Token token = obtenerTokenActual();
        if (token != null && token.getType().equals(tipoEsperado)) {
            avanzar();
            return true;
        }
        return false;
    }

    private boolean consumirSiExiste(String tipo) {
        Token token = obtenerTokenActual();
        if (token != null && token.getType().equals(tipo)) {
            avanzar();
            return true;
        }
        return false;
    }

    private boolean encontrarClausula(String clausula) {
        while (currentPosition < tokens.size()) {
            Token token = obtenerTokenActual();
            if (token.getValue().equalsIgnoreCase(clausula)) {
                avanzar();
                return true;
            }
            if (esInicioNuevaSentencia(token.getValue())) {
                return false;
            }
            avanzar();
        }
        return false;
    }

    private void validarEstadoFinal() {
        if (!clausulaStack.isEmpty()) {
            agregarError("Sentencia SQL incompleta", null);
        }
    }

    private void analizarInsert() {
        esperarToken("INSERT");
        esperarToken("INTO");

        if (!esperarTipo("IDENTIFICADOR")) {
            agregarError("Se esperaba nombre de tabla después de INTO", obtenerTokenActual());
            return;
        }

        // Analizar lista de columnas opcional
        if (consumirSiExiste("PAREN_IZQ")) {
            while (true) {
                if (!esperarTipo("IDENTIFICADOR")) {
                    agregarError("Se esperaba identificador en lista de columnas", obtenerTokenActual());
                    return;
                }
                if (!consumirSiExiste("COMA")) break;
            }
            esperarToken("PAREN_DER");
        }

        esperarToken("VALUES");
        esperarToken("PAREN_IZQ");

        // Analizar valores
        while (true) {
            if (!analizarValor()) {
                agregarError("Se esperaba valor en INSERT", obtenerTokenActual());
                return;
            }
            if (!consumirSiExiste("COMA")) break;
        }

        esperarToken("PAREN_DER");
    }
    private boolean analizarValor() {
        Token token = obtenerTokenActual();
        if (token == null) return false;

        if (token.getType().equals("IDENTIFICADOR") ||
                token.getType().equals("NUMERO_ENTERO") ||
                token.getType().equals("NUMERO_DECIMAL") ||
                token.getType().equals("CADENA")) {
            avanzar();
            return true;
        }
        return false;






    }



}