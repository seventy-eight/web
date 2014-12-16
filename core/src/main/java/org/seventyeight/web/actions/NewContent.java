package org.seventyeight.web.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.RedirectResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;

/**
 * @author cwolfgang
 */
public class NewContent implements Node {

    private static Logger logger = LogManager.getLogger( NewContent.class );

    private Node parent;

    private Core core;

    public NewContent( Core core, Node parent ) {
        this.parent = parent;
        this.core = core;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public String getDisplayName() {
        return "New content";
    }

    @PostMethod
    public WebResponse doCreate( Request request ) throws IOException {
        String className = request.getValue( "className" );

        if( className == null ) {
            throw new IOException( "No className given" );
        }

        /* Get the resource descriptor from the className name */
        NodeDescriptor<?> descriptor = null;
        try {
            descriptor = (NodeDescriptor<?>) core.getDescriptor( className );
        } catch( ClassNotFoundException e ) {
            throw new IOException( e );
        }

        if( descriptor == null ) {
            throw new IOException( new MissingDescriptorException( "Could not find descriptor for " + className ) );
        }

        /* First of all we need to create the resource node */
        logger.debug( "Newing resource" );
        AbstractNode r = null;
        try {
            String title = request.getValue( "title", "" );
            logger.debug( "Title is " + title );
            r = (AbstractNode) descriptor.newInstance( request, this );
            r.save();
        } catch( ItemInstantiationException e ) {
            throw new IOException( e );
        }
        logger.debug( "RESOURCE IS " + r );

        r.setOwner( request.getUser() );
        r.save();

        //response.sendRedirect( r.getUrl() + "configure" );
        //ResourceUtils.getConfigureResourceView( request, response, r, descriptor );
        return new RedirectResponse(r.getUrl() + "configure");
    }

    @Override
    public String getMainTemplate() {
        return null;
    }
}
