package org.seventyeight.database.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.seventyeight.database.Document;

import java.util.*;

/**
 * @author cwolfgang
 */
public class MongoDocument implements Document {

    private DBObject document;

    public MongoDocument() {
        document = new BasicDBObject();
    }

    public MongoDocument( BasicDBObject document ) {
        this.document = document;
    }

    public MongoDocument( DBObject document ) {
        this.document = document;
    }

    public DBObject getDBObject() {
        return document;
    }

    @Override
    public <T> T get( String key ) {
        Object data = (T) document.get( key );
        if( data instanceof BasicDBList ) {
            return (T) getList( (BasicDBList) data );
        } else if( data instanceof DBObject ) {
            return (T) new MongoDocument( (DBObject) data );
        } else {
            return (T) data;
        }
    }

    /*
    public <T> T getOrCreate(String key) {
        Object data = (T) document.get( key );
        if(data != null) {
            if( data instanceof BasicDBList ) {
                return (T) getList( (BasicDBList) data );
            } else if( data instanceof DBObject ) {
                return (T) new MongoDocument( (DBObject) data );
            } else {
                return (T) data;
            }
        } else {

        }
    }
    */

    private List<Object> getList( BasicDBList list ) {
        if( list != null ) {
            List<Object> docs = new ArrayList<Object>( list.size() );

            for( Object o : list ) {
                docs.add( _get( o ) );
            }

            return docs;
        } else {
            return Collections.emptyList();
        }
    }

    private <T> T _get( Object o ) {
        if( o instanceof DBObject ) {
            return (T) new MongoDocument( ((DBObject)o) );
        } else {
            return (T) o;
        }
    }

    public MongoDocument getr( String ... keys ) {

        DBObject current = document;
        int i = 0;
        for( ; i < keys.length ; i++ ) {
            String key = keys[i];
            Object o = current.get( key );

            if( o == null ) {
                break;
            } else {
                if( o instanceof DBObject ) {
                    current = (DBObject) o;
                }
            }
        }

        for( ; i < keys.length ; i++ ) {
            String key = keys[i];
            DBObject o = new BasicDBObject(  );
            current.put( key, o );
            current = o;
        }

        return new MongoDocument( current );
    }

    public MongoDocument getSubDocument( String key, MongoDocument defaultDoc ) {
        if( document.containsField( key ) ) {
            return new MongoDocument( (DBObject) document.get( key ) );
        } else {
            return defaultDoc;
        }
    }

    @Override
    public <T> T get( String key, T defaultValue ) {
        if( document.containsField( key ) ) {
            return (T) document.get( key );
        } else {
            return defaultValue;
        }
    }

    @Override
    public <T, R extends Document> R set( String key, T value ) {
        if( value instanceof MongoDocument ) {
            document.put( key, ((MongoDocument)value).getDBObject() );
        } else if(value instanceof Map) {
            Map<Object, Object> t = new HashMap<Object, Object>(  );
            for(Object o : ( (Map) value ).keySet()) {
                if(( (Map) value ).get( o ) instanceof MongoDocument) {
                    t.put( o, ( (MongoDocument) ( (Map) value ).get( o ) ).getDBObject() );
                } else {
                    t.put( o, t.get( o ) );
                }
            }
            document.put( key, t );
        } else {
            document.put( key, value );
        }

        return (R) this;
    }

    public MongoDocument setList( String key ) {
        document.put( key, new BasicDBList() );

        return this;
    }

    public <T> MongoDocument addToList( String key, T value ) {
        BasicDBList list = (BasicDBList) document.get( key );

        if( list == null ) {
            list = new BasicDBList();
            document.put( key, list );
            //list = (BasicDBList) document.get( key );
        }
        System.out.println( "------>" + list );

        if( value instanceof MongoDocument ) {
            list.add( ((MongoDocument)value).getDBObject() );
        } else {
            list.add( value );
        }

        return this;
    }

    /**
     * Add an entry to a field. If the entry doesn't exist, it will be created.
     *
     * <code>
     *  { field: {
     *      key: { value }
     *           }
     *  }
     *  </code>
     */
    public <T> MongoDocument add( String field, String key, T value ) {
        if( document.containsField( field ) ) {
            ((DBObject)document.get( field )).put( key, ( value instanceof MongoDocument ? ( (MongoDocument) value ).getDBObject() : value ) );
        } else {
            document.put( field, new BasicDBObject( key, ( value instanceof MongoDocument ? ( (MongoDocument) value ).getDBObject() : value ) ) );
        }

        return this;
    }

    public List<MongoDocument> getList( String key ) {
        List<BasicDBObject> list = (List<BasicDBObject>) document.get( key );

        if( list != null ) {
            List<MongoDocument> docs = new ArrayList<MongoDocument>( list.size() );

            for( BasicDBObject o : list ) {
                docs.add( new MongoDocument( o ) );
            }

            return docs;
        } else {
            return Collections.emptyList();
        }
    }

    public List<Object> getObjectList( String key ) {
        BasicDBList list = (BasicDBList) document.get( key );

        if( list != null ) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public <T extends Object> List<T> getObjectList2( String key ) {
        BasicDBList list = (BasicDBList) document.get( key );

        if( list != null ) {
            List<T> r = new ArrayList<T>( list.size() );
            for(Object o : list) {
                r.add( (T) o );
            }
            return r;
        } else {
            return Collections.emptyList();
        }
    }

    public List<MongoDocument> getList( String key, int offset, int number ) {
        List<BasicDBObject> list = (List<BasicDBObject>) document.get( key );

        if( list != null ) {
            List<MongoDocument> docs = new ArrayList<MongoDocument>( list.size() );

            for( int i = offset ; ( i < docs.size() || i < offset + number ) ; i++ ) {
                BasicDBObject o = list.get( i );
                docs.add( new MongoDocument( o ) );
            }

            return docs;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     *
     */
    public List<MongoDocument> getMappedList( String key, String collection ) {
        List<String> list = (List<String>) document.get( key );

        //System.out.println( "DA LIST: " + list );
        MongoDBCollection col = MongoDBCollection.get( collection );

        if( list != null ) {
            List<MongoDocument> docs = new ArrayList<MongoDocument>( list.size() );

            for( String o : list ) {
                MongoDocument d = col.getDocumentById( o );
                docs.add( d );
            }

            return docs;
        } else {
            return Collections.emptyList();
        }
    }

    public MongoDocument addExtension( MongoDocument extensionData ) {
        document.put( EXTENSIONS, extensionData );

        return this;
    }

    public MongoDocument removeField( String fieldName ) {
        document.removeField( fieldName );

        return this;
    }

    public boolean isNull() {
        return document == null;
    }

    public String getIdentifier() {
        //ObjectId id = get( "_id" );
        //return id.toString();
        return get( "_id" );
    }

    public boolean arrayHas( String field, String value ) {

        return true;
    }

    public Map getMap() {
        return document.toMap();
    }

    public Set<String> getKeys() {
        return document.keySet();
    }

    @Override
    public String toString() {
        if( document != null ) {
            return document.toString();
        } else {
            return "Internal document is null";
        }
    }
}
