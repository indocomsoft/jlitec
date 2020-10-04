package jlitec.parsetree;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import java_cup.runtime.ComplexSymbolFactory;
import jlitec.parsetree.expr.BinaryExpr;

public class GsonExclusionStrategy implements ExclusionStrategy {
  @Override
  public boolean shouldSkipField(FieldAttributes f) {
    if (f.getDeclaringClass() == BinaryExpr.class && f.getName().equals("type")) {
      return true;
    }
    if (f.getDeclaredType() == ComplexSymbolFactory.Location.class) {
      return true;
    }
    return false;
  }

  @Override
  public boolean shouldSkipClass(Class<?> clazz) {
    return false;
  }
}
