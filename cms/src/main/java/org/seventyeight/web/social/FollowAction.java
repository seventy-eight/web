package org.seventyeight.web.social;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.DatabaseException;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.DeleteMethod;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.ExtensionGroup;
import org.seventyeight.web.model.Action;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import com.google.gson.JsonObject;

/**
 * A list of 'things' the parent follows.
 */
public class FollowAction extends Action<FollowAction> {
	
	private static Logger logger = LogManager.getLogger(FollowAction.class);

	public FollowAction(Core core, Node parent, MongoDocument document) {
		super(core, parent, document);
	}

	@Override
	public String getMainTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateNode(JsonObject jsonData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDisplayName() {
		return "Follow";
	}
	
	/**
	 * 
	 */
	/*
	public boolean isFollowing(String id) {
		try {
			User user = core.getNodeById(this, id);
			MongoDocument d = getDocument(user);
			if(d != null && !d.isNull()) {
				return d.get("following", Collections.emptyList()).contains(id);
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	*/
	
	/**
	 * Determines whether or not this parent follows the node with the given id.
	 */
	public boolean isFollowing(String id) {
		List<String> ids = document.getObjectList2("following");
		logger.debug("FOLLOW: {}", ids);
		return ids.contains(id);
	}
	
	public void follow(String id) {
		logger.debug("IS LFOOWOFLS {}", isFollowing(id));
		if(!isFollowing(id)) {
			document.addToList("following", id);
			logger.debug("DOC IS NOW: {}", document);
			save();
		}
	}
	
	public void unfollow(String id) {
		if(!document.contains("following")) {
			return;
		}
		
		MongoDocument f = document.get("following");
		f.removeField(id);
	}
	
	public void addFollow(String id, String type) throws DatabaseException {
		if(!document.contains("following")) {
			document.set("following", new MongoDocument());
		}
		
		MongoDocument f = document.get("following");
		f.putrarray(type, true, id);
		save();
	}
	
	@PostMethod
	@DeleteMethod
	public WebResponse doIndex(Request request) throws IOException, DatabaseException {
    	String id = request.getValue("id");
    	if(request.isRequestPost()) {
	    	logger.debug("Following {}", id);
	    	addFollow(id, "ALL");
	    	return WebResponse.makeJsonResponse().appendBody("{\"id\": \"" + id + "\", \"following\": true}");
    	} else if(request.isRequestDelete()) {
	    	logger.debug("Unfollowing {}", id);
	    	unfollow(id);
	    	return WebResponse.makeJsonResponse().appendBody("{\"id\": \"" + id + "\", \"following\": false}");    		
    	}
    	
    	throw new IllegalArgumentException("Request is not supported.");
	}
	
    @GetMethod
    public WebResponse doIsFollowing(Request request) throws IOException {
    	String id = request.getValue("id");
    	logger.debug("Is following.... {}", id);
    	//response.setContentType("application/json");
    	//response.getWriter().print("{\"following\":" + (isFollowing(id) ? "true" : "false") + "}");
    	//response.getWriter().flush();
    	WebResponse response = WebResponse.makeJsonResponse().appendBody("{\"id\": \"" + id + "\", \"following\":" + (isFollowing(id) ? "true" : "false") + "}");
    	logger.debug("RESPOENE: {}", response);
    	
    	return response;
    }
    
    /*
    @GetMethod
    public void doFollow(Request request, Response response) {
    	
    }
    */
	
	public static class FollowActionDescriptor extends ActionDescriptor<FollowAction> {

		public FollowActionDescriptor(Core core) {
			super(core);
		}

		@Override
		public boolean isApplicable(Node node) {
			return node instanceof User;
		}

		@Override
		public String getExtensionName() {
			return "follow";
		}

		@Override
		public Class<?> getExtensionClass() {
			return FollowAction.class;
		}
		
		/*
        @Override
        public ExtensionGroup getExtensionGroup() {
            return new ExtensionGroup( getClazz(), "Follow" );
        }
        */


		@Override
		public String getDisplayName() {
			return "Follow";
		}

		@Override
		public boolean isOmnipresent() {
			return true;
		}
	}

}
