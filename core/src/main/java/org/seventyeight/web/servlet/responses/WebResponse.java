package org.seventyeight.web.servlet.responses;

import java.io.IOException;
import java.util.List;

import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

public class WebResponse {
	
	public static final String CONTENT_TYPE_HTML = "text/html";
	public static final String CONTENT_TYPE_JSON = "application/json";
	
	private StringBuilder buffer = new StringBuilder();
	
	/** By default, everything is ok */
    protected int code = 200;
    
    /** And the header is ok */
    protected String header = "OK";
    
    protected String contentType = CONTENT_TYPE_HTML;
    
    public static WebResponse makeJsonResponse() {
    	WebResponse wr = new WebResponse();
    	wr.contentType = CONTENT_TYPE_JSON;
    	return wr;
    }
    
    public static WebResponse makeEmptyJsonResponse() {
    	WebResponse wr = new WebResponse();
    	wr.contentType = CONTENT_TYPE_JSON;
    	wr.buffer.append("{}");
    	return wr;
    } 
    
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
    
    public WebResponse notFound() {
    	this.code = 404;
    	return this;
    }
    
    public WebResponse notAccepted() {
    	this.code = 406;
    	return this;
    }
    
    public WebResponse accepted() {
    	this.code = 202;
    	return this;
    }
    
    public WebResponse serverError() {
    	this.code = 500;
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
