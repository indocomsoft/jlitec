package jlitec.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jlitec.ParserWrapper;
import jlitec.ast.Program;
import jlitec.ast.expr.Expr;
import jlitec.ast.expr.ExprExclusionStrategy;
import jlitec.ast.expr.ExprSerializer;
import jlitec.ast.stmt.Stmt;
import jlitec.ast.stmt.StmtSerializer;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.IOException;

public class AstCommand implements Command {
  private final Gson gson =
          new GsonBuilder()
                  .setPrettyPrinting()
                  .registerTypeAdapter(Expr.class, new ExprSerializer())
                  .registerTypeAdapter(Stmt.class, new StmtSerializer())
                  .setExclusionStrategies(new ExprExclusionStrategy())
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
    } catch (IOException e) {
      System.err.println("Unable to read file.");
    }
  }
}
