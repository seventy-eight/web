package org.seventyeight.web.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.web.Core;
import org.seventyeight.web.CoreException;
import org.seventyeight.web.handlers.template.TemplateException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * @author cwolfgang
 */
public class WebResponse extends HttpServletResponseWrapper {

    private static Logger logger = LogManager.getLogger( Response.class );

    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1
    // week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";

    public enum RenderType {
        MAIN,
        NONE
    }

    private RenderType renderType = RenderType.MAIN;

    public enum ContentType {
    	JSON("application/json");
    	
    	private String type;
    	ContentType(String type) {
    		this.type = type;
    	}
    	
    	@Override
    	public String toString() {
    		return type;
    	}
    }

    public WebResponse( HttpServletResponse response ) {
        super( response );
    }


    public void deliverFile( Request request, File file /*, String contentType */, boolean content ) throws IOException {

        logger.fatal( "FILE IS " + file );

        // Prepare some variables. The ETag is an unique identifier of the file.
        String fileName = file.getName();
        long length = file.length();
        long lastModified = file.lastModified();
        String eTag = fileName + "_" + length + "_" + lastModified;

        // Validate request headers for caching
        // ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return
        // 304.
        String ifNoneMatch = request.getHeader( "If-None-Match" );
        if( ifNoneMatch != null && matches( ifNoneMatch, eTag ) ) {
            setHeader( "ETag", eTag ); // Required in 304.
            sendError( HttpServletResponse.SC_NOT_MODIFIED );
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so,
        // then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader( "If-Modified-Since" );
        if( ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified ) {
            setHeader( "ETag", eTag ); // Required in 304.
            sendError( HttpServletResponse.SC_NOT_MODIFIED );
            return;
        }

        // Validate request headers for resume
        // ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader( "If-Match" );
        if( ifMatch != null && !matches( ifMatch, eTag ) ) {
            sendError( HttpServletResponse.SC_PRECONDITION_FAILED );
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If
        // not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader( "If-Unmodified-Since" );
        if( ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified ) {
            sendError( HttpServletResponse.SC_PRECONDITION_FAILED );
            return;
        }

        // Validate and process range
        // -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range( 0, length - 1, length );
        List<Range> ranges = new ArrayList<Range>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader( "Range" );
        if( range != null ) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not,
            // then return 416.
            if( !range.matches( "^bytes=\\d*-\\d*(,\\d*-\\d*)*$" ) ) {
                setHeader( "Content-Range", "bytes */" + length ); // Required
                // in
                // 416.
                sendError( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE );
                return;
            }

            // If-Range header should either match ETag or be greater then
            // LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader( "If-Range" );
            if( ifRange != null && !ifRange.equals( eTag ) ) {
                try {
                    long ifRangeTime = request.getDateHeader( "If-Range" ); // Throws
                    // IAE
                    // if
                    // invalid.
                    if( ifRangeTime != -1 && ifRangeTime + 1000 < lastModified ) {
                        ranges.add( full );
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add( full );
                }
            }

            // If any valid If-Range header, then process each part of byte
            // range.
            if( ranges.isEmpty() ) {
                for( String part : range.substring( 6 ).split( "," ) ) {
                    // Assuming a file with length of 100, the following
                    // examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20
                    // (length-20=80 to length=100).
                    long start = sublong( part, 0, part.indexOf( "-" ) );
                    long end = sublong( part, part.indexOf( "-" ) + 1, part.length() );

                    if( start == -1 ) {
                        start = length - end;
                        end = length - 1;
                    } else if( end == -1 || end > length - 1 ) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then
                    // return 416.
                    if( start > end ) {
                        setHeader( "Content-Range", "bytes */" + length ); // Required
                        // in
                        // 416.
                        sendError( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE );
                        return;
                    }

                    // Add range.
                    ranges.add( new Range( start, end, length ) );
                }
            }
        }

        // Prepare and initialize response
        // --------------------------------------------------------

        // Get content type by file name and set default GZIP support and
        // content disposition.
        //String contentType = new MimetypesFileTypeMap().getContentType(fileName);
        //InputStream is = new BufferedInputStream(new FileInputStream(file));
        //String contentType = URLConnection.guessContentTypeFromStream(is);
        //is.close();
        String contentType = getMimeType2( file );
        //String contentType = URLConnection.guessContentTypeFromName( file.getName() );
        logger.debug( "MIME TYPE: " + contentType );
        boolean acceptsGzip = false;
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see:
        // http://www.w3schools.com/media/media_mimeref.asp
        // To save new content types, save new mime-mapping entry in web.xml.
        if( contentType == null ) {
            contentType = "application/octet-stream";
        }

        // If content type is text, then determine whether GZIP content encoding
        // is supported by
        // the browser and expand content type with the one and right character
        // encoding.
        if( contentType.startsWith( "text" ) ) {
            String acceptEncoding = request.getHeader( "Accept-Encoding" );
            acceptsGzip = acceptEncoding != null && accepts( acceptEncoding, "gzip" );
            contentType += ";charset=UTF-8";
        }

        // Else, expect for images, determine content disposition. If content
        // type is supported by
        // the browser, then set to inline, else attachment which will pop a
        // 'save as' dialogue.
        else if( !contentType.startsWith( "image" ) ) {
            String accept = request.getHeader( "Accept" );
            disposition = accept != null && accepts( accept, contentType ) ? "inline" : "attachment";
        }

        // Initialize response.
        reset();
        setBufferSize( DEFAULT_BUFFER_SIZE );
        setHeader( "Content-Disposition", disposition + ";filename=\"" + fileName + "\"" );
        setHeader( "Accept-Ranges", "bytes" );
        setHeader( "ETag", eTag );
        setDateHeader( "Last-Modified", lastModified );
        setDateHeader( "Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME );

        // Send requested file (part(s)) to client
        // ------------------------------------------------

        // Prepare streams.
        RandomAccessFile input = null;
        OutputStream output = null;

        try {
            // Open streams.
            input = new RandomAccessFile( file, "r" );
            output = getOutputStream();

            if( ranges.isEmpty() || ranges.get( 0 ) == full ) {

                // Return full file.
                Range r = full;
                setContentType( contentType );
                setHeader( "Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total );

                if( content ) {
                    if( acceptsGzip ) {
                        // The browser accepts GZIP, so GZIP the content.
                        setHeader( "Content-Encoding", "gzip" );
                        output = new GZIPOutputStream( output, DEFAULT_BUFFER_SIZE );
                    } else {
                        // Content length is not directly predictable in case of
                        // GZIP.
                        // So only save it if there is no means of GZIP, else
                        // browser will hang.
                        setHeader( "Content-Length", String.valueOf( r.length ) );
                    }

                    // Copy full range.
                    copy( input, output, r.start, r.length );
                }

            } else if( ranges.size() == 1 ) {

                // Return single part of file.
                Range r = ranges.get( 0 );
                setContentType( contentType );
                setHeader( "Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total );
                setHeader( "Content-Length", String.valueOf( r.length ) );
                setStatus( HttpServletResponse.SC_PARTIAL_CONTENT ); // 206.

                if( content ) {
                    // Copy single part range.
                    copy( input, output, r.start, r.length );
                }

            } else {

                // Return multiple parts of file.
                setContentType( "multipart/byteranges; boundary=" + MULTIPART_BOUNDARY );
                setStatus( HttpServletResponse.SC_PARTIAL_CONTENT ); // 206.

                if( content ) {
                    // Cast back to ServletOutputStream to get the easy println
                    // methods.
                    ServletOutputStream sos = (ServletOutputStream) output;

                    // Copy multi part range.
                    for( Range r : ranges ) {
                        // Add multipart boundary and header fields for every
                        // range.
                        sos.println();
                        sos.println( "--" + MULTIPART_BOUNDARY );
                        sos.println( "Content-Type: " + contentType );
                        sos.println( "Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total );

                        // Copy single part range of multi part range.
                        copy( input, output, r.start, r.length );
                    }

                    // End with multipart boundary.
                    sos.println();
                    sos.println( "--" + MULTIPART_BOUNDARY + "--" );
                }
            }
        } finally {
            // Gently close streams.
            close( output );
            close( input );
        }
    }



    // Helpers (can be refactored to public utility class)
    // ----------------------------------------

    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader
     *            The accept header.
     * @param toAccept
     *            The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts( String acceptHeader, String toAccept ) {
        String[] acceptValues = acceptHeader.split( "\\s*(,|;)\\s*" );
        Arrays.sort( acceptValues );
        return Arrays.binarySearch( acceptValues, toAccept ) > -1 || Arrays.binarySearch( acceptValues, toAccept.replaceAll( "/.*$", "/*" ) ) > -1 || Arrays.binarySearch( acceptValues, "*/*" ) > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     *
     * @param matchHeader
     *            The match header.
     * @param toMatch
     *            The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches( String matchHeader, String toMatch ) {
        String[] matchValues = matchHeader.split( "\\s*,\\s*" );
        Arrays.sort( matchValues );
        return Arrays.binarySearch( matchValues, toMatch ) > -1 || Arrays.binarySearch( matchValues, "*" ) > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index
     * to the given end index as a long. If the substring is empty, then -1 will
     * be returned
     *
     * @param value
     *            The string value to return a substring as long for.
     * @param beginIndex
     *            The begin index of the substring to be returned as long.
     * @param endIndex
     *            The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring
     *         is empty.
     */
    private static long sublong( String value, int beginIndex, int endIndex ) {
        String substring = value.substring( beginIndex, endIndex );
        return ( substring.length() > 0 ) ? Long.parseLong( substring ) : -1;
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param input
     *            The input to copy the given range to the given output for.
     * @param output
     *            The output to copy the given range from the given input for.
     * @param start
     *            Start of the byte range.
     * @param length
     *            Length of the byte range.
     * @throws java.io.IOException
     *             If something fails at I/O level.
     */
    private static void copy( RandomAccessFile input, OutputStream output, long start, long length ) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;

        if( input.length() == length ) {
            // Write full range.
            while( ( read = input.read( buffer ) ) > 0 ) {
                output.write( buffer, 0, read );
            }
        } else {
            // Write partial range.
            input.seek( start );
            long toRead = length;

            while( ( read = input.read( buffer ) ) > 0 ) {
                if( ( toRead -= read ) > 0 ) {
                    output.write( buffer, 0, read );
                } else {
                    output.write( buffer, 0, (int) toRead + read );
                    break;
                }
            }
        }
    }

    /**
     * Close the given resource.
     *
     * @param resource
     *            The resource to be closed.
     */
    private static void close( Closeable resource ) {
        if( resource != null ) {
            try {
                resource.close();
            } catch (IOException ignore) {
                // Ignore IOException. If you want to handle this anyway, it
                // might be useful to know
                // that this will generally only be thrown when the client
                // aborted the request.
            }
        }
    }

    public static Map<String, String> mimeTypes = new HashMap<String, String>(  );

    static {
        mimeTypes.put( "css", "text/css" );
    }

    public static String getMimeType( File file ) throws IOException {
        InputStream is = new BufferedInputStream( new FileInputStream(file ) );
        String mimeType = URLConnection.guessContentTypeFromStream( is );
        is.close();
        return mimeType;
    }

    public static String getMimeType2( File file ) throws IOException {
        InputStream is = new BufferedInputStream( new FileInputStream(file ) );
        String mimeType = URLConnection.guessContentTypeFromStream( is );
        is.close();

        if( mimeType == null ) {
            int i = file.getName().lastIndexOf( "." );
            if( i > -1 ) {
                String s = file.getName().substring( (i+1), file.getName().length() );
                mimeType = mimeTypes.get( s );
            }
        }

        return mimeType;
    }

    // Inner classes
    // ------------------------------------------------------------------------------

    /**
     * This class represents a byte range.
     */
    protected class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start
         *            Start of the byte range.
         * @param end
         *            End of the byte range.
         * @param total
         *            Total length of the byte source.
         */
        public Range( long start, long end, long total ) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

    }

    public static final HttpCode BAD_REQUEST_400 = new HttpCode( 400, "Bad request", "$request.getRequestURI()" );

    public static final HttpCode NOT_FOUND_404 = new HttpCode( 404, "Page not found", "$request.getRequestURI()" );
    public static final HttpCode NOT_ACCEPTABLE_406 = new HttpCode( 406, "Not acceptable", "Not accepted" );

    public static final HttpCode INTERNAL_SERVER_ERROR_500 = new HttpCode( 500, "Internal server error", "Error" );

    private static Map<Integer, HttpCode> errors = new HashMap<Integer, HttpCode>(  );

    static {
        errors.put( 400, BAD_REQUEST_400 );
        errors.put( 404, NOT_FOUND_404 );
        errors.put( 406, NOT_ACCEPTABLE_406 );

        errors.put( 500, INTERNAL_SERVER_ERROR_500 );
    }

    public void renderError( Request request, CoreException e ) throws IOException {
        logger.debug( "Render error " + e );
        try {
            renderError( request, this, e );
        } catch( TemplateException te ) {
            try {
                INTERNAL_SERVER_ERROR_500.render( request, this, te );
            } catch( TemplateException e1 ) {
                logger.error( te );
            }
        }
    }

    public void renderError( Request request, WebResponse response, Exception e ) throws TemplateException, IOException {
        if( e instanceof CoreException ) {
            request.getContext().put( "header", ( (CoreException) e ).getHeader() );
            request.getContext().put( "code", ( (CoreException) e ).getCode() );
        } else {
            request.getContext().put( "header", "Bad request" );
            request.getContext().put( "code", 400 );
        }

        request.getContext().put( "exception", e );
        request.getContext().put( "request", request );

        Core core = request.getCore();

        response.getWriter().write( core.getTemplateManager().getRenderer( request ).render( "org/seventyeight/web/http/error.vm" ) );
    }

    public static class HttpCode {
        private int code;
        private String errorHeader;
        private String defaultMessage;

        public HttpCode( int code, String errorHeader, String defaultMessage ) {
            this.code = code;
            this.errorHeader = errorHeader;
            this.defaultMessage = defaultMessage;
        }

        public void render( Request request, WebResponse response ) throws TemplateException, IOException {
            render( request, response, defaultMessage );
        }

        public void render( Request request, WebResponse response, Exception e ) throws TemplateException, IOException {
            if( e != null ) {
                if( e instanceof CoreException ) {
                    this.errorHeader = ( (CoreException) e ).getHeader();
                }
                render( request, response, e.getMessage() );
            } else {
                render( request, response, defaultMessage );
            }
        }

        public void render( Request request, WebResponse response, String message ) throws TemplateException, IOException {
            request.getContext().put( "header", errorHeader );
            request.getContext().put( "message", message );
            request.getContext().put( "code", code );
            request.getContext().put( "request", request );

            Core core = request.getCore();

            response.getWriter().write( core.getTemplateManager().getRenderer( request ).render( "org/seventyeight/web/http/error.vm" ) );
        }
    }

    public RenderType getRenderType() {
        return renderType;
    }

    public boolean isRenderingMain() {
        return renderType == RenderType.MAIN;
    }

    public void setRenderType( RenderType renderType ) {
        this.renderType = renderType;
    }

}
