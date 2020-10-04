package jlitec.parsetree;

import java_cup.runtime.ComplexSymbolFactory;

public interface Locatable {
  ComplexSymbolFactory.Location leftLocation();

  ComplexSymbolFactory.Location rightLocation();
}
