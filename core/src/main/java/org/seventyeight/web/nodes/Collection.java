package org.seventyeight.web.nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.extensions.MenuContributor;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.SearchHelper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Collection extends Resource<Collection> implements Getable<Node> {

    private static Logger logger = LogManager.getLogger( Collection.class );

    public static final String SORT_FIELD = "sort";
    public static final String ELEMENTS_FIELD = "elements";

    /*
    public class CollectionItem implements Node {

        private Node node;
        private int next = -1;
        private int previous = -1;

        public CollectionItem(Node node, int next, int previous) {
            this.node = node;
            this.next = next;
            this.previous = previous;
        }

        @Override
        public Node getParent() {
            return this;
        }

        @Override
        public String getDisplayName() {
            return node.toString();
        }

        @Override
        public String getMainTemplate() {
            return null;
        }

        public boolean hasPrevious() {
            return previous > -1;
        }

        public String getPreviousUrl() {
            return Collection.this.getUrl() + "get/" + previous;
        }

        public boolean hasNext() {
            return next < Collection.this.size();
        }

        public String getNextUrl() {
            return Collection.this.getUrl() + "get/" + next;
        }

        public Node getNode() {
            return node;
        }
    }
    */

    private int id = -1;

    public Collection( Node parent, MongoDocument document ) {
        super( parent, document );
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
      /* Implementation is a no op */
    }

    public void getResources(int offset, int number) throws NotFoundException, ItemInstantiationException {
        List<MongoDocument> docs = document.getList( "elements" );

        for(MongoDocument d : docs) {
            Node n = Core.getInstance().getNodeById( this, d.getIdentifier() );
            //d.set( "badge", Core.getInstance().getTemplateManager().getRenderer( request ).renderObject( n, "badge.vm" ) );
            d.removeField( "extensions" );
        }

    }

    public int length() {
        List<MongoDocument> docs = document.getList( ELEMENTS_FIELD );
        if( docs == null ) {
            return 0;
        } else {
            return docs.size();
        }
    }

    @PostMethod
    public void doAdd( Request request, Response response ) {
        String id = request.getValue( "id", null );
        if( id != null ) {
            if( Resource.exists( id ) ) {
                addCall( id );
                response.setStatus( HttpServletResponse.SC_OK );
            } else {
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }
        } else {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
        }
    }

    @PostMethod
    public void doSearch( Request request, Response response ) throws IOException, NotFoundException, ItemInstantiationException, TemplateException {
        SearchHelper sh = new SearchHelper( this, request, response );
        sh.search();

        for( MongoDocument d : sh.getDocuments() ) {
            d.set( "incollection", containsId( d.getIdentifier() ) );
        }

        sh.render();
    }

    /*
    @Override
    public List<ContributingView> getContributingViews( Locale locale ) {
        List<ContributingView> parts = super.getContributingViews( locale );

        parts.add( new ContributingView( "viewSkills", Core.getInstance().getMessages().getString( "view.skills", Collection.class, locale ), this ) );

        logger.debug( parts );
        return parts;
    }
    */

    public boolean containsId( String id ) {
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() ).is( ELEMENTS_FIELD + "._id", id );
        return MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).count( query ) > 0;
    }

    public void doFetch( Request request, Response response ) throws NotFoundException, ItemInstantiationException, TemplateException, IOException {
        int offset = request.getInteger( "offset", 0 );
        int number = request.getInteger( "number", 10 );
        response.setRenderType( Response.RenderType.NONE );

        logger.debug( "Fetching " + number + " from " + offset + " from " + this );

        List<MongoDocument> docs = document.getList( ELEMENTS_FIELD );
        List<MongoDocument> result = new ArrayList<MongoDocument>( number );

        int stop = docs.size() > offset + number ? offset + number : docs.size();

        int counter = offset;
        if( docs.size() > offset ) {
            //for( MongoDocument d : docs ) {
            for( int i = offset ; i < stop ; i++ ) {
                MongoDocument d = docs.get( i );
                Node n = Core.getInstance().getNodeById( this, d.getIdentifier() );
                d.set( "avatar", Core.getInstance().getTemplateManager().getRenderer( request ).renderObject( n, "avatar.vm" ) );
                d.set("counter", counter);
                result.add( d );

                counter++;
            }
        }

        PrintWriter writer = response.getWriter();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        writer.write( gson.toJson( result ) );
    }

    public Node getItem(int itemNumber) throws NotFoundException, ItemInstantiationException {
        List<MongoDocument> docs = document.getList( ELEMENTS_FIELD );

        if(docs.size() >= itemNumber ) {
            return Core.getInstance().getNodeById( this, docs.get( itemNumber ).getIdentifier() );
        } else {
            throw new IllegalStateException( "Item number " + itemNumber + " not found" );
        }
    }

    public int size() {
        return document.getList( ELEMENTS_FIELD ).size();
    }

    /*
    public void doItem(Request request, Response response) {

    }
    */

    public void addCall( Resource<?> resource ) {
        addCall( resource.getIdentifier() );
    }

    public void addCall( String id ) {
        logger.debug( "Adding " + id + " to " + this );
        //document.addToList( "elements", new MongoDocument().set( "id", resource.getIdentifier() ).set( "sort", sortValue ) );
        int sortValue = length();
        logger.debug( "Next element is at " + sortValue );
        MongoDocument field = new MongoDocument().set( "_id", id ).set( SORT_FIELD, sortValue );
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() );
        //MongoDocument sort = new MongoDocument().set( SORT_FIELD, 1 );
        //MongoUpdate update = new MongoUpdate().push( "elements", field, sort );
        MongoUpdate update = new MongoUpdate().push( "elements", field );

        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).update( query, update );

        setUpdatedCall();
    }

    public void add( Resource<?> resource ) {
        int next = length();
        document.addToList( ELEMENTS_FIELD, new MongoDocument().set( "id", resource.getIdentifier() ).set( SORT_FIELD, next ) );
    }

    public void add( List<Resource<?>> resources ) {
        logger.debug( "Adding " + resources.size() + " resources to " + this );

        int c = length();
        for( Resource<?> r : resources ) {
            document.addToList( ELEMENTS_FIELD, new MongoDocument().set( "id", r.getIdentifier() ).set( SORT_FIELD, c ) );

            c++;
        }
    }

    /**
     * Update the entire collection.
     */
    public void update( List<Resource<?>> resources ) {
        logger.debug( "Updating " + this + " with " + resources.size() + " resources" );

        removeAll();
        add( resources );
    }

    /**
     * Remove all elements from the {@link Collection}.
     */
    public void removeAllCall() {
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() );
        MongoUpdate update = new MongoUpdate().pull( ELEMENTS_FIELD, new MongoDocument() );

        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).update( query, update );
    }

    public void removeAll() {
        document.setList( ELEMENTS_FIELD );
    }

    public void updateSortValue( Resource<?> resource, int sortValue ) {
        logger.debug( "Updating " + resource + " in " + this + " with " + sortValue );
        MongoDocument getID = new MongoDocument().set( "id", resource.getIdentifier() );
        MongoDBQuery query = new MongoDBQuery().elemMatch( "elements", getID ).getId( this.getIdentifier() );
        MongoUpdate update = new MongoUpdate().set( "elements.$.sort", sortValue );

        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).update( query, update );
    }

    public void remove( Resource<?> resource ) {
        logger.debug( "Removing " + resource + " frin " + this );
        MongoDocument getID = new MongoDocument().set( "id", resource.getIdentifier() );
        MongoDBQuery query = new MongoDBQuery().elemMatch( "elements", getID ).getId( this.getIdentifier() );

        MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).remove( query );
    }

    /*
    @Override
    public List<ContributingView> getAdministrativePartitions( Request request ) {
        List<ContributingView> parts = super.getAdministrativePartitions( request );

        try {
            request.checkPermissions( this, ACL.Permission.ADMIN );
            parts.add( new ContributingView( Core.getInstance().getMessages().getString( "List", Collection.class, request.getLocale() ), "listConfig", this ) );
        } catch( NoAuthorizationException e ) {
            logger.debug( e.getMessage() );
        }

        return parts;
    }
    */

    public int getIndex() {
        return id;
    }

    public String getEditListUrl() {
        return getUrl() + "listConfig";
    }

    @Override
    public Node get( String token ) throws NotFoundException {
        try {
            id = Integer.parseInt( token );
            return getItem( id );
        } catch( Exception e ) {
            throw new NotFoundException( "the id " + token + " does not exist, " + e.getMessage() );
        }
    }

    public static class CollectionDescriptor extends NodeDescriptor<Collection> implements MenuContributor<AbstractNode<Collection>> {

        @Override
        public String getType() {
            return "collection";
        }

        @Override
        public String getDisplayName() {
            return "Collection";
        }

        @Override
        public void addContributingMenu( AbstractNode<Collection> node, Menu menu ) {
            if(node instanceof Collection) {
                menu.addItem( "Collection", new Menu.MenuItem("List edit", ((Collection)node).getEditListUrl(), ACL.Permission.ADMIN) );
            }
        }
    }
}
