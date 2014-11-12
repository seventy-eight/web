package org.seventyeight.web.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.seventyeight.utils.StopWatch;
import org.seventyeight.web.Core;
import org.seventyeight.web.UserAgent;
import org.seventyeight.web.authentication.NoAuthorizationException;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.authorization.AccessControlled;
import org.seventyeight.web.model.PersistedNode;
import org.seventyeight.web.model.Theme;
import org.seventyeight.web.model.CoreRequest;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.utilities.JsonException;
import org.seventyeight.web.utilities.JsonUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author cwolfgang
 */
public class Request extends HttpServletRequestWrapper implements CoreRequest {

    private static Logger logger = LogManager.getLogger( Request.class );

    private static final String __MULTIPART = "multipart/form-data";

    private RequestMethod method = RequestMethod.GET;
    private Theme theme = null;
    private VelocityContext context;

    //private PersistedNode modelObject;

    private String template = "org/seventyeight/web/main.vm";

    private boolean transactional = false;

    private User user;
    private boolean authenticated;

    private String[] requestParts;

    private String view;

    private Locale locale = new Locale( "da", "DK" );

    private StopWatch stopWatch = null;

    private JsonObject json = null;

    public enum RequestMethod {
        GET,
        POST,
        DELETE,
        PUT
    }

    public Request( HttpServletRequest httpServletRequest, String defaultTemplate ) {
        super( httpServletRequest );
        setRequestMethod( httpServletRequest.getMethod() );

        this.template = defaultTemplate;

        //
        ServletContext context = getSession().getServletContext();
        Theme.Platform platform = (Theme.Platform) context.getAttribute( "platform" );
        logger.debug( "PLATFORM: {}", platform );

        if(platform == null) {
            String userAgentString = getHeader("User-Agent");
            UserAgent userAgent = UserAgent.getUserAgent( userAgentString );

            // Determine platform
            if(userAgent.getPlatform() == UserAgent.Platform.Android ||
               userAgent.getPlatform() == UserAgent.Platform.IPhone ||
               userAgent.getPlatform() == UserAgent.Platform.IPod ||
               userAgent.getPlatform() == UserAgent.Platform.IPad) {
                platform = Theme.Platform.Mobile;
            } else {
                platform = Theme.Platform.Desktop;
            }

            context.setAttribute( "platform", platform );
        }
    }

    public Theme.Platform getPlatform() {
        Theme.Platform platform = (Theme.Platform) getSession().getServletContext().getAttribute( "platform" );
        if(platform == null) {
            platform = Theme.Platform.Desktop;
        }

        return platform;
    }

    @Override
    public Core getCore() {
        Core core = (Core) getServletContext().getAttribute( "core" );
        if(core == null) {
            throw new IllegalStateException( "No core available in request" );
        }

        return core;
    }

    public void setRequestMethod( String m ) {
        this.method = RequestMethod.valueOf( m );
    }

    public boolean isRequestPost() {
        return method.equals( RequestMethod.POST );
    }

    public RequestMethod getRequestMethod() {
        return method;
    }

    public boolean isRequestPut() {
        return method.equals( RequestMethod.PUT );
    }

    public boolean isRequestDelete() {
        return method.equals( RequestMethod.DELETE );
    }

    public String[] getRequestParts() {
        return requestParts;
    }

    public void setRequestParts( String[] parts ) {
        this.requestParts = parts;
    }

    public void setTransactional( boolean t ) {
        this.transactional = t;
    }

