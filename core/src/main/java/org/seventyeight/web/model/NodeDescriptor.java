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

    public enum Status {
        CREATED,
        UPDATED
    }

    @Override
    public Node getParent() {
        return Core.getInstance();
    }

    /*
    public T newInstance( String title ) throws ItemInstantiationException {
        return newInstance( title, this );
    }
    */

    //@Override
    public T newInstance( CoreRequest request, Node parent ) throws ItemInstantiationException {
        JsonObject json = null;
        try {
            json = JsonUtils.getJsonFromRequest( request );
        } catch( JsonException e ) {
            throw new ItemInstantiationException( "Unable to instantiate node, no json object provided" );
        }
        return newInstance( json, parent );
    }

    @Override
    public T newInstance( JsonObject json, Node parent ) throws ItemInstantiationException {
        String title = JsonUtils.get( json, "title", null );
        if(title == null) {
            throw new IllegalArgumentException( "Title must be provided" );
        }

        return newInstance( json, parent, title );
    }

    public T newInstance( CoreRequest request, Node parent, String title ) throws ItemInstantiationException {
        logger.debug( "New instance of " + getType() + " with title " + title + "(" + allowIdenticalNaming() + ")" );
        if( !allowIdenticalNaming() ) {
            if( titleExists( title, getType() ) ) {
                throw new ItemInstantiationException( "Multiple instances of " + getType() + " with the same title is not allowed." );
            }
        }

        T node = create( title, parent );

        node.getDocument().set( "type", getType() );
        node.getDocument().set( "title", title );
        //node.getDocument().set( "status", Status.CREATED );

        // TODO possibly have a system user account
        setOwner( request, node );

        /* Save */
        //MongoDBCollection.get( getCollectionName() ).save( node.getDocument() );

        return node;
    }

    protected void setOwner( CoreRequest request, T node ) {
        if(request.getUser() != null) {
            node.getDocument().set( "owner", request.getUser().getIdentifier() );
        }
    }

    @PostMethod
    public void doCreate( Request request, Response response ) throws ItemInstantiationException, IOException, ClassNotFoundException, JsonException {
        JsonObject json = JsonUtils.getJsonFromRequest( request );
        String title = JsonUtils.get( json, "title", null );
        if( title != null ) {
            logger.debug( "Creating " + title );
            T instance = newInstance(json, this);
            instance.updateConfiguration( json );
            instance.save();
            response.sendRedirect( instance.getUrl() );
        } else {
            throw new ItemInstantiationException( "No title provided" );
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
        MongoDocument doc = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).findOne( new MongoDBQuery().is( Core.NAME_FIELD, title ).is( "type", type ) );
        return !doc.isNull();
    }

    @Override
    protected T create( String title, Node parent ) throws ItemInstantiationException {
        T instance = super.create( title, parent );

        String id = Core.getInstance().getUniqueName( this );
        instance.getDocument().set( "_id", id );
        instance.getDocument().set( "class", clazz.getName() );
        Date now = new Date();
        instance.getDocument().set( "created", now );
        instance.getDocument().set( "updated", now );
        //document.set( "updated", now );
        instance.getDocument().set( "revision", 0 );
        instance.getDocument().set( "status", Status.CREATED.name() );

        return instance;
    }

    @Override
    public String getCollectionName() {
        return Core.NODES_COLLECTION_NAME;
    }

    public abstract String getType();

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
    public T get( String token ) throws NotFoundException {
        logger.debug( "Getting " + token );

        /* First, get by id */
        try {
            if( Integer.parseInt( token ) > 0 ) {
                return Core.getInstance().getNodeById( this, getType() + "-" + token );
            }
        } catch( Exception e ) {
            logger.debug( "the id " + token + " for " + getType() + " does not exist, " + e.getMessage() );
        }

        try {
            return Core.getInstance().getNodeById( this, token );
        } catch( Exception e ) {
            logger.debug( "the id " + token + " does not exist, " + e.getMessage() );
        }

        /* Get resource by title */
        T node = AbstractNode.getNodeByTitle( this, token, getType() );
        if( node != null ) {
            return node;
        } else {
            throw new NotFoundException( "The resource " + token + " was not found" );
        }
    }


    @Override
    public List<ExtensionGroup> getApplicableExtensions() {
        ArrayList<ExtensionGroup> groups = new ArrayList<ExtensionGroup>(  );
        groups.add( Core.getInstance().getExtensionGroup( Tags.class.getName() ) );
        groups.add( Core.getInstance().getExtensionGroup( Event.class.getName() ) );
        groups.add( Core.getInstance().getExtensionGroup( AbstractPortrait.class.getName() ) );
        groups.add( Core.getInstance().getExtensionGroup( NodeExtension.class.getName() ) );

        return groups;
    }
}
