package org.seventyeight.web.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.ast.Root;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.parser.Tokenizer;
import org.seventyeight.parser.impl.SimpleSearchQueryParser;
import org.seventyeight.web.Core;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.FeatureSearch;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.QueryParser;
import org.seventyeight.web.utilities.QueryVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class SearchHelper {

    private static Logger logger = LogManager.getLogger( SearchHelper.class  );

    private Request request;

    private Node parent;

    private int defaultOffset = 0;

    private int defaultNumber = 10;

    private boolean removeExtensionFields = true;

    private boolean renderBadge = true;

    private List<MongoDocument> documents;

    private static QueryParser queryParser = new QueryParser();
    //private static SimpleSearchQueryParser parser = new SimpleSearchQueryParser();

    private Core core;

    public SearchHelper( Node parent, Request request ) {
        this.request = request;
        this.parent = parent;

        this.core = request.getCore();
    }


    public SearchHelper search() {
        int offset = request.getInteger( "offset", defaultOffset );
        int number = request.getInteger( "number", defaultNumber );
        String query = request.getValue( "query", null );

        logger.debug( query + ", OFFSET: " + offset + ", NUMBER: " + number );

        if( query != null && !query.isEmpty() ) {
            //LinkedList<String> tokens = tokenizer.tokenize( query );
            Root root = queryParser.parse( query );
            QueryVisitor visitor = new QueryVisitor( core );
            visitor.visit( root );

            logger.debug( "I GOT {}", visitor.getQuery() );

            documents = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( visitor.getQuery(), offset, number );

            logger.debug( "DOCS: " + documents );
        } else {
            documents = new ArrayList<MongoDocument>( 0 );
        }

        logger.debug( "SIZE: " + documents.size() );

        return this;
    }

    public SearchHelper search2() {
        int offset = request.getInteger( "offset", defaultOffset );
        int number = request.getInteger( "number", defaultNumber );
        String query = request.getValue( "query", null );

        logger.debug( query + ", OFFSET: " + offset + ", NUMBER: " + number );

        if( query != null && !query.isEmpty() ) {
            MongoDBQuery dbquery = FeatureSearch.getSimpleQuery( core, query );
            logger.debug( "QUERY: " + dbquery );

            documents = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( dbquery, offset, number );

            logger.debug( "DOCS: " + documents );
        } else {
            documents = new ArrayList<MongoDocument>( 0 );
        }

        logger.debug( "SIZE: " + documents.size() );

        return this;
    }

    public WebResponse render() throws TemplateException, NotFoundException, ItemInstantiationException, IOException {
        //response.setRenderType( renderType );
        //response.setContentType( contentType );
    	
        if( documents.size() > 0 ) {
            for( MongoDocument d : documents ) {
                logger.debug( "TYPE: {}, {}", d.get( "type", "N/A" ), d.getIdentifier() );
                Node n = core.getNodeById( parent, d.getIdentifier() );

                if( renderBadge ) {
                    d.set( "badge", core.getTemplateManager().getRenderer( request ).renderObject( n, "badge.vm" ) );
                }

                if( renderBadge ) {
                    d.set( "avatar", core.getTemplateManager().getRenderer( request ).renderObject( n, "avatar.vm" ) );
                }

                if( removeExtensionFields ) {
                    d.removeField( "extensions" );
                }
            }

            //PrintWriter writer = response.getWriter();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            //writer.write( gson.toJson( documents ) );
            return WebResponse.makeJsonResponse().appendBody(gson.toJson( documents ));
        } else {
          //response.getWriter().write( "{}" );
        	return WebResponse.makeEmptyJsonResponse();
        }
    }

    public List<MongoDocument> getDocuments() {
        return documents;
    }
}
