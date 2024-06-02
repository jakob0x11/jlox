package com.craftinginterpreters.lox;

class RpnPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return polishize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
        return polishize("", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		if (expr.value == null) return "nil";
        return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
        // Need a different operator representation, because '-' can only be 
        // interpreted as a binary operator in RPN. ref: https://stackoverflow.com/a/64868283
        if (expr.operator.type == TokenType.MINUS) {
            return polishize("NEGATE", expr.right);
        }
		return polishize(expr.operator.lexeme, expr.right);
	}

    private String polishize(String name, Expr ...exprs) {
        StringBuilder builder = new StringBuilder();

        for (Expr expr : exprs) {
            builder.append(expr.accept(this)).append(" ");
        }

        return builder.toString() + name;
    }

    public static void main(String[] args) {
        Expr expression = 
        new Expr.Binary(
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1), 
                        new Expr.Literal(1)),
                    new Token(TokenType.MINUS, "+", null, 1),
                    new Expr.Literal(2))
        ), 
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(4), 
                    new Token(TokenType.PLUS, "-", null, 1),
                    new Expr.Literal(3))
            )
        );        

        System.out.println(new RpnPrinter().print(expression));
    }
}
