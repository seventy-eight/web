package org.seventyeight.web.model;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.markup.HtmlGenerator;
import org.seventyeight.markup.SimpleParser;
import org.seventyeight.utils.DeleteMethod;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.utils.PutMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.authorization.Ownable;
import org.seventyeight.web.authorization.PublicACL;
import org.seventyeight.web.authorization.PublicACL.PublicACLDescriptor;
import org.seventyeight.web.extensions.MenuContributor;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.Menu.MenuItem;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.Response.RenderType;
import org.seventyeight.web.servlet.responses.ErrorResponse;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.ExtensionUtils;
import org.seventyeight.web.utilities.JsonException;
import org.seventyeight.web.utilities.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 *
 * This is the base implementation of a node in the tree.
 *
 * @author cwolfgang
 */
public abstract class AbstractNode<T extends AbstractNode<T>> extends PersistedNode implements TopLevelNode, Describable<T>, Ownable, DeletingParent {

    private static Logger logger = LogManager.getLogger( AbstractNode.class );

    public static final String MODERATORS = "moderators";
    public static final String VIEWERS = "viewers";

    public enum Visibility {
        VISIBLE,
        INVISIBLE
    }

    protected Node parent;

    public AbstractNode( Core core, Node parent, MongoDocument document ) {
        super( core, document );

        this.parent = parent;
    }

    public String getIdentifier() {
        return document.get( "_id" ).toString();
    }

    public String getUrl() {
        return "/resource/" + getIdentifier() + "/";
    }

    public String getFileUrl() {
        return getUrl() + "file";
    }

    public String getConfigUrl() {
        return getUrl() + "configure";
    }

    public void addRelation(Relation relation, TopLevelNode node) {
        addRelation( relation, node.getIdentifier() );
    }

    public void addRelation(Relation relation, String id) {
        MongoDocument d = document.get( "relations" );
        if(d == null || d.isNull()) {
            d = new MongoDocument();
            document.set( "relations", d );
        }
        d.addToList( relation.getName(), id );
    }


    /*
    public void applyExtension(Class<? extends Extension> extensionClass) {
        logger.debug( "Layoutable: " + Core.getInstance().getExtensions( extensionClass ) );

        for( Extension d : Core.getInstance().getExtensions( extensionClass ) ) {
            if( d.isApplicable( this ) &&
                Core.getInstance().getTemplateManager().templateForClassExists( theme, platform, d.getClass(), template ) ) {
                ds.add( d );
            }
        }
    }
    */

    /**
     * Return a {@link List} of {@link org.seventyeight.web.model.AbstractExtension.ExtensionDescriptor}'s that are applicable and have a certain template.
     */
    public List<Layoutable> getLayoutableHavingTemplate( Theme theme, Theme.Platform platform, String template ) {
        List<Layoutable> ds = new ArrayList<Layoutable>(  );

        logger.debug( "Layoutable: " + core.getExtensions( Layoutable.class ) );
        //logger.debug( "LIST: " +  );

        for( Layoutable d : core.getExtensions( Layoutable.class ) ) {
            if( d.isApplicable( this ) &&
                core.getTemplateManager().templateForClassExists( theme, core.getDefaultTheme(), platform, d.getClass(), template ) ) {
                ds.add( d );
            }
        }

        return ds;
    }

