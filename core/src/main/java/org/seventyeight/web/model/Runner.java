package org.seventyeight.web.model;

import org.seventyeight.web.servlet.responses.WebResponse;

public interface Runner {
	public WebResponse run();
	public void injectContext(CallContext context);
}
