package org.seventyeight.web.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.database.orm.SimpleORM;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.CoreException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.RedirectResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;

/**
 * @author cwolfgang
 */
public abstract class Configurable {

    private static Logger logger = LogManager.getLogger( Configurable.class );

    public static final String CONFIGURATION_COLLECTION_NAME = "configurations";

    public String getId() {
        return getClass().getName();
    }

    /**
     * Get the {@link MongoDocument} for the {@link Descriptor}. Will never return null.
     * @return
     */
    public MongoDocument getConfigurationDocument() {
        MongoDocument doc = MongoDBCollection.get( CONFIGURATION_COLLECTION_NAME ).findOne( new MongoDBQuery().is( "_id", this.getId() ) );
        if( !doc.isNull() ) {
            logger.debug( "Found an existing configuration" );
            return doc;
        } else {
            logger.debug( "New empty document" );
            MongoDocument newdoc = new MongoDocument();
            newdoc.set( "_id", getId() );
            return newdoc;
        }
    }

    public void loadConfiguration() throws CoreException {
        MongoDocument doc = getConfigurationDocument();
        logger.debug( "Configuration for {}: {}", this, doc );
        try {
            SimpleORM.bindToObject( this, doc );
        } catch( IllegalAccessException e ) {
            throw new CoreException( "Unable to load " + this, e );
        }
    }

    @GetMethod
    public WebResponse doSubmit( Request request ) throws IOException {
        save( request );
        MongoDocument doc = getConfigurationDocument();
        try {
            SimpleORM.storeFromObject( this, doc );
        } catch( IllegalAccessException e ) {
            throw new IOException( e );
        }

        logger.debug( "SAVING " + doc );
        MongoDBCollection.get( Core.DESCRIPTOR_COLLECTION_NAME ).save( doc );

        /* TODO get a better URL */
        //response.sendRedirect( "/" );
        return new RedirectResponse("/");
    }


    /**
     * Base implementation that does nothing
     * @param request
     * @param response
     */
    public void save( Request request ) {
        logger.debug( "Saving " + getClass() );
    }
}
