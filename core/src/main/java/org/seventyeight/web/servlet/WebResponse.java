package org.seventyeight.web.servlet;

public class WebResponse {
	
	private StringBuilder buffer = new StringBuilder();
	
	/** By default, everything is ok */
    protected int code = 200;
    
    /** No need for a header */
    protected String header = null;
    
    
}
