package com.analyzer.model.ExpresionesRegulares;

import com.analyzer.model.Token;
import java.util.*;
import java.util.regex.*;

public class SQLRegexTokenizer {
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(?i)\\b(SELECT|FROM|WHERE|AND|OR|INSERT|UPDATE|DELETE)\\b|" + // Keywords
        "'[^']*'|" +                                                    // Strings
        "\\b\\d+(\\.\\d+)?\\b|" +                                      // Numbers
        "[,;()]|" +                                                     // Punctuation
        "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b|" +                             // Identifiers
        "\\s+|" +                                                       // Whitespace
        ".",                                                           // Any other char
        Pattern.CASE_INSENSITIVE
    );

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        int line = 1;
        int column = 0;

        while (matcher.find()) {
            String value = matcher.group();
            
            if (value.trim().isEmpty()) {
                if (value.contains("\n")) {
                    line++;
                    column = 0;
                }
                continue;
            }

            String type = getTokenType(value);
            tokens.add(new Token(value, type, line, column));
            column += value.length();
        }

        return tokens;
    }

    private String getTokenType(String value) {
        if (value.matches("(?i)\\b(SELECT|FROM|WHERE|AND|OR|INSERT|UPDATE|DELETE)\\b")) {
            return "KEYWORD";
        } else if (value.matches("'[^']*'")) {
            return "CADENA";
        } else if (value.matches("\\b\\d+(\\.\\d+)?\\b")) {
            return "NUMERO";
        } else if (value.matches("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")) {
            return "IDENTIFICADOR";
        } else if (value.matches("[,;()]")) {
            return "PUNTUACION";
        } else {
            return "INVALIDO";
        }
    }
}
