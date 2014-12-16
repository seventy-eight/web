package org.seventyeight.web.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.API;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.*;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.ErrorResponse;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.JsonException;
import org.seventyeight.web.utilities.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author cwolfgang
 */
public abstract class NodeDescriptor<T extends AbstractNode<T>> extends Descriptor<T> implements Node, Getable<T> {

    private static Logger logger = LogManager.getLogger( NodeDescriptor.class );

    protected Node parent;

    public enum Status {
        CREATED,
        UPDATED,
        DELETED
    }

    protected NodeDescriptor( Node parent ) {
        super();
        this.parent = parent;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    public T newInstance( CallContext request, Node parent ) throws ItemInstantiationException {
        Core core = request.getCore();
        return newInstance( core, parent, request.getJson(), request.getUser().getIdentifier() );
    }
    
    @Override
    public final T newInstance( Core core, Node parent, JsonObject json, String ownerId ) throws ItemInstantiationException {
        String title = JsonUtils.get( json, "title", null );
        if(title == null) {
            throw new IllegalArgumentException( "Title must be provided" );
        }

        logger.debug( "New instance of " + getType() + " with title " + title + "(" + allowIdenticalNaming() + ")" );
        if( !allowIdenticalNaming() ) {
            if( titleExists( title, getType() ) ) {
                throw new ItemInstantiationException( "Multiple instances of " + getType() + " with the same title is not allowed." );
            }
        }

        T node = create( core, parent );

        node.getDocument().set( "type", getType() );
        node.getDocument().set( "title", title );
        //node.getDocument().set( "status", Status.CREATED );

        // Advanced options
    	if(json.has("advanced")) {
    		JsonObject advanced = json.get("advanced").getAsJsonObject();
    		
    		// Set the date with a timestamp
    		if(advanced.has("timestamp")) {
    			Date d = new Date(advanced.get("timestamp").getAsLong() * 1000);
    			node.getDocument().set("created", d);
    			node.setUpdated(d);
    		}
    	}
    	
    	setOwner( node, ownerId );
        
        onNewInstance(node, core, parent, json);
        
        return node;
    }

    protected void setOwner( T node, String ownerId ) {
    	logger.debug("Setting owner to {}", ownerId);
        node.getDocument().set( "owner", ownerId != null ? ownerId : "" );
    }

    /*
    protected void setOwner( T node, JsonObject json ) {
        logger.debug( "OWNER JSON::::::::::::::::::::::::::::: {}", json );
        if(json.has( Request.SESSION_USER )) {
            node.getDocument().set( "owner", json.get( CoreRequest.SESSION_USER ).getAsString() );
        }
    }
    */

    /**
     * Create a node and return the identifier.
     */
    @PostMethod
    @API
    public WebResponse doCreate( Request request ) throws ItemInstantiationException, IOException, ClassNotFoundException, JsonException {
    	JsonObject json = request.getJson();
        Core core = request.getCore();
        String title = JsonUtils.get( json, "title", null );
        if(title == null) {
        	//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print("No title provided");
        	return new WebResponse().setHeader("No title provided").notAccepted();
        }
        
        
        logger.debug( "Creating " + title );
        try {
        	T instance = newInstance( request, this);
        	instance.updateConfiguration( json );
            instance.save();
            logger.debug("Finally done!!!!");
        	//response.setStatus(Response.SC_CREATED);
            //response.getWriter().print("{\"identifier\":\"" + instance.getIdentifier() + "\"}");
            return WebResponse.makeJsonResponse().appendBody("{\"identifier\":\"" + instance.getIdentifier() + "\"}");
        } catch(Exception e) {
        	e.printStackTrace();
        	logger.log(Level.WARN, "Unable to create {}, {}", getType(), e);
        	//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print(e.getMessage());
        	return new ErrorResponse(e).setHeader(e.getMessage()).notAccepted();
        }
    }

    /*
    @Override
    public List<Class> getExtensionClasses() {
        List<Class> extensions = new ArrayList<Class>( 1 );
        extensions.add( ResourceExtension.class );
        return extensions;
    }
    */

    private boolean titleExists( String title, String type ) {
        MongoDocument doc = MongoDBCollection.get( getCollectionName() ).findOne( new MongoDBQuery().is( Core.NAME_FIELD, title ).is( "type", type ) );
        return !doc.isNull();
    }

    @Override
    protected T create( Core core, Node parent ) throws ItemInstantiationException {
        T instance = super.create( core, parent );

        String id = core.getUniqueName( this );
        instance.getDocument().set( "_id", id );
        //instance.getDocument().set( "class", clazz.getName() );
        Date now = new Date();
        instance.getDocument().set( "created", now );
        instance.getDocument().set( "updated", now );
        //document.set( "updated", now );
        instance.getDocument().set( "revision", 0 );
        instance.getDocument().set( "status", Status.CREATED.name() );
        instance.getDocument().set( "visibility", AbstractNode.Visibility.VISIBLE.name() );
        
        for(DefaultInstanceFiller filler : core.getExtensions(DefaultInstanceFiller.class)) {
        	logger.debug("Filling with {}, applicable={}", filler, filler.isApplicable(this));
        	if(filler.isApplicable(this)) {
        		filler.fill(core, instance.getDocument());
        	}
        }

        return instance;
    }

    @Override
    public String getCollectionName() {
        return Core.NODES_COLLECTION_NAME;
    }

    public abstract String getType();
    
    public abstract String getUrlName();

    /**
     * Determine whether to allow identical names or not.<br />
     * Default is true.
     */
    public boolean allowIdenticalNaming() {
        return true;
    }

    @Override
    public String getMainTemplate() {
        return "org/seventyeight/web/main.vm";
    }

    @Override
    public T get( Core core, String token ) throws NotFoundException {
        logger.debug( "Getting " + token );

        /* First, get by id */
        try {
            if( Integer.parseInt( token ) > 0 ) {
                return core.getNodeById( this, getType() + "-" + token );
            }
        } catch( Exception e ) {
            logger.debug( "the id " + token + " for " + getType() + " does not exist, " + e.getMessage() );
        }

        try {
            return core.getNodeById( this, token );
        } catch( Exception e ) {
            logger.debug( "the id " + token + " does not exist, " + e.getMessage() );
        }

        /* Get resource by title */
        T node = AbstractNode.getNodeByTitle( core, this, token, getType() );
        if( node != null ) {
            return node;
        } else {
            throw new NotFoundException( "The resource " + token + " was not found" );
        }
    }

    /*
    @Override
    public List<ExtensionGroup> getApplicableExtensions( Core core ) {
        ArrayList<ExtensionGroup> groups = new ArrayList<ExtensionGroup>(  );
        groups.add( core.getExtensionGroup( Tags.class.getName() ) );
        groups.add( core.getExtensionGroup( Event.class.getName() ) );
        groups.add( core.getExtensionGroup( AbstractPortrait.class.getName() ) );
        //groups.add( core.getExtensionGroup( NodeExtension.class.getName() ) );
        groups.add( core.getExtensionGroup( Action.class.getName() ) );

        return groups;
    }
    */
}
