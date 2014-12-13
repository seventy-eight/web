package org.seventyeight.web.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.mongodb.MongoUpdate;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.utils.PutMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Festival extends Resource<Festival> implements Getable<Event> {

    private static Logger logger = LogManager.getLogger( Festival.class );

    public Festival( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
    }

    @PostMethod
    public WebResponse doIndex(Request request) throws ItemInstantiationException, NotFoundException {
        logger.debug( "Adding event for {}", this );
        //response.setRenderType( Response.RenderType.NONE );

        String eventId = request.getValue( "resource", null );

        if(eventId != null) {
            Event event = core.getNodeById( this, eventId );
            logger.debug( "Adding {} to {}", event, this );
            addEvent( event );
            return new WebResponse();
        } else {
            //throw new IllegalArgumentException( "No event provided" );
        	return new WebResponse().badRequest().setHeader("No event provided");
        }
    }
    
    @Override
    public void deleteChild( Node node ) {
    	logger.debug("Trying to delete {}", this);

    	/*
        if(node != null && node instanceof Event) {
            if(hasEvent( ( (Artist) node ).getIdentifier() )) {
                document.removeFromList( "artists", ( (Artist) node ).getIdentifier() );
            } else {
                throw new IllegalArgumentException( node + " does not belong to " + this );
            }
        }

        save();
        */
    }


    public void addEvent(Event event) {
        event.setAsPartOf( this );
        event.save();
    }

    public List<Event> getEvents() throws ItemInstantiationException {
        logger.debug( "Getting events for {}", this );

        MongoDBQuery query = new MongoDBQuery().is( "partOf", getIdentifier() );
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query );
        List<Event> events = new ArrayList<Event>( docs.size() );
        for(MongoDocument doc : docs) {
            Event event = (Event) core.getNode( this, doc );
            events.add( event );
        }

        return events;
    }
    
    public List<String> getEventIds(int offset, int number) {
        logger.debug( "Getting event ids for {}", this );

        MongoDBQuery query = new MongoDBQuery().is( "partOf", getIdentifier() );
        return core.getIds(query, offset, number, null);
    }
    
    public boolean hasEvent(String eventId) {
    	MongoDBQuery query = new MongoDBQuery().is( "partOf", getIdentifier() ).getId(eventId);
    	MongoDocument d = core.getId(query);
    	
    	return (d != null && !d.isNull());
    }

    @Override
    public Event get( Core core, String token ) throws NotFoundException {
        if(hasEvent( token )) {
            try {
                return core.getNodeById( this, token );
            } catch( ItemInstantiationException e ) {
                throw new NotFoundException( token + " not found, " + e.getMessage() );
            }
        } else {
            throw new NotFoundException( token );
        }
    }


    public static class FestivalDescriptor extends ResourceDescriptor<Festival> {

        public FestivalDescriptor( Node parent ) {
            super( parent );
        }
        
        @Override
		public String getUrlName() {
			return "festivals";
		}

        @Override
        public String getType() {
            return "festival";
        }

        @Override
        public String getDisplayName() {
            return "Festival";
        }
    }
}
