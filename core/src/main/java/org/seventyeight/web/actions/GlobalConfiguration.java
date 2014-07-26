package org.seventyeight.web.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import java.io.IOException;

/**
 * @author cwolfgang
 */
public class GlobalConfiguration implements Node, Parent {

    private static Logger logger = LogManager.getLogger( GlobalConfiguration.class );

    private Core core;

    public GlobalConfiguration( Core core ) {
        this.core = core;
    }

    @Override
    public Node getParent() {
        return core.getRoot();
    }

    @Override
    public Node getChild( String name ) throws NotFoundException {
        logger.debug( "Configuration for " + name );

        try {
            Descriptor d = core.getDescriptor( name );
            return new ViewWrapper( core, (Node)d, "globalConfigure" ).setClassOffset( d.getClazz() ).setPostViewTemplate( "submit" );
        } catch( ClassNotFoundException e ) {
            logger.debug( e );
            return null;
        }
    }

    public void doGlobal( Request request, Response response ) throws IOException {
        response.getWriter().println( "BOOOOOOM!" );
    }

    @Override
    public String getDisplayName() {
        return "Global configuration";
    }

    @Override
    public String getMainTemplate() {
        return Core.MAIN_TEMPLATE;
    }
}
