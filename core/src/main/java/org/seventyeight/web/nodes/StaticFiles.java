package org.seventyeight.web.nodes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.web.Core;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.Autonomous;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.FileResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * @author cwolfgang
 */
public class StaticFiles implements Autonomous, Node {

    private static Logger logger = LogManager.getLogger( StaticFiles.class );

    private Core core;

    public StaticFiles( Core core ) {
        this.core = core;
    }

    @Override
    public Node getParent() {
        return core.getRoot();
    }

    @Override
    public String getDisplayName() {
        return "Static files";
    }

    @Override
    public WebResponse autonomize( Request request ) throws IOException {
        String requestedFile = request.getPathInfo();
        //response.setRenderType( Response.RenderType.NONE );

        requestedFile = requestedFile.replaceFirst( "^/?.*?/", "" );
        logger.debug( "[Request file] " + requestedFile );

        if( requestedFile == null ) {
            return new WebResponse().setCode(404).setHeader("File not found");
        }

        String filename = URLDecoder.decode( requestedFile, "UTF-8" );

        File file = null;

        file = core.getTemplateManager().getStaticFile( filename );
        //response.deliverFile( request, file, true );
        return new FileResponse(file);
    }

    @Override
    public String getMainTemplate() {
        return "org/seventyeight/web/main.vm";
    }
}
