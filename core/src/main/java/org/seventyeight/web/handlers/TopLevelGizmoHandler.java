package org.seventyeight.web.handlers;

import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.seventyeight.web.SeventyEight;
import org.seventyeight.web.exceptions.ActionHandlerException;
import org.seventyeight.web.exceptions.CouldNotLoadItemException;
import org.seventyeight.web.exceptions.NoSuchJsonElementException;
import org.seventyeight.web.model.*;
import org.seventyeight.web.model.resources.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.utilities.ClassUtils;
import org.seventyeight.web.utilities.JsonUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author cwolfgang
 *         Date: 17-01-13
 *         Time: 14:10
 */
public class TopLevelGizmoHandler {

    private static Logger logger = Logger.getLogger( TopLevelGizmoHandler.class );

    // (0)/(1)handlers/(2)first((3)second/(n)last
    // n is either an actions index or an action method

    public void execute( TopLevelGizmo gizmo, Request request, Response response ) throws ActionHandlerException {
        if( gizmo instanceof ItemType ) {
            handleItemType( (ItemType) gizmo, request, response );
        } else if( gizmo instanceof TopLevelAction ) {
            handleTopLevelAction( (TopLevelAction) gizmo, request, response );
        } else if( gizmo instanceof TopLevelExecutor ) {
            handleExecutor( (TopLevelExecutor) gizmo, request, response );
        } else {
            throw new ActionHandlerException( "WHAT??? " + gizmo );
        }
    }

    private void handleExecutor( TopLevelExecutor executor, Request request, Response response ) throws ActionHandlerException {
        executor.execute( request, response );
    }

    private void handleTopLevelAction( TopLevelAction action, Request request, Response response ) throws ActionHandlerException {
        actions( (Item) action, 2, request, response );
    }

    private void handleItemType( ItemType type, Request request, Response response ) throws ActionHandlerException {
        if( request.getRequestParts().length > 2 ) {
            String name = request.getRequestParts()[2];
            AbstractItem item = null;
            try {
                item = type.getItem( name, request.getDB() );
            } catch( CouldNotLoadItemException e ) {
                throw new ActionHandlerException( e );
            }

            /* Authorization */
            checkAuthorization( item, request.getUser(), Authorizer.Authorization.get( request.isRequestPost() ) );

            request.getContext().put( "title", item.getDisplayName() );

            if( item instanceof Actionable ) {
                actions( item, 3, request, response );
            } else {
                if( request.getRequestParts().length > 2 ) {
                    throw new ActionHandlerException( "No such action, " + request.getRequestURI() );
                } else {
                    executeThing( request, response, item, "index" );
                }
            }

        } else {
            /* TODO, what? */
        }
    }

    private void checkAuthorization( Item item, User user, Authorizer.Authorization requiredAuthorization ) throws ActionHandlerException {
        logger.debug( "[Authorization check] "  + user + " for " + item );
        if( item instanceof Authorizable ) {
            Authorizable a = (Authorizable) item;
            Authorizer authorizer = a.getAuthorizer();

            logger.debug( authorizer.getAuthorization( user ).ordinal() + " >= " + requiredAuthorization.ordinal() );

            if( authorizer.getAuthorization( user ).ordinal() < requiredAuthorization.ordinal() ) {
                throw new ActionHandlerException( user + " was not authorized" );
            }
        }
    }

    public void actions( Item item, int uriStart, Request request, Response response ) throws ActionHandlerException {

        int i = uriStart;
        int l = request.getRequestParts().length;
        Action action = null;
        Item lastItem = item;
        String urlName = "index";
        for( ; i < l ; i++ ) {
            urlName = request.getRequestParts()[i];
            logger.debug( "Url name is " + urlName );

            lastItem = item;
            action = null;

            if( item instanceof Actionable ) {
                for( Action a : ((Actionable)item).getActions() ) {
                    logger.debug( "Action is " + a );
                    if( a.getUrlName().equals( urlName ) ) {
                        action = a;
                        break;
                    }
                }
                item = (Item) action;
            } else {
                i++;
                break;
            }

            if( action == null ) {
                logger.debug( "Action was null, breaking" );
                i++;
                break;
            }
        }

        logger.debug( "[Action method] " + urlName + " -> " + action + "/" + lastItem );

        if( action != null ) {
            /* Last sub space was an action, do its index method */
            logger.debug( "Action was NOT null" );
            executeThing( request, response, action, "index" );
        } else {
            // i:3  l:3
            //if( i == l - 1 ) {
            if( i == l ) {
                /* We came to an end */
                logger.debug( "Action was null" );
                executeThing( request, response, lastItem, urlName );

            } else {
                throw new ActionHandlerException( urlName + " not defined for " + lastItem );
            }
        }

    }

    private void executeThing( Request request, Response response, Item item, String urlName ) throws ActionHandlerException {
        logger.debug( "EXECUTE: " + item + ", " + urlName );

        /* First try to find a view, if not a POST */
        try {
            logger.debug( "Item: " + item + " -> " + urlName );
            executeMethod( item, request, response, urlName );
            return;
        } catch( Exception e ) {
            e.printStackTrace();
            logger.debug( item + " does not not have " + urlName + ", " + e.getMessage() );
        }

            logger.debug( "TRYING VIEW FILE" );

        if( !request.isRequestPost() ) {
            try {
                request.getContext().put( "content", SeventyEight.getInstance().getTemplateManager().getRenderer( request ).renderObject( item, urlName + ".vm" ) );
                response.getWriter().print( SeventyEight.getInstance().getTemplateManager().getRenderer( request ).render( request.getTemplate() ) );
                return;
            } catch( Exception e ) {
                logger.debug( "Unable to view " + urlName + " for " + item + ": " + e.getMessage() );
                throw new ActionHandlerException( e );
            }
        }
    }

    private void executeMethod( Item item, Request request, Response response, String actionMethod ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchJsonElementException {
        logger.debug( "METHOD: " + item + ", " + actionMethod );

        Method method = getRequestMethod( item, actionMethod, request.isRequestPost() );
        logger.debug( "FOUND METHOD: " + method );

        if( request.isRequestPost() ) {
            JsonObject json = null;
            try {
                json = JsonUtils.getJsonFromRequest( request );
            } catch ( Exception e ) {
                logger.debug( e.getMessage() );
            }
            method.invoke( item, request, response, json );
        } else {
            method.invoke( item, request, response );
        }
    }

    private Method getRequestMethod( Item item, String method, boolean post ) throws NoSuchMethodException {
        String m = "do" + method.substring( 0, 1 ).toUpperCase() + method.substring( 1, method.length() );
        logger.debug( "Method(P:" + post + "): " + method + " = " + m );
        if( post ) {
            //return resource.getClass().getDeclaredMethod( m, ParameterRequest.class, JsonObject.class );
            return ClassUtils.getEnheritedMethod( item.getClass(), m, Request.class, Response.class, JsonObject.class );
        } else {
            //return action.getClass().getDeclaredMethod( m, Request.class, HttpServletResponse.class );
            return ClassUtils.getEnheritedMethod( item.getClass(), m, Request.class, Response.class );
        }
    }
}
