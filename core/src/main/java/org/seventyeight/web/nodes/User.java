package org.seventyeight.web.nodes;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.utils.API;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.utils.Utils;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.Authorizable;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.JsonResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author cwolfgang
 */
public class User extends Resource<User> implements Authorizable {

    private static Logger logger = LogManager.getLogger( User.class );

    public static final String USERS = "users";

    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";
    public static final String USERNAME = "username";

    public enum Visibility {
        VISIBLE,
        HIDDEN;
    }

    public User( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    /**
     * Return the document corresponding to the given username,
     */
    public static MongoDocument getUserDocumentByUsername( String username ) {
        MongoDocument doc = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).findOne( new MongoDBQuery().is( "username", username ) );

        if( doc != null && !doc.isNull() ) {
            return doc;
        } else {
            logger.debug( "The username " + username + " was not found" );
            return null;
        }
    }

    @Override
    public void updateNode( JsonObject jsonData ) {

        if(jsonData == null) {
            throw new IllegalArgumentException( "Json object was null" );
        }

        /* Set username */
        String username = jsonData.getAsJsonPrimitive( "username" ).getAsString();
        //String username = request.getValue( "username", null );
        if( username == null || username.isEmpty() ) {
            throw new IllegalArgumentException( "The username must be set" );
        }

        /* Set email */
        //String email = request.getValue( "email", null );
        String email = jsonData.getAsJsonPrimitive( "email" ).getAsString();
        if( email == null || email.isEmpty() ) {
            throw new IllegalArgumentException( "The email must be set" );
        }

        /* Password */
        //String password1 = request.getValue( "password", null );
        //String password2 = request.getValue( "password_again", null );
        String password1 = jsonData.getAsJsonPrimitive( "password" ).getAsString();
        String password2 = jsonData.getAsJsonPrimitive( "password_again" ).getAsString();
        if( password2 != null && !password2.isEmpty() ) {
            if( ( password1 == null || password1.isEmpty() ) || ( password2 == null || password2.isEmpty() ) ) {
                throw new IllegalArgumentException( "The password cannot be empty" );
            }

            if( !password1.equals( password2 ) ) {
                throw new IllegalArgumentException( "Passwords does not match" );
            }
            String hashed = "";
            try {
                hashed = Utils.md5( password1 );
            } catch( NoSuchAlgorithmException e ) {
                throw new IllegalArgumentException( "Unable to hash password" );
            }

            document.set( "password", hashed );
        }

        document.set( "username", username );
        document.set( "email", email );

    }

    public List<Group> getGroups() {
        logger.debug( "Listing groups for " + this );

        List<MongoDocument> docs = document.getMappedList( Group.GROUPS, Core.NODES_COLLECTION_NAME );

        List<Group> groups = new ArrayList<Group>( docs.size() );

        for( MongoDocument d : docs ) {
            groups.add( new Group( core, this, d ) );
        }

        return groups;
    }

