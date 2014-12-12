package org.seventyeight.web.servlet.responses;

public class RedirectResponse extends ResponseAction {

	private String url;
	
	public RedirectResponse(String url) {
		this.url = url;
	}
}
