package org.seventyeight.utils;

import org.apache.log4j.Logger;

/**
 * @author cwolfgang
 */
public class TimeUtils {

    private static Logger logger = Logger.getLogger( TimeUtils.class );

    private TimeUtils() {}

    public final static int SECONDS = 1000;
    public final static int MINUTES = SECONDS * 60;
    public final static int HOURS = MINUTES * 60;
    public final static int DAYS = HOURS * 24;
    public final static int WEEKS = DAYS * 7;

    public static class Time {
        public int millis = 0;
        public int seconds = 0;
        public int minutes = 0;
        public int hours = 0;
        public int days = 0;
        public int weeks = 0;
    }

    public static Time getTime( long millis ) {
        Time t = new Time();

        t.days = (int) (millis / DAYS);
        int rest = (int) (millis % DAYS);

        t.hours = rest / HOURS;
        rest = rest % HOURS;

        t.minutes = rest / MINUTES;
        rest = rest % MINUTES;

        t.seconds = rest / SECONDS;

        t.millis = rest % SECONDS;

        return t;
    }

    public static String getSmallTimeString( long millis ) {
        if( millis < MINUTES ) {
            return "Less than one minute";
        } else if( millis < HOURS ) {
            long m = millis / MINUTES;
            return m + ( m == 1 ? " minute" : " minutes" );
        } else if( millis < DAYS ) {
            long m = millis / HOURS;
            return m + ( m == 1 ? " hour" : " hours" );
        } else if( millis < WEEKS ) {
            long m = millis / DAYS;
            return m + ( m == 1 ? " day" : " days" );
        } else {
            long m = millis / WEEKS;
            return m + ( m == 1 ? " week" : " weeks" );
        }
    }

    public static String getTimeString( long millis ) {
        Time t = getTime( millis );

        StringBuilder b = new StringBuilder();

        b.append( t.days ).append( "d " ).append( t.hours ).append( "h " ).append( t.minutes ).append( "m " ).append( t.seconds ).append( "s " ).append( t.millis ).append( "ms" );

        return b.toString();
    }
}