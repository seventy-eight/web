package org.seventyeight.web.servlet.responses;

public class ErrorResponse extends WebResponse {

	private Throwable e;
	
	public ErrorResponse(Throwable e) {
		this.e = e;
	}
}
