package jlitec.parsetree;

import java_cup.runtime.ComplexSymbolFactory.Location;

public record Name(String id, Location leftLocation, Location rightLocation) implements Locatable {}
