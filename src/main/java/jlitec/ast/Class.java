package jlitec.ast;

import java.util.List;

public class Class {
  public final String cname;
  public final List<Var> fields;
  public final List<Method> methods;

  /**
   * The only constructor.
   *
   * @param cname class name.
   * @param fields list of class fields.
   * @param methods list of class methods.
   */
  public Class(String cname, List<Var> fields, List<Method> methods) {
    this.cname = cname;
    this.fields = fields;
    this.methods = methods;
  }
}