    public boolean hasTransaction() {
        return transactional;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated( boolean authenticated ) {
        this.authenticated = authenticated;
    }

    public void checkPermissions( Node node, ACL.Permission permission ) throws NoAuthorizationException {
        logger.debug( "Checking authorization for " + user );

        while( node != null ) {
            logger.debug( "Checking " + node );
            if( node instanceof AccessControlled ) {
                if( ((AccessControlled)node).getACL().getPermission( user ).ordinal() < permission.ordinal() ) {
                    throw new NoAuthorizationException( user + " was not authorized to " + node );
                }
            }

            node = node.getParent();
        }

        logger.debug( "Was authorized" );
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate( String template ) {
        this.template = template;
    }

    public JsonObject getJsonField() {
        if(json == null) {
            try {
                logger.fatal( "THE CURRENT SESSION USER IS {}", user );
                json = JsonUtils.getJsonFromRequest( this );
            } catch( JsonException e ) {
                logger.debug( "No json field associated with this request, {}", e.getMessage());
                json = new JsonObject();
            }
        }

        // It should not be possible to have json == null
        if(user != null) {
            json.addProperty( SESSION_USER, user.getIdentifier() );
        }

        return json;
    }
    
    public JsonObject getJson() {
        if(json == null) {
            try {
                logger.fatal( "THE CURRENT SESSION USER IS {}", user );
                json = JsonUtils.getJsonRequest( this );
            } catch( IOException e ) {
                logger.debug( "No json field associated with this request, {}", e.getMessage());
                json = new JsonObject();
            }
        }

        // It should not be possible to have json == null
        if(user != null) {
            json.addProperty( SESSION_USER, user.getIdentifier() );
        }

        return json;
    }
    
    public String getView() {
        return view;
    }

    public void setView( String view ) {
        this.view = view;
    }

    public void setTheme( Theme theme ) {
        this.theme = theme;
    }

    public Theme getTheme() {
        return theme;
    }

    public VelocityContext getContext() {
        return context;
    }

    public void setContext( VelocityContext context ) {
        this.context = context;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser( User user ) {
        this.user = user;
    }

    @Override
    public <T> T getValue( String key ) {
        return (T) this.getParameter( key );
    }

    @Override
    public <T> T getValue( String key, T defaultValue ) {
        if( this.getParameter( key ) != null ) {
            return (T) this.getParameter( key );
        } else {
            return defaultValue;
        }
    }

    @Override
    public Integer getInteger( String key ) {
        return getInteger( key, null );
    }

    @Override
    public Integer getInteger( String key, Integer defaultValue ) {
        Integer i = defaultValue;
        if( this.getParameter( key ) != null ) {
            String val = this.getParameter( key );
            try {
                i = Integer.parseInt( val );
            } catch( NumberFormatException e ) {
                /* No op, default value is used */
            }
        }

        return i;
    }

    /**
     * @deprecated use {@link #getInteger} instead
     */
    public int getInt( String key, int defaultValue ) {
        if( this.getParameter( key ) != null ) {
            return Integer.parseInt( this.getParameter( key ) );
        } else {
            return defaultValue;
        }
    }

    public static boolean isMultipart( HttpServletRequest request) {
        if( request.getContentType() != null ) {
            return request.getContentType().startsWith( __MULTIPART );
        } else {
            return false;
        }
    }

    public boolean isUser( User user ) {
        if( this.user != null && user != null ) {
            return this.user.equals( user );
        } else {
            return false;
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocaleFromCookie( String name) {
        Cookie cookie = getCookie( name );
        String language;
        if( cookie != null ) {
            language = cookie.getValue();
        } else {
            language = "en_US";
        }

        logger.debug( "Setting language to {}.", language );

        this.locale = new Locale( language );
    }

    public void setStopWatch( StopWatch stopWatch ) {
        this.stopWatch = stopWatch;
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    private Map<String, Cookie> cookies;

    @Override
	public Cookie[] getCookies() {
		
		Cookie[] cookies = super.getCookies();
		
		if(cookies != null) {
			return cookies;
		} else {
			return new Cookie[0];
		}
	}

	public Cookie getCookie( String name ) {
        if( cookies == null ) {
            logger.debug( "Getting COOOOOOOOKEIESS:...." );
            cookies = new HashMap<String, Cookie>(  );
            for( Cookie cookie : getCookies() ) {
                logger.debug( "COOKIE {} = {}", cookie.getName(), cookie.getValue() );
                cookies.put( cookie.getName(), cookie );
            }
        }

        logger.debug( "COOKIES: " + cookies );

        return cookies.get( name );
    }
}
