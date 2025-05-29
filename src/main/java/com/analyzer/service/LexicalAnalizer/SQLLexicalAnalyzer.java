package com.analyzer.service.LexicalAnalizer;

import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Analizador léxico especializado para SQL.
 * Maneja keywords, identificadores, números, strings y operadores SQL.
 */
public  class SQLLexicalAnalyzer implements ILexicalAnalyzer {

    // Patrones regex para SQL
    private static final Pattern PATRON_KEYWORD = Pattern.compile(
            "\\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|TABLE|INDEX|VIEW|DATABASE|SCHEMA|JOIN|INNER|LEFT|RIGHT|OUTER|ON|GROUP|ORDER|BY|HAVING|UNION|DISTINCT|COUNT|SUM|AVG|MIN|MAX|AS|AND|OR|NOT|NULL|PRIMARY|KEY|FOREIGN|REFERENCES|CONSTRAINT|DEFAULT|UNIQUE|CHECK|BEGIN|END|COMMIT|ROLLBACK|TRANSACTION)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATRON_IDENTIFICADOR = Pattern.compile(
            "[a-zA-Z_][a-zA-Z0-9_]*"
    );

    private static final Pattern PATRON_NUMERO = Pattern.compile(
            "\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"
    );

    private static final Pattern PATRON_STRING = Pattern.compile(
            "'(?:[^'']|'')*'"
    );

    private static final Pattern PATRON_OPERADOR = Pattern.compile(
            ">=|<=|<>|!=|[=<>+\\-*/%]"
    );

    private static final Pattern PATRON_COMENTARIO_LINEA = Pattern.compile(
            "--.*$"
    );

    private static final Pattern PATRON_COMENTARIO_BLOQUE = Pattern.compile(
            "/\\*[\\s\\S]*?\\*/", Pattern.MULTILINE | Pattern.DOTALL
    );

    // Conjunto de keywords SQL
    private static final Set<String> KEYWORDS_SQL = Set.of(
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER",
            "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "JOIN", "INNER", "LEFT", "RIGHT",
            "OUTER", "ON", "GROUP", "ORDER", "BY", "HAVING", "UNION", "DISTINCT", "COUNT",
            "SUM", "AVG", "MIN", "MAX", "AS", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "CONSTRAINT", "DEFAULT", "UNIQUE", "CHECK", "BEGIN",
            "END", "COMMIT", "ROLLBACK", "TRANSACTION"
    );

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

        // Normalizar código SQL (mayúsculas para keywords)
        String codigoNormalizado = normalizarCodigoSQL(fuente);

        // Analizar línea por línea
        String[] lineas = codigoNormalizado.split("\n", -1);

        for (int numeroLinea = 0; numeroLinea < lineas.length; numeroLinea++) {
            String linea = lineas[numeroLinea];
            tokenizarLineaSQL(linea, numeroLinea + 1, tokens, errores);
        }

        // Validaciones finales
        validarEstructuraSQL(tokens, errores);

