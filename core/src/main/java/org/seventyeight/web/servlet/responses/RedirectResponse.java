package org.seventyeight.web.servlet.responses;

import java.io.IOException;

import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

public class RedirectResponse extends WebResponse {

	private String url;
	
	public RedirectResponse(String url) {
		this.url = url;
	}

	@Override
	protected void writeBody(Request request, Response response) throws IOException {
		response.sendRedirect(url);
	}
	
	
}
