package jlitec.backend.passes.optimization;

import jlitec.backend.passes.lower.Program;
import jlitec.backend.passes.optimization.algebraic.AlgebraicPass;
import jlitec.backend.passes.optimization.binarybit.BinaryBitOptimizationPass;
import jlitec.backend.passes.optimization.constantfolding.ConstantFoldingOptimizationPass;
import jlitec.backend.passes.optimization.deadcode.DeadcodeOptimizationPass;

public class PassManager {
  // Prevent instantiation
  private PassManager() {}

  public static Program performOptimizationPasses(Program input) {
    var output = input;
    while (true) {
      final var binaryBitOutput = new BinaryBitOptimizationPass().pass(output);
      final var algebraicOutput = new AlgebraicPass().pass(binaryBitOutput);
      final var deadcodeOutput = new DeadcodeOptimizationPass().pass(algebraicOutput);
      final var constantFoldingOutput = new ConstantFoldingOptimizationPass().pass(deadcodeOutput);
      if (output.equals(constantFoldingOutput)) {
        return output;
      }
      output = constantFoldingOutput;
    }
  }
}
