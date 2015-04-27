package org.seventyeight.web.utilities;

import com.google.gson.*;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.servlet.Request;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author cwolfgang
 */
public class JsonUtils {

    private static Logger logger = LogManager.getLogger( JsonUtils.class );

    private JsonUtils() {

    }

	public static String toHtml(String key, JsonElement json) {
		StringBuilder sb = new StringBuilder();
		
		if(json.isJsonObject()) {
			JsonObject o = (JsonObject) json;
			if(key != null) {
				sb.append("<div ");			
				sb.append("name=\"");
				sb.append(key);
				sb.append("\" ");
				sb.append("class=\"targetObject\">");
			}
			
			for(Entry<String, JsonElement> e : o.entrySet()) {
				sb.append(toHtml(e.getKey(), e.getValue()));
			}
			if(key != null) {
				sb.append("</div>");
			}
		} else if(json.isJsonArray()) {
			JsonArray a = (JsonArray) json;
			sb.append("<div name=\"");
			sb.append(key);
			sb.append("\">");
			for(JsonElement e : a) {
				sb.append(toHtml(null, e));
			}
			sb.append("</div>");
		} else {
			sb.append("<input name=\"");
			sb.append(key);
			sb.append("\" value=");
			sb.append(json.toString());
			sb.append(">");
		}
		
		return sb.toString();
	}
    
    public static boolean hasACLExtension(JsonObject json) {
    	if(json == null) {
    		return false;
    	}
    	
    	if(!json.has("extensions") || !json.get("extensions").isJsonArray()) {
    		return false;
    	}
    	
    	for(JsonElement e : json.get("extensions").getAsJsonArray()) {
    		if(e.isJsonObject()) {
    			JsonObject o = e.getAsJsonObject();
    			if(o.has("extension")) {
    				
    			}
    		}
    	}
    	
    	return false;
    }
    
    public static JsonObject getJsonFromField( Request request ) throws JsonException {
        String json = request.getParameter( "json" );
        if( json == null ) {
            throw new JsonException( "Json was null" );
        }

        JsonParser parser = new JsonParser();
        JsonObject jo = (JsonObject) parser.parse( json );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println( gson.toJson( jo ) );

        return jo;
    }
    
    public static JsonObject getJsonRequest2(Request request) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(request.getInputStream(), writer, "UTF-8");
        /*
        while ((s = request.getReader().readLine()) != null) {
        	sb.append(s);
        }
        */
        
        JsonParser parser = new JsonParser();
        JsonObject jo = (JsonObject) parser.parse( writer.toString() );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println( gson.toJson( jo ) );

        return jo;
    }
    
    public static JsonObject getJsonRequest(Request request) throws IOException {
        StringBuilder sb = new StringBuilder();
        String s;
        while ((s = request.getReader().readLine()) != null) {
        	sb.append(s);
        }
        
        JsonParser parser = new JsonParser();
        JsonObject jo = (JsonObject) parser.parse( sb.toString() );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println( gson.toJson( jo ) );

        return jo;
    }


    public static final String CONFIGURATION = "config";
    //public static final String __JSON_EXTENSION_NAME = "extensions";
    public static final String CLASS_NAME = "class";
    public static final String EXTENSION = "extension";
    public static final String CONFIGURATIONS = "configurations";

    public static String get(JsonObject json, String key, String defaultValue) {
        if(json.has( key )) {
            return json.get( key ).getAsString();
        } else {
            return defaultValue;
        }
    }

    public static int get(JsonObject json, String key, int defaultValue) {
        if(json.has( key )) {
            return json.get( key ).getAsInt();
        } else {
            return defaultValue;
        }
    }

    public enum JsonType {
        config,
        extensionClass
    }

    public static List<JsonObject> getJsonObjects( JsonObject obj ) {
        return getJsonObjects( obj, "extensions" );
    }

    public static List<JsonObject> getJsonObjects( JsonObject obj, String type ) {
        logger.debug( "Getting " + type + " Json objects" );

        List<JsonObject> objects = new ArrayList<JsonObject>();

        JsonElement configElement = obj.get( type );

        /**/
        if( configElement != null ) {
            if( configElement.isJsonObject() ) {
                logger.debug( "obj is JsonObject" );
                objects.add( configElement.getAsJsonObject() );
            } else if( configElement.isJsonArray() ) {
                logger.debug( "obj is JsonArray" );
                JsonArray jarray = configElement.getAsJsonArray();

                for( JsonElement e : jarray ) {
                    logger.debug( "e is jsonObject, {}, {}", e, e.isJsonObject() );
                    if( e.isJsonObject() ) {
                        objects.add( e.getAsJsonObject() );
                    }
                }
            }
        }

        return objects;
    }
}
