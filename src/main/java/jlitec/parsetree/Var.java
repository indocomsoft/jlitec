package jlitec.parsetree;

import java_cup.runtime.ComplexSymbolFactory.Location;

public record Var(Type type, Name name, Location leftLocation, Location rightLocation)
    implements Locatable {}
