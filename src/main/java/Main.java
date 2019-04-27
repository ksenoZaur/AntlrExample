
import antlr4.ExpLexer;
import antlr4.ExpParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) throws Exception {

        String[] input = new String[]{"src/main/example/test.xen"};
        System.out.println("Исследуем: " + input[0]);
        ExpLexer lexer = new ExpLexer(new ANTLRFileStream(input[0]));
        ExpParser parser = new ExpParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.prog();
        EvalVisitor visitor = new EvalVisitor();
        visitor.visit(tree);

    }
}