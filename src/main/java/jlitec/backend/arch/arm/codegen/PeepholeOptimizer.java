package jlitec.backend.arch.arm.codegen;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jlitec.backend.arch.arm.BitInsn;
import jlitec.backend.arch.arm.CompareInsn;
import jlitec.backend.arch.arm.Condition;
import jlitec.backend.arch.arm.DataBinaryInsn;
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
import jlitec.backend.arch.arm.insn.TEQInsn;
import jlitec.backend.arch.arm.insn.TSTInsn;

public class PeepholeOptimizer {
  // Prevent instantiation
  private PeepholeOptimizer() {}

  public static Program pass(Program program) {
    var input = program;
    while (true) {
      final var output =
          Stream.of(input)
              .map(PeepholeOptimizer::passMovSameReg)
              .map(PeepholeOptimizer::passMov)
              .map(PeepholeOptimizer::passShiftOperand2)
              .map(PeepholeOptimizer::passMulSdivOne)
              .map(PeepholeOptimizer::passRegPlusMinusZero)
              .map(PeepholeOptimizer::passImmediatePlusMinusZero)
              .map(PeepholeOptimizer::passMovItself)
              .map(PeepholeOptimizer::passMovOp2Imm)
              .map(PeepholeOptimizer::passMovSubRsb)
              .collect(Collectors.toList())
              .get(0);

      // TODO add pass similar to AlgPass andTrue andFalse orTrue orFalse multiplyDivideZero

      // TODO add inlining of println_bool(true) and println(false)

      if (output.equals(input)) {
        return input;
      }
      input = output;
    }
  }

