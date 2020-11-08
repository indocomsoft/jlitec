package jlitec.backend.arm.codegen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jlitec.backend.arm.AssemblerDirective;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Insn;
import jlitec.backend.arm.Program;
import jlitec.backend.arm.Register;
import jlitec.backend.arm.insn.BInsn;
import jlitec.backend.arm.insn.BXInsn;
import jlitec.backend.arm.insn.LDMFDInsn;
import jlitec.backend.arm.insn.LabelInsn;
import jlitec.ir3.Method;
import jlitec.ir3.stmt.GotoStmt;
import jlitec.ir3.stmt.LabelStmt;
import jlitec.ir3.stmt.ReturnStmt;
import jlitec.passes.flow.Block;
import jlitec.passes.flow.FlowGraph;

public class Simple {
  // Prevent instantiation
  private Simple() {}

  public static Program gen(jlitec.ir3.Program program, Map<Method, FlowGraph> methodToFlowGraph) {
    final var insnList = new ArrayList<Insn>();
    for (final var method : program.methodList()) {
      insnList.add(new AssemblerDirective("global", method.id().equals("main") ? "main" : "func"));
      insnList.add(new AssemblerDirective("type", method.id() + ", %function"));
      insnList.add(new LabelInsn(method.id()));
      for (final var block : methodToFlowGraph.get(method).blocks()) {
        if (block instanceof Block.Basic bb) {
          insnList.addAll(genBlock(bb));
        } else {
          final var finalInsn = insnList.get(insnList.size() - 1);
          if (finalInsn.equals(new BXInsn(Condition.AL, Register.LR))) {
            continue;
          }
          if (finalInsn instanceof LDMFDInsn ldmfdInsn
              && ldmfdInsn.registers().contains(Register.PC)) {
            continue;
          }
          insnList.add(new BXInsn(Condition.AL, Register.LR));
        }
      }
    }
    return new Program(insnList);
  }

  private static List<Insn> genBlock(Block.Basic block) {
    final var regDesc = new EnumMap<Register, Set<String>>(Register.class);
    for (final var register : Register.values()) {
      regDesc.put(register, new HashSet<>());
    }
    final var addrDesc = new HashMap<String, Set<LocationDescriptor>>();

    final var insnList = new ArrayList<Insn>();
    for (final var stmt : block.stmtList()) {
      final List<Insn> insnChunk =
          switch (stmt.getStmtType()) {
            case LABEL -> {
              final var ls = (LabelStmt) stmt;
              yield List.of(new LabelInsn(ls.label()));
            }
            case CMP -> null;
            case GOTO -> {
              final var gs = (GotoStmt) stmt;
              yield List.of(new BInsn(Condition.AL, gs.dest().label()));
            }
            case READLN -> null;
            case PRINTLN -> null;
            case VAR_ASSIGN -> null;
            case FIELD_ASSIGN -> null;
            case CALL -> null;
            case RETURN -> {
              final var rs = (ReturnStmt) stmt;
              if (rs.maybeValue().isEmpty()) {
                yield List.of(new BXInsn(Condition.AL, Register.LR));
              } else {
                yield null;
              }
            }
          };
      if (insnChunk != null) {
        insnList.addAll(insnChunk);
      }
    }

    return insnList;
  }
}
