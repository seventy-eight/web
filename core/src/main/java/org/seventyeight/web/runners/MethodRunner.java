package org.seventyeight.web.runners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.model.RunnerException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.WebResponse;

public class MethodRunner implements Runner {

	private Method method;
	private Object object;
	
	private Request context;
	
	public MethodRunner(Object object, Method method) {
		this.object = object;
		this.method = method;
	}
	
	@Override
	public WebResponse run() throws RunnerException {
		try {
			method.invoke( object, context, response );
		} catch(InvocationTargetException e) {
			throw new RunnerException("Unable to run method, " + method.getName(), (Exception)e.getCause());
		} catch (Exception e) {
			throw new RunnerException("Failed while running " + method.getName(), e);
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
