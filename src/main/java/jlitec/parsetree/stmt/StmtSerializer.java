package jlitec.parsetree.stmt;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class StmtSerializer implements JsonSerializer<Stmt> {
  @Override
  public JsonElement serialize(Stmt src, Type typeOfSrc, JsonSerializationContext context) {
    final var jsonObject = context.serialize(src).getAsJsonObject();
    jsonObject.addProperty("stmtType", src.getStmtType().toString());
    return jsonObject;
  }
}
