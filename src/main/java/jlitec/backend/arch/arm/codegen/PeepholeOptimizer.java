package jlitec.backend.arch.arm.codegen;

import java.util.ArrayList;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.Insn;
import jlitec.backend.arch.arm.Operand2;
import jlitec.backend.arch.arm.Program;
import jlitec.backend.arch.arm.insn.ADDInsn;
import jlitec.backend.arch.arm.insn.ANDInsn;
import jlitec.backend.arch.arm.insn.CMNInsn;
import jlitec.backend.arch.arm.insn.CMPInsn;
import jlitec.backend.arch.arm.insn.EORInsn;
import jlitec.backend.arch.arm.insn.MOVInsn;
import jlitec.backend.arch.arm.insn.MULInsn;
import jlitec.backend.arch.arm.insn.ORRInsn;
import jlitec.backend.arch.arm.insn.RSBInsn;
import jlitec.backend.arch.arm.insn.SDIVInsn;
import jlitec.backend.arch.arm.insn.SUBInsn;

public class PeepholeOptimizer {
  // Prevent instantiation
  private PeepholeOptimizer() {}

  public static Program pass(Program program) {
    var input = program;
    while (true) {
      final var mulSdivOneOutput = passMulSdivOne(input);
      final var regPlusMinusZeroOutput = passRegPlusMinusZero(mulSdivOneOutput);
      final var immediatePlusMinusZeroOutput = passImmediatePlusMinusZero(regPlusMinusZeroOutput);
      final var movOp2ImmOutput = passMovOp2Imm(immediatePlusMinusZeroOutput);
      final var movSubRsbOutput = passMovSubRsb(movOp2ImmOutput);
      final var mulZeroOutput = passMulZero(movSubRsbOutput);
      final var movOutput = passMov(mulZeroOutput);
      if (movOutput.equals(input)) {
        return input;
      }
      input = movOutput;
    }
  }

  /**
   * Replace <code>
   *   MOV R0, #0
   *   MUL R3, R0, R1
   * </code> with <code>
   *   MOV R3, #0
   * </code>.
   */
  private static Program passMulZero(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m
          && m.condition() == Condition.AL
          && m.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof MULInsn mul) {
        if (mul.src1().equals(m.register()) || mul.src2().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, mul.dst(), new Operand2.Immediate(0)));
          i++;
          continue;
        }
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }

  /**
   * Replace <code>
   *   MOV R0, #1
   *   MUL R3, R0, R1
   * </code> with <code>
   *   MOV R3, R1
   * </code>, similarly with SDIV.
   */
  private static Program passMulSdivOne(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m
          && m.condition() == Condition.AL
          && m.op2() instanceof Operand2.Immediate op2
          && op2.value() == 1)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof MULInsn mul) {
        if (mul.src1().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, mul.dst(), new Operand2.Register(mul.src2())));
          i++;
          continue;
        }
        if (mul.src2().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, mul.dst(), new Operand2.Register(mul.src1())));
          i++;
          continue;
        }
      }
      if (nextInsn instanceof SDIVInsn div) {
        if (div.src2().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, div.dst(), new Operand2.Register(div.src1())));
          i++;
          continue;
        }
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }

  /**
   * Replace <code>
   *   MOV R1, #5
   *   MOV R0, R1
   * </code> with <code>
   *   MOV R0, #5
   * </code>
   */
  private static Program passMov(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m && m.condition() == Condition.AL)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof MOVInsn mov
          && mov.condition() == Condition.AL
          && mov.op2() instanceof Operand2.Register op2
          && op2.reg().equals(m.register())) {
        // Pattern matched
        insnList.add(new MOVInsn(Condition.AL, mov.register(), m.op2()));
        i++;
        continue;
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }

  /**
   * Replace <code>
   *   MOV R1, #1
   *   ADD R0, R0, R1
   * </code> with <code>
   *   MOV R0, #1
   * </code>, similarly with SUB.
   */
  private static Program passRegPlusMinusZero(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m
          && m.condition() == Condition.AL
          && m.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof ADDInsn add) {
        if (add.src().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, add.dst(), add.op2()));
          i++;
          continue;
        }
        if (add.op2() instanceof Operand2.Register op2reg && op2reg.reg().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, add.dst(), new Operand2.Register(add.src())));
          i++;
          continue;
        }
      }
      if (nextInsn instanceof SUBInsn sub) {
        if (sub.op2() instanceof Operand2.Register op2reg && op2reg.reg().equals(m.register())) {
          // Pattern matched
          insnList.add(new MOVInsn(Condition.AL, sub.dst(), new Operand2.Register(sub.src())));
          i++;
          continue;
        }
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
        insnList.add(new MOVInsn(Condition.AL, add.dst(), new Operand2.Register(add.src())));
      } else if (insn instanceof SUBInsn sub
          && sub.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0) {
        insnList.add(new MOVInsn(Condition.AL, sub.dst(), new Operand2.Register(sub.src())));
      } else {
        insnList.add(insn);
      }
    }
    return new Program(insnList);
  }

  /**
   * Replace <code>
   *   MOV R1, (imm)
   *   ADD R0, R3, R1
   * </code>
   */
  private static Program passMovOp2Imm(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m
          && m.condition() == Condition.AL
          && m.op2() instanceof Operand2.Immediate op2)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof ADDInsn add
          && add.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new ADDInsn(
                Condition.AL,
                add.updateConditionFlags(),
                add.dst(),
                add.src(),
                new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      if (nextInsn instanceof ANDInsn and
          && and.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new ANDInsn(
                Condition.AL,
                and.updateConditionFlags(),
                and.dst(),
                and.src(),
                new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      if (nextInsn instanceof CMNInsn cmn
          && cmn.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new CMNInsn(Condition.AL, cmn.register(), new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      if (nextInsn instanceof CMPInsn cmp
          && cmp.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new CMPInsn(Condition.AL, cmp.register(), new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      if (nextInsn instanceof EORInsn eor
          && eor.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new EORInsn(
                Condition.AL,
                eor.updateConditionFlags(),
                eor.dst(),
                eor.src(),
                new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      if (nextInsn instanceof ORRInsn orr
          && orr.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new ORRInsn(
                Condition.AL,
                orr.updateConditionFlags(),
                orr.dst(),
                orr.src(),
                new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      if (nextInsn instanceof SUBInsn sub
          && sub.op2() instanceof Operand2.Register op2Reg
          && op2Reg.reg().equals(m.register())) {
        // Pattern Matched
        insnList.add(
            new SUBInsn(
                Condition.AL,
                sub.updateConditionFlags(),
                sub.dst(),
                sub.src(),
                new Operand2.Immediate(op2.value())));
        i++;
        continue;
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }

  private static Program passMovSubRsb(Program input) {
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof MOVInsn m
          && m.condition() == Condition.AL
          && m.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof SUBInsn sub && sub.src().equals(m.register())) {
        if (sub.op2() instanceof Operand2.Immediate op2imm) {
          // Pattern matched
          insnList.add(
              new MOVInsn(Condition.AL, sub.dst(), new Operand2.Immediate(-op2imm.value())));
          i++;
          continue;
        }
        if (sub.op2() instanceof Operand2.Register op2reg) {
          // Pattern matched
          insnList.add(
              new RSBInsn(
                  Condition.AL,
                  sub.updateConditionFlags(),
                  sub.dst(),
                  op2reg.reg(),
                  new Operand2.Immediate(0)));
          i++;
          continue;
        }
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }
}