    public List<AbstractExtension.ExtensionDescriptor<?>> getLayoutableHavingTemplate2( Theme theme, Theme.Platform platform, String template ) {
        List<AbstractExtension.ExtensionDescriptor<?>> ds = new ArrayList<AbstractExtension.ExtensionDescriptor<?>>(  );

        logger.debug( "DS: " + core.getExtensionDescriptors( Layoutable.class ) );

        for( Descriptor d : core.getExtensionDescriptors( Layoutable.class ) ) {
            if( (( AbstractExtension.ExtensionDescriptor)d).isApplicable( this ) &&
                core.getTemplateManager().templateForClassExists( theme, core.getDefaultTheme(), platform, d.getClazz(), template ) ) {
                ds.add( (AbstractExtension.ExtensionDescriptor<?>) d );
            }
        }

        return ds;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    public boolean isOwner( User user ) {
        logger.debug( "USER: " + user );
        logger.debug( "OWENR: " + getOwnerId() );
        return user.getIdentifier().equals( getOwnerId() );
    }

    public String getOwnerId() {
        return document.get( "owner" );
    }

    @Override
    public Descriptor<T> getDescriptor() {
        return core.getDescriptor( getClass() );
    }

    /**
     * Save the document of the {@link Node}.
     */
    @Override
    public void save() {
        logger.debug( "Saving {}: {}", this, document );
        setUpdated( null );
        //MongoDBCollection.get( getDescriptor().getCollectionName() ).save( document );
        core.saveNode( this );
    }

    public void setOwner( User owner ) {
        logger.debug( "Setting owner to " + owner );
        document.set( "owner", owner.getIdentifier() );
    }

    public User getOwner() throws ItemInstantiationException, NotFoundException {
        return core.getNodeById( this, (String) document.get( "owner" ) );
    }

    public String getOwnerName() {
        return document.get( "owner" );
    }

    public String getType() {
        return document.get( "type", null );
    }

    public Date getCreated() {
        return getField( "created" );
    }

    public void update( User owner ) {
        update( owner, true );
    }

    public void update( User owner, boolean updateRevision ) {
        logger.debug( "Updating " + this );

        Date now = new Date();

        // Only set owner if not previously set and argument is not null.
        if( owner != null && document.get( "owner", null ) == null ) {
            logger.debug( "Owner was set to " + owner );
            setOwner( owner );
        }

        document.set( "updated", now );
        if(updateRevision) {
            document.set( "revision", getRevision() + 1 );
        }
    }

    public void setUpdated(Date date) {
    	logger.debug("UPDATED IS {}", document.get("updated"));
        if(date != null) {
            document.set( "updated", date );
        } else {
            document.set( "updated", new Date() );
        }
        logger.debug("UPDATED IS {}", document.get("updated"));
    }

    public void setUpdatedCall() {
        setUpdatedCall( null );
    }

    /**
     * Should be removed? 
     * Do not use .update()!?
     */
    public void setUpdatedCall(Date date) {
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() );
        MongoUpdate update;
        if(date == null) {
            update = new MongoUpdate().set( "updated", new Date() );
        } else {
            update = new MongoUpdate().set( "updated", date );
        }

        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).update( query, update );
    }

    public Date getUpdated() {
        return getField( "updated", null );
    }

    public void setVisibility(Visibility visibility) {
        this.document.set("visibility", visibility.name());
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
        document.set( "status", NodeDescriptor.Status.DELETED );
        document.set( "deleted", new Date() );
    }

    public Date getDeleted() {
        return getField( "deleted" );
    }

    public Date getUpdatedOrCreated() {
        if(getUpdated() != null) {
            return getUpdated();
        } else {
            return getCreated();
        }
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
    public WebResponse doConfigurationSubmit( Request request ) throws JsonException, ClassNotFoundException, SavingException, ItemInstantiationException, IOException {
        logger.debug( "Configuration submit" );

        try {
	        JsonObject json = request.getJson();
	
	        updateConfiguration( json );
	
	        // Update user + revision
	        update( request.getUser() );
	        
	        this.setVisibility(Visibility.VISIBLE);
	
	        // Lastly, save
	        save();

	        //response.getWriter().print("{\"id\":\"" + getIdentifier() + "\"}");
	        return WebResponse.makeJsonResponse().appendBody("{\"id\":\"" + getIdentifier() + "\"}");
        } catch(Exception e) {
        	//response.sendError(Response.SC_NOT_ACCEPTABLE, e.getMessage());
        	return new ErrorResponse(e).notAccepted();
        }
    }
    
    public void touch() {
    	logger.debug("Touching {}", this);
    	update(null, false);
    	this.setVisibility(Visibility.VISIBLE);
    	save();
    }

    public void updateConfiguration(JsonObject json) throws ClassNotFoundException, ItemInstantiationException {

        // Default fields
        String title = JsonUtils.get( json, "title", null );
        if(title != null) {
            setField( "title", title );
        } else {
            throw new IllegalArgumentException( "Title not provided" );
        }

        // Update the node's extensions
        updateExtensions(json);

        // Update this nodes fields
        updateNode(json);

        postUpdate();
    }
    
    @PostMethod
    public WebResponse doChown(Request request) throws IOException {
    	JsonObject json = request.getJson();
    	if(json == null) {
    		//response.setStatus(Response.SC_NOT_ACCEPTABLE);
    		//response.getWriter().println("No json provided");
    		return new WebResponse().setHeader("No Json provided").notAccepted();
    	}
    	
    	if(!json.has("newOwner")) {
    		//response.setStatus(Response.SC_NOT_ACCEPTABLE);
    		//response.getWriter().println("No new owner provided");
    		return new WebResponse().setHeader("No new owner provided").notAccepted();
    	}
    	
    	String ownerId = json.get("newOwner").getAsString();
		try {
			User owner = request.getCore().getNodeById(this, ownerId);
			//response.setStatus(Response.SC_OK);
			//response.getWriter().println("{}");
			this.setOwner(owner);
			return WebResponse.makeEmptyJsonResponse();
		} catch (Exception e) {
    		//response.setStatus(Response.SC_NOT_ACCEPTABLE);
    		//response.getWriter().println("No valid owner provided, " + ownerId);
			return new WebResponse().setHeader("No valid owner provided").notAccepted();
    	}
    }
    
    public static final String fullAclField = EXTENSIONS + "." + Descriptor.getJsonId(ACL.class.getName());
    public static final String fullAclReadField = fullAclField + ".read";
    protected static final String aclField = Descriptor.getJsonId(ACL.class.getName());
    
    protected void setACL(MongoDocument acl) {
    	logger.debug("Setting acl, {} for {}", acl, this);
    	//document.set(aclField, acl);
    	((MongoDocument)document.get(EXTENSIONS)).set(aclField, acl);
    }
    
    @DeleteMethod
    public WebResponse doIndex(Request request) throws Exception {
    	//response.setRenderType(RenderType.NONE);
    	
    	if(parent instanceof DeletingParent) {
    		logger.debug("Deleting {}", this);
    		((DeletingParent) parent).deleteChild(this);
    		return new WebResponse();
    	} else {
    		//response.sendError(Response.SC_METHOD_NOT_ALLOWED);
    		return new WebResponse().methodNotAllowed();
    	}
    }

    public void postUpdate() {
        document.set( "status", NodeDescriptor.Status.UPDATED.name() );
    }

    public NodeDescriptor.Status getStatus() {
        return NodeDescriptor.Status.valueOf( document.get( "status", "UPDATED" ) );
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


    public void updateField( String collection, MongoUpdate update ) {
        MongoDBCollection.get( collection ).update( new MongoDBQuery().is( "_id", getObjectId() ), update );
    }

    /**
     * Get the first {@link Node} having the title.
     */
    public static <N extends Node> N getNodeByTitle( Core core, Node parent, String title ) {
        return getNodeByTitle( core, parent, title, null );
    }

    /**
     * Get the first {@link Node} having the title of the given type.
     * Type can be null and will then be disregarded.
     */
    public static <N extends Node> N getNodeByTitle( Core core, Node parent, String title, String type ) {
        MongoDBQuery q = new MongoDBQuery().is( "title", title );
        if( type != null && !type.isEmpty() ) {
            q.is( "type", type );
        }
        MongoDocument docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).findOne( q );
        logger.debug( "DOC IS: " + docs );

        if( docs != null ) {
            try {
                return (N) core.getNode( parent, docs );
            } catch( ItemInstantiationException e ) {
                logger.warn( e.getMessage() );
                return null;
            }
        } else {
            logger.debug( "The node " + title + " was not found" );
            return null;
        }
    }

    public static <N extends Node> List<N> getNodesByTitle( Core core, Node parent, String title, String type ) {
        MongoDBQuery q = new MongoDBQuery().is( "title", title );
        if( type != null && !type.isEmpty() ) {
            q.is( "type", type );
        }
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( q );
        logger.debug( "Docs are: " + docs );

        List<N> nodes = new ArrayList<N>( docs.size() );

        if( docs != null ) {
            for( MongoDocument doc : docs ) {
                try {
                    nodes.add( (N)core.getNode( parent, doc ) );
                } catch( ItemInstantiationException e ) {
                    /* TODO should this fail the entire method???? */
                    logger.error( e.getMessage() );
                }
            }
        } else {
            logger.debug( "Any node with title " + title + " was not found" );
        }

        return nodes;
    }

    public static <N extends Node> N getNodeById( Core core, Node parent, String id ) {
        MongoDocument doc = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).getDocumentById( id );
        if( doc != null ) {
            try {
                return (N) core.getNode( parent, doc );
            } catch( ItemInstantiationException e ) {
                logger.warn( e.getMessage() );
                return null;
            }
        } else {
            logger.debug( "The node with id " + id + " was not found" );
            return null;
        }
    }

    /* Texts */
    public static final String TEXTS_COLLECTION = "texts";
    private static SimpleParser textParser = new SimpleParser( new HtmlGenerator() );

    public enum TextType {
        markUp,
        html
    }

    public String getText( String field, String language, String type ) {
        logger.debug( "Getting text " + field + ", " + language + " for " + getIdentifier() );

        MongoDocument doc = document.getr2("texts", field, "translations", language);
        if( doc == null || doc.isNull() ) {
            throw new IllegalStateException( language + " for " + field + " not found" );
        }

        return doc.get( type, "" );
    }

    /**
     * Set an internationalized text.
     * @param type The type of text, eg description
     * @param text The text itself
     * @param language
     */
    public void setText( String field, String language, String text ) {
        logger.debug( "Setting text " + field + " for " + getIdentifier() );
        
        MongoDocument textDoc = document.getr("texts", field, "translations", language);

        // Set the version of the parser and generator
        String version = textParser.getVersion() + ":" + textParser.getGeneratorVersion();
        textDoc.set( "version", version );

        // Set the texts
        StringBuilder output = textParser.parse( text );
        textDoc.set( TextType.markUp.name(), text );
        textDoc.set( TextType.html.name(), output.toString() );
    }

    @GetMethod
    public WebResponse doGetView(Request request) throws TemplateException, IOException {
        //response.setRenderType( Response.RenderType.NONE );

        String template = request.getValue( "view", "simpleIndex" );

        //response.getWriter().write( core.getTemplateManager().getRenderer( request ).renderObject( this, template + ".vm" ) );
        return new WebResponse().appendBody(core.getTemplateManager().getRenderer( request ).renderObject( this, template + ".vm" ));
    }


    /*
    public Menu getContributingViews( Request request ) {
        Menu menu = new Menu();

        //partitions.add( new ContributingView( "Main", "view", this ) );

        // Get extensions adding to the list
        for( MenuContributor pc : Core.getInstance().getExtensions( MenuContributor.class ) ) {
            pc.addContributingMenu( menu );
        }

        return menu;
    }
    */

    public Menu getMenu() {
        logger.debug( "Getting menu for {}", this );
        Menu menu = new Menu();

        for( MenuContributor pc : core.getExtensions( MenuContributor.class ) ) {
            logger.debug( "Menu contributor {}", pc );
            if(pc.isApplicable(this)) {
            	pc.addContributingMenu( this, menu );
            }
            logger.debug( "ENDING::::.:.::::.:" );
        }

        return menu;
    }

    @Override
    public String getDisplayName() {
        return getTitle();
    }

    @Override
    public String toString() {
        //return getDisplayName();
        return getIdentifier();
    }

    @Override
    public String getMainTemplate() {
        return "org/seventyeight/web/main.vm";
    }

    /**
     * Base implementation just delegates the deletion operation to its own parent.
     * @param node
     */
    @Override
    public void deleteChild( Node node ) {
        logger.debug( "{} deleting {}", this, node );
        if(parent != null && parent instanceof DeletingParent) {
            ( (DeletingParent) parent ).deleteChild( node );
        } else {
            logger.debug( "No deleting operation for {}", this );
        }
    }
}
