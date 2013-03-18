package org.seventyeight.web.model;

import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.utils.Date;
import org.seventyeight.web.Core;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.utilities.JsonUtils;

import java.util.List;

/**
 *
 * This is the base implementation of a node in the tree.
 *
 * @author cwolfgang
 */
public abstract class AbstractNodeItem extends PersistedObject implements NodeItem, Authorizer, Describable {

    private static Logger logger = Logger.getLogger( AbstractNodeItem.class );

    public static final String MODERATORS = "moderators";
    public static final String VIEWERS = "viewers";

    protected NodeItem parent;

    public AbstractNodeItem( NodeItem parent, MongoDocument document ) {
        super( document );

        this.parent = parent;
    }


    public void save( CoreRequest request, JsonObject jsonData ) throws ClassNotFoundException, ItemInstantiationException, SavingException {
        logger.debug( "Begin saving" );

        Saver saver = getSaver( request );

        saver.save();

        if( jsonData != null ) {
            logger.debug( "Handling extensions" );
            handleJsonConfigurations( request, jsonData );
        }

        update();

        if( saver.getId() != null ) {
            logger.debug( "Setting id to " + saver.getId() );
            document.set( "_oid", saver.getId() );
        }

        /* Persist */
        MongoDBCollection.get( Core.getInstance().getDescriptor( getClass() ).getCollectionName() ).save( document );
    }

    public Saver getSaver( CoreRequest request ) {
        return new Saver( this, request );
    }

    public static class Saver {
        protected PersistedObject modelObject;
        protected CoreRequest request;

        public Saver( PersistedObject modelObject, CoreRequest request ) {
            this.modelObject = modelObject;
            this.request = request;
        }

        public PersistedObject getModelObject() {
            return modelObject;
        }

        public void save() throws SavingException {

        }

        public Object getId() {
            return null;
        }
    }

    public Object getIdentifier() {
        return document.get( "_id" );
    }

    public String getUrl() {
        return "/get/" + getIdentifier();
    }

    public void handleJsonConfigurations( CoreRequest request, JsonObject jsonData ) throws ClassNotFoundException, ItemInstantiationException {
        logger.debug( "Handling extension class Json data" );

        List<JsonObject> extensionsObjects = JsonUtils.getJsonObjects( jsonData, JsonUtils.JsonType.extensionClass );
        logger.debug( "I got " + extensionsObjects.size() + " extension types" );

        for( JsonObject obj : extensionsObjects ) {
            handleJsonExtensionClass( request, obj );
        }
    }

    public void handleJsonExtensionClass( CoreRequest request, JsonObject extensionConfiguration ) throws ClassNotFoundException, ItemInstantiationException {
        String extensionClassName = extensionConfiguration.get( JsonUtils.__JSON_CLASS_NAME ).getAsString();
        logger.debug( "Extension class name is " + extensionClassName );

        /* Get Json configuration objects */
        List<JsonObject> configs = JsonUtils.getJsonObjects( extensionConfiguration );
        logger.debug( "I got " + configs.size() + " configurations" );

        document.setList( EXTENSIONS );
        for( JsonObject c : configs ) {
            Describable d = handleJsonConfiguration( request, c );
            document.addToList( EXTENSIONS, d.getDocument() );
        }
    }


    public Describable handleJsonConfiguration( CoreRequest request, JsonObject jsonData ) throws ItemInstantiationException, ClassNotFoundException {
        /* Get Json configuration object class name */
        String cls = jsonData.get( JsonUtils.__JSON_CLASS_NAME ).getAsString();
        logger.debug( "Configuration class is " + cls );

        Class<?> clazz = Class.forName( cls );
        Descriptor<?> d = Core.getInstance().getDescriptor( clazz );
        logger.debug( "Descriptor is " + d );

        Describable e = d.newInstance();

        /* Remove data!? */
        if( d.doRemoveDataItemOnConfigure() ) {
            logger.debug( "This should remove the data attached to this modelObject" );
        }

        return e;

    }

    @Override
    public NodeItem getParent() {
        return parent;
    }

    public boolean isOwner( User user ) {
        return true;
    }

    /**
     * Fast track saving the node
     */
    public void save() {
        MongoDBCollection.get( Core.getInstance().getDescriptor( getClass() ).getCollectionName() ).save( document );
    }

    @Override
    public Authorization getAuthorization( User user ) throws AuthorizationException {

        /* First check ownerships */
        if( isOwner( user ) ) {
            return Authorization.MODERATE;
        }


        List<MongoDocument> docs = document.getList( MODERATORS );
        for( MongoDocument d : docs ) {
            Authoritative a = null;
            try {
                a = (Authoritative) getItem( d );
            } catch( ItemInstantiationException e ) {
                throw new AuthorizationException( e );
            }
            if( a.isAuthoritative( user ) ) {
                return Authorization.MODERATE;
            }
        }

        List<MongoDocument> viewers = document.getList( VIEWERS );
        for( MongoDocument d : docs ) {
            Authoritative a = null;
            try {
                a = (Authoritative) getItem( d );
            } catch( ItemInstantiationException e ) {
                throw new AuthorizationException( e );
            }
            if( a.isAuthoritative( user ) ) {
                return Authorization.VIEW;
            }
        }

        logger.debug( "None of the above" );
        return Authorization.NONE;
    }

    public void setOwner( User owner ) {
        document.set( "owner", owner.getIdentifier() );
    }

    public Date getCreatedAsDate() {
        return new Date( (Long)getField( "created" ) );
    }

    public Long getCreated() {
        return getField( "created" );
    }

    public void update() {
        document.set( "updated", new Date().getTime() );
    }

    public Date getUpdatedAsDate() {
        Long l = getField( "updated", null );
        if( l != null ) {
            return new Date( l );
        } else {
            return null;
        }
    }

    public void setTitle( String title ) {
        document.set( "title", title );
    }

    public Long getUpdated() {
        return getField( "updated", null );
    }


    public void delete() {
        document.set( "deleted", new Date().getTime() );
    }


    public Date getDeletedAsDate() {
        Long l = getField( "deleted", null );
        if( l != null ) {
            return new Date( l );
        } else {
            return null;
        }
    }

    public Long getDeleted() {
        return getField( "deleted" );
    }


    public Long getViews() {
        return getField( "views", 0l );
    }

    public void incrementViews() {
        document.set( "views", getViews() + 1 );
    }

    public int getRevision() {
        return getField( "revision", 1 );
    }

    public ObjectId getObjectId() {
        return document.get( "_id" );
    }

    @Override
    public boolean equals( Object obj ) {
        if( obj == this ) {
            return true;
        }

        if( getClass().isInstance( obj ) ) {
            AbstractNodeItem n = (AbstractNodeItem) obj;
            if( getIdentifier().equals( n.getIdentifier() ) ) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public Descriptor<?> getDescriptor() {
        return Core.getInstance().getDescriptor( getClass() );
    }

    public void updateField( String collection, MongoUpdate update ) {
        MongoDBCollection.get( collection ).update( update, new MongoDBQuery().is( "_id", getObjectId() ) );
    }
}
