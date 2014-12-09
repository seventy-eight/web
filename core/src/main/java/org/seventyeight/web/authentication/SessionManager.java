package org.seventyeight.web.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.Utils;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;


public class SessionManager implements Node {
	
	private static Logger logger = LogManager.getLogger( SessionManager.class );
	
	public static class Login {
		private String username;
		private String password;
		
		public String getUsername() {
			return username;
		}
		
		public String getPassword() {
			return password;
		}

		@Override
		public String toString() {
			return "login[" + username + "/" + password + "]";
		}
	}

    public static final String SESSIONS = "sessions";
    public static final String SESSION = "session";
    public static final String SESSIONS_COLLECTION_NAME = "sessions";

    private Core core;

	public SessionManager( Core core ) {
        //db.createIndex( INDEX_SESSIONS, IndexType.UNIQUE, IndexValueType.STRING );
		//logger.debug( "Initializing session index" );
        this.core = core;
    }
	
	/**
	 * Get login based on the given request.
	 */
	public static Login getCredentials(Request request) {
		Login login = null;
        if(request.getValue( Authentication.NAME_KEY, null ) != null && request.getValue( Authentication.PASS_KEY, null ) != null) {
        	login = new Login();
            login.username = request.getValue( Authentication.NAME_KEY );
            login.password = request.getValue( Authentication.PASS_KEY );
        }

        if(login == null) {
			//JsonObject json = request.getJson();
        	JsonObject json = new JsonObject();
			logger.debug("THIS JSON EFLEMENTNT IS {}", json);
			if(json.has("credentials")) {
				JsonObject credentials = json.get("credentials").getAsJsonObject();
				if(credentials.has("username") & credentials.has("password")) {
					login = new Login();
					login.username = credentials.get("username").getAsString();
					login.password = credentials.get("password").getAsString();
				}
			}
        }
		
		return login;
	}

	public Session createSession( User user, Date date, int ttl ) throws ItemInstantiationException, AuthenticationException {
		logger.debug( "Creating session for " + user + ", " + ttl );

		String hash = "";
		try {
			hash = Utils.md5( user.getUsername() + date.getTime() );
		} catch( NoSuchAlgorithmException e ) {
			logger.warn( "Unable to create session: " + e.getMessage() );
			throw new AuthenticationException( e );
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime( date );
		calendar.add( Calendar.SECOND, ttl );

		//Session session = createSessionNode( hash, calendar.getTime() );

        Session.SessionsDescriptor descriptor = core.getDescriptor( Session.class );
        Session session = descriptor.newInstance( core, this );
        session.setTitle( "Session for " + user.getDisplayName() );
        session.getDocument().set( "created", new Date() );
        session.setEndDate( calendar.getTime() );
        //session.setCreated();
        session.setTimeToLive( ttl );
        session.getDocument().set( "_id", hash );
        session.setUserId( user.getIdentifier() );

		return session;
	}
	
	public Session getSession( String hash ) {
		logger.debug( "Getting session for " + hash );

        //MongoDBQuery query = new MongoDBQuery().getId( hash );
        //MongoDocument doc = MongoDBCollection.get( SESSIONS_COLLECTION_NAME ).findOne( query );
        MongoDocument doc = core.getSessionCache().get( hash );

        logger.debug( "Session doc: " + doc );

        Session actual = null;
        if(doc != null) {
            Session session = new Session( core, core.getRoot(), doc );
            logger.debug( "Comparing " + session.getEndingAsDate() + " with " + new Date() + "(" + session.getCreated() + ")" );
            if( session.getEndingAsDate().after( new Date() ) ) {
                logger.debug( "A valid session found" );
                actual = session;
            } else {
                logger.debug( "Session has expired" );
                /* TODO remove session??? */
            }
        }

		return actual;
	}
	
	public void removeSession( String hash ) {
        logger.debug( "[Removing sessions] " + hash );
        MongoDBCollection.get( SESSIONS_COLLECTION_NAME ).remove( new MongoDBQuery().getId( hash ) );
	}

    @Override
    public Node getParent() {
        return core.getRoot();
    }

    @Override
    public String getDisplayName() {
        return "Session manager";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }
}
