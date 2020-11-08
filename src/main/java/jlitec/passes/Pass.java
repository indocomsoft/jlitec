package jlitec.passes;

public interface Pass<InputType, OutputType> {
  OutputType pass(InputType input);
}
