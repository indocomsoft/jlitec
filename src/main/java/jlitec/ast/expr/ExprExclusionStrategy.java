package jlitec.ast.expr;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class ExprExclusionStrategy implements ExclusionStrategy {
  @Override
  public boolean shouldSkipField(FieldAttributes f) {
    if (f.getDeclaringClass() == BinaryExpr.class && f.getName().equals("type")) {
      return true;
    }
    return false;
  }

  @Override
  public boolean shouldSkipClass(Class<?> clazz) {
    return false;
  }
}
