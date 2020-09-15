package jlitec.ast.expr;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class ExprSerializer implements JsonSerializer<Expr> {
  @Override
  public JsonElement serialize(Expr src, Type typeOfSrc, JsonSerializationContext context) {
    final var jsonObject = context.serialize(src).getAsJsonObject();
    jsonObject.addProperty("exprType", src.getExprType().toString());
    return jsonObject;
  }
}
