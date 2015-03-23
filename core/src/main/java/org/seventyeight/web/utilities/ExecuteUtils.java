package org.seventyeight.web.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.utils.ClassUtils;
import org.seventyeight.utils.DeleteMethod;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.utils.PutMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.model.Runner;
import org.seventyeight.web.runners.MethodRunner;
import org.seventyeight.web.runners.RenderRunner;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author cwolfgang
 */
public class ExecuteUtils {

    public static Logger logger = LogManager.getLogger( ExecuteUtils.class );

    private ExecuteUtils() {

    }

    public static Runner getRunner( CallContext request, Object object, String urlName ) throws Exception {
        return getRunner( request, object, urlName, object.getClass() );
    }

    public static Runner getRunner( CallContext request, Object object, String urlName, Class<?> imposter ) throws Exception {
        logger.debug( "Executing: {}, {}", object, urlName );

        if( imposter == null ) {
            imposter = object.getClass();
        }

        /* First try to find a view, if not a POST???? */
        try {
            return getMethodRunner( object, request, urlName );
        } catch( ReflectiveOperationException e ) {
            logger.debug( "{} does not have {}, {} ", object, urlName, e.getMessage() );
        }

        if( request.getMethodType().equals(CallContext.MethodType.GET) ) {
            return getRenderRunner(object, urlName, imposter );
        }
        
        //return null;
        throw new IllegalArgumentException(request.getUri() + ", " + request.getMethodType().toString() + " was not found");
    }

    public static void render(Object object, String method ) throws NotFoundException, TemplateException, IOException {
        getRenderRunner(object, method, object.getClass() );
    }

    public static RenderRunner getRenderRunner(Object object, String method, Class<?> imposter ){
        return new RenderRunner(object, method, imposter);
    }

    private static MethodRunner getMethodRunner( Object object, CallContext request, String actionMethod ) throws NoSuchMethodException {
        logger.debug( "Executing method : {} on {}", actionMethod, object );

        Method method = getRequestMethod( object, actionMethod, request);

        return new MethodRunner(object, method);
        // Implement MethodRunner
        //method.invoke( object, request, response );
    }

    private static Method getRequestMethod( Object object, String method, CallContext context ) throws NoSuchMethodException {
        String m = "do" + method.substring( 0, 1 ).toUpperCase() + method.substring( 1, method.length() );

        switch( context.getMethodType() ) {
        case POST:
            return ClassUtils.getInheritedAnnotatedMethod( object.getClass(), m, PostMethod.class, context.getRequestClass());

        case GET:
            return ClassUtils.getInheritedAnnotatedMethod( object.getClass(), m, GetMethod.class, context.getRequestClass());

        case PUT:
            return ClassUtils.getInheritedAnnotatedMethod( object.getClass(), m, PutMethod.class, context.getRequestClass());

        case DELETE:
            return ClassUtils.getInheritedAnnotatedMethod( object.getClass(), m, DeleteMethod.class, context.getRequestClass());

        default:
            return ClassUtils.getInheritedAnnotatedMethod( object.getClass(), m, GetMethod.class, context.getRequestClass());
        }
    }
}
