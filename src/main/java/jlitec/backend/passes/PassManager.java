package jlitec.backend.passes;

import jlitec.backend.passes.lower.Program;
import jlitec.backend.passes.optimization.algebraic.AlgebraicPass;
import jlitec.backend.passes.optimization.constantfolding.ConstantFoldingOptimizationPass;
import jlitec.backend.passes.optimization.deadcode.DeadcodeOptimizationPass;

public class PassManager {
  // Prevent instantiation
  private PassManager() {}

  public static Program performOptimizationPasses(Program input) {
    var output = input;
    while (true) {
      final var algebraicOutput = new AlgebraicPass().pass(output);
      final var deadcodeOutput = new DeadcodeOptimizationPass().pass(algebraicOutput);
      final var constantFoldingOutput = new ConstantFoldingOptimizationPass().pass(deadcodeOutput);
      if (output.equals(constantFoldingOutput)) {
        return output;
      }
      output = constantFoldingOutput;
    }
  }
}
