package org.seventyeight.web.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.DeleteMethod;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.authorization.AccessControlled;
import org.seventyeight.web.extensions.*;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.nodes.Conversation;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.RedirectResponse;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.DocumentFinder;
import org.seventyeight.web.utilities.JsonException;
import org.seventyeight.web.utilities.JsonUtils;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author cwolfgang
 */
public abstract class Resource<T extends Resource<T>> extends AbstractNode<T> implements CreatableNode, Portraitable, Parent, AccessControlled {

    public static final String RESOURCES_COLLECTION_NAME = "resources";

    private static Logger logger = LogManager.getLogger( Resource.class );

    public Resource( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    /**
     * Determines whether a {@link Resource} exists or not
     */
    public static boolean exists( String id ) {
        MongoDBQuery query = new MongoDBQuery().getId( id );
        return MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).count( query ) > 0;
    }

    @PostMethod
    public WebResponse doSetPortrait( Request request ) throws IOException, JsonException {
        logger.debug( "Setting portrait" );
        //response.setRenderType( Response.RenderType.NONE );

        JsonObject json = request.getJson();
        //List<JsonObject> objs = JsonUtils.getJsonObjects( json );
        if( json != null ) {
            setPortrait( json );
        }

        /* Redirect */
        //response.sendRedirect( "" );
        return new RedirectResponse("");
    }

    public void setPortrait(JsonObject json) {
        try {
            AbstractPortrait.AbstractPortraitDescriptor descriptor = (AbstractPortrait.AbstractPortraitDescriptor) core.getDescriptor( json.get( "class" ).getAsString() );
            json.addProperty( "title", "portrait" );
            AbstractPortrait abstractPortrait = descriptor.newInstance( core, this, json);
            abstractPortrait.updateExtensions( json );
            //ExtensionUtils.retrieveExtensions( request, json, abstractPortrait );
            //abstractPortrait.save( request, json );
            document.set( "portrait", abstractPortrait.getDocument() );
            save();
        } catch( Exception e ) {
            logger.warn( "failed", e );
        }
    }

