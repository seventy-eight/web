package org.seventyeight.web.extensions;

import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.model.AbstractExtension;
import org.seventyeight.web.model.Node;

/**
 * @author cwolfgang
 */
public abstract class ResourceExtension extends NodeExtension {

    public ResourceExtension( Node parent, MongoDocument document ) {
        super( parent, document );
    }
}