  private static Program passMovItself(Program input) {
    // elimintes MOV R1, R1
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

  private static Program passShiftOperand2(Program input) {
    // Turn
    //     LSL R0, #5
    //     ADD R1, R1, R0
    //   to
    //     ADD R1, R1, R0, LSL #5
    final var insnList = new ArrayList<Insn>();
    for (int i = 0; i < input.insnList().size(); i++) {
      final var insn = input.insnList().get(i);
      if (i == input.insnList().size() - 1) {
        insnList.add(insn);
        continue;
      }
      if (!(insn instanceof BitInsn b)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof DataBinaryInsn d && d.condition().equals(b.condition())) {
        if (d.src().equals(b.dst()) && d.op2() instanceof Operand2.Register r && r.isPlain()) {
          // Pattern matched
          final var newInsn =
              switch (d.type()) {
                case ADD -> new ADDInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    r.reg(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case AND -> new ANDInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    r.reg(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case EOR -> new EORInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    r.reg(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case ORR -> new ORRInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    r.reg(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case RSB -> new RSBInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    r.reg(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case SUB -> new SUBInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    r.reg(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
              };
          insnList.add(newInsn);
          i++;
          continue;
        }
        if (d.op2() instanceof Operand2.Register r && r.isPlain() && r.reg().equals(b.dst())) {
          // Pattern matched;
          final var newInsn =
              switch (d.type()) {
                case ADD -> new ADDInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    d.src(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case AND -> new ANDInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    d.src(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case EOR -> new EORInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    d.src(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case ORR -> new ORRInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    d.src(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case RSB -> new RSBInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    d.src(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
                case SUB -> new SUBInsn(
                    d.condition(),
                    d.updateConditionFlags(),
                    d.dst(),
                    d.src(),
                    new Operand2.Register(b.src(), b.op(), b.shift()));
              };
          insnList.add(newInsn);
          i++;
          continue;
        }
      }
      if (nextInsn instanceof CompareInsn c && c.condition().equals(b.condition())) {
        if (c.op2() instanceof Operand2.Register r && r.isPlain() && r.reg().equals(b.dst())) {
          // Pattern matched;
          final var newInsn =
              switch (c.type()) {
                case CMN -> new CMNInsn(
                    c.condition(), c.register(), new Operand2.Register(b.src(), b.op(), b.shift()));
                case CMP -> new CMPInsn(
                    c.condition(), c.register(), new Operand2.Register(b.src(), b.op(), b.shift()));
                case TEQ -> new TEQInsn(
                    c.condition(), c.register(), new Operand2.Register(b.src(), b.op(), b.shift()));
                case TST -> new TSTInsn(
                    c.condition(), c.register(), new Operand2.Register(b.src(), b.op(), b.shift()));
              };
          insnList.add(newInsn);
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
   *   MOV R0, #0
   *   MUL R3, R0, R1 or MUL R3, R1, R0
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
      if (nextInsn instanceof MULInsn mul
          && mul.condition() == Condition.AL
          && !mul.updateConditionFlags()) {
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
      if (nextInsn instanceof MULInsn mul && !mul.updateConditionFlags()) {
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
   *   MOV R1, #0
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
      if (nextInsn instanceof ADDInsn add
          && add.condition() == Condition.AL
          && !add.updateConditionFlags()) {
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
      if (nextInsn instanceof SUBInsn sub
          && sub.condition() == Condition.AL
          && !sub.updateConditionFlags()) {
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
          && add.condition() == Condition.AL
          && !add.updateConditionFlags()
          && add.op2() instanceof Operand2.Immediate op2
          && op2.value() == 0) {
        insnList.add(new MOVInsn(Condition.AL, add.dst(), new Operand2.Register(add.src())));
      } else if (insn instanceof SUBInsn sub
          && sub.condition() == Condition.AL
          && !sub.updateConditionFlags()
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
      if (!(insn instanceof MOVInsn m && m.op2() instanceof Operand2.Immediate op2)) {
        insnList.add(insn);
        continue;
      }
      final var nextInsn = input.insnList().get(i + 1);
      if (nextInsn instanceof DataBinaryInsn d
          && d.condition().equals(m.condition())
          && d.op2() instanceof Operand2.Register r
          && r.isPlain()
          && r.reg().equals(m.register())) {
        // Pattern matched;
        final var newInsn =
            switch (d.type()) {
              case ADD -> new ADDInsn(
                  d.condition(),
                  d.updateConditionFlags(),
                  d.dst(),
                  d.src(),
                  new Operand2.Immediate(op2.value()));
              case AND -> new ANDInsn(
                  d.condition(),
                  d.updateConditionFlags(),
                  d.dst(),
                  d.src(),
                  new Operand2.Immediate(op2.value()));
              case EOR -> new EORInsn(
                  d.condition(),
                  d.updateConditionFlags(),
                  d.dst(),
                  d.src(),
                  new Operand2.Immediate(op2.value()));
              case ORR -> new ORRInsn(
                  d.condition(),
                  d.updateConditionFlags(),
                  d.dst(),
                  d.src(),
                  new Operand2.Immediate(op2.value()));
              case RSB -> new RSBInsn(
                  d.condition(),
                  d.updateConditionFlags(),
                  d.dst(),
                  d.src(),
                  new Operand2.Immediate(op2.value()));
              case SUB -> new SUBInsn(
                  d.condition(),
                  d.updateConditionFlags(),
                  d.dst(),
                  d.src(),
                  new Operand2.Immediate(op2.value()));
            };
        insnList.add(newInsn);
        i++;
        continue;
      }
      if (nextInsn instanceof CompareInsn c
          && c.condition().equals(m.condition())
          && c.op2() instanceof Operand2.Register r
          && r.reg().equals(m.register())) {
        // Pattern matched
        final var newInsn =
            switch (c.type()) {
              case CMN -> new CMNInsn(
                  c.condition(), c.register(), new Operand2.Immediate(op2.value()));
              case CMP -> new CMPInsn(
                  c.condition(), c.register(), new Operand2.Immediate(op2.value()));
              case TST -> new TSTInsn(
                  c.condition(), c.register(), new Operand2.Immediate(op2.value()));
              case TEQ -> new TEQInsn(
                  c.condition(), c.register(), new Operand2.Immediate(op2.value()));
            };
        insnList.add(newInsn);
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
      if (nextInsn instanceof SUBInsn sub
          && sub.condition() == Condition.AL
          && !sub.updateConditionFlags()
          && sub.src().equals(m.register())) {
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
              new RSBInsn(Condition.AL, false, sub.dst(), op2reg.reg(), new Operand2.Immediate(0)));
          i++;
          continue;
        }
      }
      insnList.add(insn);
    }
    return new Program(insnList);
  }
}