        return tokens;
    }

    /**
     * Normaliza el código SQL manteniendo strings intactos
     */
    private String normalizarCodigoSQL(String codigo) {
        StringBuilder resultado = new StringBuilder();
        boolean enString = false;
        boolean enComentario = false;
        char caracterString = 0;

        for (int i = 0; i < codigo.length(); i++) {
            char c = codigo.charAt(i);

            if (!enString && !enComentario) {
                if (c == '\'') {
                    enString = true;
                    caracterString = c;
                } else if (c == '-' && i + 1 < codigo.length() && codigo.charAt(i + 1) == '-') {
                    enComentario = true;
                }
            } else if (enString && c == caracterString) {
                // Verificar si es escape ''
                if (i + 1 < codigo.length() && codigo.charAt(i + 1) == caracterString) {
                    resultado.append(c); // Agregar el primer '
                    resultado.append(codigo.charAt(++i)); // Agregar el segundo '
                    continue;
                } else {
                    enString = false;
                }
            } else if (enComentario && c == '\n') {
                enComentario = false;
            }

            // Normalizar a mayúsculas solo fuera de strings y comentarios
            if (!enString && !enComentario && Character.isLetter(c)) {
                resultado.append(Character.toUpperCase(c));
            } else {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }

    /**
     * Tokeniza una línea SQL completa
     */
    private void tokenizarLineaSQL(String linea, int numeroLinea, List<Token> tokens, List<AnalysisError> errores) {
        if (linea.trim().isEmpty()) {
            return;
        }

        int posicion = 0;

        while (posicion < linea.length()) {
            // Saltar espacios
            if (Character.isWhitespace(linea.charAt(posicion))) {
                posicion++;
                continue;
            }

            // Intentar tokenización
            ResultadoTokenSQL resultado = intentarTokenizarSQL(linea, posicion, numeroLinea, errores);

            if (resultado.exito) {
                if (resultado.token != null) {
                    tokens.add(resultado.token);
                }
                posicion = resultado.siguientePosicion;
            } else {
                // Carácter no reconocido
                registrarCaracterInvalidoSQL(linea.charAt(posicion), numeroLinea, posicion, errores);
                posicion++;
            }
        }
    }

    /**
     * Intenta tokenizar en la posición actual
     */
    private ResultadoTokenSQL intentarTokenizarSQL(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        // 1. Comentarios de bloque
        ResultadoTokenSQL resultado = reconocerComentarioBloque(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 2. Comentarios de línea
        resultado = reconocerComentarioLinea(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 3. Strings
        resultado = reconocerStringSQL(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 4. Números
        resultado = reconocerNumeroSQL(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 5. Operadores
        resultado = reconocerOperadorSQL(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 6. Identificadores/Keywords
        resultado = reconocerIdentificadorSQL(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 7. Puntuación
        resultado = reconocerPuntuacionSQL(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce comentarios de bloque /* */

    private ResultadoTokenSQL reconocerComentarioBloque(String linea, int posicion, int numeroLinea) {
        Matcher matcher = PATRON_COMENTARIO_BLOQUE.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String comentario = matcher.group();
            Token token = new Token(comentario, "COMENTARIO_BLOQUE", numeroLinea, posicion);
            return new ResultadoTokenSQL(true, token, matcher.end());
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce comentarios de línea --
     */
    private ResultadoTokenSQL reconocerComentarioLinea(String linea, int posicion, int numeroLinea) {
        if (posicion + 1 < linea.length() &&
                linea.charAt(posicion) == '-' && linea.charAt(posicion + 1) == '-') {
            String comentario = linea.substring(posicion);
            Token token = new Token(comentario, "COMENTARIO", numeroLinea, posicion);
            return new ResultadoTokenSQL(true, token, linea.length());
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce strings SQL '...'
     */
    private ResultadoTokenSQL reconocerStringSQL(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_STRING.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String valorString = matcher.group();

            // Validar string SQL
            validarStringSQL(valorString, numeroLinea, posicion, errores);

            Token token = new Token(valorString, "STRING", numeroLinea, posicion);
            return new ResultadoTokenSQL(true, token, matcher.end());
        }

        // Verificar string sin terminar
        if (posicion < linea.length() && linea.charAt(posicion) == '\'') {
            String stringIncompleto = linea.substring(posicion);
            errores.add(new AnalysisError(
                    "String SQL sin terminar: " + stringIncompleto,
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion
            ));

            Token token = new Token(stringIncompleto, "STRING_SIN_TERMINAR", numeroLinea, posicion);
            return new ResultadoTokenSQL(true, token, linea.length());
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce números SQL
     */
    private ResultadoTokenSQL reconocerNumeroSQL(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_NUMERO.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String valorNumero = matcher.group();

            // Validar número SQL
            validarNumeroSQL(valorNumero, numeroLinea, posicion, errores);

            String tipoToken = valorNumero.contains(".") ? "NUMERO_DECIMAL" : "NUMERO_ENTERO";
            Token token = new Token(valorNumero, tipoToken, numeroLinea, posicion);

            return new ResultadoTokenSQL(true, token, matcher.end());
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce operadores SQL
     */
    private ResultadoTokenSQL reconocerOperadorSQL(String linea, int posicion, int numeroLinea) {
        Matcher matcher = PATRON_OPERADOR.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String operador = matcher.group();
            Token token = new Token(operador, "OPERADOR", numeroLinea, posicion);
            return new ResultadoTokenSQL(true, token, matcher.end());
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce identificadores y keywords SQL
     */
    private ResultadoTokenSQL reconocerIdentificadorSQL(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_IDENTIFICADOR.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String identificador = matcher.group();

            // Validar identificador SQL
            validarIdentificadorSQL(identificador, numeroLinea, posicion, errores);

            // Determinar tipo
            String tipoToken = KEYWORDS_SQL.contains(identificador.toUpperCase()) ? "KEYWORD" : "IDENTIFICADOR";
            Token token = new Token(identificador, tipoToken, numeroLinea, posicion);

            return new ResultadoTokenSQL(true, token, matcher.end());
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Reconoce puntuación SQL
     */
    private ResultadoTokenSQL reconocerPuntuacionSQL(String linea, int posicion, int numeroLinea) {
        char c = linea.charAt(posicion);
        if ("()[]{}:;,.".indexOf(c) != -1) {
            String tipoPuntuacion = obtenerTipoPuntuacionSQL(c);
            Token token = new Token(String.valueOf(c), tipoPuntuacion, numeroLinea, posicion);
            return new ResultadoTokenSQL(true, token, posicion + 1);
        }

        return new ResultadoTokenSQL(false, null, posicion);
    }

    /**
     * Valida un string SQL
     */
    private void validarStringSQL(String valorString, int numeroLinea, int posicion, List<AnalysisError> errores) {
        // Verificar strings muy largos
        if (valorString.length() > 8000) {
            errores.add(new AnalysisError(
                    "String SQL muy largo (" + valorString.length() + " caracteres)",
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }

        // Verificar caracteres de control
        if (valorString.matches(".*[\\x00-\\x1F\\x7F].*")) {
            errores.add(new AnalysisError(
                    "String contiene caracteres de control",
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }
    }

    /**
     * Valida un número SQL
     */
    private void validarNumeroSQL(String valorNumero, int numeroLinea, int posicion, List<AnalysisError> errores) {
        try {
            if (valorNumero.contains(".")) {
                double valor = Double.parseDouble(valorNumero);
                if (Double.isInfinite(valor) || Double.isNaN(valor)) {
                    errores.add(new AnalysisError(
                            "Número decimal fuera de rango: " + valorNumero,
                            AnalysisError.ErrorType.LEXICAL,
                            numeroLinea, posicion
                    ));
                }
            } else {
                long valor = Long.parseLong(valorNumero);
                if (valor > Integer.MAX_VALUE) {
                    errores.add(new AnalysisError(
                            "Número entero muy grande: " + valorNumero,
                            AnalysisError.ErrorType.WARNING,
                            numeroLinea, posicion
                    ));
                }
            }
        } catch (NumberFormatException e) {
            errores.add(new AnalysisError(
                    "Formato de número inválido: " + valorNumero,
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion
            ));
        }
    }

    /**
     * Valida un identificador SQL
     */
    private void validarIdentificadorSQL(String identificador, int numeroLinea, int posicion, List<AnalysisError> errores) {
        // Verificar longitud
        if (identificador.length() > 128) {
            errores.add(new AnalysisError(
                    "Identificador SQL muy largo (" + identificador.length() + " caracteres)",
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }

        // Verificar keywords mal escritos
        Map<String, String> erroresComunes = Map.of(
                "SELEC", "SELECT",
                "FORM", "FROM",
                "WHRE", "WHERE",
                "CREAT", "CREATE",
                "INSER", "INSERT"
        );

        String sugerencia = erroresComunes.get(identificador.toUpperCase());
        if (sugerencia != null) {
            errores.add(new AnalysisError(
                    "Posible keyword mal escrito: '" + identificador + "', ¿quiso decir '" + sugerencia + "'?",
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }
    }

    /**
     * Obtiene el tipo de puntuación SQL
     */
    private String obtenerTipoPuntuacionSQL(char c) {
        switch (c) {
            case '(': return "PAREN_IZQ";
            case ')': return "PAREN_DER";
            case '[': return "CORCHETE_IZQ";
            case ']': return "CORCHETE_DER";
            case '{': return "LLAVE_IZQ";
            case '}': return "LLAVE_DER";
            case ':': return "DOS_PUNTOS";
            case ';': return "PUNTO_COMA";
            case ',': return "COMA";
            case '.': return "PUNTO";
            default: return "PUNTUACION";
        }
    }

    /**
     * Registra carácter inválido SQL
     */
    private void registrarCaracterInvalidoSQL(char caracter, int numeroLinea, int posicion, List<AnalysisError> errores) {
        errores.add(new AnalysisError(
                "Carácter no reconocido en SQL: '" + caracter + "'",
                AnalysisError.ErrorType.LEXICAL,
                numeroLinea, posicion
        ));
    }

    /**
     * Validaciones finales de estructura SQL
     */
    private void validarEstructuraSQL(List<Token> tokens, List<AnalysisError> errores) {
        // Verificar queries básicas
        validarEstructuraQueries(tokens, errores);

        // Verificar delimitadores balanceados
        validarDelimitadoresSQL(tokens, errores);
    }

    /**
     * Valida estructura básica de queries SQL
     */
    private void validarEstructuraQueries(List<Token> tokens, List<AnalysisError> errores) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if ("KEYWORD".equals(token.getType())) {
                switch (token.getValue().toUpperCase()) {
                    case "SELECT":
                        validarSelectQuery(tokens, i, errores);
                        break;
                    case "INSERT":
                        validarInsertQuery(tokens, i, errores);
                        break;
                    case "UPDATE":
                        validarUpdateQuery(tokens, i, errores);
                        break;
                    case "DELETE":
                        validarDeleteQuery(tokens, i, errores);
                        break;
                }
            }
        }
    }

    /**
     * Valida query SELECT básica
     */
    private void validarSelectQuery(List<Token> tokens, int inicioSelect, List<AnalysisError> errores) {
        boolean tieneFrom = false;

        for (int i = inicioSelect + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("KEYWORD".equals(token.getType()) && "FROM".equals(token.getValue())) {
                tieneFrom = true;
                break;
            }
            if ("KEYWORD".equals(token.getType()) && "SELECT".equals(token.getValue())) {
                break; // Otro SELECT
            }
        }

        if (!tieneFrom) {
            errores.add(new AnalysisError(
                    "SELECT sin cláusula FROM",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(inicioSelect).getLine(), tokens.get(inicioSelect).getColumn()
            ));
        }
    }

    /**
     * Valida query INSERT básica
     */
    private void validarInsertQuery(List<Token> tokens, int inicioInsert, List<AnalysisError> errores) {
        boolean tieneInto = false;
        boolean tieneValues = false;

        for (int i = inicioInsert + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("KEYWORD".equals(token.getType())) {
                if ("INTO".equals(token.getValue())) {
                    tieneInto = true;
                } else if ("VALUES".equals(token.getValue())) {
                    tieneValues = true;
                } else if ("INSERT".equals(token.getValue())) {
                    break; // Otro INSERT
                }
            }
        }

        if (!tieneInto) {
            errores.add(new AnalysisError(
                    "INSERT debe incluir INTO",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(inicioInsert).getLine(), tokens.get(inicioInsert).getColumn()
            ));
        }

        if (!tieneValues) {
            errores.add(new AnalysisError(
                    "INSERT debe incluir VALUES",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(inicioInsert).getLine(), tokens.get(inicioInsert).getColumn()
            ));
        }
    }

    /**
     * Valida query UPDATE básica
     */
    private void validarUpdateQuery(List<Token> tokens, int inicioUpdate, List<AnalysisError> errores) {
        boolean tieneSet = false;

        for (int i = inicioUpdate + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("KEYWORD".equals(token.getType()) && "SET".equals(token.getValue())) {
                tieneSet = true;
                break;
            }
            if ("KEYWORD".equals(token.getType()) && "UPDATE".equals(token.getValue())) {
                break; // Otro UPDATE
            }
        }

        if (!tieneSet) {
            errores.add(new AnalysisError(
                    "UPDATE debe incluir SET",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(inicioUpdate).getLine(), tokens.get(inicioUpdate).getColumn()
            ));
        }
    }

    /**
     * Valida query DELETE básica
     */
    private void validarDeleteQuery(List<Token> tokens, int inicioDelete, List<AnalysisError> errores) {
        boolean tieneFrom = false;
        boolean tieneWhere = false;

        for (int i = inicioDelete + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("KEYWORD".equals(token.getType())) {
                if ("FROM".equals(token.getValue())) {
                    tieneFrom = true;
                } else if ("WHERE".equals(token.getValue())) {
                    tieneWhere = true;
                } else if ("DELETE".equals(token.getValue())) {
                    break; // Otro DELETE
                }
            }
        }

        if (!tieneFrom) {
            errores.add(new AnalysisError(
                    "DELETE debe incluir FROM",
                    AnalysisError.ErrorType.SYNTACTIC,
                    tokens.get(inicioDelete).getLine(), tokens.get(inicioDelete).getColumn()
            ));
        }

        if (!tieneWhere) {
            errores.add(new AnalysisError(
                    "DELETE sin WHERE eliminará todos los registros - PELIGRO",
                    AnalysisError.ErrorType.WARNING,
                    tokens.get(inicioDelete).getLine(), tokens.get(inicioDelete).getColumn()
            ));
        }
    }

    /**
     * Valida delimitadores balanceados
     */
    private void validarDelimitadoresSQL(List<Token> tokens, List<AnalysisError> errores) {
        int contadorParentesis = 0;

        for (Token token : tokens) {
            if ("PAREN_IZQ".equals(token.getType())) {
                contadorParentesis++;
            } else if ("PAREN_DER".equals(token.getType())) {
                contadorParentesis--;
                if (contadorParentesis < 0) {
                    errores.add(new AnalysisError(
                            "Paréntesis de cierre sin apertura",
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        if (contadorParentesis > 0) {
            errores.add(new AnalysisError(
                    contadorParentesis + " paréntesis sin cerrar",
                    AnalysisError.ErrorType.SYNTACTIC,
                    1, 0
            ));
        }
    }

    /**
     * Clase auxiliar para resultados de tokenización SQL
     */
    private static class ResultadoTokenSQL {
        final boolean exito;
        final Token token;
        final int siguientePosicion;

        ResultadoTokenSQL(boolean exito, Token token, int siguientePosicion) {
            this.exito = exito;
            this.token = token;
            this.siguientePosicion = siguientePosicion;
        }
    }
}

