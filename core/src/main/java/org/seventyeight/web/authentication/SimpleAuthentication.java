package org.seventyeight.web.authentication;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.utils.Utils;
import org.seventyeight.web.Core;
import org.seventyeight.web.authentication.SessionManager.Login;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import javax.servlet.http.Cookie;

import java.security.NoSuchAlgorithmException;
import java.util.Date;


public class SimpleAuthentication implements Authentication {

	private static Logger logger = LogManager.getLogger( SimpleAuthentication.class );

    private Core core;

    public SimpleAuthentication( Core core ) {
        this.core = core;
    }

    public void authenticate( Request request, Response response ) throws AuthenticationException {

		String hash = null;

        request.getStopWatch().start( "Finding cookies" );

        for( Cookie cookie : request.getCookies() ) {
			logger.debug( "Cookie: " + cookie.getName() + "=" + cookie.getValue() );
			if( cookie.getName().equals( SESSION_NAME ) ) {
				hash = cookie.getValue();
				break;
			}
		}

        Session session = null;

        request.getStopWatch().stop( "Finding cookies" );
        request.getStopWatch().start( "Creating session" );
        
        // If session hash is not found in cookies, try the url parameters
        if(hash == null) {
        	String requestSession = request.getValue(SESSION_NAME, null);
        	if(requestSession != null) {
        		logger.debug("Request session found");
        		hash = requestSession;
        	}
        }

        // If no cookie session was found, create a new.
        if( hash == null ) {
        	Login login = SessionManager.getCredentials(request);
            if(login != null) {
                logger.debug( "{}", login );
                //try {
                    session = login( login.getUsername(), login.getPassword());
                //} catch( Exception e ) {
                //	logger.log(Level.ERROR, "FAILED TO LOGIN", e);
                    /* Never mind, just move along... */
                //}
            }
        } else {
			logger.debug( "Found hash: " + hash );
			session = core.getSessionManager().getSession( hash );
		}

        request.getStopWatch().stop( "Creating session" );
        request.getStopWatch().start( "Getting user" );

        // Registering the session user
        if( session != null ) {
            //User user = User.getUserByUsername( Core.getInstance(), session.getUser() );
            User user;
            try {
                user = core.getNodeById( core.getRoot(), session.getUserId() );
            } catch( Exception e ) {
                throw new AuthenticationException( e );
            }
            //User user = null;
            if( user != null ) {
                logger.debug( "Session user is " + user );
                request.setAuthenticated( true );
                request.setUser( user );
                return;
            } else {
                logger.debug( "NOT VALID USER" );
            }
        }
	}

    public Session login( String username, String password ) throws AuthenticationException {
    	logger.debug("LOGGING IN {}", username);
        if( !username.isEmpty() ) {
            User user = User.getUserByUsername( core, null, username );

            if( user == null ) {
                throw new AuthenticationException( "The user " + username + " does not exist!" );
            }

            String hashed = "";
            try {
                hashed = Utils.md5( password );
            } catch( NoSuchAlgorithmException e ) {
                throw new AuthenticationException( e );
            }
            if( user.getPassword() != null && !hashed.equals( user.getPassword() ) ) {
                logger.debug( "Wrong password" );
                throw new AuthenticationException( "Password is incorrect" );
            }

            Session session = null;
            try {
                session = core.getSessionManager().createSession( user, new Date(), 10 * 60 *60 );
            } catch( ItemInstantiationException e ) {
                throw new AuthenticationException( e );
            }
            return session;
        } else {
            throw new AuthenticationException( "" );
        }
    }

}
