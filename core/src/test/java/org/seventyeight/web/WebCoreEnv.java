package org.seventyeight.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.seventyeight.database.DatabaseException;
import org.seventyeight.database.mongodb.MongoDatabase;
import org.seventyeight.web.installers.GroupInstall;
import org.seventyeight.web.installers.UserInstall;
import org.seventyeight.web.model.RootNode;
import org.seventyeight.web.nodes.Group;
import org.seventyeight.web.nodes.User;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * @author cwolfgang
 */
public abstract class WebCoreEnv<C extends Core> implements TestRule {

    private static Logger logger = LogManager.getLogger( WebCoreEnv.class );

    protected File path;
    protected String databaseName;
    protected MongoDatabase db;
    protected C core;
    protected RootNode root;

    public WebCoreEnv( RootNode root, String databaseName ) {
        this.databaseName = databaseName;
    }

    protected abstract C getCore( RootNode root, File path, String databaseName ) throws CoreException;

    protected void before( RootNode root, File path ) throws UnknownHostException, CoreException {
        this.core = getCore( root, path, databaseName );
        db = core.getDatabase();
    }

    protected void after() {
        logger.debug( "Runner after!!!!!!!!!!!!!!!!!!!!!!!" );
        db.remove();
    }

    public Core getCore() {
        return core;
    }

    public RootNode getRoot() {
        return root;
    }

    public User createUser( String name ) throws DatabaseException {
        UserInstall ui = new UserInstall( core, name, name + "@seventyeight.org" );
        ui.install();
        return ui.getValue();
    }

    public Group createGroup( String name, User owner ) throws DatabaseException {
        GroupInstall i = new GroupInstall( core, name, owner );
        i.install();
        return i.getValue();
    }

    @Override
    public Statement apply( final Statement base, final Description description ) {
        try {
            path = File.createTempFile( "SEVENTYEIGHT", "web" );

            if( !path.delete() ) {
                System.out.println( path + " could not be deleted" );
            }

            if( !path.mkdir() ) {
                System.out.println( "DAMN!" );
            }
            System.out.println( "Path: " + path );
        } catch( IOException e ) {
            System.out.println( "Unable to create temporary path" );
        }

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Thread t = Thread.currentThread();
                String o = t.getName();
                t.setName( "Executing " + description.getDisplayName() );
                System.out.println( " ===== Setting up Seventy Eight Web Environment =====" );

                try {
                    before( root, path );
                    System.out.println( " ===== Running test: " + description.getDisplayName() + " =====" );
                    base.evaluate();
                } catch( Exception e ) {
                    //System.out.println( "Caught exception: " + e.getMessage() );
                    e.printStackTrace();
                } finally {
                    System.out.println( " ===== Tearing down Seventy Eight Web Environment =====" );
                    after();
                    t.setName( o );
                }
            }
        };
    }
}
