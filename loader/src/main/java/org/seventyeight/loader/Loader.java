package org.seventyeight.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Loader extends HashMap<Class<? extends Annotation>, Handler> {

    private static Logger logger = LogManager.getLogger( Loader.class );

	protected ClassLoader cl;

	public Loader( ClassLoader cl ) {
		this.cl = cl;
	}
	
	public void load( File path ) throws IOException {
        logger.debug( "Adding {} to class loader", path );

		/* Add URL to */
		cl.addUrls( new URL[] { path.toURI().toURL() } );
		
		/* Find classes */
		List<String> classes = getClasses( path, "" );
		
		for( String className : classes ) {
			try {
				load( className );
			} catch( ClassNotFoundException e ) {
				System.out.println( e.getMessage() );
			}
		}
	}

    /**
     * @deprecated
     */
	public void load( File path, String className ) throws IOException, ClassNotFoundException {
        load( className );
    }

    public void load( String className ) throws IOException, ClassNotFoundException {
        Class<?> eclass = Class.forName( className, true, cl );
        load( eclass );
    }

    public void load( Class<?> eclass ) throws IOException, ClassNotFoundException {
		Annotation[] as = eclass.getAnnotations();
		for( Annotation a : as ) {
			if( this.containsKey( a.annotationType() ) ) {
				Handler h = this.get( a.annotationType() );
				try {
					h.handle( a, eclass );
				} catch( Exception e ) {
					System.out.println( e.getMessage() );
				}
			}
		}
	}
	
	public List<String> getClasses( File root, String path ) {
        File directory = new File( root, path );
        logger.debug( "Loading " + directory.getAbsoluteFile() );

		File[] dirs = directory.listFiles( new DirFilter() );
		File[] classFiles = directory.listFiles( new ClassFilter() );

        logger.debug( "------>->" + classFiles );
        logger.debug( "------>->" + Arrays.asList( classFiles ) );

		
		List<String> classes = new ArrayList<String>();
		
		for( File classFile : classFiles ) {
			classes.add( path.replace( "/", "." ) + classFile.getName().substring( 0, ( classFile.getName().length() - 6 ) ) );
		}
		
		for( File dir : dirs ) {
			classes.addAll( getClasses( root, path + dir.getName() + "/" ) );
		}
		
		return classes;
	}
	
	
	private class ClassFilter implements FilenameFilter {
		public boolean accept( File f, String n ) {
			return n.endsWith( ".class" );
		}
	}
	
	private class DirFilter implements FileFilter {

		public boolean accept( File pathname ) {
			if( pathname.isDirectory() ) {
				return !pathname.getName().matches( "^[\\.]{1,2}$" );
			} else {
				return false;
			}
		}

	}
}
