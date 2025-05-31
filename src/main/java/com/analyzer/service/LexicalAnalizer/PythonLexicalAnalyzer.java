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
    public List<Token> analyzeLexical(String source, List<AnalysisError> errors) {
        List<Token> tokens = tokenizer.tokenize(source);
        symbolTable = new ArrayList<>();
        errorList = errors;

        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int line = token.getLine();
            int column = token.getColumn();

            // Registro de símbolos
            if ("IDENTIFIER".equals(type)) {
                Symbol symbol = new Symbol(lexeme, SymbolType.VARIABLE);
                symbol.setDeclarationLine(line);
                symbol.setDeclarationColumn(column);
                symbolTable.add(symbol);
            }

            // Manejo de errores
            if (type.startsWith("ERROR_") || type.equals("INVALID_COMMENT") || type.equals("INVALIDO")) {
                String description;
                switch (type) {
                    case "ERROR_COMMENT":

                        errors.add(new AnalysisError(
                                "Comentario mal formado",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));

                        break;


                    case "ERROR_CADENA_DOUBLE":
                        errors.add(new AnalysisError(
                                "Cadena doble sin cerrar",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));

                        break;
                    case "ERROR_CADENA_SINGLE":
                        errors.add(new AnalysisError(
                                "Cadena simple sin cerrar",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ))
                        ;
                        break;
                    case "ERROR_IDENTIFICADOR":
                        errors.add(new AnalysisError(
                                "Identificador inválido (no puede comenzar con un dígito)",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));
                        break;
                    case "ERROR_NUMERO_MULT_PUNTOS":
                        errors.add(new AnalysisError(
                                "Número con múltiples puntos decimales",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));

                        break;
                    case "ERROR_LIT_BINARIO":
                        errors.add(new AnalysisError(
                                "Literal binario inválido",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));

                        break;



                    case "ERROR_OPERADOR_SEQ":
                        errors.add(new AnalysisError(
                                "operador secuencial inválido",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));

                        break;

                    case "INVALIDO":
                        errors.add(new AnalysisError(
                                "Token inválido: '" + lexeme + "'",
                                AnalysisError.ErrorType.LEXICAL,
                                line,
                                column
                        ));

                        break;
                    default:

                        break;
                }
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
