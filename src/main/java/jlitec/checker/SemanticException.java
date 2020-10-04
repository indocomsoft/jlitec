package jlitec.checker;

import java.util.Collections;
import java.util.List;
import jlitec.ast.Locatable;

public class SemanticException extends Exception {
  public final List<? extends Locatable> locatableList;

  public SemanticException(String message, List<? extends Locatable> locatable) {
    super(message);
    this.locatableList = Collections.unmodifiableList(locatable);
  }
}
