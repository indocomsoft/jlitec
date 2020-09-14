package jlitec.ast;

import java.util.List;

public class Program {
  public final List<Klass> klassList;

  public Program(List<Klass> klassList) {
    this.klassList = klassList;
  }
}
