package org.seventyeight.web.servlet;

import java.io.IOException;

import org.seventyeight.web.servlet.responses.ResponseAction;

/**
 * @author cwolfgang
 */
public class Response {

	private ResponseAction action;
	
	public Response setAction(ResponseAction action) {
		this.action = action;
		return this;
	}
	
	public void respond(Request request, WebResponse response) throws IOException {
		if(action == null) {
			
		}
		
		action.respond(request, response);
	}
}