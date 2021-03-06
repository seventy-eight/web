package org.seventyeight.web.extensions;

import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.AbstractExtension;
import org.seventyeight.web.model.Node;

/**
 * @author cwolfgang
 */
public abstract class ResourceExtension<T extends NodeExtension<T>> extends NodeExtension<T> {

    public ResourceExtension( Core core, Node parent, MongoDocument document ) {
        super( core, parent, document );
    }
}
