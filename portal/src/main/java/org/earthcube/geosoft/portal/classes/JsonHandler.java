package org.earthcube.geosoft.portal.classes;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

public class JsonHandler {
	public static Gson createGson() {
		return new GsonBuilder().disableHtmlEscaping().create();
	}
	
	public static Gson createPrettyGson() {
		return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	}
	
  public static Gson createDataGson() {
    GsonBuilder gson = new GsonBuilder();
    gson.registerTypeAdapter(XSDDateTime.class, new XSDDateTimeSerializer());
    return gson.disableHtmlEscaping().setPrettyPrinting().create();
  }
}

/**
 * Date Serializer
 * -- convert to timestamp (long) 
 */
class DateSerializer implements JsonSerializer<Date> {
  public JsonElement serialize(Date date, Type typeOfSrc,
      JsonSerializationContext context) {
    return context.serialize(date.getTime()/1000);
  }
}
/**
 * XSDDateTime Serializer
 * -- convert to string 
 *
 */
class XSDDateTimeSerializer implements JsonSerializer<XSDDateTime> {
  public JsonElement serialize(XSDDateTime dateTime, Type typeOfSrc,
      JsonSerializationContext context) {
    SimpleDateFormat xsddate_format = new SimpleDateFormat("yyyy-MM-dd");
    return context.serialize(xsddate_format.format(dateTime.asCalendar().getTime()));
  }
}