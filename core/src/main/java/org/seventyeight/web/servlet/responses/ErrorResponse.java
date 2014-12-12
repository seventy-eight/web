package org.seventyeight.web.servlet.responses;

public class ErrorResponse extends ResponseAction {

	private Throwable e;
	
	public ErrorResponse(Throwable e) {
		this.e = e;
		this.code = 500;
		this.header = e.getMessage();
	}
}
