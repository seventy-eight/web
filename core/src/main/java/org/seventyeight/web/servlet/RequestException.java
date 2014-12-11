package org.seventyeight.web.servlet;

import org.seventyeight.web.CoreException;

public class RequestException extends CoreException {

	public RequestException(String m, String header, int code) {
		super(m, header, code);
	}

}
