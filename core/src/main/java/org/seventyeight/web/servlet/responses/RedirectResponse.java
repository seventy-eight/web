package org.seventyeight.web.servlet.responses;

public class RedirectResponse extends WebResponse {

	private String url;
	
	public RedirectResponse(String url) {
		this.url = url;
	}
}
