package org.seventyeight.web.actions;

import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.SearchAction;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.util.List;

/**
 * @author cwolfgang
 */
public class NodeSearch extends SearchAction {

    public NodeSearch( Node parent ) {
        super( parent );
    }

    protected List<MongoDocument> getDocumentsWithStartingTitle( String term, String type, int limit ) {
        //MongoDBQuery q = new MongoDBQuery().is( "title", string );
        MongoDBQuery q = new MongoDBQuery().regex( "title", "(?i)" + term + ".*" );
        if( type != null ) {
            q.is( "type", type );
        }
        return MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( q, 0, limit );
    }

    /*
    protected List<String> getTitlesWithTitleLike( String string, String type, int limit ) {
        List<MongoDocument> docs = getDocumentsWithStartingTitle( string, type, limit );
    }
    */

    @GetMethod
    public WebResponse doIndex( Request request ) throws IOException {
        String type = request.getValue( "type", null );
        String term = request.getValue( "term", "" );
        int limit = Integer.parseInt( request.getValue( "limit", "10" ) );

        if( term.length() > 2 ) {
            //response.getWriter().print( getDocumentsWithStartingTitle( term, type, limit ) );
        	return WebResponse.makeJsonResponse().appendBody(getDocumentsWithStartingTitle( term, type, limit ));
        } else {
        	return WebResponse.makeEmptyJsonResponse();
        }
    }

    @Override
    public String getDisplayName() {
        return "Search nodes";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

}
