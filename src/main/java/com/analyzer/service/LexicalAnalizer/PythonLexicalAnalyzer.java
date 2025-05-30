package com.analyzer.service.LexicalAnalizer;

import java.util.*;
import java.util.regex.*;
import com.analyzer.model.Token;
import com.analyzer.model.Symbol;
import com.analyzer.model.Symbol.SymbolType;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.ExpresionesRegulares.PythonRegexTokenizer;

/**
 * Implementación de ILexicalAnalyzer para Python.
 * Usa expresiones regulares para tokenizar, llenar tabla de símbolos y reportar errores.
 */
public class PythonLexicalAnalyzer implements ILexicalAnalyzer {

    private final PythonRegexTokenizer tokenizer = new PythonRegexTokenizer();
    private List<Symbol> symbolTable;
    private List<AnalysisError> errorList;

    /**
     * Tokeniza el código Python en una lista de tokens.
     */
    @Override
    public List<Token> tokenize(String code) {
        return tokenizer.tokenize(code);
    }

    /**
     * Realiza el análisis léxico: tokeniza y llena 'errores' con AnalysisError.
     * Devuelve la lista de tokens generados.
     */
    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        this.errorList = errores;
        List<Token> tokens = tokenizer.tokenize(fuente);
        symbolTable = new ArrayList<>();
        int line = 1;


        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int column = token.getColumn();

            // Actualiza línea según saltos en el lexema
            for (char c : lexeme.toCharArray()) {
                if (c == '\n') line++;
            }

            // Registra símbolo
            SymbolType symType;
            if ("IDENTIFICADOR".equals(type)) {
                symType = SymbolType.VARIABLE;
            } else if ("NUMBER".equals(type) || "STRING".equals(type)) {
                symType = SymbolType.CONSTANT;
            } else {
                symType = SymbolType.UNKNOWN;
            }
            Symbol sym = new Symbol(lexeme, symType);
            sym.setDeclarationLine(line);
            sym.setDeclarationColumn(column);
            symbolTable.add(sym);

            // Reporta errores léxicos
            if (type.startsWith("ERROR_") || "INVALIDO".equals(type)) {
                String description;
                switch (type) {
                    case "ERROR_COMMENT":           description = "Comentario inválido"; break;
                    case "INVALID_COMMENT":         description = "Comentario mal formado"; break;
                    case "ERROR_CADENA_DOUBLE":    description = "Cadena doble sin cerrar"; break;
                    case "ERROR_CADENA_SINGLE":    description = "Cadena simple sin cerrar"; break;
                    case "ERROR_NUMERO_MULT_PUNTOS": description = "Número con múltiples puntos"; break;
                    case "ERROR_LIT_BINARIO":      description = "Literal binario inválido"; break;
                    case "ERROR_IDENTIFICADOR":    description = "Identificador inválido"; break;
                    case "ERROR_OPERADOR_SEQ":     description = "Secuencia de operador inválida"; break;
                    case "INVALIDO":               description = "Token desconocido"; break;
                    default:                       description = "Error léxico"; break;
                }
                errores.add(new AnalysisError(
                    String.format("%s: '%s' (línea %d)", description, lexeme.trim(), line),
                    AnalysisError.ErrorType.LEXICAL,
                    line,
                    column
                ));
            }
        }
        return tokens;
    }

    /**
     * Método genérico de análisis (interfaz): devuelve tokens y omite errores.
     */
    @Override
    public List<Token> analyze(String code, LanguageType language) {
        // language se ignora en esta implementación
        return analyzeLexical(code, new ArrayList<>());
    }

    public List<AnalysisError> getErrorList() {
        return Collections.unmodifiableList(errorList);
    }

    /**
     * Obtiene la tabla de símbolos tras el análisis.
     */
    public List<Symbol> getSymbolTable() {
        return Collections.unmodifiableList(symbolTable);
    }
}
