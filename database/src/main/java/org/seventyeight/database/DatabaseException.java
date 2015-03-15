package org.seventyeight.database;

/**
 * @author cwolfgang
 */
public class DatabaseException extends Exception {

    public DatabaseException( Throwable throwable ) {
        super( throwable );
    }
    
    public DatabaseException(String s) {
        super(s);
    }

    public DatabaseException( String s, Throwable throwable ) {
        super( s, throwable );
    }
}
