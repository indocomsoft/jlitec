package jlitec.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.GsonExclusionStrategy;
import jlitec.parsetree.Program;
import jlitec.parsetree.expr.Expr;
import jlitec.parsetree.expr.ExprSerializer;
import jlitec.parsetree.stmt.Stmt;
import jlitec.parsetree.stmt.StmtSerializer;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ParseTreeCommand implements Command {
  private final Gson gson =
      new GsonBuilder()
          .setPrettyPrinting()
          .registerTypeAdapter(Expr.class, new ExprSerializer())
          .registerTypeAdapter(Stmt.class, new StmtSerializer())
          .setExclusionStrategies(new GsonExclusionStrategy())
          .create();

  @Override
  public String helpMessage() {
    return "Show the AST in JSON";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
  }

  @Override
  public void run(Namespace parsed) {
    final String filename = parsed.getString("filename");
    try {
      final Program program = new ParserWrapper(filename).parse();
      System.out.println(this.gson.toJson(program));
    } catch (LexException e) {
      System.err.println("Lexing failed.");
    } catch (IOException e) {
      System.err.println("Unable to read file.");
    } catch (Exception e) {
      System.err.println("Parsing failed: " + e.getMessage());
    }
  }
}
