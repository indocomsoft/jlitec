package jlitec.command;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;
import jlitec.backend.c.codegen.CCodeGen;
import jlitec.checker.KlassDescriptor;
import jlitec.checker.ParseTreeStaticChecker;
import jlitec.checker.SemanticException;
import jlitec.ir3.codegen.Ir3CodeGen;
import jlitec.lexer.LexException;
import jlitec.parser.ParserWrapper;
import jlitec.parsetree.Program;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class CCommand implements Command {
  @Override
  public String helpMessage() {
    return "Generate C code";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
    subparser
        .addArgument("--cc")
        .type(String.class)
        .help("C compiler to use, if omitted, compiler will not be called.");
    subparser.addArgument("-O").type(String.class).help("optimization level");
    subparser.addArgument("--cpu").type(String.class).help("Specify -mcpu= to be passed to $CC");
    final var group = subparser.addMutuallyExclusiveGroup();
    group.addArgument("-S").help("Print the assembly output").action(Arguments.storeTrue());
    group.addArgument("-o").type(String.class).help("output location");
  }

  @Override
  public void run(Namespace parsed) {
    final String filename = parsed.getString("filename");
    final ParserWrapper parser;
    try {
      parser = new ParserWrapper(filename);
    } catch (IOException e) {
      System.err.println("Unable to read file.");
      return;
    }

    final Program program;

    try {
      program = parser.parse();
    } catch (LexException e) {
      System.err.println("Lexing failed.");
      return;
    } catch (Exception e) {
      System.err.println("Parsing failed: " + e.getMessage());
      return;
    }

    final Map<String, KlassDescriptor> classDescriptorMap;
    final jlitec.ast.Program astProgram;
    try {
      classDescriptorMap = ParseTreeStaticChecker.produceClassDescriptor(program);
      astProgram = ParseTreeStaticChecker.toAst(program, classDescriptorMap);
    } catch (SemanticException e) {
      System.err.println(ParseTreeStaticChecker.generateErrorMessage(e, parser));
      return;
    }

    final jlitec.ir3.Program ir3Program = Ir3CodeGen.generate(astProgram);
    final jlitec.backend.c.Program cProgram = CCodeGen.gen(ir3Program);
    final var cOutput = cProgram.print(0);
    final var maybeCC = Optional.ofNullable(parsed.getString("cc"));

    if (maybeCC.isEmpty()) {
      System.out.println(cOutput);
      return;
    }
    System.err.println(cOutput);

    final var cc = maybeCC.get();
    try {
      if (Runtime.getRuntime().exec(new String[] {"which", cc}).waitFor() != 0) {
        System.err.println("CC=" + cc + " not found.");
        return;
      }
      final var command = Lists.newArrayList(cc, "-x", "c");
      Optional.ofNullable(parsed.getString("o")).ifPresent(o -> command.add("-o" + o));
      Optional.ofNullable(parsed.getString("O")).ifPresent(opt -> command.add("-O" + opt));
      Optional.ofNullable(parsed.getString("cpu")).ifPresent(cpu -> command.add("-mcpu=" + cpu));
      if (parsed.getBoolean("S")) {
        command.add("-S");
        command.add("-o");
        command.add("-");
      }
      command.add("-");
      System.err.println("Running " + command);
      final var process =
          new ProcessBuilder(command)
              .redirectOutput(ProcessBuilder.Redirect.INHERIT)
              .redirectError(ProcessBuilder.Redirect.INHERIT)
              .start();
      final var printWriter = new PrintWriter(process.getOutputStream());
      printWriter.println(cOutput);
      printWriter.flush();
      printWriter.close();
      final var exitCode = process.waitFor();
      if (exitCode != 0) {
        System.err.println("CC=" + cc + " exited with non-successful exit code " + exitCode);
      }
    } catch (IOException e) {
      System.err.println("Unable to check for existence of CC=" + cc);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
