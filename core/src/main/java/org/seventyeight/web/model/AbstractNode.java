package org.seventyeight.web.model;

import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.utilities.JsonException;
import org.seventyeight.web.utilities.JsonUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 *
 * This is the base implementation of a node in the tree.
 *
 * @author cwolfgang
 */
public abstract class AbstractNode<T extends AbstractNode<T>> extends PersistedObject implements Node, Authorizer, Describable<T> {

    private static Logger logger = Logger.getLogger( AbstractNode.class );

    public static final String MODERATORS = "moderators";
    public static final String VIEWERS = "viewers";

    protected Node parent;

    public AbstractNode( Node parent, MongoDocument document ) {
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

        update( request.getUser() );

        /*
        if( saver.getId() != null ) {
            logger.debug( "Setting id to " + saver.getId() );
            document.set( "_id", saver.getId() );
            //document.set( "_id", getUniqueIdentifier() );
        }
        */

        /* Persist */
        MongoDBCollection.get( getDescriptor().getCollectionName() ).save( document );
    }

    public Saver getSaver( CoreRequest request ) {
        return new Saver( this, request );
    }

    public static class Saver {
        protected AbstractNode modelObject;
        protected CoreRequest request;

        public Saver( AbstractNode modelObject, CoreRequest request ) {
            this.modelObject = modelObject;
            this.request = request;
        }

        public PersistedObject getModelObject() {
            return modelObject;
        }

        public void save() throws SavingException {
        }

        protected String set( String key ) throws SavingException {
            return set( key, key, true );
        }

        protected String set( String formkey, String dbkey ) throws SavingException {
            return set( formkey, dbkey, true );
        }

        protected String set( String formkey, String dbkey, boolean mandatory ) throws SavingException {
            String v = request.getValue( formkey, null );
            if( mandatory && ( v == null || v.isEmpty() ) ) {
                throw new SavingException( "The " + formkey + " must be set" );
            }
            modelObject.document.set( dbkey, v );

            return v;
        }
    }

    public String getIdentifier() {
        return document.get( "_id" ).toString();
    }

    public String getUrl() {
        //return "/get/" + getIdentifier();
        return "/" + getDescriptor().getType() + "/" + getTitle() + "/";
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

        Describable e = d.newInstance( "" );

        /* Remove data!? */
        if( d.doRemoveDataItemOnConfigure() ) {
            logger.debug( "This should remove the data attached to this modelObject" );
        }

        return e;

    }

    @Override
    public Node getParent() {
        return parent;
    }

    public boolean isOwner( User user ) {
        return true;
    }

    /**
     * Fast track saving the node
     */
    public void save() {
        logger.debug( "BEFORE SAVING: " + document );
        MongoDBCollection.get( getDescriptor().getCollectionName() ).save( document );
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
                a = (Authoritative) getSubDocument( d );
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
                a = (Authoritative) getSubDocument( d );
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

    public User getOwner() throws ItemInstantiationException {
        return User.getUserByUsername( this, (String) document.get( "owner" ) );
    }

    public String getOwnerName() {
        return document.get( "owner" );
    }

    public Date getCreatedAsDate() {
        //return new Date( (Long)getField( "created" ) );
        return DatatypeConverter.parseDateTime( (String) getField( "created" ) ).getTime();
    }

    public String getType() {
        return document.get( "type", "unknown" );
    }

    public Date getCreated() {
        return getField( "created" );
    }

    public void update( User owner ) {
        logger.debug( "Updating " + this );

        Date now = new Date();

        Date date = document.get( "created", null );
        if( date == null ) {
            logger.debug( "Was created" );
            document.set( "created", now );
        }

        if( document.get( "owner", null ) == null ) {
            logger.debug( "Owner was set to " + owner );
            setOwner( owner );
        }

        document.set( "updated", now );
    }

    public Date getUpdatedAsDate() {
        String d = getField( "updated", null );
        if( d != null ) {
            return DatatypeConverter.parseDateTime( d ).getTime();
        } else {
            return null;
        }
    }

    public void setTitle( String title ) {
        document.set( "title", title );
    }

    public String getTitle() {
        String title = getField( "title", null );
        if( title != null ) {
            return title;
        } else {
            throw new IllegalStateException( "A node must have a title" );
        }
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
        Object o = document.get( "_id" );
        logger.info( "OBJECT: " + o );
        return ObjectId.massageToObjectId( o.toString() );
    }

    public MongoDocument getUniqueIdentifier() {
        MongoDocument d = new MongoDocument().set( "type", ((NodeDescriptor)getDescriptor()).getType() ).set( "name", getTitle() );
        return d;
    }

    @PostMethod
    public void doConfigurationSubmit( Request request, Response response ) throws JsonException, ClassNotFoundException, SavingException, ItemInstantiationException, IOException {
        logger.debug( "Configuration submit" );

        JsonObject jsonData = JsonUtils.getJsonFromRequest( request );
        save( request, jsonData );
        response.sendRedirect( getUrl() );
    }

    @Override
    public boolean equals( Object obj ) {
        if( obj == this ) {
            return true;
        }

        if( getClass().isInstance( obj ) ) {
            AbstractNode n = (AbstractNode) obj;
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
    public NodeDescriptor<T> getDescriptor() {
        return Core.getInstance().getDescriptor( getClass() );
    }

    public void updateField( String collection, MongoUpdate update ) {
        MongoDBCollection.get( collection ).update( new MongoDBQuery().is( "_id", getObjectId() ), update );
    }

    public static Node getNodeByTitle( Node parent, String title ) {
        return getNodeByTitle( parent, title, null );
    }

    public static Node getNodeByTitle( Node parent, String title, String type ) {
        MongoDBQuery q = new MongoDBQuery().is( "title", title );
        if( type != null && !type.isEmpty() ) {
            q.is( "type", type );
        }
        MongoDocument docs = MongoDBCollection.get( Core.NODE_COLLECTION_NAME ).findOne( q );
        logger.debug( "DOC IS: " + docs );

        if( docs != null ) {
            try {
                return (Node) Core.getInstance().getItem( parent, docs );
            } catch( ItemInstantiationException e ) {
                logger.warn( e.getMessage() );
                return null;
            }
        } else {
            logger.debug( "The user " + title + " was not found" );
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return getTitle();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
