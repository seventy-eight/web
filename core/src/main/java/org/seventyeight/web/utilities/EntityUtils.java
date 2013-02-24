package org.seventyeight.web.utilities;

import org.apache.log4j.Logger;
import org.seventyeight.database.Database;
import org.seventyeight.web.Core;
import org.seventyeight.web.SeventyEight;
import org.seventyeight.web.exceptions.*;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.AbstractResource;
import org.seventyeight.web.model.Descriptor;
import org.seventyeight.web.model.Entity;
import org.seventyeight.web.model.ResourceDescriptor;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author cwolfgang
 *         Date: 17-01-13
 *         Time: 21:24
 */
public class EntityUtils {

    private static Logger logger = Logger.getLogger( EntityUtils.class );

    private EntityUtils() {

    }

    public static void getConfigureResourceView( Request request, Response response, Entity entity, Descriptor descriptor ) throws IOException, TemplateException {
        logger.debug( "Configuring " + entity );

        //ResourceDescriptor descriptor = (ResourceDescriptor) entity.getDescriptor();

        request.getContext().put( "url", "/entity/" + entity.getIdentifier() + "/configurationSubmit" );
        //request.getContext().put( "url", "configurationSubmit" );
        request.getContext().put( "class", descriptor.getClazz().getName() );
        request.getContext().put( "header", "Configuring " + entity.getDisplayName() );
        request.getContext().put( "descriptor", descriptor );

        /* Required javascrips */
        request.getContext().put( "javascript", descriptor.getRequiredJavascripts() );

        logger.fatal( "NU ER VI HER " + request.getContext().get( "item" ) );
        request.getContext().put( "content", Core.getInstance().getTemplateManager().getRenderer( request ).renderObject( entity, "configure.vm" ) );
        response.getWriter().print( Core.getInstance().getTemplateManager().getRenderer( request ).render( request.getTemplate() ) );

    }


    public static AbstractResource getResource( Database db, String name ) throws CouldNotLoadItemException {
        Long id = null;
        AbstractResource r = null;
        try {
            id = new Long( name );
            r = SeventyEight.getInstance().getResource( db, id );

        } catch( NumberFormatException e ) {
            /* This is an identifier, let's try the title */
            String s = "";
            try {
                s = URLDecoder.decode( name, "UTF-8" );
                logger.debug( "Finding " + s );
                //r = SeventyEight.getInstance().getResourceByTitle( s );
            } catch( UnsupportedEncodingException e1 ) {
                logger.warn( s + " not found" );
                throw new CouldNotLoadItemException( "Unable to find resource[" + s + "]: " + e1.getMessage());
            }
        }

        return r;
    }
}
