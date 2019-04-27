import antlr4.ExpBaseVisitor;
import antlr4.ExpParser;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvalVisitor extends ExpBaseVisitor<Value> {

    // Точность для сравнения чисел с плавающей точкой
    public static final double SMALL_VALUE = 0.00000000001;
    // Храним переменные и значения
    private Map<String, Value> memory = new HashMap<String, Value>();

    // Присваивание
    @Override
    public Value visitAssignment(ExpParser.AssignmentContext ctx) {
        String id = ctx.ID().getText();
        Value value = this.visit(ctx.expr());
        return memory.put(id, value);
    }

    @Override
    public Value visitIdAtom(ExpParser.IdAtomContext ctx) {
        String id = ctx.getText();
        Value value = memory.get(id);
        if(value == null) {
            throw new RuntimeException("Не обьявленная переменная: " + id);
        }
        return value;
    }

    // Правим строку
    @Override
    public Value visitStringAtom(ExpParser.StringAtomContext ctx) {
        String str = ctx.getText();
        str = str.substring(1, str.length() - 1).replace("\"\"", "\"");
        return new Value(str);
    }

    @Override
    public Value visitNumberAtom(ExpParser.NumberAtomContext ctx) {
        Value test= new Value(Double.valueOf(ctx.getText()));
        return test;
    }

    @Override
    public Value visitBooleanAtom(ExpParser.BooleanAtomContext ctx) {
        return new Value(Boolean.valueOf(ctx.getText()));
    }

    @Override
    public Value visitNilAtom(ExpParser.NilAtomContext ctx) {
        return new Value(null);
    }

    @Override
    public Value visitParExpr(ExpParser.ParExprContext ctx) {
        return this.visit(ctx.expr());
    }

    @Override
    public Value visitPowExpr(ExpParser.PowExprContext ctx) {
        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));
        return new Value(Math.pow(left.asDouble(), right.asDouble()));
    }

    // Отрицаельное число
    @Override
    public Value visitUnaryMinusExpr(ExpParser.UnaryMinusExprContext ctx) {
        Value value = this.visit(ctx.expr());
        return new Value(-value.asDouble());
    }

    @Override
    public Value visitNotExpr(ExpParser.NotExprContext ctx) {
        Value value = this.visit(ctx.expr());
        return new Value(!value.asBoolean());
    }

    @Override
    public Value visitMultiplicationExpr(@NotNull ExpParser.MultiplicationExprContext ctx) {

        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case ExpParser.MULT:
                return new Value(left.asDouble() * right.asDouble());
            case ExpParser.DIV:
                return new Value(left.asDouble() / right.asDouble());
            case ExpParser.MOD:
                return new Value(left.asDouble() % right.asDouble());
            default:
                throw new RuntimeException("Неизвестный оператор: " + ExpParser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitAdditiveExpr(@NotNull ExpParser.AdditiveExprContext ctx) {

        ExpParser.ExprContext a = ctx.expr(1);
        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case ExpParser.PLUS:
                return left.isDouble() && right.isDouble() ?
                        new Value(left.asDouble() + right.asDouble()) :
                        new Value(left.asString() + right.asString());
            case ExpParser.MINUS:
                return new Value(left.asDouble() - right.asDouble());
            default:
                throw new RuntimeException("Неизвестный оператор: " + ExpParser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitRelationalExpr(@NotNull ExpParser.RelationalExprContext ctx) {

        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case ExpParser.LT:
                return new Value(left.asDouble() < right.asDouble());
            case ExpParser.LTEQ:
                return new Value(left.asDouble() <= right.asDouble());
            case ExpParser.GT:
                return new Value(left.asDouble() > right.asDouble());
            case ExpParser.GTEQ:
                return new Value(left.asDouble() >= right.asDouble());
            default:
                throw new RuntimeException("Неизвестный оператор: " + ExpParser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitEqualityExpr(@NotNull ExpParser.EqualityExprContext ctx) {

        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));

        switch (ctx.op.getType()) {
            case ExpParser.EQ:
                return left.isDouble() && right.isDouble() ?
                        new Value(Math.abs(left.asDouble() - right.asDouble()) < SMALL_VALUE) :
                        new Value(left.equals(right));
            case ExpParser.NEQ:
                return left.isDouble() && right.isDouble() ?
                        new Value(Math.abs(left.asDouble() - right.asDouble()) >= SMALL_VALUE) :
                        new Value(!left.equals(right));
            default:
                throw new RuntimeException("Неизвестный оператор: " + ExpParser.tokenNames[ctx.op.getType()]);
        }
    }

    @Override
    public Value visitAndExpr(ExpParser.AndExprContext ctx) {
        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));
        return new Value(left.asBoolean() && right.asBoolean());
    }

    @Override
    public Value visitOrExpr(ExpParser.OrExprContext ctx) {
        Value left = this.visit(ctx.expr(0));
        Value right = this.visit(ctx.expr(1));
        return new Value(left.asBoolean() || right.asBoolean());
    }

    // Пишем в консоль
    @Override
    public Value visitConsole(ExpParser.ConsoleContext ctx) {
        Value value = this.visit(ctx.expr());
        System.out.println(value);
        return value;
    }

    @Override
    public Value visitIf_stat(ExpParser.If_statContext ctx) {

        List<ExpParser.Condition_blockContext> conditions =  ctx.condition_block();

        boolean evaluatedBlock = false;

        // Проход по всем if / elseif
        for(ExpParser.Condition_blockContext condition : conditions) {

            Value evaluated = this.visit(condition.expr());

            if(evaluated.asBoolean()) {
                evaluatedBlock = true;
                this.visit(condition.stat_block());
                break;
            }
        }

        if(!evaluatedBlock && ctx.stat_block() != null) {
            // Попадаем в else
            this.visit(ctx.stat_block());
        }

        return Value.VOID;
    }

    // while override
    @Override
    public Value visitWhile_stat(ExpParser.While_statContext ctx) {

        Value value = this.visit(ctx.expr());

        while(value.asBoolean()) {

            // Выполняем тело цикла
            this.visit(ctx.stat_block());
            // Снова вычисляем выражение
            value = this.visit(ctx.expr());

        }

        return Value.VOID;
    }
}
