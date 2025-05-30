package com.analyzer.service.LexicalAnalizer;

import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Analizador léxico especializado para Python.
 * Maneja keywords, identificadores, números, strings, operadores e indentación.
 */
public  class PythonLexicalAnalyzer implements ILexicalAnalyzer {

    // Patrones regex para Python
//    private static final Pattern PATRON_KEYWORD = Pattern.compile(
//            "\\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b"
//    );

    private static final Pattern PATRON_IDENTIFICADOR = Pattern.compile(
            "[a-zA-Z_][a-zA-Z0-9_]*"
    );

    private static final Pattern PATRON_NUMERO = Pattern.compile(
            "(?:0[bB][01]+(?:_[01]+)*|0[oO][0-7]+(?:_[0-7]+)*|0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*|(?:\\d+(?:_\\d+)*(?:\\.\\d+(?:_\\d+)*)?(?:[eE][+-]?\\d+(?:_\\d+)*)?|\\.\\d+(?:_\\d+)*(?:[eE][+-]?\\d+(?:_\\d+)*)?))(?![a-zA-Z_])"
    );

    private static final Pattern PATRON_STRING = Pattern.compile(
            "(?:r?b?|b?r?)(?:'''(?:[^\\\\']|\\\\.)*?'''|\"\"\"(?:[^\\\\\"]|\\\\.)*?\"\"\"|'(?:[^\\\\']|\\\\.)*?'|\"(?:[^\\\\\"]|\\\\.)*?\")"
    );

    private static final Pattern PATRON_OPERADOR = Pattern.compile(
            "//=|\\*\\*=|<<=|>>=|:=|==|!=|<=|>=|//|\\*\\*|<<|>>|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|->|[+\\-*/%=<>!&|^~@]"
    );

    private static final Pattern PATRON_COMENTARIO = Pattern.compile(
            "#.*$"
    );

    private static final Pattern PATRON_INDENTACION = Pattern.compile(
            "^[ \\t]*"
    );

    // Conjunto de keywords Python
    private static final Set<String> KEYWORDS_PYTHON = Set.of(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "range",
            "continue", "def", "del", "elif", "else", "except", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
            "raise", "return", "try", "while", "with", "yield", "print", "exec", "delattr", "getattr", "setattr", "hasattr"
    );

    // Control de indentación
    private final Stack<Integer> pilaIndentacion = new Stack<>();
    private boolean tieneEspacios = false;
    private boolean tieneTabs = false;

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

        // Inicializar pila de indentación
        pilaIndentacion.clear();
        pilaIndentacion.push(0);
        tieneEspacios = false;
        tieneTabs = false;

        // Analizar línea por línea
        String[] lineas = fuente.split("\n", -1);

        for (int numeroLinea = 0; numeroLinea < lineas.length; numeroLinea++) {
            String linea = lineas[numeroLinea];

            // Procesar indentación
            procesarIndentacion(linea, numeroLinea + 1, errores);

            // Tokenizar contenido de la línea
            tokenizarLineaPython(linea, numeroLinea + 1, tokens, errores);
        }

        // Validaciones finales
        validarEstadoFinal(tokens, errores);

