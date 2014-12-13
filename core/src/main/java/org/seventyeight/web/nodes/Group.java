package org.seventyeight.web.nodes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.Authorizable;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.JsonResponse;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.JsonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Group extends Resource<Group> implements Authorizable {

    private static Logger logger = LogManager.getLogger( Group.class );

    public static final String GROUPS = "groups";
    public static final String GROUP = "group";

    public Group( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    @Override
    public void updateNode(JsonObject jsonData) {
    	if(jsonData != null) {
	        JsonElement usersElement = jsonData.get( "users" );
	        if(usersElement == null || usersElement.isJsonNull()) {
	
	        } else {
	            JsonArray usersArray = usersElement.getAsJsonArray();
	            //List<String> artists = new ArrayList<String>( artistsArray.size() );
	            for( JsonElement k : usersArray) {
	                try {
	                    User user = core.getNodeById( this, k.getAsString() );
	                    addMember( user );
	                } catch( Exception e ) {
	                    logger.log( Level.WARN, "Unable to add " + k, e );
	                }
	            }
	
	            //document.set( "members", artists );
	        }
    	}
    }

    protected String getGroupType() {
        return GROUPS;
    }
    
    @PostMethod
    public WebResponse doIndex(Request request) throws IOException {
    	String userId = request.getValue("user", null);
    	
    	if(userId == null) {
    		//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print("No user provided");
        	return new WebResponse().setCode(406).setHeader("No user provided");
    	}
    	
		try {
			User user = core.getNodeById( this, userId );
			addMember( user );

	    	//response.setStatus(Response.SC_ACCEPTED);
	    	//response.getWriter().print("{\"id\":\"" + getIdentifier() + "\"}");
	    	return new WebResponse().appendBody("{\"id\":\"" + getIdentifier() + "\"}").setCode(202);
		} catch (Exception e) {
			logger.error(e);
    		//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print(e.getMessage());
			return new WebResponse().setCode(406).setHeader(e.getMessage());
		}
		
    }
    
    @PostMethod
    public WebResponse doAdd(Request request) throws IOException {
    	JsonObject json = request.getJson();
        Core core = request.getCore();
        
        if(json == null) {
        	//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print("No data provided");
        	return new WebResponse().setCode(406).setHeader("No data provided");
        }
        
        if(!json.has("users")) {
        	//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print("No users provided");
        	return new WebResponse().setCode(406).setHeader("No users provided");
        }
        
        JsonArray missing = new JsonArray();
        
        JsonArray ja = json.get("users").getAsJsonArray();
        for(JsonElement je : ja) {
			try {
				User user = core.getNodeById( this, je.getAsString() );
				addMember( user );
			} catch (Exception e) {
				JsonObject m = new JsonObject();
				m.addProperty("user", je.getAsShort());
				m.addProperty("error", e.getMessage());
				missing.add(m);
			}
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("id", getIdentifier());
        result.add("missing", missing);
        
    	//response.setStatus(Response.SC_ACCEPTED);
        //response.getWriter().print(result.toString());
        return new WebResponse().setCode(202).appendBody(result.toString());
    }

    public List<AbstractNode<?>> getActivities(Request request) throws NotFoundException, ItemInstantiationException {
        logger.debug( "Getting activities for {}", this );
        MongoDocument sort = new MongoDocument().set( "updated", -1 );
        MongoDBQuery query = new MongoDBQuery().notExists( "parent" ).is( "ACL.read", getIdentifier() );

        logger.debug( "Query is {}", query );

        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10, sort );
        List<AbstractNode<?>> nodes = new ArrayList<AbstractNode<?>>( docs.size() );

        for( MongoDocument d : docs ) {
            AbstractNode<?> n = core.getNodeById( this, d.getIdentifier() );
            //d.set( "badge", Core.getInstance().getTemplateManager().getRenderer( request ).renderObject( n, "badge.vm" ) );
            nodes.add( n );

            //nodes.add( new Activity( d ) );
        }

        return nodes;
    }
    
    public static List<String> getGroupIds(User user) {
    	List<String> gs = user.getDocument().getObjectList2("groups");
    	return gs;
    }

    /**
     * Add a {@link User} to this {@link Group}.
     */
    public void addMember( User user ) {
        if( !isMember( user ) ) {
            logger.debug( "Adding " + user + " to " + this );
            //document.addToList( getGroupType(), user.getIdentifier() );
            user.getDocument().addToList( getGroupType(), getIdentifier() );
            user.save();
        } else {
            logger.debug( user + " is already a member of " + this );
        }
    }

    /**
     * Determine whether or not the {@link User} is member of this {@link Group}.
     */
    public boolean isMember( User user ) {
        List<Group> groups = user.getGroups();
        logger.debug( "User is in {}", groups );

        for( Group g : groups ) {
            if( this.equals( g ) ) {
                return true;
            }
        }

        return false;
    }

    public List<User> getMembers() {
        logger.debug( "Getting members for " + this );

        MongoDBQuery q = new MongoDBQuery().is( GROUPS, getIdentifier() );
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( q );

        logger.debug( "DOCS ARE: " + docs );

        List<User> users = new ArrayList<User>( docs.size() );

        for( MongoDocument d : docs ) {
            users.add( new User( core, this, d ) );
        }

        return users;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    @Override
    public String getMainTemplate() {
        return "org/seventyeight/web/main.vm";
    }

    public static class GroupDescriptor extends ResourceDescriptor<Group> {

        public GroupDescriptor( Node parent ) {
            super( parent );
        }
        
        @Override
		public String getUrlName() {
			return GROUPS;
		}

        @Override
        public String getDisplayName() {
            return GROUP;
        }

        @Override
        public String getType() {
            return GROUP;
        }
        
        @GetMethod
        public WebResponse doGetGroups(Request request) throws IOException {
        	String term = request.getValue( "term", "" );
            boolean exact = request.getBoolean("exact", false);

            if( term.length() > 1 ) {
                MongoDBQuery query = new MongoDBQuery().is( "type", "group" ).regex( "title", "(?i)" + term + (exact ? "" : ".*") );

                //PrintWriter writer = response.getWriter();
                //writer.print( MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ) );
                return new WebResponse().appendBody(MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ));
            } else {
                //response.getWriter().write( "{}" );
            	return JsonResponse.empty();
            }
        }

    }
}
