package org.seventyeight.web.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.utils.PutMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Concert extends Resource<Concert> implements Event, Getable<Artist> {

    private static Logger logger = LogManager.getLogger( Concert.class );

    public Concert( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
        if(jsonData != null) {
            JsonElement venueElement = jsonData.get( "venue" );
            logger.debug( "VENUE ELEMENT: {}", venueElement );
            if(venueElement.isJsonNull()) {
                throw new IllegalArgumentException( "Venue must be provided" );
            }
            document.set( "venue", venueElement.getAsString() );

            JsonElement artistsElement = jsonData.get( "artists" );
            if(artistsElement == null || artistsElement.isJsonNull()) {
                logger.debug( "No artists provided" );
            } else {
                JsonArray artistsArray = artistsElement.getAsJsonArray();
                List<String> artists = new ArrayList<String>( artistsArray.size() );
                for( JsonElement k : artistsArray) {
                    artists.add( k.getAsString() );
                }

                document.set( "artists", artists );
            }
        }
    }

    public List<String> getArtistIds() {
        return document.get( "artists", Collections.EMPTY_LIST );
    }

    public void setVenue(Venue venue) {
        document.set( "venue", venue.getIdentifier() );
    }

    public String getVenueId() {
        return document.get( "venue", null );
    }

    public Venue getVenue() throws NotFoundException, ItemInstantiationException {
        return core.getNodeById( this, getVenueId() );
    }

    public void setAsPartOf(Resource<?> resource) {
        document.set( "partOf", resource.getIdentifier() );
    }

    public boolean hasArtist(String id) {
        return document.arrayHasId( "artists", id );
    }

    @Override
    public Artist get( Core core, String token ) throws NotFoundException {
        if(hasArtist( token )) {
            try {
                return core.getNodeById( this, token );
            } catch( ItemInstantiationException e ) {
                throw new NotFoundException( token + " not found, " + e.getMessage() );
            }
        } else {
            throw new NotFoundException( token );
        }
    }

    @PostMethod
    public WebResponse doIndex(Request request) throws IOException {
        //response.setRenderType( Response.RenderType.NONE );

        String artist = request.getValue( "resource", null );
        logger.debug( "Adding {} to {}", artist, this );

        if(hasArtist( artist )) {
            //response.sendError( HttpServletResponse.SC_CONFLICT, "The artist with id " + artist + ", is already added" );
            return new WebResponse().conflict().setHeader("The artist with id " + artist + ", is already added");
        }

        if(artist != null) {
            document.addToList( "artists", artist );
            save();
            return new WebResponse();
        } else {
            //throw new IllegalArgumentException( "No artist provided" );
        	return new WebResponse().badRequest().setHeader("No artist provided");
        }
    }

    @Override
    public void deleteChild( Node node ) {
    	logger.debug("Trying to delete {}", this);

        if(node != null && node instanceof Artist) {
            if(hasArtist( ( (Artist) node ).getIdentifier() )) {
                document.removeFromList( "artists", ( (Artist) node ).getIdentifier() );
            } else {
                throw new IllegalArgumentException( node + " does not belong to " + this );
            }
        }

        save();
    }

    public static class ConcertDescriptor extends ResourceDescriptor<Concert> {

        public ConcertDescriptor( Node parent ) {
            super( parent );
        }
        
        @Override
		public String getUrlName() {
			return "concerts";
		}

        @GetMethod
        public WebResponse doGetConcerts(Request request) throws IOException {
            //response.setRenderType( Response.RenderType.NONE );

            String term = request.getValue( "term", "" );
            String venueTerm = request.getValue( "venue", null );

            if( term.length() > 1 ) {
                // First fetch artists
                MongoDBQuery artistQuery = new MongoDBQuery().is( "type", "artist" ).regex( "title", "(?i)" + term + ".*" );
                List<MongoDocument> artistDocs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( artistQuery, 0, 10 );

                MongoDBQuery query = new MongoDBQuery();
                List<MongoDBQuery> queries = new ArrayList<MongoDBQuery>( 3 );
                queries.add( new MongoDBQuery().is( "type", "concert" ).regex( "title", "(?i)" + term + ".*" ) );
                List<String> ids = new ArrayList<String>( artistDocs.size() );
                for(MongoDocument d : artistDocs) {
                    ids.add( d.get( "_id", "" ) );
                }
                queries.add( new MongoDBQuery().in( "artists", ids ) );

                query.or( true, queries );
                logger.debug( "QUERY IS {}", query );

                //PrintWriter writer = response.getWriter();
                //writer.print( MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ) );
                return WebResponse.makeJsonResponse().appendBody(MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ));
            } else {
                return WebResponse.makeEmptyJsonResponse();
            }
        }

        @Override
        public String getType() {
            return "concert";
        }

        @Override
        public String getDisplayName() {
            return "Concert";
        }
    }
}
