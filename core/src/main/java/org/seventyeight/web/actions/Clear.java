package org.seventyeight.web.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.utils.DeleteMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.authorization.AccessControlled;
import org.seventyeight.web.model.Comment;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

public class Clear implements Node, AccessControlled {

	private static Logger logger = LogManager.getLogger(Clear.class);

	@Override
	public String getMainTemplate() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return "Clear";
	}
	
	@DeleteMethod
	public WebResponse doIndex(Request request) {
		logger.warn("Truncating databases");
		
		MongoDBCollection.get(Core.NODES_COLLECTION_NAME).truncate();
		MongoDBCollection.get(Core.NUMBERS_COLLECTION).truncate();
		MongoDBCollection.get(Core.DESCRIPTOR_COLLECTION_NAME).truncate();
		MongoDBCollection.get(Comment.COMMENTS_COLLECTION).truncate();
		
		return new WebResponse();
	}

	@Override
	public ACL getACL() {
		return ACL.ALL_ACCESS;
	}

	@Override
	public Node getParent() {
		return null;
	}

}
