package org.seventyeight.web.servlet.responses;

import com.google.gson.JsonElement;

public class JsonResponse extends ResponseAction {

	public JsonResponse(String json) {
		// TODO: Possibly do a check?
		append(json);
	}
	
	public JsonResponse(JsonElement json) {
		append(json.getAsString());
	}
}
