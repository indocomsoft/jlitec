package jlitec.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import java_cup.runtime.DefaultSymbolFactory;
import jlitec.ast.Program;
import jlitec.ast.expr.Expr;
import jlitec.ast.expr.ExprSerializer;
import jlitec.ast.stmt.Stmt;
import jlitec.ast.stmt.StmtSerializer;
import jlitec.generated.Lexer;
import jlitec.generated.parser;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class AstCommand implements Command {
  private Gson gson =
      new GsonBuilder()
          .setPrettyPrinting()
          .registerTypeAdapter(Expr.class, new ExprSerializer())
          .registerTypeAdapter(Stmt.class, new StmtSerializer())
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
    final Program program;
    try {
      program =
          (Program)
              new parser(
                      new Lexer(new FileReader(filename, StandardCharsets.UTF_8)),
                      new DefaultSymbolFactory())
                  .parse()
                  .value;
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Unable to find file \"%s\"", filename), e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println(this.gson.toJson(program));
  }
}
