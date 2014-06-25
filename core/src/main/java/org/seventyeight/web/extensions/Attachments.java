package org.seventyeight.web.extensions;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.*;
import org.seventyeight.web.nodes.FileResource;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import javax.servlet.http.HttpServletResponse;

/**
 * @author cwolfgang
 */
public class Attachments extends Action<Attachments> {

    private static Logger logger = LogManager.getLogger( Attachments.class );

    public static final String FILE_ATTACHMENTS_ELEMENT = "attachments";

    public Attachments( Node parent, MongoDocument document ) {
        super( parent, document );
    }

    @Override
    public String getDisplayName() {
        return "Attachments";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    @PostMethod
    public void doAttachFile( Request request, Response response ) {
        try {
            //FileResource fr = FileResource.upload( request, response );
            //addFile( fr );
            Core.superSave( this );

            response.setStatus( HttpServletResponse.SC_OK );
        } catch( Exception e ) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
        }
    }

    public void addFile( FileResource fileResource ) {
        document.addToList( FILE_ATTACHMENTS_ELEMENT, fileResource.getIdentifier() );
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
      /* Implementation is a no op */
    }

    public static class AttachmentsDescriptor extends ActionDescriptor<Attachments> {

        @Override
        public String getDisplayName() {
            return "Attachments";
        }

        @Override
        public String getExtensionName() {
            return "attachments";
        }

        @Override
        public Class<Attachments> getExtensionClass() {
            return Attachments.class;
        }

        @Override
        public boolean isApplicable( Node node ) {
            logger.debug( "Is" + node + " applicable" );

            if( node instanceof Resource ) {
                MongoDocument sd = getExtensionDocument( (Resource) node );
                return sd.get( "enabled", true );
            } else {
                return false;
            }
        }
    }
}
