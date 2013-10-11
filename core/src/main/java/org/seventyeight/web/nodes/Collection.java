package org.seventyeight.web.nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Collection extends Resource<Collection> {

    private static Logger logger = Logger.getLogger( Collection.class );

    public static final String SORT_FIELD = "sort";
    public static final String ELEMENTS_FIELD = "elements";

    public Collection( Node parent, MongoDocument document ) {
        super( parent, document );
    }

    public void get( int offset, int number ) {
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() );

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

    public void doFetch( Request request, Response response ) throws NotFoundException, ItemInstantiationException, TemplateException, IOException {
        int offset = request.getInteger( "offset", 0 );
        int number = request.getInteger( "number", 10 );

        logger.debug( "Fetching " + number + " from " + offset + " from " + this );

        int stop = offset + number;

        List<MongoDocument> docs = document.getList( ELEMENTS_FIELD );
        List<MongoDocument> result = new ArrayList<MongoDocument>( number );

        if( docs.size() > offset ) {
            //for( MongoDocument d : docs ) {
            for( int i = offset ; i < stop ; i++ ) {
                MongoDocument d = docs.get( i );
                Node n = Core.getInstance().getNodeById( this, d.getIdentifier() );
                d.set( "badge", Core.getInstance().getTemplateManager().getRenderer( request ).renderObject( n, "badge.vm" ) );
            }
        }

        PrintWriter writer = response.getWriter();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        writer.write( gson.toJson( docs ) );
    }

    public void addCall( Resource<?> resource ) {
        addCall( resource.getIdentifier() );
    }

    public void addCall( String id ) {
        logger.debug( "Adding " + id + " to " + this );
        //document.addToList( "elements", new MongoDocument().set( "id", resource.getIdentifier() ).set( "sort", sortValue ) );
        int sortValue = length();
        logger.debug( "Next element is at " + sortValue );
        MongoDocument field = new MongoDocument().set( "id", id ).set( SORT_FIELD, sortValue );
        MongoDBQuery query = new MongoDBQuery().getId( this.getIdentifier() );
        MongoDocument sort = new MongoDocument().set( SORT_FIELD, 1 );
        MongoUpdate update = new MongoUpdate().push( "elements", field, sort );

        MongoDBCollection.get( Core.RESOURCES_COLLECTION_NAME ).update( query, update );
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

        MongoDBCollection.get( Core.RESOURCES_COLLECTION_NAME ).update( query, update );
    }

    public void removeAll() {
        document.setList( ELEMENTS_FIELD );
    }

    public void updateSortValue( Resource<?> resource, int sortValue ) {
        logger.debug( "Updating " + resource + " in " + this + " with " + sortValue );
        MongoDocument getID = new MongoDocument().set( "id", resource.getIdentifier() );
        MongoDBQuery query = new MongoDBQuery().elemMatch( "elements", getID ).getId( this.getIdentifier() );
        MongoUpdate update = new MongoUpdate().set( "elements.$.sort", sortValue );

        MongoDBCollection.get( Core.RESOURCES_COLLECTION_NAME ).update( query, update );
    }

    public void remove( Resource<?> resource ) {
        logger.debug( "Removing " + resource + " frin " + this );
        MongoDocument getID = new MongoDocument().set( "id", resource.getIdentifier() );
        MongoDBQuery query = new MongoDBQuery().elemMatch( "elements", getID ).getId( this.getIdentifier() );

        MongoDBCollection.get( Core.RESOURCES_COLLECTION_NAME ).remove( query );
    }

    public static class CollectionDescriptor extends ResourceDescriptor<Collection> {

        @Override
        public String getType() {
            return "collection";
        }

        @Override
        public String getDisplayName() {
            return "Collection";
        }
    }
}