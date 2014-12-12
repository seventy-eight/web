package org.seventyeight.web.model;

import org.seventyeight.web.servlet.Response;

public interface Runner {
	public void run(Response response) throws RunnerException;
	public void injectContext(CallContext context);
}
