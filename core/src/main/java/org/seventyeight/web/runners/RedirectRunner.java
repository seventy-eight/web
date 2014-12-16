package org.seventyeight.web.runners;

import java.io.IOException;

import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.model.RunnerException;
import org.seventyeight.web.servlet.responses.RedirectResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

public class RedirectRunner implements Runner {

	private String url;
	
	public RedirectRunner(String url) {
		this.url = url;
	}
	
	@Override
	public WebResponse run() {
		return new RedirectResponse(url);
	}

	@Override
	public void injectContext(CallContext context) {
		// TODO Auto-generated method stub
		
	}

}
