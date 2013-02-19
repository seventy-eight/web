package org.seventyeight.web.model;

import org.seventyeight.web.User;

/**
 * @author cwolfgang
 *         Date: 28-01-13
 *         Time: 13:40
 */
public interface Authorizer {
    public enum Authorization {
        NONE,
        VIEW,
        MODERATE;

        public static Authorization get( boolean isPost ) {
            if( isPost ) {
                return MODERATE;
            } else {
                return VIEW;
            }
        }
    }

    public Authorization getAuthorization( User user ) throws ItemInstantiationException;
}
