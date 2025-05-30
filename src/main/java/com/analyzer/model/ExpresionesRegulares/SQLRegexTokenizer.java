package com.analyzer.model.ExpresionesRegulares;

import com.analyzer.model.Token;
import java.util.*;
import java.util.regex.*;

public class SQLRegexTokenizer {

    private static class PatronToken {
        final Pattern patron;
        final String tipo;
        PatronToken(String tipo, String regex) {
            this.tipo = tipo;
            this.patron = Pattern.compile("^(" + regex + ")", Pattern.CASE_INSENSITIVE);
        }
    }

    private static final List<PatronToken> patrones;
    static {
        List<PatronToken> lista = new ArrayList<>();

        // ===== ORDEN CRÍTICO: ERRORES ANTES QUE TOKENS VÁLIDOS =====

        // 1. COMENTARIOS (errores primero)
        lista.add(new PatronToken("ERROR_COMENTARIO", "--[^\\r\\n]*$")); // comentario sin cerrar
        lista.add(new PatronToken("COMENTARIO", "--[^\\r\\n]*"));
        lista.add(new PatronToken("COMENTARIO", "/\\*[\\s\\S]*?\\*/"));

        lista.add(new PatronToken("ERROR_CADENA_DOBLE", "\"[^\"\\n]*(?=\\n|$)")); // cadena doble sin cerrar
        lista.add(new PatronToken("ERROR_CADENA_SIMPLE", "'[^'\\n]*(?=\\n|$)")); // cadena simple sin cerrar

        // 3. CADENAS VÁLIDAS (después de errores)
        lista.add(new PatronToken("CADENA_DOBLE", "\"(?:[^\"\\\\]|\\\\.)*\""));
        lista.add(new PatronToken("CADENA_SIMPLE", "'(?:[^'\\\\]|\\\\.)*'"));

        // 4. ERRORES DE NÚMEROS (ANTES que números válidos)
        lista.add(new PatronToken("ERROR_NUMERO_DECIMAL", "\\d+(?:\\.\\d+){2,}")); // múltiples puntos
        lista.add(new PatronToken("ERROR_NUMERO_PUNTO_LETRAS", "\\d+\\.[a-zA-Z]+")); // 12.abc
        //lista.add(new PatronToken("ERROR_NUMERO_LETRAS", "\\d+[a-zA-Z]+")); // 123abc
        lista.add(new PatronToken("ERROR_IDENTIFICADOR_NUMEROS", "\\d+[a-zA-Z]+"));// Identificador que empieza con número


        // 5. NÚMEROS VÁLIDOS (después de errores)
        lista.add(new PatronToken("NUMERO_DECIMAL", "\\d+\\.\\d+"));
        lista.add(new PatronToken("NUMERO_ENTERO", "\\d+"));

        // 6. ERRORES DE PALABRAS CLAVE (ANTES que palabras válidas)
        String[] erroresPalabras = {
                "\\bSELE?C*T?\\b(?<!SELECT)", // SELEC, SELCT, etc.
                "\\bF+R+O*M+\\b(?<!FROM)", // FRON, FRROM, etc.
                "\\bUP+[DT]*A?T?E*\\b(?<!UPDATE)", // UPDTE, UPATE, etc.
                "\\bDELE*T*E?\\b(?<!DELETE)", // DELET, DELE, etc.
                "\\bINSE?R*T*\\b(?<!INSERT)", // INSRT, INSER, etc.
                "\\bCR+E*A*T*E?\\b(?<!CREATE)", // CREAT, CRETE, etc.
                "\\bTA?B*L*E*\\b(?<!TABLE)(?=\\s)", // TABL, TALE, etc.
                "\\bWH*E*R*E?\\b(?<!WHERE)(?=\\s)", // WHER, WHR, etc.
                "\\bV[AL]*U*E*S*\\b(?<!VALUES)", // VALES, VALS, etc.
                "\\bGR+O*U*P*\\b(?<!GROUP)(?=\\s+BY)", // GRUP, GROOP, etc.
                "\\bOR+D*E*R*\\b(?<!ORDER)(?=\\s+BY)", // ORDE, ORDR, etc.
                "\\bHAV+I*N*G*\\b(?<!HAVING)", // HAVIN, HAVNG, etc.
                "\\bPR+I*M+A*R*Y*\\b(?<!PRIMARY)(?=\\s+KEY)", // PRIMRY, PRIMAY, etc.
                "\\bFOR+E*I*G*N*\\b(?<!FOREIGN)(?=\\s+KEY)", // FOREIG, FORIGN, etc.
                "\\bDEF+A*U*L*T*\\b(?<!DEFAULT)", // DEFALT, DEFULT, etc.
                "\\bU+N*I*Q*U*E*\\b(?<!UNIQUE)" // UNIQ, UNQUE, etc.
        };
        for (String error : erroresPalabras) {
            lista.add(new PatronToken("ERROR_PALABRA_CLAVE", error));
        }

        // 7. PALABRAS CLAVE VÁLIDAS (después de errores, ANTES que tipos de datos)
        String[] palabrasClave = {
                "CREATE|DROP|ALTER",
                "TABLE|DATABASE|SCHEMA|INDEX",
                "PRIMARY|FOREIGN|KEY|REFERENCES|CONSTRAINT",
                "UNIQUE|NOT|NULL|DEFAULT|CHECK",
                "AUTO_INCREMENT|IDENTITY",
                "SELECT|INSERT|UPDATE|DELETE",
                "FROM|WHERE|SET|VALUES",
                "GROUP|BY|ORDER|HAVING",
                "AND|OR|IN|LIKE|BETWEEN|IS",
                "ASC|DESC|DISTINCT|ALL|AS"
        };
        for (String grupo : palabrasClave) {
            lista.add(new PatronToken("PALABRA_CLAVE", "\\b(?:" + grupo + ")\\b"));
        }

        // 8. TIPOS DE DATO (después de palabras clave, ANTES que identificadores)
        lista.add(new PatronToken("TIPO_DATO_ENTERO", "\\b(?:INT|INTEGER|BIGINT|SMALLINT|TINYINT)\\b"));
        lista.add(new PatronToken("TIPO_DATO_NUMERICO", "\\b(?:DECIMAL|NUMERIC|FLOAT|DOUBLE|REAL)\\b"));
        lista.add(new PatronToken("TIPO_DATO_TEXTO", "\\b(?:VARCHAR|CHAR|TEXT|NVARCHAR|NCHAR)\\b"));
        lista.add(new PatronToken("TIPO_DATO_FECH_HOR", "\\b(?:DATE|TIME|DATETIME|TIMESTAMP|YEAR)\\b"));
        lista.add(new PatronToken("TIPO_DATO_BOOL", "\\b(?:BOOLEAN|BOOL|BIT)\\b"));
        lista.add(new PatronToken("TIPO_DATO_BINARIO", "\\b(?:BLOB|CLOB|JSON|XML)\\b"));

        // 9. VALORES BOOLEANOS (antes que identificadores)
        lista.add(new PatronToken("VALOR_BOOLEANO", "\\b(?:TRUE|FALSE)\\b"));

        // 10. IDENTIFICADORES CON DELIMITADORES (antes que errores de identificadores)
        lista.add(new PatronToken("IDENTIFICADOR_ENTRE_COMILLAS", "`[^`]+`"));
        lista.add(new PatronToken("IDENTIFICADOR_ENTRE_CORCH", "\\[[^\\]]+\\]"));

        // 11. ERRORES DE IDENTIFICADORES (ANTES que identificadores válidos)
        // Identificador que empieza con número
        // Identificador con caracteres especiales inválidos
        lista.add(new PatronToken("ERROR_IDENTIFICADOR_CARACTER_INVAL", "\\b[a-zA-Z_][a-zA-Z0-9_]*[^a-zA-Z0-9_\\s()\\[\\]{},;.+\\-*/%<>=!]+[a-zA-Z0-9_]*\\b"));// Identificador con caracteres especiales inválidos

        // 12. IDENTIFICADORES VÁLIDOS (después de todos los errores)
        lista.add(new PatronToken("IDENTIFICADOR", "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b"));

        // 13. OPERADORES (orden específico: compuestos antes que simples)
        lista.add(new PatronToken("OPERADOR_COMPARACION", "<=|>=|<>|!=|=="));
        lista.add(new PatronToken("OPERADOR_COMP_SIMPLE", "[<>=!]"));
        lista.add(new PatronToken("OPERADOR_ARITMETICO", "[+\\-*/%]"));

        // 14. SEPARADORES Y PUNTUACIÓN
        lista.add(new PatronToken("SEPARADOR", "[()\\[\\]{},;]"));
        lista.add(new PatronToken("PUNTO", "\\."));

        // 15. ESPACIOS Y SALTOS (se filtran en el método tokenize)
        lista.add(new PatronToken("ESPACIO", "[ \\t]+"));
        lista.add(new PatronToken("SALTO_LINEA", "\\r?\\n"));

        // 16. CARACTERES INVÁLIDOS (AL FINAL para capturar cualquier cosa no reconocida)
        lista.add(new PatronToken("INVALIDO", "."));

        patrones = Collections.unmodifiableList(lista);
    }

