package org.seventyeight.web.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author cwolfgang
 */
public abstract class Widget extends Configurable implements ExtensionPoint, Node {

    private static Logger logger = LogManager.getLogger( Widget.class );

    public abstract String getDisplayName();

    public abstract String getName();

    @GetMethod
    public WebResponse doView(Request request) throws TemplateException, IOException {
        //response.setRenderType( Response.RenderType.NONE );

        Core core = request.getCore();

        //PrintWriter writer = response.getWriter();
        //writer.write( core.getTemplateManager().getRenderer( request ).renderObject( this, "view.vm" ) );
        return new WebResponse().appendBody(core.getTemplateManager().getRenderer( request ).renderObject( this, "view.vm" ));
    }
}
