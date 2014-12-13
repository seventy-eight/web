package org.seventyeight.web.music;

import com.google.gson.JsonObject;

import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NodeDescriptor;
import org.seventyeight.web.model.Resource;
import org.seventyeight.web.model.ResourceDescriptor;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author cwolfgang
 */
public class Artist extends Resource<Artist> {
    public Artist( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    @Override
    public void updateNode( JsonObject jsonData ) {

    }

    public static final class ArtistDescriptor extends ResourceDescriptor<Artist> {

        public ArtistDescriptor( Node parent ) {
            super( parent );
        }
        
        @Override
		public String getUrlName() {
			return "artists";
		}

        @GetMethod
        public WebResponse doGetArtists(Request request) throws IOException {
            //response.setRenderType( Response.RenderType.NONE );

            String term = request.getValue( "term", "" );

            if( term.length() > 1 ) {
                MongoDBQuery query = new MongoDBQuery().is( "type", "artist" ).regex( "title", "(?i)" + term + ".*" );

                //PrintWriter writer = response.getWriter();
                //writer.print( MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ) );
                return WebResponse.makeJsonResponse().appendBody(MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ));
            } else {
                return WebResponse.makeEmptyJsonResponse();
            }
        }

        @Override
        public String getType() {
            return "artist";
        }

        @Override
        public String getDisplayName() {
            return "Artist";
        }
    }
}
