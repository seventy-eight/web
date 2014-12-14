package org.seventyeight.web.servlet.responses;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.seventyeight.web.Core;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

public class ErrorResponse extends WebResponse {
	
	private static Logger logger = LogManager.getLogger(ErrorResponse.class);

	private Throwable e;
	
	public ErrorResponse(Throwable e) {
		this.e = e;
		this.header = e.getMessage();
		this.code = 500;
	}

	@Override
	protected void writeBody(Request request, Response response) throws IOException {
        logger.error( "Generating error: {}", e.getMessage() );
        PrintWriter writer = response.getWriter();
        try {
            VelocityContext vc = new VelocityContext();

            Core core = request.getCore();

            org.seventyeight.web.model.Error error = new org.seventyeight.web.model.Error( (Exception)e );

            writer.write( core.getTemplateManager().getRenderer( request ).setContext( vc ).renderObject( error, "view.vm" ) );
            //request.getContext().put( "title", e.getMessage() );
            //writer.print( core.getTemplateManager().getRenderer( request ).render( "org/seventyeight/web/main.vm" ) );
            logger.debug("Done writing error");
        } catch( Exception ec ) {
        	logger.log(Level.ERROR, "Failed writing error", ec);
            request.getContext().put( "content", "Error while displaying exception" );
        }
	}
}
