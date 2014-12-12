package org.seventyeight.web.servlet.responses;

import java.io.IOException;

import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.WebResponse;

public class ResponseAction {
	
	private StringBuilder buffer = new StringBuilder();
	
	/** By default, everything is ok */
    protected int code = 200;
    
    /** And the header is ok */
    protected String header = "OK";
    
    public ResponseAction setCode(int code) {
    	this.code = code;
    	return this;
    }
    
    public ResponseAction setHeader(String header) {
    	this.header = header;
    	return this;
    }
    
    public ResponseAction append(String s) {
    	this.buffer.append(s);
    	return this;
    }
    
    public int getCode() {
    	return code;
    }
    
    public String getHeader() {
    	return header;
    }
    
    public boolean hasBody() {
    	return buffer.length() > 0;
    }
    
    public StringBuilder getBuffer() {
    	return buffer;
    }
    
    public void respond(Request request, WebResponse response) throws IOException {
		response.setIntHeader(header, code);
    	if(buffer.length() > 0) {
    		response.getWriter().write(buffer.toString());
    	} else {
    		
    	}
    }
}
