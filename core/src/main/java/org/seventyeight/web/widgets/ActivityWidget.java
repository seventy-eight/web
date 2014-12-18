package org.seventyeight.web.widgets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.model.*;
import org.seventyeight.web.nodes.Group;
import org.seventyeight.web.servlet.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class ActivityWidget extends Widget {

	private static Logger logger = LogManager.getLogger(ActivityWidget.class);
	
	private static final String ACLField = AbstractExtension.EXTENSIONS + "." + Descriptor.getJsonId(ACL.class.getName()) + ".read";

    @Override
    public Node getParent() {
        return null;  /* Implementation is a no op */
    }

    @Override
    public String getDisplayName() {
        return "Activity";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    @Override
    public String getName() {
        return "Activity widget";
    }

    public List<AbstractNode<?>> getActivities(Request request) throws NotFoundException, ItemInstantiationException {
    	// Groups
    	List<String> groupIds = Group.getGroupIds(request.getUser());
    	groupIds.add(request.getUser().getIdentifier());
    	groupIds.add(ACL.ALL);
    	
        MongoDocument sort = new MongoDocument().set( "updated", -1 );
        //MongoDBQuery query = new MongoDBQuery().notExists( "parent" ).in("ACL.read", groupIds);
        
        MongoDBQuery query = new MongoDBQuery().notExists( "parent" ).in(ACLField, groupIds);
        query.notExists( "relations." + Relation.BasicRelation.CREATED_FOR.getName() );
        query.notEquals( "visibility", AbstractNode.Visibility.INVISIBLE.name() );
        
        System.out.println("BUUM: " + query);
        
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, 0, 10, sort );
        List<AbstractNode<?>> nodes = new ArrayList<AbstractNode<?>>( docs.size() );

        Core core = request.getCore();

        for( MongoDocument d : docs ) {
            AbstractNode<?> n = core.getNodeById( this, d.getIdentifier() );
            //d.set( "badge", Core.getInstance().getTemplateManager().getRenderer( request ).renderObject( n, "badge.vm" ) );
            nodes.add( n );

            //nodes.add( new Activity( d ) );
        }

        return nodes;
    }
}