        return tokens;
    }

    /**
     * Procesa la indentación de una línea Python
     */
    private void procesarIndentacion(String linea, int numeroLinea, List<AnalysisError> errores) {
        // Ignorar líneas vacías y comentarios para indentación
        if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
            return;
        }

        InfoIndentacion info = analizarIndentacion(linea);

        // Verificar consistencia tabs/espacios
        verificarConsistenciaIndentacion(info, numeroLinea, errores);

        // Validar nivel de indentación
        validarNivelIndentacion(info, numeroLinea, errores);
    }

    /**
     * Analiza la indentación de una línea
     */
    private InfoIndentacion analizarIndentacion(String linea) {
        int espacios = 0;
        int tabs = 0;
        int nivelIndentacion = 0;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == ' ') {
                espacios++;
                nivelIndentacion++;
            } else if (c == '\t') {
                tabs++;
                nivelIndentacion += 8; // Python considera cada tab como 8 espacios
            } else {
                break;
            }
        }


        return new InfoIndentacion(espacios, tabs);
    }



    /**
     * Verifica consistencia entre tabs y espacios
     */
    private void verificarConsistenciaIndentacion(InfoIndentacion info, int numeroLinea, List<AnalysisError> errores) {
        // Mezcla en la misma línea
        if (info.espacios > 0 && info.tabs > 0) {
            errores.add(new AnalysisError(
                    "Mezcla inconsistente de tabs y espacios en la indentación",
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, 0
            ));
        }

        // Registro de uso en el archivo
        if (info.espacios > 0) tieneEspacios = true;
        if (info.tabs > 0) tieneTabs = true;

        // Mezcla en el archivo
        if (tieneEspacios && tieneTabs) {
            errores.add(new AnalysisError(
                    "Uso inconsistente de tabs y espacios en el archivo (TabError)",
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, 0
            ));
        }
    }

    /**
     * Valida el nivel de indentación
     */
    private void validarNivelIndentacion(InfoIndentacion info, int numeroLinea, List<AnalysisError> errores) {
        int nivelActual = info.espacios + (info.tabs * 8); // Tab = 8 espacios
        int nivelPrevio = pilaIndentacion.peek();

        if (nivelActual > nivelPrevio) {
            // Incremento de indentación
            pilaIndentacion.push(nivelActual);
        } else if (nivelActual < nivelPrevio) {
            // Decremento de indentación
            while (!pilaIndentacion.isEmpty() && pilaIndentacion.peek() > nivelActual) {
                pilaIndentacion.pop();
            }

            if (pilaIndentacion.isEmpty() || pilaIndentacion.peek() != nivelActual) {
                errores.add(new AnalysisError(
                        "El nivel de indentación no coincide con ningún nivel externo",
                        AnalysisError.ErrorType.LEXICAL,
                        numeroLinea, 0
                ));
            }
        }
    }

    /**
     * Tokeniza una línea Python completa
     */
    private void tokenizarLineaPython(String linea, int numeroLinea, List<Token> tokens, List<AnalysisError> errores) {
        if (linea.trim().isEmpty()) {
            return;
        }

        int posicion = 0;

        // Saltar indentación inicial
        while (posicion < linea.length() && Character.isWhitespace(linea.charAt(posicion))) {
            posicion++;
        }

        while (posicion < linea.length()) {
            // Saltar espacios entre tokens
            if (Character.isWhitespace(linea.charAt(posicion))) {
                posicion++;
                continue;
            }

            // Si empieza con número, verificar si es parte de un identificador inválido
            if (Character.isDigit(linea.charAt(posicion))) {
                int inicio = posicion;
                while (posicion < linea.length() &&
                        (Character.isLetterOrDigit(linea.charAt(posicion)) ||
                                linea.charAt(posicion) == '_')) {
                    posicion++;
                }
                String token = linea.substring(inicio, posicion);

                // Si contiene letras después del número, es un identificador inválido
                if (token.matches("\\d+[a-zA-Z_].*")) {
                    errores.add(new AnalysisError(
                            "Error léxico: identificador inválido '" + token + "' - no puede comenzar con número",
                            AnalysisError.ErrorType.LEXICAL,
                            numeroLinea,
                            inicio
                    ));
                    tokens.add(new Token(token, "ERROR_LEXICO", numeroLinea, inicio));
                    continue;
                }
                // Si es solo un número, retroceder y dejar que se procese normalmente
                posicion = inicio;
            }

            // Intentar tokenización normal
            ResultadoTokenPython resultado = intentarTokenizarPython(linea, posicion, numeroLinea, errores);

            if (resultado.exito) {
                if (resultado.token != null) {
                    tokens.add(resultado.token);
                }
                posicion = resultado.siguientePosicion;
            } else {
                // Carácter inválido
                registrarCaracterInvalidoPython(linea.charAt(posicion), numeroLinea, posicion, errores);
                posicion++;
            }
        }
    }

    /**
     * Intenta tokenizar en la posición actual
     */
    private ResultadoTokenPython intentarTokenizarPython(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        // 1. Comentarios
        ResultadoTokenPython resultado = reconocerComentarioPython(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 2. Strings
        resultado = reconocerStringPython(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 3. Números
        resultado = reconocerNumeroPython(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 4. Operadores
        resultado = reconocerOperadorPython(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        // 5. Identificadores/Keywords
        resultado = reconocerIdentificadorPython(linea, posicion, numeroLinea, errores);
        if (resultado.exito) return resultado;

        // 6. Puntuación
        resultado = reconocerPuntuacionPython(linea, posicion, numeroLinea);
        if (resultado.exito) return resultado;

        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Reconoce comentarios Python (#)
     */
    private ResultadoTokenPython reconocerComentarioPython(String linea, int posicion, int numeroLinea) {
        if (posicion < linea.length() && linea.charAt(posicion) == '#') {
            String comentario = linea.substring(posicion);
            Token token = new Token(comentario, "COMENTARIO", numeroLinea, posicion);
            return new ResultadoTokenPython(true, token, linea.length());
        }
        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Reconoce strings Python
     */
    private ResultadoTokenPython reconocerStringPython(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_STRING.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String valorString = matcher.group();

            // Validar string
            validarStringPython(valorString, numeroLinea, posicion, errores);

            String tipoToken = determinarTipoString(valorString);
            Token token = new Token(valorString, tipoToken, numeroLinea, posicion);

            return new ResultadoTokenPython(true, token, matcher.end());
        }

        // Verificar string sin terminar
        char c = linea.charAt(posicion);
        if (c == '"' || c == '\'') {
            String stringIncompleto = linea.substring(posicion);
            errores.add(new AnalysisError(
                    "String sin terminar: " + stringIncompleto,
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion
            ));

            Token token = new Token(stringIncompleto, "STRING_SIN_TERMINAR", numeroLinea, posicion);
            return new ResultadoTokenPython(true, token, linea.length());
        }

        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Reconoce números Python
     */
    private ResultadoTokenPython reconocerNumeroPython(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_NUMERO.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String valorNumero = matcher.group();

            // Validar número
            validarNumeroPython(valorNumero, numeroLinea, posicion, errores);

            String tipoToken = determinarTipoNumero(valorNumero);
            Token token = new Token(valorNumero, tipoToken, numeroLinea, posicion);

            return new ResultadoTokenPython(true, token, matcher.end());
        }

        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Reconoce operadores Python
     */
    private ResultadoTokenPython reconocerOperadorPython(String linea, int posicion, int numeroLinea) {
        Matcher matcher = PATRON_OPERADOR.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String operador = matcher.group();
            Token token = new Token(operador, "OPERADOR", numeroLinea, posicion);
            return new ResultadoTokenPython(true, token, matcher.end());
        }

        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Reconoce identificadores y keywords Python
     */
    private ResultadoTokenPython reconocerIdentificadorPython(String linea, int posicion, int numeroLinea, List<AnalysisError> errores) {
        Matcher matcher = PATRON_IDENTIFICADOR.matcher(linea);
        matcher.region(posicion, linea.length());

        if (matcher.lookingAt()) {
            String identificador = matcher.group();

            // Validar identificador
            validarIdentificadorPython(identificador, numeroLinea, posicion, errores);

            // Determinar tipo
            String tipoToken = determinarTipoIdentificador(identificador);
            Token token = new Token(identificador, tipoToken, numeroLinea, posicion);

            return new ResultadoTokenPython(true, token, matcher.end());
        }

        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Reconoce puntuación Python
     */
    private ResultadoTokenPython reconocerPuntuacionPython(String linea, int posicion, int numeroLinea) {
        char c = linea.charAt(posicion);
        if ("()[]{}:;,.@".indexOf(c) != -1) {
            String tipoPuntuacion = obtenerTipoPuntuacion(c);
            Token token = new Token(String.valueOf(c), tipoPuntuacion, numeroLinea, posicion);
            return new ResultadoTokenPython(true, token, posicion + 1);
        }

        return new ResultadoTokenPython(false, null, posicion);
    }

    /**
     * Valida un string Python
     */
    private void validarStringPython(String valorString, int numeroLinea, int posicion, List<AnalysisError> errores) {
        // Verificar secuencias de escape inválidas
        Pattern escapeInvalido = Pattern.compile("\\\\[^\\\\\"'nrtbfav0xuUN]");
        Matcher matcher = escapeInvalido.matcher(valorString);

        while (matcher.find()) {
            errores.add(new AnalysisError(
                    "Secuencia de escape inválida: " + matcher.group(),
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion + matcher.start()
            ));
        }

        // Verificar escapes hex incompletos
        Pattern hexIncompleto = Pattern.compile("\\\\x([0-9a-fA-F]{0,1})(?![0-9a-fA-F])");
        matcher = hexIncompleto.matcher(valorString);
        while (matcher.find()) {
            errores.add(new AnalysisError(
                    "Escape hexadecimal incompleto: \\x requiere exactamente 2 dígitos",
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion + matcher.start()
            ));
        }
    }

    /**
     * Valida un número Python
     */
    private void validarNumeroPython(String valorNumero, int numeroLinea, int posicion, List<AnalysisError> errores) {
        // Verificar underscores mal ubicados
        if (valorNumero.startsWith("_") || valorNumero.endsWith("_") || valorNumero.contains("__")) {
            errores.add(new AnalysisError(
                    "Uso inválido de underscore en literal numérico",
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion
            ));
        }

        // Verificar dígitos inválidos por base
        if (valorNumero.startsWith("0b")) {
            Pattern digitoInvalido = Pattern.compile("[2-9]");
            Matcher matcher = digitoInvalido.matcher(valorNumero.substring(2));
            if (matcher.find()) {
                errores.add(new AnalysisError(
                        "Dígito inválido '" + matcher.group() + "' en literal binario",
                        AnalysisError.ErrorType.LEXICAL,
                        numeroLinea, posicion + matcher.start() + 2
                ));
            }
        } else if (valorNumero.startsWith("0o")) {
            Pattern digitoInvalido = Pattern.compile("[89]");
            Matcher matcher = digitoInvalido.matcher(valorNumero.substring(2));
            if (matcher.find()) {
                errores.add(new AnalysisError(
                        "Dígito inválido '" + matcher.group() + "' en literal octal",
                        AnalysisError.ErrorType.LEXICAL,
                        numeroLinea, posicion + matcher.start() + 2
                ));
            }
        } else if (valorNumero.startsWith("0x")) {
            Pattern digitoInvalido = Pattern.compile("[g-zG-Z]");
            Matcher matcher = digitoInvalido.matcher(valorNumero.substring(2));
            if (matcher.find()) {
                errores.add(new AnalysisError(
                        "Dígito inválido '" + matcher.group() + "' en literal hexadecimal",
                        AnalysisError.ErrorType.LEXICAL,
                        numeroLinea, posicion + matcher.start() + 2
                ));
            }
        }
    }

    /**
     * Valida un identificador Python
     */
    private void validarIdentificadorPython(String identificador, int numeroLinea, int posicion, List<AnalysisError> errores) {
        // Verificar que no empiece con número (debería estar cubierto por el patrón)
        if (Character.isDigit(identificador.charAt(0))) {
            errores.add(new AnalysisError(
                    "Identificador no puede empezar con dígito: " + identificador,
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion
            ));
        }

        // Verificar caracteres inválidos (guiones)
        if (identificador.contains("-")) {
            errores.add(new AnalysisError(
                    "Carácter inválido en identificador: '-' (use underscore)",
                    AnalysisError.ErrorType.LEXICAL,
                    numeroLinea, posicion
            ));
        }

        // Advertencias sobre nombres muy cortos
        if (identificador.length() == 1 && !"ijklmnxyztfgabcde_".contains(identificador)) {
            errores.add(new AnalysisError(
                    "Nombre de identificador muy corto: '" + identificador + "'",
                    AnalysisError.ErrorType.WARNING,
                    numeroLinea, posicion
            ));
        }
    }

    /**
     * Determina el tipo de string
     */
    private String determinarTipoString(String valor) {
        if (valor.startsWith("b") || valor.startsWith("B")) return "STRING_BYTES";
        if (valor.startsWith("r") || valor.startsWith("R")) return "STRING_RAW";
        if (valor.startsWith("f") || valor.startsWith("F")) return "STRING_FORMAT";
        if (valor.startsWith("'''") || valor.startsWith("\"\"\"")) return "STRING_TRIPLE";
        return "STRING";
    }

    /**
     * Determina el tipo de número
     */
    private String determinarTipoNumero(String valor) {
        if (valor.startsWith("0b") || valor.startsWith("0B")) return "ENTERO_BINARIO";
        if (valor.startsWith("0o") || valor.startsWith("0O")) return "ENTERO_OCTAL";
        if (valor.startsWith("0x") || valor.startsWith("0X")) return "ENTERO_HEXADECIMAL";
        if (valor.contains(".") || valor.contains("e") || valor.contains("E")) return "FLOTANTE";
        return "ENTERO";
    }

    /**
     * Determina el tipo de identificador
     */
    private String determinarTipoIdentificador(String identificador) {
        if (KEYWORDS_PYTHON.contains(identificador)) return "KEYWORD";
        if (identificador.startsWith("__") && identificador.endsWith("__")) return "METODO_MAGICO";
        if (identificador.startsWith("_")) return "IDENTIFICADOR_PRIVADO";
        if (identificador.matches("[A-Z_]+")) return "CONSTANTE";
        return "IDENTIFICADOR";
    }

    /**
     * Obtiene el tipo de puntuación
     */
    private String obtenerTipoPuntuacion(char c) {
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
            case '@': return "ARROBA";
            default: return "PUNTUACION";
        }
    }

    /**
     * Registra carácter inválido
     */
    private void registrarCaracterInvalidoPython(char caracter, int numeroLinea, int posicion, List<AnalysisError> errores) {
        String mensaje;
        switch (caracter) {
            case '$':
                mensaje = "Carácter inválido '$' - no está definido como token en Python";
                break;
            case '¡':
                mensaje = "Carácter inválido '¡' - no permitido en Python";
                break;
            case '¿':
                mensaje = "Carácter inválido '¿' - no permitido en Python";
                break;
            default:
                mensaje = String.format("Carácter inválido: '%c' (Unicode: U+%04X)", caracter, (int)caracter);
                break;
        }

        errores.add(new AnalysisError(
                mensaje,
                AnalysisError.ErrorType.LEXICAL,
                numeroLinea, posicion
        ));
    }

    /**
     * Validaciones finales
     */
    private void validarEstadoFinal(List<Token> tokens, List<AnalysisError> errores) {
        // Verificar delimitadores balanceados
        int contadorParentesis = 0;
        int contadorCorchetes = 0;
        int contadorLlaves = 0;

        for (Token token : tokens) {
            switch (token.getType()) {
                case "PAREN_IZQ": contadorParentesis++; break;
                case "PAREN_DER": contadorParentesis--; break;
                case "CORCHETE_IZQ": contadorCorchetes++; break;
                case "CORCHETE_DER": contadorCorchetes--; break;
                case "LLAVE_IZQ": contadorLlaves++; break;
                case "LLAVE_DER": contadorLlaves--; break;
            }
        }

        if (contadorParentesis != 0) {
            errores.add(new AnalysisError(
                    "EOF en declaración multilínea: " + Math.abs(contadorParentesis) + " paréntesis sin cerrar",
                    AnalysisError.ErrorType.LEXICAL,
                    1, 0
            ));
        }

        if (contadorCorchetes != 0) {
            errores.add(new AnalysisError(
                    "EOF en declaración multilínea: " + Math.abs(contadorCorchetes) + " corchetes sin cerrar",
                    AnalysisError.ErrorType.LEXICAL,
                    1, 0
            ));
        }

        if (contadorLlaves != 0) {
            errores.add(new AnalysisError(
                    "EOF en declaración multilínea: " + Math.abs(contadorLlaves) + " llaves sin cerrar",
                    AnalysisError.ErrorType.LEXICAL,
                    1, 0
            ));
        }
    }

    /**
     * Clase auxiliar para información de indentación
     */
    private static class InfoIndentacion {
        final int espacios;
        final int tabs;

        InfoIndentacion(int espacios, int tabs) {
            this.espacios = espacios;
            this.tabs = tabs;
        }
    }

    /**
     * Clase auxiliar para resultados de tokenización Python
     */
    private static class ResultadoTokenPython {
        final boolean exito;
        final Token token;
        final int siguientePosicion;

        ResultadoTokenPython(boolean exito, Token token, int siguientePosicion) {
            this.exito = exito;
            this.token = token;
            this.siguientePosicion = siguientePosicion;
        }
    }
}