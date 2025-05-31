package com.analyzer.service.LexicalAnalizer;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import com.analyzer.model.*;
import com.analyzer.model.ExpresionesRegulares.SQLRegexTokenizer;
import com.analyzer.service.interfaces.ILexicalAnalyzer;

public class SQLLexicalAnalyzer implements ILexicalAnalyzer {

    private final SQLRegexTokenizer tokenizer = new SQLRegexTokenizer();

    @Override
    public List<Token> tokenize(String code) {
        return tokenizer.tokenize(code);
    }

    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        // CREAR COPIAS LOCALES PARA EVITAR MODIFICACIÓN CONCURRENTE
        List<Token> tokens = tokenizer.tokenize(fuente);
        List<Symbol> localSymbolTable = new ArrayList<>();

        // CREAR LISTA LOCAL SI ERRORES ES NULL
        List<AnalysisError> localErrors = errores != null ? errores : new ArrayList<>();

        // PROCESAR TOKENS DE FORMA THREAD-SAFE
        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int line = token.getLine();
            int column = token.getColumn();

            // Registro de símbolos
            Symbol.SymbolType symType = determineSymbolType(type);

            if (symType != Symbol.SymbolType.UNKNOWN) {
                Symbol sym = new Symbol(lexeme, symType);
                sym.setDeclarationLine(line);
                sym.setDeclarationColumn(column);
                sym.setScope("global");
                sym.setDataType(type);
                localSymbolTable.add(sym);
            }

            // Manejo de errores - AGREGAR A LISTA LOCAL PRIMERO
            List<AnalysisError> tokenErrors = processTokenErrors(type, lexeme, line, column);

            // AGREGAR ERRORES DE FORMA SINCRONIZADA
            synchronized (localErrors) {
                localErrors.addAll(tokenErrors);
            }
        }

        return new ArrayList<>(tokens); // RETORNAR COPIA
    }

    private Symbol.SymbolType determineSymbolType(String type) {
        switch (type) {
            case "IDENTIFICADOR":
                return Symbol.SymbolType.VARIABLE;
            case "NUMERO_ENTERO":
            case "NUMERO_DECIMAL":
            case "CADENA_SIMPLE":
            case "CADENA_DOBLE":
                return Symbol.SymbolType.CONSTANT;
            default:
                return Symbol.SymbolType.UNKNOWN;
        }
    }

    private List<AnalysisError> processTokenErrors(String type, String lexeme, int line, int column) {
        List<AnalysisError> errors = new ArrayList<>();

        if (type.startsWith("ERROR_") || "INVALIDO".equals(type)) {
            switch (type) {
                case "ERROR_COMENTARIO":
                    errors.add(new AnalysisError("Comentario mal formado",
                            AnalysisError.ErrorType.LEXICAL, line, column));
                    break;
                case "ERROR_CADENA":
                case "ERROR_CADENA_SIMPLE":
                case "ERROR_CADENA_DOBLE":
                    errors.add(new AnalysisError("Cadena sin cerrar",
                            AnalysisError.ErrorType.LEXICAL, line, column));
                    break;
                case "ERROR_NUMERO":
                case "ERROR_NUMERO_DECIMAL":
                    errors.add(new AnalysisError("Número mal formado",
                            AnalysisError.ErrorType.LEXICAL, line, column));
                    break;
                case "ERROR_IDENTIFICADOR":
                    errors.add(new AnalysisError("Identificador inválido",
                            AnalysisError.ErrorType.LEXICAL, line, column));
                    break;
                case "ERROR_OPERADOR":
                    errors.add(new AnalysisError("Operador inválido",
                            AnalysisError.ErrorType.LEXICAL, line, column));
                    break;
                case "INVALIDO":
                    errors.add(new AnalysisError("Token desconocido: '" + lexeme + "'",
                            AnalysisError.ErrorType.LEXICAL, line, column));
                    break;
            }
        }

        return errors;
    }

    @Override
    public List<Token> analyze(String code, LanguageType language) {
        return analyzeLexical(code, new ArrayList<>());
    }
}