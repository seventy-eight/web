package org.seventyeight.web.model;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.utilities.JsonException;
import org.seventyeight.web.utilities.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    /*
    public T newInstance( String title ) throws ItemInstantiationException {
        return newInstance( title, this );
    }
    */

    //@Override
    public T newInstance( CoreRequest request, Node parent ) throws ItemInstantiationException {
        Core core = request.getCore();
        return newInstance( core, request.getJsonField(), parent );
    }

    @Override
    public T newInstance( Core core, JsonObject json, Node parent ) throws ItemInstantiationException {
        String title = JsonUtils.get( json, "title", null );
        if(title == null) {
            throw new IllegalArgumentException( "Title must be provided" );
        }

        return newInstance( core, json, parent, title );
    }

    public T newInstance( Core core, JsonObject json, Node parent, String title ) throws ItemInstantiationException {
        logger.debug( "OWNER JSON::::::::::::::::::::::::::::: {}", json );
        String ownerId = null;
        if(json.has( Request.SESSION_USER )) {
            ownerId = json.get( CoreRequest.SESSION_USER ).getAsString();
        }

        return newInstance( core, ownerId, parent, title );
    }

    public T newInstance( Core core, String ownerId, Node parent, String title ) throws ItemInstantiationException {
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

        // TODO possibly have a system user account
        setOwner( node, ownerId );

        /* Save */
        //MongoDBCollection.get( getCollectionName() ).save( node.getDocument() );

        return node;
    }

    protected void setOwner( T node, String ownerId ) {
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
    public void doCreate( Request request, Response response ) throws ItemInstantiationException, IOException, ClassNotFoundException, JsonException {
        //JsonObject json = request.getJsonField();
    	//JsonObject json = request.getJson();
    	JsonObject json = request.getJson();
        Core core = request.getCore();
        String title = JsonUtils.get( json, "title", null );
        if( title != null ) {
        	response.sendError(Response.SC_NOT_ACCEPTABLE, "No title provided");
        	return;
        }
        
        logger.debug( "Creating " + title );
        T instance = newInstance( core, json, this);
        try {
        	instance.updateConfiguration( json );
        } catch(Exception e) {
        	response.sendError(Response.SC_NOT_ACCEPTABLE, e.getMessage());
        	return;        	
        }
        instance.save();
        //response.sendRedirect( instance.getUrl() );
        response.getWriter().print("{\"id\":\"" + instance.getIdentifier() + "\"}");
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
