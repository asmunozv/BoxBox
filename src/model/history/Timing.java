package model.history;

import com.google.gson.*;
import java.time.*;
import java.util.Locale;

public final class Timing {
    private Timing(){}
    public static String text(JsonObject o,String key){JsonElement v=o==null?null:o.get(key);return v==null||v.isJsonNull()?null:v.getAsString();}
    public static Integer integer(JsonObject o,String key){String v=text(o,key);if(v==null||v.isBlank())return null;try{return Integer.valueOf(v);}catch(NumberFormatException e){return null;}}
    public static Double decimal(JsonObject o,String key){String v=text(o,key);if(v==null||v.isBlank())return null;try{double n=Double.parseDouble(v);return Double.isFinite(n)?n:null;}catch(NumberFormatException e){return null;}}
    public static JsonObject object(JsonObject o,String key){JsonElement v=o==null?null:o.get(key);return v!=null&&v.isJsonObject()?v.getAsJsonObject():new JsonObject();}
    public static JsonArray array(JsonObject o,String key){JsonElement v=o==null?null:o.get(key);return v!=null&&v.isJsonArray()?v.getAsJsonArray():new JsonArray();}
    public static String or(String s,String fallback){return s==null||s.isBlank()?fallback:s;}
    public static Double seconds(String value){
        if(value==null||value.isBlank())return null;
        try{double result=0;for(String p:value.split(":"))result=result*60+Double.parseDouble(p);return Double.isFinite(result)&&result>0?result:null;}
        catch(NumberFormatException e){return null;}
    }
    public static Instant instant(String s){if(s==null)return null;try{return OffsetDateTime.parse(s).toInstant();}catch(DateTimeException e){return null;}}
    public static String format(Double seconds){if(seconds==null||!Double.isFinite(seconds))return "—";return String.format(Locale.ROOT,"%d:%06.3f",(int)(seconds/60),seconds%60);}
    public static String number(Double value){return value==null?"—":String.format(Locale.ROOT,"%.3f",value);}
}
