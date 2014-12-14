package org.seventyeight.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StopWatch {

    private static final String SEP = System.getProperty( "line.separator" );

    private class Task {
        long totalNanos = 0;
        long startTime;
        String title;

        public Task( String title ) {
            this.title = title;
        }
    }

    public static final long PRECISION_SECOND = 1;
    public static final long PRECISION_MILLI  = 1000;
    public static final long PRECISION_MICRO  = 1000000;
    public static final long PRECISION_NANO   = 1000000000;

    private Map<String, Task> tasks = new HashMap<String, Task>();
    //private Task currentTask;

    public StopWatch() {
        this.initial = System.nanoTime();
    }

    private long initial = 0;

    public void start() {
        start( "N/A" );
    }

    public void start( String title ) {
        long now = System.nanoTime();

        if( !tasks.containsKey( title ) ) {
            tasks.put( title, new Task( title ) );
        }
        Task task = tasks.get( title );
        task.startTime = now;
    }

    public void stop( String title ) {
    	stop(title, true);
    }
    
    public void stop( String title, boolean nothrow ) {
        if( !tasks.containsKey( title ) ) {
        	if(!nothrow) {
        		throw new IllegalStateException( "Task " + title + " does not exist." );
        	} else {
        		return;
        	}
        }

        Task task = tasks.get( title );
        long now = System.nanoTime();
        task.totalNanos += (now-task.startTime);
        task.startTime = 0;
    }

    /**
     * Stop all tasks
     */
    public void stop() {
        for( String key : tasks.keySet() ) {
            Task task = tasks.get( key );
            if( task.startTime > 0 ) {
                stop( key );
            }
        }
    }

    private static final int MAX_TITLE_LENGTH = 32;
    private static final int MAX_PERCENTAGE_LENGTH = 10;
    private static final int MAX_TIME_LENGTH = 10;

    public String print( long precision ) {
        return print( precision, true );
    }

    public String print( long precision, boolean millis ) {
        StringBuilder sb = new StringBuilder();

        long now = System.nanoTime();
        long full = now - initial;

        //System.out.println( "NOW: " + now + ", INITIAL: " + initial + " = " + ( ( now - initial ) / PRECISION_NANO ) );

        long total = 0;
        if( tasks.size() > 0 ) {
            for( String title : tasks.keySet() ) {
                Task t = tasks.get( title );
                total += t.totalNanos;
            }

            sb.append( " Title                           %          Seconds" + SEP );
            sb.append( "-" + repeat( MAX_PERCENTAGE_LENGTH + MAX_TIME_LENGTH + MAX_TITLE_LENGTH, 0, "-" ) + SEP );

            for( String title : tasks.keySet() ) {
                Task t = tasks.get( title );
                Double p = Math.round( ( (double)t.totalNanos / total ) * 10000.0 ) / 100.0;
                sb.append( " " + t.title + spaces( MAX_TITLE_LENGTH, t.title.length() ) + p + "%" + spaces( MAX_PERCENTAGE_LENGTH, ( p + "" ).length() ) +
                           ( millis ? toMillis( t.totalNanos, precision ) : toSeconds( t.totalNanos, precision ) ) + SEP );
            }

            sb.append( "-" + repeat( MAX_PERCENTAGE_LENGTH + MAX_TIME_LENGTH + MAX_TITLE_LENGTH, 0, "-" ) + SEP );
        }

        sb.append( "Total time: " + ( millis ? toMillis( full, precision ) : toSeconds( full, precision ) ) + "s" );

        return sb.toString();
    }

    public String simplePrint( long precision ) {
        StringBuilder sb = new StringBuilder();

        long now = System.nanoTime();
        long full = now - initial;

        //System.out.println( "NOW: " + now + ", INITIAL: " + initial + " = " + ( ( now - initial ) / PRECISION_NANO ) );

        long total = 0;
        if( tasks.size() > 0 ) {
            for( String title : tasks.keySet() ) {
                Task t = tasks.get( title );
                total += t.totalNanos;
            }

            for( String title : tasks.keySet() ) {
                Task t = tasks.get( title );
                Double p = Math.round( ( (double)t.totalNanos / total ) * 10000.0 ) / 100.0;
            }
        }

        sb.append( "Total time : " + toSeconds( full, precision ) + "s" );

        return sb.toString();
    }

    private String spaces( int max, int length ) {
        return repeat( max, length, " " );
    }

    private String repeat( int max, int length, String chr ) {
        if( max > length ) {
            return new String( new char[max - length] ).replace( "\0", chr );
        } else {
            return " ";
        }
    }

    public void reset() {
        initial = System.nanoTime();
        tasks = new HashMap<String, Task>();
    }

    public double getSeconds() {
        long now = System.nanoTime();
        return ( (double) (now-initial) / 1000000000 );
    }

    public static double toSeconds( long time, long precision ) {
        return ( ( Math.round( ( (double) time / PRECISION_NANO ) * precision ) ) / (double) precision );
    }

    public static double toMillis( long time, long precision ) {
        return ( ( Math.round( ( (double) time / PRECISION_MICRO ) * precision ) ) / (double) precision );
    }

    public String toString() {
        return getSeconds() + "s";
    }
}