    @Override
    public NodeDescriptor<T> getDescriptor() {
        return core.getDescriptor( getClass() );
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public Node getChild( String name ) {
        return null;
    }

    @Override
    public String getPortrait() {
        //logger.debug( "Getting portrait for {}", this );

        MongoDocument portrait = getExtension( AbstractPortrait.class );
//        logger.debug( "PORTTRAIT: {}", portrait );

        if( portrait != null && !portrait.isNull() && portrait.get( "class", null ) != null ) {
            try {
                AbstractPortrait up = core.getNode( this, portrait );
                return up.getUrl();
            } catch( ItemInstantiationException e ) {
                logger.warn( "Unable to get the portrait from " + portrait );
                return "/theme/type/" + getType() + ".png";
            }
        } else {
            return "/theme/type/" + getType() + ".png";
        }
    }

    @Override
    public boolean hasPortrait() {
        /*
        MongoDocument portrait = getExtension( AbstractPortrait.class );

        return ( portrait != null && !portrait.isNull() && portrait.get( "class", null ) != null );
        */

        return true;
    }

    public List<Action.ActionDescriptor<?>> getActions() {

        List<Action.ActionDescriptor<?>> ds = new ArrayList<Action.ActionDescriptor<?>>(  );

        for( Descriptor d : core.getExtensionDescriptors( Action.class ) ) {
            if( (( AbstractExtension.ExtensionDescriptor)d).isApplicable( this ) ) {
                ds.add( (Action.ActionDescriptor<?>) d );
            }
        }

        return ds;
    }

    protected void setMandatoryFields( User owner ) {
        logger.debug( "The mandatory fields, " + owner );
        setOwner( owner );
    }

    @GetMethod
    public WebResponse doBadge( Request request ) throws TemplateException, IOException {
        //response.getWriter().write( core.getTemplateManager().getRenderer( request ).renderObject( this, "badge.vm" ) );
    	return new WebResponse().appendBody(core.getTemplateManager().getRenderer( request ).renderObject( this, "badge.vm" ));
    }

    public Set<String> getConfiguredExtensionTypes() {
        MongoDocument doc = document.getSubDocument( Core.Relations.EXTENSIONS, null );
        if( doc != null && !doc.isNull()) {
            return doc.getKeys();
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public List<AbstractExtension.ExtensionDescriptor> getExtensionDescriptors() {
        return core.getExtensionDescriptors( ResourceExtension.class );
    }

    /*
    public List<AbstractExtension> getConfiguredExtensions() throws ItemInstantiationException {
        logger.debug( "Getting configured extensions for " + this );
        List<AbstractExtension> extensions = new ArrayList<AbstractExtension>(  );
        for( String type : getConfiguredExtensionTypes() ) {
            logger.debug( "Type: " + type );

            MongoDocument doc = document.getr( Core.Relations.EXTENSIONS, type );
            logger.debug( "Extension document is " + doc );
            for( String e : doc.getKeys() ) {
                logger.debug( "Extension: " + e );

                MongoDocument ext = doc.getSubDocument( e, null );
                if( ext != null && !ext.isNull() ) {
                    AbstractExtension instance = Core.getInstance().getNode( this, ext );
                    extensions.add( instance );
                }
            }
        }

        return extensions;
    }
    */

    /*
    @Override
    public List<ContributingView> getContributingViews( Locale locale ) {
        List<ContributingView> partitions = new ArrayList<ContributingView>();
        partitions.add( new ContributingView( "Main", "view", this ) );

        // Get extensions adding to the list
        for( ViewContributor pc : Core.getInstance().getExtensions( ViewContributor.class ) ) {
            pc.addContributingViews( partitions );
        }

        return partitions;
    }

    @Override
    public List<ContributingView> getAdministrativePartitions( Request request ) {
        List<ContributingView> partitions = new ArrayList<ContributingView>();

        try {
            request.checkPermissions( this, ACL.Permission.ADMIN );
            partitions.add( new ContributingView( Core.getInstance().getMessages().getString( "Configure", Resource.class, request.getLocale() ), "configure", this ) );
        } catch( NoAuthorizationException e ) {
            logger.debug( e.getMessage() );
        }

        return partitions;
    }
    */

    /*
    @Override
    public ContributingView getActiveView( Request request ) {
        //String current = request.getValue( "view", "" );
        String current = request.getView();
        if( current.length() > 0 ) {
            return new ContributingView( current, current, this );
        } else {
            return new ContributingView( "Main", "view", this );
            //return null;
        }
    }
    */
        
    @GetMethod
    public WebResponse doGetLatestComment(Request request) throws ItemInstantiationException, NotFoundException, TemplateException, IOException {
        //response.setRenderType( Response.RenderType.NONE );

        //String username = request.getValue( "username", null );
        int number = request.getInteger( "number", 1 );

        DocumentFinder finder = new DocumentFinder( core, this, request, 1, 0 );
        finder.getQuery().is("owner", request.getUser().getIdentifier()).is( "type", "comment" );
        finder.getSort().set( "created", 1 );

        List<MongoDocument> d = finder.findNext();

        if(!d.isEmpty()) {
            //PrintWriter writer = response.getWriter();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            //writer.write( gson.toJson( d.get( 0 ) ) );
            return WebResponse.makeJsonResponse().appendBody(gson.toJson( d.get( 0 ) ));
        } else {
            //response.getWriter().write( "{}" );
        	return WebResponse.makeEmptyJsonResponse();
        }

    }

    /*
    public List<Authorizable> getAuthorizable(String p) {
        ACL.Permission permission = ACL.Permission.valueOf(p);
        return getACL().getAuthorized( permission );
    }
    */

    @GetMethod
    public WebResponse doGetAuthorizable(Request request) throws IOException {
        //response.setRenderType( Response.RenderType.NONE );

        String term = request.getValue( "term", "" );
        int limit = request.getValue( "limit", 10 );

        logger.debug( "Getting authorizable for {} with term {}", this, term );

        if( term.length() > 1 ) {
            MongoDBQuery q = new MongoDBQuery().regex( "title", "(?i)" + term + ".*" ).in( "type", "user", "group" );

            //response.getWriter().print( MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( q, 0, limit ) );
            return WebResponse.makeJsonResponse().appendBody(MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( q, 0, limit ));
        } else {
        	return WebResponse.makeEmptyJsonResponse();
        }
    }

    @Override
    public ACL getACL() {
        MongoDocument doc = document.get( fullAclField );

        //
        if( doc == null || doc.isNull() ) {
            return ACL.ALL_ACCESS;
        } else {
            try {
                return core.getNode( this, doc );
            } catch( ItemInstantiationException e ) {
                throw new IllegalStateException( "Unable to instantiate ACL for " + this, e );
            }
        }
    }

    @PostMethod
    public WebResponse doDelete(Request request) throws IOException {
        //response.setRenderType( Response.RenderType.NONE );

        logger.debug( "Deleting {}", this );

        if(parent != null && parent instanceof DeletingParent) {
            ( (DeletingParent) parent ).deleteChild( this );
            return new WebResponse();
        } else {
            //response.sendError( HttpServletResponse.SC_BAD_REQUEST, "Whoops" );
        	return new WebResponse().badRequest().setHeader("Failed to delete");
        }
    }
}
