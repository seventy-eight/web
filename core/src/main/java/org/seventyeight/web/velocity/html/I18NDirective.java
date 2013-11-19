package org.seventyeight.web.velocity.html;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.seventyeight.web.handlers.Dictionary;

import java.io.IOException;
import java.io.Writer;

public class I18NDirective extends Directive {

	private static Logger logger = LogManager.getLogger( I18NDirective.class );
	
	@Override
	public String getName() {
		return "i18n";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render( InternalContextAdapter context, Writer writer, Node node ) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

		String text = "";
		//Locale locale = (Locale) context.get( "locale" );
		Dictionary dictionary = (Dictionary)context.get( "i18n" );
		
		try {
			if( node.jjtGetChild( 0 ) != null ) {
				text = String.valueOf( node.jjtGetChild( 0 ).value( context ) );
			}
			
		} catch( Exception e ) {
			/* ... And we're done */
		}
		
		if( dictionary != null ) {
			//writer.write( dictionary.get( locale.getLanguage(), text ) );
		} else {
			writer.write( text );
		}

		return true;
	}

}
