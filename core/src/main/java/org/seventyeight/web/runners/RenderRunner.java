package org.seventyeight.web.runners;

import java.io.IOException;

import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.model.RunnerException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.ResponseAction;

public class RenderRunner implements Runner {

	private Class<?> imposter;
	private Object object;
	private String method;
	
	private Request context;
	
	public RenderRunner(Object object, String method, Class<?> imposter) {
		this.object = object;
		this.method = method;
		this.imposter = imposter;
	}
	
	@Override
	public void run(Response roidesponse) {
		try {
			//WebResponse r = new WebResponse();
			//r.append(context.getCore().getTemplateManager().getRenderer(context).renderClass( object, imposter, method + ".vm"));
			//return r;
		} catch(Exception e) {
			//throw new RunnerException("Unable to run render " + method, e);
			//return new WebResponse().setCode(500).setHeader("Unable to render " + method + " for " + imposter);
		}
	}

	@Override
	public void injectContext(CallContext context) {
		if(!(context instanceof Request)) {
			throw new IllegalArgumentException("Not correct context, " + context);
		}
		
		this.context = (Request) context;
	}

}
