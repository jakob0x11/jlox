package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class UnhandledParseError extends RuntimeException {
    }

    private static class HandledParseError {
        final Token errorPoint;
        final String message;

        HandledParseError(Token errorPoint, String message) {
            this.errorPoint = errorPoint;
            this.message = message;
        }
    }

    private List<HandledParseError> handledParseErrors = new ArrayList<>();

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            Expr expr = expression();
            for (HandledParseError handledParseError : this.handledParseErrors) {
                Lox.error(handledParseError.errorPoint, handledParseError.message);
            }
            return expr;
        } catch (UnhandledParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return comma();
    }

    // Comma operator has the lowest precedence
    private Expr comma() {
        Expr expr = conditional();

        if (match(COMMA)) {
            // This discards the left operand as a side effect
            expr = expression();
        }

        return expr;
    }

    // conditional -> equality ( "?" conditional ":" ( conditional | equality ) )* ;
    private Expr conditional() {
        Expr expr = equality();

        if (match(QUESTION_MARK)) {
            expr = new Expr.Binary(expr, previous(), conditional());
            consume(COLON, "Expect ':' after '?'.");
            return new Expr.Binary(expr, previous(), conditional());
        }

        return expr;
    }

    // equality -> comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            if (checkForMissingExpression(expr, "Binary operators must have a left and right operand.")) {
                advance();
                return expression();
            }
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        return new Expr.Nothing("There's nothing here.");
    }

    private boolean checkForMissingExpression(Expr expr, String errorMessage) {
        if (expr instanceof Expr.Nothing) {
            HandledParseError error = new HandledParseError(previous(), errorMessage);
            this.handledParseErrors.add(error);
            return true;
        }
        return false;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private UnhandledParseError error(Token token, String message) {
        Lox.error(token, message);
        return new UnhandledParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
