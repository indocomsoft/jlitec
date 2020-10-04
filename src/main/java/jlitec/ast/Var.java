package jlitec.ast;

import java_cup.runtime.ComplexSymbolFactory.Location;

public record Var(Type type, String id, Location leftLocation, Location rightLocation)
    implements Locatable {}
