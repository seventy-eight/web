package org.seventyeight.web.runners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.model.RunnerException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.ErrorResponse;
import org.seventyeight.web.servlet.responses.ResponseAction;

public class MethodRunner implements Runner {

	private Method method;
	private Object object;
	
	private Request context;
	
	public MethodRunner(Object object, Method method) {
		this.object = object;
		this.method = method;
	}
	
	@Override
	public void run(Response response)  {
		try {
			method.invoke( object, context, response );
		} catch(InvocationTargetException e) {
			//return new ErrorResponse(e.getCause()).setCode(500).setHeader("Unable to run method, " + method.getName());
		} catch (Exception e) {
			//return new ErrorResponse(e).setCode(500).setHeader("Failed while running, " + method.getName());
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