    public List<Token> tokenize(String codigo) {
        List<Token> tokens = new ArrayList<>();
        int longitud = codigo.length();
        int pos = 0;
        int linea = 1;
        int columna = 1;

        while (pos < longitud) {
            CharSequence sub = codigo.subSequence(pos, longitud);
            boolean coincidio = false;

            for (PatronToken pt : patrones) {
                Matcher matcher = pt.patron.matcher(sub);
                if (matcher.lookingAt()) {
                    String lexema = matcher.group(1);

                    // Solo agregar tokens que no sean espacios o saltos de línea
                    if (!pt.tipo.equals("ESPACIO") && !pt.tipo.equals("SALTO_LINEA")) {
                        tokens.add(new Token(lexema, pt.tipo, linea, columna));
                    }

                    // Actualizar posición de línea y columna
                    for (char c : lexema.toCharArray()) {
                        if (c == '\n') {
                            linea++;
                            columna = 1;
                        } else {
                            columna++;
                        }
                    }

                    pos += lexema.length();
                    coincidio = true;
                    break;
                }
            }

            if (!coincidio) {
                // Si no coincide ningún patrón, avanzar un carácter
                char c = codigo.charAt(pos);
                if (c == '\n') {
                    linea++;
                    columna = 1;
                } else {
                    columna++;
                }
                pos++;
            }
        }

        return tokens;
    }
}