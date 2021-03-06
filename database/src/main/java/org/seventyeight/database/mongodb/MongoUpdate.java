package org.seventyeight.database.mongodb;

import com.mongodb.BasicDBObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.Document;

/**
 * @author cwolfgang
 */
public class MongoUpdate {

    private static Logger logger = LogManager.getLogger( MongoUpdate.class );

    private BasicDBObject update = new BasicDBObject();

    private boolean multi = false;
    private boolean upsert = false;

    public BasicDBObject getDocument() {
        return update;
    }

    /*
    public MongoUpdate add() {
        update
    }
    */

    public MongoUpdate set( String key, Object value ) {
        return update( "$set", key, value );
    }

    public MongoUpdate pull( String key, Object value ) {
        return update( "$pull", key, value );
    }

    /**
     * extensions: {
     *     action: {
     *         t: 2
     *     }
     * }
     *
     */
    public MongoUpdate pull( String path, String field, Object value ) {

        BasicDBObject dbo = new BasicDBObject();
        dbo.put( path, new BasicDBObject( field, value ) );
        update.put( "$pull", dbo );
        upsert = false;
        multi = true;

        return this;
    }

    public MongoUpdate push( String key, Object value ) {
        return update( "$push", key, value );
    }

    public MongoUpdate push( String key, Object value, MongoDocument sort ) {
        if( value instanceof MongoDocument ) {
            ( (MongoDocument) value ).set( "$sort", sort );
        }

        return update( "$push", key, value );
    }

    public MongoUpdate unset( String key ) {
        return update( "$unset", key, 1 );
    }

    public MongoUpdate unset( String key, int index ) {
        return update( "$unset", key + "." + index, 1 );
    }

    private MongoUpdate update( String type, String key, Object value ) {
        BasicDBObject set = null;
        if( update.containsField( type ) ) {
            set = (BasicDBObject) update.get( type );
        } else {
            set = new BasicDBObject();
            update.put( type, set );
        }

        logger.debug( "SET: " + set );

        if( value instanceof MongoDocument ) {
            set.append( key, ((MongoDocument)value).getDBObject() );
        } else {
            set.append( key, value );
        }

        return this;
    }

    public MongoUpdate setMulti() {
        multi = true;

        return this;
    }

    public boolean isMulti() {
        return multi;
    }
    
    public MongoUpdate setUpsert() {
        upsert = true;
        
        return this;
    }

    public boolean isUpsert() {
        return upsert;
    }
}
