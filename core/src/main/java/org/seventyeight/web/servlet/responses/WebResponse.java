package org.seventyeight.web.servlet.responses;

import java.io.IOException;
import java.util.List;

import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

public class WebResponse {
	
	private StringBuilder buffer = new StringBuilder();
	
	/** By default, everything is ok */
    protected int code = 200;
    
    /** And the header is ok */
    protected String header = "OK";
    
    public WebResponse setCode(int code) {
    	this.code = code;
    	return this;
    }
    
    public WebResponse setHeader(String header) {
    	this.header = header;
    	return this;
    }
    
    public WebResponse appendBody(String s) {
    	this.buffer.append(s);
    	return this;
    }
    
    public WebResponse appendBody(List<MongoDocument> documents) {
		appendBody(documents.toString());
		return this;
    }
    
    public WebResponse appendBody(long l) {
    	this.buffer.append(l);
    	return this;
    }
    
    public void respond(Request request, Response response) throws IOException {
    	response.setIntHeader(header, code);
    	if(buffer.length() > 0) {
    		response.getWriter().write(buffer.toString());
    	} else {
    		
    	}
    }
}
