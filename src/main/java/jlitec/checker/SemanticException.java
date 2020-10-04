package jlitec.checker;

import java.util.Collections;
import java.util.List;
import jlitec.ast.Locatable;

public class SemanticException extends Exception {
  public final List<? extends Locatable> locatableList;
  public final String shortMessage;

  public SemanticException(String message, String shortMessage, List<? extends Locatable> locatable) {
    super(message);
    this.shortMessage = shortMessage;
    this.locatableList = Collections.unmodifiableList(locatable);
  }
}
