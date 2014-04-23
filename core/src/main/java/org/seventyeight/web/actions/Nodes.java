package org.seventyeight.web.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class Nodes implements Node {

    private static Logger logger = LogManager.getLogger( Nodes.class );

    public List<Node> getNodes( int offset, int number ) {
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( new MongoDBQuery(), offset, number );

        List<Node> nodes = new ArrayList<Node>( docs.size() );

        for( MongoDocument doc : docs ) {
            try {
                nodes.add( (Node) Core.getInstance().getNode( this, doc ) );
            } catch( ItemInstantiationException e ) {
                logger.warn( e.getMessage() );
            }
        }

        return nodes;
    }

    @Override
    public Node getParent() {
        return Core.getInstance();
    }

    @Override
    public String getDisplayName() {
        return "Nodes";
    }

    @Override
    public String getMainTemplate() {
        return "org/seventyeight/web/main.vm";
    }
}
