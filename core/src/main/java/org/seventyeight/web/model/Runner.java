package org.seventyeight.web.model;

import org.seventyeight.web.servlet.WebResponse;

public interface Runner {
	public WebResponse run() throws RunnerException;
	public void injectContext(CallContext context);
}
