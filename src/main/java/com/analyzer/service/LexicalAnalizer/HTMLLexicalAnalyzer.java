package com.analyzer.service.LexicalAnalizer;

import java.util.*;
import java.util.regex.*;

import com.analyzer.model.Token;
import com.analyzer.model.Symbol;
import com.analyzer.model.Symbol.SymbolType;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.ExpresionesRegulares.HTMLRegexTokenizer;

public class HTMLLexicalAnalyzer implements ILexicalAnalyzer {

    private final HTMLRegexTokenizer tokenizer = new HTMLRegexTokenizer();
    private List<Symbol> symbolTable;
    private List<AnalysisError> errorList;

    @Override
    public List<Token> tokenize(String code) {
        return tokenizer.tokenize(code);
    }

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
            SymbolType symType;
            if ("TAG_NAME".equals(type)) {
                symType = SymbolType.TAG;
            } else if ("ATTRIBUTE_NAME".equals(type)) {
                symType = SymbolType.ATTRIBUTE;
            } else if ("STRING".equals(type) || "NUMBER".equals(type)) {
                symType = SymbolType.CONSTANT;
            } else {
                symType = SymbolType.UNKNOWN;
            }

            Symbol sym = new Symbol(lexeme, symType);
            sym.setDeclarationLine(line);
            sym.setDeclarationColumn(column);
            sym.setScope("global");
            sym.setDataType(type);
            symbolTable.add(sym);

            // Manejo de errores
            if (type.startsWith("ERROR_") || "INVALID".equals(type)) {
                switch (type) {
                    case "ERROR_COMENTARIO":
                        errors.add(new AnalysisError(
                            "Comentario mal formado",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_DOCTYPE":
                        errors.add(new AnalysisError(
                            "DOCTYPE mal formado",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_ATRIBUTO":
                        errors.add(new AnalysisError(
                            "Atributo mal formado",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_ENTIDAD":
                        errors.add(new AnalysisError(
                            "Entidad HTML mal formada",
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
                }
            }
        }
        return tokens;
    }

    @Override
    public List<Token> analyze(String code, LanguageType language) {
        return analyzeLexical(code, new ArrayList<>());
    }

    public List<Symbol> getSymbolTable() {
        return Collections.unmodifiableList(symbolTable);
    }

    public List<AnalysisError> getErrorList() {
        return Collections.unmodifiableList(errorList);
    }
}
