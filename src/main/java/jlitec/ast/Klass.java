package jlitec.ast;

import java.util.List;

public record Klass(String cname, List<Var> fields, List<Method> methods) { }
