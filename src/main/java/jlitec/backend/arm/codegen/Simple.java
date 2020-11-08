package jlitec.backend.arm.codegen;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import jlitec.backend.arm.AssemblerDirective;
import jlitec.backend.arm.Condition;
import jlitec.backend.arm.Insn;
import jlitec.backend.arm.MemoryAddress;
import jlitec.backend.arm.Program;
import jlitec.backend.arm.Register;
import jlitec.backend.arm.Size;
import jlitec.backend.arm.insn.BInsn;
import jlitec.backend.arm.insn.BXInsn;
import jlitec.backend.arm.insn.LDMFDInsn;
import jlitec.backend.arm.insn.LabelInsn;
import jlitec.backend.arm.insn.STRInsn;
import jlitec.ir3.Method;
import jlitec.ir3.Type;
import jlitec.ir3.Var;
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
    final var stringGen = new StringGen();
    for (final var method : program.methodList()) {
      final var typeMap = Stream.concat(method.args().stream(), method.vars().stream()).collect(Collectors.toUnmodifiableMap(Var::id, Var::type));
      final var stackDesc = buildStackDesc(method);

      insnList.add(new AssemblerDirective("global", method.id().equals("main") ? "main" : "func"));
      insnList.add(new AssemblerDirective("type", method.id() + ", %function"));
      insnList.add(new LabelInsn(method.id()));

      // Store arguments to stack
      for (int i = 0; i < 4 && i < method.args().size(); i++) {
        final var arg = method.args().get(i);
        final var offset = stackDesc.get(arg.id()).offset();
        insnList.add(new STRInsn(Condition.AL, Size.WORD, Register.fromInt(i), new MemoryAddress.ImmediateOffset(Register.SP, Optional.of(offset), false)));
      }

      for (final var block : methodToFlowGraph.get(method).blocks()) {
        if (block instanceof Block.Basic bb) {
          insnList.addAll(genBlock(bb, stringGen, typeMap, stackDesc));
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

  private static Map<String, LocationDescriptor.Stack> buildStackDesc(Method method) {
    final var result = new HashMap<String, LocationDescriptor.Stack>();
    int offset = 0;
    for (int i = 4; i < method.args().size(); i++) {
      final var v = method.args().get(i);
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    for (int i = 0; i < 4 && i < method.args().size(); i++) {
      final var v = method.args().get(i);
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    for (final var v : method.vars()) {
      result.put(v.id(), new LocationDescriptor.Stack(offset));
      offset += 4;
    }
    return Collections.unmodifiableMap(result);
  }

  private static List<Insn> genBlock(Block.Basic block, StringGen stringGen, Map<String, Type> typeMap, Map<String, LocationDescriptor.Stack> stackDesc) {
    final var availableRegs = new BitSet(Register.USER_MAX);
    availableRegs.set(0, Register.USER_MAX + 1);
    final var regDesc = HashMultimap.<Register, String>create();

    final var addrDesc = HashMultimap.<String, LocationDescriptor>create();
    for (final var entry : stackDesc.entrySet()) {
      addrDesc.put(entry.getKey(), entry.getValue());
    }

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
              }

              yield null;
            }
          };
      if (insnChunk != null) {
        insnList.addAll(insnChunk);
      }
    }

    return insnList;
  }

  private static GetRegChunk getReg(String x, String y, String z, BitSet availableRegs, HashMultimap<Register, String> regDesc, HashMultimap<String, LocationDescriptor> addrDesc) {
    final var inverseRegDesc = ImmutableMultimap.copyOf(regDesc).inverse();
    final var insnList = new ArrayList<Insn>();
    final Register xReg;
    final Register yReg;
    final Register zReg;
    if (inverseRegDesc.containsKey(y)) {
      yReg = inverseRegDesc.get(y).asList().get(0);
    } else if (!availableRegs.isEmpty()) {
      final var regNumber = availableRegs.nextSetBit(0);
      availableRegs.clear(regNumber);
      yReg = Register.fromInt(regNumber);
    } else {
      final Map<Register, Integer> scores = new HashMap<>();

    }

    return new GetRegChunk(insnList, xReg, yReg, zReg);
  }
}
