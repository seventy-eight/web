package org.seventyeight.web;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.*;
import org.seventyeight.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

public class Server {

    private static Logger logger = LogManager.getLogger( Server.class );

    private boolean clean = false;

    private int port = 8080;

    private String warFileName;

    public Server setClean(boolean clean) {
        this.clean = clean;

        return this;
    }

    public Server setHttpPort(int port) {
        this.port = port;

        return this;
    }

    /*
    public Server setWarFileName(String warFileName) {
        this.warFileName = warFileName;
        return this;
    }
    */

    protected String getWarFileName() {
        return "cms.war";
    }

    /**
     * Default server main
     */
    public static void main( String[] args ) throws Exception {
        Server server = new Server();

        if( args.length > 0 ) {
            for( int i = 0 ; i < args.length ; ++i ) {
                //System.out.println( "[" + i + "] " + args[i] );
                if( args[i].equals( "--clean" ) ) {
                    server.setClean( true );
                } else if( args[i].equals( "--httpPort" ) ) {
                    i++;
                    server.setHttpPort( Integer.parseInt( args[i] ) );
                }
            }
        }

        server.init();
    }

    private void init() throws Exception {

        System.out.println( "Seventy Eight 0.1.0" );

        System.out.println( "Cleaning : " + clean );
        System.out.println( "Http port: " + port );

        File path = getHome();
        System.out.println( "Path: " + path.getAbsolutePath() );

        if( clean ) {
            System.out.println( "Cleaning " + path );
            FileUtils.deleteDirectory( path );
        }

        path.mkdirs();

        File warfile = new File( path, getWarFileName() );
        System.out.println( "Extracting war file: " + warfile );

        extractWar( path );
        FileUtilities.extractArchive( warfile, path );
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(8080);

        //SelectChannelConnector connector0 = new SelectChannelConnector();
        //ServerConnector connector0 = new ServerConnector(server);
        //connector0.setPort( port );
        //connector0.setMaxIdleTime( 30000 );
        //connector0.setRequestHeaderSize( 8192 );
        //connector0.setIdleTimeout(30000);

        //server.setConnectors( new Connector[] { connector0 } );
        
        
        ServletContextHandler handler = new ServletContextHandler();
        /*
        handler.setContextPath("/");
        handler.addServlet(Rest.class, "/*");
        handler.addEventListener(new CMSListener());
		*/

        //ServletContextHandler servletHandler = new ServletContextHandler( server, "/", true, false );
        //servletHandler.addServlet( Rest.class);
        WebAppContext context = new WebAppContext();
        context.setDescriptor( path + "/WEB-INF/web.xml" );
        context.setResourceBase( path.toString() );

        context.setContextPath( "/" );
        context.setParentLoaderPriority( true );

        context.setConfigurations( new Configuration[] {
                new AnnotationConfiguration(),
                new WebInfConfiguration(),
                new WebXmlConfiguration(),
                new MetaInfConfiguration(),
                //new FragmentConfiguration(),
                new EnvConfiguration(),
                new PlusConfiguration(),
                new JettyWebXmlConfiguration()
        } );

        server.setHandler( context );
        //server.setHandler(handler);

        server.start();
        server.join();
    }

    private void extractWar( File path ) {
        File jar = new File( Server.class.getProtectionDomain().getCodeSource().getLocation().getPath() );
        System.out.println( "Jar location: " + jar );

        FileUtilities.extractFile( jar, path, getWarFileName() );
    }

    private File getHome() {
        File def = new File( System.getProperty( "user.home" ), ".78" );
        try {
            String path = System.getenv( "seventy_eight" );
            if( path != null && path.length() > 0 ) {
                return new File( path );
            } else {
                return def;
            }
        } catch( Exception e ) {
            return def;
        }
    }

}
