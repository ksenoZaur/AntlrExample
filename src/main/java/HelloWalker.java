import antlr4.HelloBaseListener;
import antlr4.HelloParser;

public class HelloWalker extends HelloBaseListener {
    public void enterR(HelloParser.RContext ctx ) {
        System.out.println( "Entering R : " + ctx.ID().getText() );
    }

    public void exitR(HelloParser.RContext ctx ) {
        System.out.println( "Exiting R" );
    }
}