package org.seventyeight.web.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.search.SearchFormatter;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.*;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cwolfgang
 */
public class ResourcesAction implements Node, DeletingParent {

    private static Logger logger = LogManager.getLogger( ResourcesAction.class );

    private ConcurrentHashMap<String, SearchFormatter> formatters = new ConcurrentHashMap<String, SearchFormatter>(  );

    private Core core;

    public ResourcesAction( Core core ) {
        this.core = core;
    }

    @Override
    public Node getParent() {
        return core.getRoot();
    }

    @Override
    public String getDisplayName() {
        return "Resources";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    public void addFormatter( SearchFormatter formatter ) {
        logger.debug( "Adding formatter {}", formatter.getName() );
        formatters.put( formatter.getName(), formatter );
    }

    @PostMethod
    public WebResponse doSearch( Request request ) throws IOException, NotFoundException, ItemInstantiationException, TemplateException {
        //response.setRenderType( Response.RenderType.NONE );
        //response.setContentType( "json/app" );

        int offset = request.getInteger( "offset", 0 );
        int number = request.getInteger( "number", 10 );
        String query = request.getValue( "query", null );

        logger.debug( query + ", OFFSET: " + offset + ", NUMBER: " + number );

        if( query != null && !query.isEmpty() ) {
            MongoDBQuery dbquery = FeatureSearch.getSimpleQuery( core, query );
            logger.debug( "QUERY: " + dbquery );

            List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( dbquery, offset, number );

            logger.debug( "DOCS: " + docs );

            //
            String[] formatters = request.getValue( "format", "" ).split( "," );


            for( MongoDocument d : docs ) {
                logger.debug( "TYPE: {}, {}", d.get( "type", "N/A" ), d.getIdentifier() );
                Node n = core.getNodeById( this, d.getIdentifier() );
                d.set( "badge", core.getTemplateManager().getRenderer( request ).renderObject( n, "badge.vm" ) );

                if( formatters.length > 0 ) {
                    for( String formatter : formatters ) {
                        logger.debug( "FORMAYTERERE: {}, {}", formatter, this.formatters.get( formatter ) );
                        this.formatters.get( formatter ).format( d, n );
                    }
                }
                //d.removeField( "extensions" );
            }

            logger.debug( "SIZE: " + docs.size() );

            ///PrintWriter writer = response.getWriter();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            //writer.write( gson.toJson( docs ) );
            return WebResponse.makeJsonResponse().appendBody(gson.toJson( docs ));
        } else {
            //response.getWriter().write( "{}" );
        	return WebResponse.makeEmptyJsonResponse();
        }
    }


    @GetMethod
    public WebResponse doList( Request request ) throws IOException {
        //response.setRenderType( Response.RenderType.NONE );
        //response.setContentType( "json/app" );

        int offset = request.getInteger( "offset", 0 );
        int number = request.getInteger( "number", 10 );
        String type = request.getValue( "type", "" );
        String username = request.getValue( "username", "" );

        MongoDBQuery query = new MongoDBQuery();
        if( !type.isEmpty() ) {
            query.is( "type", type );
        }
        if( !username.isEmpty() ) {
            User user = User.getUserByUsername( core, this, username );
            query.is( "owner", user.getIdentifier() );
        }

        logger.debug( "Query: {}", query );

        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, offset, number );

        if( docs.size() > 0 ) {
            //PrintWriter writer = response.getWriter();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            //writer.write( gson.toJson( docs ) );
            return WebResponse.makeJsonResponse().appendBody(gson.toJson( docs ));
        } else {
            //response.getWriter().write( "{}" );
        	return WebResponse.makeEmptyJsonResponse();
        }
    }

    @Override
    public void deleteChild( Node node ) {
        core.getRoot().deleteChild( node );
    }
}
