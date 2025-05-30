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

    @Override
    public List<Token> tokenize(String code) {
        return tokenizer.tokenize(code);
    }

    @Override
    public List<Token> analyzeLexical(String source, List<AnalysisError> errors) {
        List<Token> tokens = tokenizer.tokenize(source);
        symbolTable = new ArrayList<>();
        int line = 1;

        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int column = token.getColumn();

            for (char c : lexeme.toCharArray()) {
                if (c == '\n') line++;
            }

            SymbolType symType;
            if ("TAG_NAME".equals(type)) {
                symType = SymbolType.FUNCTION;
            } else if ("ATTRIBUTE_NAME".equals(type)) {
                symType = SymbolType.VARIABLE;
            } else if ("STRING".equals(type) || "NUMBER".equals(type)) {
                symType = SymbolType.CONSTANT;
            } else {
                symType = SymbolType.UNKNOWN;
            }

            Symbol sym = new Symbol(lexeme, symType);
            sym.setDeclarationLine(line);
            sym.setDeclarationColumn(column);
            symbolTable.add(sym);

            if (type.startsWith("ERROR_") || "INVALID".equals(type)) {
                String description;
                switch (type) {
                    case "ERROR_COMENTARIO": description = "Comentario mal formado"; break;
                    case "ERROR_DOCTYPE": description = "DOCTYPE mal formado"; break;
                    case "ERROR_ATRIBUTO": description = "Atributo mal formado"; break;
                    case "ERROR_ENTIDAD" : description = "Entidad HTML mal formada"; break;
                    case "INVALIDO": description = "Token inválido"; break;
                    default: description = "Error léxico"; break;
                }
                errors.add(new AnalysisError(
                        String.format("%s: '%s' (línea %d)", description, lexeme.trim(), line),
                        AnalysisError.ErrorType.LEXICAL,
                        line,
                        column
                ));
            }
        }

        return tokens;
    }
    //funcion para retornar los errores léxicos




    @Override
    public List<Token> analyze(String code, LanguageType language) {//
        return analyzeLexical(code, new ArrayList<>());
    }

    public List<Symbol> getSymbolTable() {
        return Collections.unmodifiableList(symbolTable);
    }
}
