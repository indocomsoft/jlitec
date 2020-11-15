package jlitec.backend.passes;

public interface Pass<InputType, OutputType> {
  OutputType pass(InputType input);
}
