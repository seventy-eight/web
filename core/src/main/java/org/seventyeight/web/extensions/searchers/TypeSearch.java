package org.seventyeight.web.extensions.searchers;

import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.Searchable;

/**
 * @author cwolfgang
 */
public class TypeSearch extends Searchable {
    @Override
    public Class<? extends Node> getClazz() {
        return Node.class;
    }

    @Override
    public String getName() {
        return "Type search";
    }

    @Override
    public String getMethodName() {
        return "type";
    }

    @Override
    public void search( MongoDBQuery query, String term ) {
        query.regex( "type", "(?i)" + term );
    }
}