package jlitec.backend.arch.arm.codegen;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.Insn;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Program;
import jlitec.backend.arch.arm.insn.ADDInsn;
import jlitec.backend.arch.arm.insn.ANDInsn;
import jlitec.backend.arch.arm.insn.LDRInsn;
import jlitec.backend.arch.arm.insn.MOVInsn;
import jlitec.backend.arch.arm.insn.ORRInsn;
import jlitec.backend.arch.arm.insn.STRInsn;
import jlitec.backend.arch.arm.insn.SUBInsn;

public class PeepholeOptimizer {
  // Prevent instantiation
  private PeepholeOptimizer() {}

  public static Program pass(Program program) {
    var input = program;
    while (true) {
      final var output =
          Stream.of(input)
              .map(PeepholeOptimizer::passImmediatePlusMinusZero)
              .map(PeepholeOptimizer::passAndTrueImm)
              .map(PeepholeOptimizer::passAndFalseImm)
              .map(PeepholeOptimizer::passOrTrueImm)
              .map(PeepholeOptimizer::passOrFalseImm)
              .map(PeepholeOptimizer::passRemoveUselessMov)
              .map(PeepholeOptimizer::passRemoveUselessLdr)
              .map(PeepholeOptimizer::passMovItself)
              .map(PeepholeOptimizer::passMovSameReg)
              .collect(Collectors.toList())
              .get(0);
      if (output.equals(input)) {
        return input;
      }
      input = output;
    }
  }

  private static Program passAndTrueImm(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (final var insn : input.insnList()) {
      if (insn instanceof ANDInsn and
          && and.op2() instanceof Operand2.Immediate imm
          && imm.value() == 1) {
        insnList.add(new MOVInsn(and.condition(), and.dst(), new Operand2.Register(and.src())));
      } else {
        insnList.add(insn);
      }
    }
    return new Program(insnList);
  }

  private static Program passAndFalseImm(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (final var insn : input.insnList()) {
      if (insn instanceof ANDInsn and
          && and.op2() instanceof Operand2.Immediate imm
          && imm.value() == 0) {
        insnList.add(new MOVInsn(and.condition(), and.dst(), new Operand2.Immediate(0)));
      } else {
        insnList.add(insn);
      }
    }
    return new Program(insnList);
  }

  private static Program passOrTrueImm(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (final var insn : input.insnList()) {
      if (insn instanceof ORRInsn or
          && or.op2() instanceof Operand2.Immediate imm
          && imm.value() == 1) {
        insnList.add(new MOVInsn(or.condition(), or.dst(), new Operand2.Immediate(1)));
      } else {
        insnList.add(insn);
      }
    }
    return new Program(insnList);
  }

  private static Program passOrFalseImm(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (final var insn : input.insnList()) {
      if (insn instanceof ORRInsn or
          && or.op2() instanceof Operand2.Immediate imm
          && imm.value() == 0) {
        insnList.add(new MOVInsn(or.condition(), or.dst(), new Operand2.Register(or.src())));
      } else {
        insnList.add(insn);
      }
    }
    return new Program(insnList);
  }

  private static Program passMovItself(Program input) {
    // eliminates MOV R1, R1
    final var insnList =
        input.insnList().stream()
            .filter(
                i ->
                    !(i instanceof MOVInsn m)
                        || !(m.op2() instanceof Operand2.Register r)
                        || !r.isPlain()
                        || !m.register().equals(r.reg()))
            .collect(Collectors.toUnmodifiableList());
    return new Program(insnList);
  }

  private static Program passMovSameReg(Program input) {
    // turn
    //     MOV R0, #1
    //     MOV R0, #0
    //  to
    //     MOV R0, #0
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof MOVInsn m2
          && m2.condition().equals(m.condition())
          && m2.updateConditionFlags() == m.updateConditionFlags()
          && m.register().equals(m2.register())) {
        // Pattern matched
        insnList.add(m2);
        i++;
        continue;
      }

      insnList.add(insn);
    }
    return new Program(insnList);
  }

  /**
   * Replace <code>
   *   ADD R0, R1, #0
   * </code> with <code>
   *   MOV R0, R1
   * </code>
   */
  private static Program passImmediatePlusMinusZero(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (final var insn : input.insnList()) {
      if (insn instanceof ADDInsn add
          && add.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0) {
        insnList.add(
            new MOVInsn(
                add.condition(),
                add.updateConditionFlags(),
                add.dst(),
                new Operand2.Register(add.src())));
      } else if (insn instanceof SUBInsn sub
          && sub.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0) {
        insnList.add(
            new MOVInsn(
                sub.condition(),
                sub.updateConditionFlags(),
                sub.dst(),
                new Operand2.Register(sub.src())));
      } else {
        insnList.add(insn);
      }
    }
    return new Program(insnList);
  }

  /**
   * Replace <code>
   *   MOV R5, R0
   *   MOV R0, R5
   * </code> with <code>
   *   MOV R5, R0
   * </code>
   */
  private static Program passRemoveUselessMov(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m && m.op2() instanceof Operand2.Register r)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof MOVInsn mov
          && mov.condition().equals(m.condition())
          && mov.updateConditionFlags() == m.updateConditionFlags()
          && mov.op2() instanceof Operand2.Register r2
          && r2.reg().equals(m.register())
          && mov.register().equals(r.reg())) {
        // Pattern matched
        insnList.add(insn);
        i++;
        continue;
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }

  private static Program passRemoveUselessLdr(Program input) {
    // Turn
    //     STR R0, [SP]
    //     LDR R0, [SP]
    // into
    //     STR R0, [SP]
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (!(insn instanceof STRInsn str
          && nextInsn instanceof LDRInsn ldr
          && ldr.register().equals(str.register())
          && ldr.memoryAddress().equals(str.memoryAddress()))) {
        insnList.add(insn);
        continue;
      }
      // Pattern matched
      insnList.add(insn);
      i++;
    }
    return new Program(insnList);
  }
}
