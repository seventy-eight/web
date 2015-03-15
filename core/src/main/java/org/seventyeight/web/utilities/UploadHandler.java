package org.seventyeight.web.utilities;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.structure.Tuple;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.nodes.FileResource;
import org.seventyeight.web.servlet.Request;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author cwolfgang
 */
public class UploadHandler implements Runnable {

    private static Logger logger = LogManager.getLogger( UploadHandler.class );

    private Core core;

    private AsyncContext context;
    private String pathPrefix;
    private Uploader uploader;
    private Filenamer filenamer;

    public UploadHandler( Core core, AsyncContext context, String pathPrefix, Uploader uploader, Filenamer filenamer ) {
        this.context = context;
        this.pathPrefix = pathPrefix;
        this.uploader = uploader;
        this.filenamer = filenamer;

        this.core = core;
    }

    @Override
    public void run() {
        logger.debug( "STARTING" );
        Request request = (Request) context.getRequest();
        List<Object> items = null;
        try {
            items = uploader.getItems( request );
        } catch( FileUploadException e ) {
            logger.error( e.getMessage() );
            return;
        }

        for( Object item : items ) {
            try {
                if( uploader.isValid( item ) ) {
                    String filename = uploader.getUploadFilename( item );
                    UploadFile uf = filenamer.getUploadDestination( pathPrefix, filename, core.getUploadPath(), "");

                    uploader.write( item, uf.file );
                    FileResource.FileDescriptor descriptor = core.getDescriptor( FileResource.class );

                    FileResource fr = null;
                    try {
                        //fr = FileResource.create( filename );
                        fr = descriptor.newInstance( request, core.getRoot() );
                        fr.setPath( uf.relativePath );
                        fr.setFilename( filename );
                        fr.setFileExtension( uf.extension );
                        fr.setSize( uploader.getSize( item ) );
                        fr.save();
                    } catch( ItemInstantiationException e ) {
                        logger.log( Level.ERROR, "Failed to create file resource for {}", item, e );
                        // TODO Delete uploaded file?
                    }
                }
            } catch( Exception e ) {
                logger.log( Level.ERROR, "Unable to store {}", item, e );
            }
        }

        logger.debug( "STOPPED" );
    }

    public static final CommonsUpload commonsUploader = new CommonsUpload();
    public static final Filenamer DefaultFilenamer = new DefaultFilenamer();

    public static class UploadFile {
        public File relativeFile;
        public File file;
        public String relativePath;
        public String extension;
        public String session;
    }
    
    public static UploadFile generateSessionFilename(String filename, String pathPrefix, File uploadPath, String session) {
        Date now = new Date();
        int mid = filename.lastIndexOf( "." );
        String fname = filename;
        String ext = null;
        if( mid > -1 ) {
            ext = filename.substring( mid + 1, filename.length() );
            fname = filename.substring( 0, mid );
        }

        String relativePathString = pathPrefix + "/" + formatYear.format( now ) + "/" + formatMonth.format( now ) + "/" + session;

        File path = new File( uploadPath, relativePathString );
        File relativeFile = new File( relativePathString, filename );
        logger.debug( "Trying to create path " + path );
        path.mkdirs();
        File file = new File( path, filename );
        int cnt = 0;
        while( file.exists() ) {
            file = new File( path, fname + "_" + cnt + ( ext != null ? "." + ext : "" ) );
            cnt++;
        }

        logger.debug( "FILE: " + file.getAbsolutePath() );
        logger.debug( "FILE: " + relativeFile );

        UploadFile uf = new UploadFile();
        uf.file = file;
        uf.relativeFile = relativeFile;
        uf.relativePath = relativePathString;
        uf.extension = ext;

        return uf;
    }

    /**
     * @param filename
     * @param pathPrefix
     * @return First is a {@link java.io.File} relative to the context path, and the second is an absolute file.
     */
    public static UploadFile generateFile( String filename, String pathPrefix, File uploadPath ) {
        Date now = new Date();
        int mid = filename.lastIndexOf( "." );
        String fname = filename;
        String ext = null;
        if( mid > -1 ) {
            ext = filename.substring( mid + 1, filename.length() );
            fname = filename.substring( 0, mid );
        }

        String relativePathString = pathPrefix + "/" + formatYear.format( now ) + "/" + formatMonth.format( now ) + "/" + ext;

        File path = new File( uploadPath, relativePathString );
        File relativeFile = new File( relativePathString, filename );
        logger.debug( "Trying to create path " + path );
        path.mkdirs();
        File file = new File( path, filename );
        int cnt = 0;
        while( file.exists() ) {
            file = new File( path, fname + "_" + cnt + ( ext != null ? "." + ext : "" ) );
            cnt++;
        }

        logger.debug( "FILE: " + file.getAbsolutePath() );
        logger.debug( "FILE: " + relativeFile );

        UploadFile uf = new UploadFile();
        uf.file = file;
        uf.relativeFile = relativeFile;
        uf.relativePath = relativePathString;
        uf.extension = ext;

        return uf;
    }


    public static interface Filenamer {
        public UploadFile getUploadDestination(String pathPrefix, String filename, File uploadPath, String uploadSession);
    }

    public static class DefaultFilenamer implements Filenamer {

        @Override
        public UploadFile getUploadDestination( String pathPrefix, String filename, File uploadPath, String uploadSession ) {
            return generateSessionFilename( filename, pathPrefix, uploadPath, uploadSession);
        }
    }

    private static SimpleDateFormat formatYear = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat formatMonth = new SimpleDateFormat( "MM" );


    /**
     * @param filename
     * @param pathPrefix
     * @return First is a {@link java.io.File} relative to the context path, and the second is an absolute file.
     */
    public static String getPathString( String pathPrefix, String filename ) {
        Date now = new Date();
        int mid = filename.lastIndexOf( "." );
        String fname = filename;
        String ext = null;
        if( mid > -1 ) {
            ext = filename.substring( mid + 1, filename.length() );
            fname = filename.substring( 0, mid );
        }

        return pathPrefix + "/" + formatYear.format( now ) + "/" + formatMonth.format( now ) + "/" + ext;
    }


    public static interface Uploader<ITEM> {
        public List<ITEM> getItems( HttpServletRequest request ) throws FileUploadException;
        public boolean isValid( ITEM item );
        public String getUploadFilename( ITEM item );
        public long getSize( ITEM item );
        public void write( ITEM item, File destination ) throws Exception;
    }

    public static class CommonsUpload implements Uploader<FileItem> {

        private static FileItemFactory factory = new DiskFileItemFactory();

        @Override
        public List<FileItem> getItems( HttpServletRequest request ) throws FileUploadException {
            ServletFileUpload upload = new ServletFileUpload( factory );
            List<FileItem> items = upload.parseRequest( request );
            return items;
        }

        @Override
        public boolean isValid( FileItem item ) {
            if( item instanceof FileItem && !((FileItem)item).isFormField() ) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getUploadFilename( FileItem item ) {
            return item.getName();
        }

        @Override
        public void write( FileItem item, File destination ) throws Exception {
            item.write( destination );
        }

        @Override
        public long getSize( FileItem item ) {
            return item.getSize();
        }
    }
}
