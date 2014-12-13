package org.seventyeight.web.servlet.responses;

import java.util.List;

import org.seventyeight.database.mongodb.MongoDocument;

import com.google.gson.JsonElement;

public class JsonResponse extends WebResponse {
	
	private JsonResponse() {
		appendBody("{}");
	}
	
	public JsonResponse(String json) {
		appendBody(json);
	}

	public JsonResponse(JsonElement json) {
		appendBody(json.getAsString());
	}
	
	public JsonResponse(List<MongoDocument> documents) {
		appendBody(documents.toString());
	}
	
	public static JsonResponse empty() {
		return new JsonResponse();
	}
}
