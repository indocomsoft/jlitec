package jlitec.command;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jlitec.backend.passes.MethodWithFlow;
import jlitec.backend.passes.Node;
import jlitec.backend.passes.flow.FlowPass;
import jlitec.backend.passes.live.LivePass;
import jlitec.backend.passes.live.LowerStmtWithLive;
import jlitec.backend.passes.lower.LowerPass;
import jlitec.backend.passes.regalloc.RegAllocPass;
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

public class InterferenceCommand implements Command {
  @Override
  public String helpMessage() {
    return "Generates interference graphs in DOT format";
  }

  @Override
  public void setUpArguments(Subparser subparser) {
    subparser.addArgument("filename").type(String.class).help("input filename");
    subparser
        .addArgument("--after-reg-alloc")
        .action(Arguments.storeTrue())
        .help("Print interference graph after register allocation");
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
    final var lowerProgram = new LowerPass().pass(ir3Program);
    if (parsed.getBoolean("after_reg_alloc")) {
      for (final var method : lowerProgram.methodList()) {
        final var regAllocOutput = new RegAllocPass().pass(method);
        final var flow = new FlowPass().pass(regAllocOutput.method().lowerStmtList());
        final var output = new LivePass().pass(new MethodWithFlow(regAllocOutput.method(), flow));
        final var edges = buildInterferenceGraph(output.lowerStmtWithLiveList());
        final var sb = new StringBuilder();
        sb.append("graph G {\n");
        for (final var edgeEntry : edges.entries()) {
          final var src = edgeEntry.getKey().print(0).replace("@", "AT_");
          final var dst = edgeEntry.getValue().print(0).replace("@", "AT_");
          sb.append("  ").append(src).append(" -- ").append(dst).append(";\n");
        }
        sb.append("}");
        System.out.println(sb.toString());
      }
    } else {
      for (final var method : lowerProgram.methodList()) {
        final var flow = new FlowPass().pass(method.lowerStmtList());
        final var output = new LivePass().pass(new MethodWithFlow(method, flow));
        final var edges = buildInterferenceGraph(output.lowerStmtWithLiveList());
        final var sb = new StringBuilder();
        sb.append("graph G {\n");
        for (final var edgeEntry : edges.entries()) {
          final var src = edgeEntry.getKey().print(0);
          final var dst = edgeEntry.getValue().print(0);
          sb.append("  ").append(src).append(" -- ").append(dst).append(";\n");
        }
        sb.append("}");
        System.out.println(sb.toString());
      }
    }
  }

  public SetMultimap<Node, Node> buildInterferenceGraph(
      List<LowerStmtWithLive> lowerStmtWithLiveList) {
    final SetMultimap<Node, Node> edges = HashMultimap.create();
    for (final var stmtWithLive : lowerStmtWithLiveList) {
      final var in =
          stmtWithLive.liveIn().stream().sorted().collect(Collectors.toUnmodifiableList());
      for (int i = 0; i < in.size() - 1; i++) {
        edges.putAll(in.get(i), in.subList(i + 1, in.size()));
      }
      final var out =
          stmtWithLive.liveOut().stream().sorted().collect(Collectors.toUnmodifiableList());
      for (int i = 0; i < out.size() - 1; i++) {
        edges.putAll(out.get(i), out.subList(i + 1, out.size()));
      }
    }
    return Multimaps.unmodifiableSetMultimap(edges);
  }
}
