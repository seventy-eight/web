package org.seventyeight.web.runners;

import org.seventyeight.web.model.Autonomous;
import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.responses.ErrorResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

public class AutonomousRunner implements Runner {

	private Autonomous a;
	private Request context;
	
	public AutonomousRunner(Autonomous a) {
		this.a = a;
	}
	
	@Override
	public WebResponse run() {
		try {
			return a.autonomize(context);
		} catch (Exception e) {
			return new ErrorResponse(e.getCause()).setCode(500).setHeader("Unable to run autonomous, " + a);
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
