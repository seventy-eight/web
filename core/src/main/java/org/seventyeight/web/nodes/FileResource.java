package org.seventyeight.web.nodes;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.Date;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.filetype.DefaultFileType;
import org.seventyeight.web.extensions.filetype.FileType;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.FileResponse;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cwolfgang
 */
public class FileResource extends UploadableNode<FileResource> {

    private static Logger logger = LogManager.getLogger( FileResource.class );

    private static SimpleDateFormat formatYear = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat formatMonth = new SimpleDateFormat( "MM" );

    public FileResource( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    @Override
    public String getPortrait() {
        return getUrl() + "file";
    }

    @Override
    public String getDisplayName() {
        return getTitle();
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    public FileType getFileType() {
        return getDescriptor().getDescriptor( getFileExtension() );
    }

    @Override
    public FileDescriptor getDescriptor() {
        return core.getDescriptor( FileResource.class );
    }

    @GetMethod
    public WebResponse doFile( Request request ) throws IOException {
        //response.setRenderType( Response.RenderType.NONE );
        //response.deliverFile( request, getFile(), true );
    	return new FileResponse(getFile());
    }

    public static String getUploadDestination( Request request ) {
        Date now = new Date();
        String strpath = request.getUser().getUsername() + "/" + formatYear.format( now ) + "/" + formatMonth.format( now );

        //return new File( Core.getInstance().getUploadPath(), strpath );
        return strpath;
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
    }

    /*
    public static FileResource upload( Request request, Response response ) throws Exception {
        List<FileResource> fileResources = ServletUtils.upload2( request, Core.getInstance().getUploadPath(), getUploadDestination( request ), false, 1 );
        if( fileResources.size() > 0 ) {
            return fileResources.get( 0 );
        } else {
            return null;
        }
    }
    */

    public static FileResource getFileByFilename( Core core, Node parent, String filename ) throws ItemInstantiationException {
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( new MongoDBQuery().is( "filename", filename ), 0, 1 );

        if( docs != null && !docs.isEmpty() ) {
            return new FileResource( core, parent, docs.get( 0 ) );
        } else {
            throw new ItemInstantiationException( "The file " + filename + " not found" );
        }
    }

    public Class<?> getFileTypeClass() {
        List<Descriptor> descriptors = core.getExtensionDescriptors( FileType.class );

        logger.debug( "List is " + descriptors );

        return null;
    }
    
    public void setUploadSession(String uploadSession) {
    	document.set("uploadSession", uploadSession);
    }

    public static class FileDescriptor extends UploadableDescriptor<FileResource> {

        private ConcurrentHashMap<String, FileType> fileTypes = new ConcurrentHashMap<String, FileType>(  );

        private DefaultFileType dft = new DefaultFileType();

        public FileDescriptor( Node parent ) {
            super( parent );
        }

        @Override
        public String getType() {
            return "file";
        }
        
        @Override
		public String getUrlName() {
			return "files";
		}

        @Override
        public String getDisplayName() {
            return "File";
        }

        public void addFileType( FileType fileType ) {
            logger.debug("Adding {} for {}", fileType, fileType.getFileExtensions() );

            for( String ext : fileType.getFileExtensions() ) {
                fileTypes.put( ext, fileType );
            }
        }

        public FileType getDescriptor( String extension ) {
            FileType f = fileTypes.get( extension.toLowerCase() );
            if(f != null) {
                return f;
            } else {
                return dft;
            }
        }
    }
}
