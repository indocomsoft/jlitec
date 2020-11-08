package jlitec.backend.arm.codegen;

import java.util.HashMap;
import java.util.Map;

public class StringGen {
  private int counter = 0;
  private Map<String, String> stringToId = new HashMap<>();

  /**
   *
   * @param string the string to use
   * @return the label for the string
   */
  public String gen(String string) {
    if (stringToId.containsKey(string)) {
      return stringToId.get(string);
    }
    final var label = "S" + counter;
    counter += 1;
    stringToId.put(string, label);
    return label;
  }

  public Map<String, String> getStringToId() {
    return stringToId;
  }
}
