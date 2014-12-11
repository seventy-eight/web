package org.seventyeight.web.runners;

import java.io.IOException;

import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.model.RunnerException;
import org.seventyeight.web.servlet.WebResponse;

public class RedirectRunner implements Runner {

	private String url;
	
	public RedirectRunner(String url) {
		this.url = url;
	}
	
	@Override
	public WebResponse run() throws RunnerException {
		try {
			response.sendRedirect(url);
		} catch (IOException e) {
			throw new RunnerException("Unable to run redirect to " + url, e);
		}
	}

	@Override
	public void injectContext(CallContext context) {
		// TODO Auto-generated method stub
		
	}

}
