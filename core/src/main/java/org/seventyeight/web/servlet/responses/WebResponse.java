package org.seventyeight.web.servlet.responses;

public class WebResponse {
	
	private StringBuilder buffer = new StringBuilder();
	
	/** By default, everything is ok */
    protected int code = 200;
    
    /** No need for a header */
    protected String header = null;
    
    public WebResponse setCode(int code) {
    	this.code = code;
    	return this;
    }
    
    public WebResponse setHeader(String header) {
    	this.header = header;
    	return this;
    }
    
    public WebResponse append(String s) {
    	this.buffer.append(s);
    	return this;
    }
}
