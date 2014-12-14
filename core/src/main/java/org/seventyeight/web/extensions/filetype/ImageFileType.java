package org.seventyeight.web.extensions.filetype;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class ImageFileType extends FileType {

    private static Logger logger = LogManager.getLogger( ImageFileType.class );

    private Integer thumbWidth;

    private Integer thumbHeight;

    @Override
    public List<String> getFileExtensions() {
        List<String> exts = new ArrayList<String>( 2 );
        exts.add( "jpg" );
        exts.add( "png" );
        return exts;
    }

    //@Override
    public void save( Request request ) {
        thumbWidth = Integer.parseInt( request.getValue( "twidth", "80" ) );
        thumbHeight = Integer.parseInt( request.getValue( "theight", "120" ) );
    }
}


