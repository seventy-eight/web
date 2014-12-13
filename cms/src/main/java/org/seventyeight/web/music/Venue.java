package org.seventyeight.web.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.extensions.MenuContributor;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Venue extends Resource<Venue> {

    private static Logger logger = LogManager.getLogger( Venue.class );

    public Venue( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
        if(jsonData != null) {
            boolean multiStaged = false;
            JsonElement e = jsonData.get( "multiStaged" );
            if(e != null && !e.isJsonNull()) {
                multiStaged = e.getAsString().equals( "on" );
            }

            document.set( "multiStaged", multiStaged );
            if(multiStaged) {
                JsonElement s = jsonData.get( "stages" );
                if(!s.isJsonNull()) {
                    logger.debug( "JSON ELEMENT: {}", s );
                    String[] stages = s.getAsString().split( "\\s*,\\s*|\\s+" );
                    document.set( "stages", stages );
                }
            }
        }
    }

    public boolean isMultiStaged() {
        return document.get( "multiStaged", false );
    }

    public void setMultiStaged(boolean multiStaged) {
        document.set( "multiStaged", multiStaged );
    }

    public void addStage(String stage) {
        document.addToList( "stages", stage );
    }

    public List<String> getStages() {
        return document.getObjectList2( "stages" );
    }

    @GetMethod
    public WebResponse doGetStage(Request request) throws IOException {
        //response.setRenderType( Response.RenderType.NONE );

        String term = request.getValue( "term", "" );

        if( term.length() > 1 ) {
            //PrintWriter writer = response.getWriter();
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            //writer.write( gson.toJson( getStageMatching( term ) ) );
            return WebResponse.makeJsonResponse().appendBody(gson.toJson( getStageMatching( term ) ));
        } else {
            //response.getWriter().write( "{}" );
        	return WebResponse.makeEmptyJsonResponse();
        }
    }

    public List<String> getStageMatching(String term) {
        String t = term.toLowerCase();

        List<String> r = new ArrayList<String>(  );
        for(String stage : getStages()) {
            if(stage.toLowerCase().contains( t )) {
                r.add( stage );
            }
        }

        return r;
    }

    @PostMethod
    public WebResponse doIndex(Request request) throws ItemInstantiationException, IOException {
        //response.setRenderType( Response.RenderType.NONE );

        Concert.ConcertDescriptor descriptor = core.getDescriptor( Concert.class );
        Concert instance = descriptor.newInstance( request, this );
        instance.setVenue( this );
        instance.save();

        //response.sendRedirect( instance.getConfigUrl() );
        
        return new WebResponse().appendBody(instance.getConfigUrl());
    }

    public static class VenueDescriptor extends ResourceDescriptor<Venue> implements MenuContributor {

        public VenueDescriptor( Node parent ) {
            super( parent );
        }
        
        @Override
		public String getUrlName() {
			return "venues";
		}
        
        @Override
    	public boolean isApplicable(Node node) {
    		return node instanceof Venue;
    	}

        @Override
        public void addContributingMenu( Node node, Menu menu ) {
            if(node instanceof Venue) {
                menu.addItem( "Venue", new Menu.MenuItem("Add concert", "addConcert", ACL.Permission.ADMIN) );
            }
        }

        @GetMethod
        public WebResponse doGetVenues(Request request) throws IOException {
            //response.setRenderType( Response.RenderType.NONE );

            String term = request.getValue( "term", "" );

            if( term.length() > 1 ) {
                MongoDBQuery query = new MongoDBQuery().is( "type", "venue" ).regex( "title", "(?i)" + term + ".*" );
                
                //PrintWriter writer = response.getWriter();
                //writer.print( MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ) );
                return WebResponse.makeJsonResponse().appendBody(MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10 ));
            } else {
                //response.getWriter().write( "{}" );
                return WebResponse.makeEmptyJsonResponse();
            }
        }


        @Override
        public String getType() {
            return "venue";
        }

        @Override
        public String getDisplayName() {
            return "Venue";
        }
    }
}
