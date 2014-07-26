package org.seventyeight.web.authorization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.ExtensionGroup;
import org.seventyeight.web.model.*;
import org.seventyeight.web.nodes.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author cwolfgang
 *
 * {
 *  read: [],
 *  write: [],
 *  admin: []
 * }
 *
 */
public class BasicResourceBasedSecurity extends ACL<BasicResourceBasedSecurity> {

    private static Logger logger = LogManager.getLogger( BasicResourceBasedSecurity.class );

    private Permission permission = null;

    public BasicResourceBasedSecurity( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }

    public List<Authorizable> getAuthorized(String permission) {
        return getAuthorized( Permission.valueOf( permission ) );
    }

    @Override
    public List<Authorizable> getAuthorized( Permission permission ) {
        List<Authorizable> list = new ArrayList<Authorizable>(  );
        List<String> l = document.get( permission.getDbname() );
        for( String id : l ) {
            try {
                list.add( core.<Authorizable>getNodeById( getParent(), id ) );
            } catch( Exception e ) {
                logger.log( Level.ERROR, "Unable to get " + id, e );
            }
        }

        return list;
    }

    public boolean hasAccess( User user ) {
        List<Authorizable> authorized = getAuthorized( Permission.READ );
        logger.debug( "Authorized: {}", authorized );
        for( Authorizable auth : authorized ) {
            try {
                if( auth.isMember( user ) ) {
                    return true;
                }
            } catch( Exception e ) {
                logger.log( Level.WARN, "Unable to get authorized", e );
            }
        }

        return false;
    }

    @Override
    public Permission getPermission( User user ) {
        if( permission == null ) {
            permission = _getPermission( user );
        }
        return permission;
    }

    private boolean isOwner(User user) {
        if( getParent() instanceof Ownable ) {
            logger.debug( "Parent, " + getParent() + ", is ownable." );
            if( ( (Ownable) getParent() ).isOwner( user ) ) {
                return true;
            }
        }

        return false;
    }

    private Permission _getPermission( User user ) {
        /* Owner? */
        if( isOwner( user ) ) {
            return Permission.ADMIN;
        }

        /* Write access */
        if( hasAccess( user ) ) {
            return Permission.WRITE;
        }

        logger.debug( user + " has no permissions at all" );
        return Permission.NONE;
    }

    @Override
    public String toString() {
        return "BRBS permission, " + permission;
    }

    @Override
    public void updateNode( JsonObject jsonData ) {
        logger.debug( "Updating {}, {}", this, jsonData );
        // Access
        if(jsonData != null) {
            // access
            JsonObject accessArray = jsonData.getAsJsonObject( "access" );
            logger.debug( "ACCESS IS {}", accessArray );

            if(accessArray.getAsJsonArray( "read" ) != null) {
                List<String> reads = new ArrayList<String>( accessArray.getAsJsonArray( "read" ).size() );
                for( JsonElement k : accessArray.getAsJsonArray( "read" )) {
                    reads.add( k.getAsString() );
                }

                document.set( "read", reads );
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Basic resource based security";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    public static class BasicResourceBasedSecurityDescriptor extends ACLDescriptor<BasicResourceBasedSecurity> {

        @Override
        public String getDisplayName() {
            return "Basic resource based security";
        }

        @Override
        public List<ExtensionGroup> getApplicableExtensions() {
            return Collections.emptyList();
        }
    }
}