    public void setSeen() {
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() );
        MongoUpdate update = new MongoUpdate().set( "seen", new Date() );
        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).update( query, update );
    }

    @Override
    public void setOwner( User owner ) {
        /* No op for users */
    }

    public void setUsername( String username ) {
        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).update( new MongoDBQuery().is( "_id", getObjectId() ), new MongoUpdate().set( "username", username ) );
    }

    public String getUsername() {
        return document.get( "username" );
    }

    public String getPassword() {
        return document.get( "password" );
    }

    public void setVisibility( Visibility visibility ) {
        document.set( "visible", visibility.equals( Visibility.VISIBLE ) );
    }

    public void setVisible( boolean visible ) {
        document.set( "visible", visible );
    }

    public boolean isVisible() {
        return document.get( "visible", true );
    }

    public String getEmail() {
        return document.get( EMAIL, "" );
    }

    @Override
    protected void setMandatoryFields( User owner ) {
        super.setMandatoryFields( owner );

        document.set( "owner", owner.getIdentifier() );
    }

    @Override
    public String getDisplayName() {
        return getTitle();
    }

    @Override
    public String toString() {
        return "User[" + getUsername() + "]" ;
    }

    /*
    @Override
    public void setPortrait( Request request, JsonObject json ) {
        try {
            AbstractPortrait.AbstractPortraitDescriptor descriptor = (AbstractPortrait.AbstractPortraitDescriptor) Core.getInstance().getDescriptor( json.get( "class" ).getAsString() );
            AbstractPortrait abstractPortrait = descriptor.newInstance(request, this, "portrait");
            abstractPortrait.update( request );
            //ExtensionUtils.retrieveExtensions( request, json, abstractPortrait );
            //abstractPortrait.save( request, json );
            document.set( "portrait", abstractPortrait.getDocument() );
            save();
        } catch( Exception e ) {
            logger.warn( "failed", e );
        }
    }
    */

    @Override
    public String getMainTemplate() {
        return "org/seventyeight/web/main.vm";
    }

    /*
    @Override
    public String getPortrait() {
        logger.debug( "Getting portrait for {}", this );

        MongoDocument portrait = document.getSubDocument( "portrait", null );

        if( portrait != null && !portrait.isNull() ) {
            try {
                AbstractPortrait up = Core.getInstance().getNode( this, portrait );
                return up.getUrl();
            } catch( ItemInstantiationException e ) {
                logger.warn( "Unable to get the portrait from " + portrait );
                return "/theme/unknown-person.png";
            }
        } else {
            return "/theme/unknown-person.png";
        }
    }
    */

    /**
     * Get a list of the registered portrait descriptors
     * @return
     */
    /*
    public List<? extends AbstractPortrait.AbstractPortraitDescriptor> getUserPortraitDescriptors() {
        List<AbstractPortrait.AbstractPortraitDescriptor> descriptors = Core.getInstance().getExtensionDescriptors( AbstractPortrait.class );

        return descriptors;
    }
    */

    /*
    public AbstractPortrait getPortraitExtension() throws ItemInstantiationException {
        MongoDocument portrait = document.getSubDocument( "portrait", null );

        if( portrait != null ) {
            AbstractPortrait up = Core.getInstance().getNode( this, portrait );
            return up;
        } else {
            return null;
        }
    }
    */


    @Override
    public boolean isMember( User user ) {
        return user.equals( this );
    }


    /**
     * Get a {@link User} by Username. Returns null if not found.
     */
    public static User getUserByUsername( Core core, Node parent, String username ) {
        if(username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        //MongoDocument userDoc = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).findOne( new MongoDBQuery().is( "username", username ) );
        MongoDocument d = core.getId( new MongoDBQuery().is( "username", username ) );

        if(d == null || d.isNull()) {
            return null;
        }

        MongoDocument userDoc = core.getDocumentCache().get( d.getIdentifier() );

        logger.debug( "USER DOOOOOOOC: {}", userDoc );
        if( userDoc != null) {
            try {
                return core.getNode( parent, userDoc );
            } catch( ItemInstantiationException e ) {
                logger.error( e );
                return null;
            }
        } else {
            logger.debug( "The user " + username + " was not found" );
            return null;
        }
    }


    @Override
    public User getOwner() throws ItemInstantiationException {
        logger.debug( "Return myself, {}", this );
        return this;
    }

    public static class UserDescriptor extends ResourceDescriptor<User> {

        public String testString;

        public UserDescriptor( Node parent ) {
            super( parent );
        }

        @Override
        public String getDisplayName() {
            return "User";
        }

        @Override
        public String getType() {
            return "user";
        }
        
        @Override
		public String getUrlName() {
			return "users";
		}

		@GetMethod
        public WebResponse doGetUsers(Request request) throws IOException {
            String term = request.getValue( "term", "" );
            boolean exact = request.getBoolean("exact", false);

            if( term.length() > 1 ) {
                MongoDBQuery query = new MongoDBQuery().is( "type", "user" ).regex( "title", "(?i)" + term + (exact ? "" : ".*") );

                //PrintWriter writer = response.getWriter();
                //writer.print( MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ) );
                return new JsonResponse(MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ));
            } else {
                //response.getWriter().write( "{}" );
            	return JsonResponse.empty();
            }
        }
		
		@GetMethod
		@API
		public void doFind(Request request) {
			JsonObject json = request.getJson();
			json.get("");
		}

        /*
        @Override
        public User newInstance(CoreRequest request) throws ItemInstantiationException {
            User u = super.newInstance(request);
            u.getDocument().set( "username", title );
            //u.getDocument().set( "_id", title );

            return u;
        }
        */

        @Override
        public void save( Request request, Response response ) {
            logger.debug( "Saving " + this );

            testString = "The millis: " + System.currentTimeMillis();
        }

        @Override
        public boolean allowIdenticalNaming() {
            return false;
        }

        @Override
        protected void setOwner( User node, String ownerId ) {
            //super.setOwner( node, ownerId );
        }
    }
}
