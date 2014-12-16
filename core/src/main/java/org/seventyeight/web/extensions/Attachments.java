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
import org.seventyeight.web.servlet.responses.WebResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * @author cwolfgang
 */
public class Attachments extends Action<Attachments> {

    private static Logger logger = LogManager.getLogger( Attachments.class );

    public static final String FILE_ATTACHMENTS_ELEMENT = "attachments";

    public Attachments( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
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
    public WebResponse doAttachFile( Request request ) {
        try {
            //FileResource fr = FileResource.upload( request, response );
            //addFile( fr );
            Core.superSave( this );

            //response.setStatus( HttpServletResponse.SC_OK );
            return new WebResponse();
        } catch( Exception e ) {
            //response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
        	return new WebResponse().notAccepted();
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

        public AttachmentsDescriptor( Core core ) {
            super( core );
        }

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
