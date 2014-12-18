package org.seventyeight.web.nodes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.BasicResourceBasedSecurity;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.AbstractNode;
import org.seventyeight.web.model.Comment;
import org.seventyeight.web.model.CallContext;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NodeDescriptor;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.model.PersistedNode;
import org.seventyeight.web.model.Resource;
import org.seventyeight.web.model.ResourceDescriptor;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.Response.RenderType;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.JsonException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Conversation extends Resource<Conversation> {
	
	private static Logger logger = LogManager.getLogger(Conversation.class);
	
	public static final String PARENT_FIELD = "parent";
	public static final String TYPE_NAME = "conversation";

	public Conversation(Core core, Node parent, MongoDocument document) {
		super(core, parent, document);
	}

	@Override
	public void updateNode(JsonObject jsonData) {
		if(jsonData != null) {
			if(jsonData.has("ids")) {
				JsonArray ids = jsonData.get("ids").getAsJsonArray();
				List<String> gids = new ArrayList<String>();
				for(JsonElement e : ids) {
					if(e.isJsonPrimitive()) {
						String id = e.getAsString();
						gids.add(id);
					}
				}
				
				try {
					BasicResourceBasedSecurity s = BasicResourceBasedSecurity.getFromGroupIds(core, this, gids);
					//document.set("ACL", s.getDocument());
					setACL(s.getDocument());
				} catch (ItemInstantiationException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
	}
		
	public Comment getRootComment() {
		logger.debug("Getting root comment, {}", core);
        MongoDBQuery query = new MongoDBQuery().is( "conversation", getIdentifier() ).is("parent", getIdentifier()).is( "type", "comment" );
        logger.debug("Query is {}", query);
        MongoDocument doc = MongoDBCollection.get( Comment.COMMENTS_COLLECTION ).findOne( query);
        logger.debug("Retrieved dfocx is {}", doc);
        doc.set("resource", document.get("parent", null));
        logger.debug("Whoop!");
        return new Comment( core, this, doc );
	}

	/*
	@GetMethod
	public void doGetLatestComments(Request request, Response response) {
		JsonObject json = request.getJson();
		
		//int offset = json.get("offset");
	}
	*/
	
    @GetMethod
    public WebResponse doGetComments(Request request) throws IOException, TemplateException {
        //response.setRenderType( Response.RenderType.NONE );

        // A map of lists of comments, keyed by the comments parent id
        Map<String, List<MongoDocument>> comments = new HashMap<String, List<MongoDocument>>();
        
        int offset = request.getInteger("offset", 0);
        int number = request.getInteger("number", 10);
        boolean reversed = request.getBoolean("reversed", false);
        
        logger.debug("Fetch comments {}, {}", offset, number);
        
        // 1) Get the root comment
        String rootCommentId = getRootComment().getIdentifier();
        logger.debug("Root comment id: {}", rootCommentId);
        
        String resourceId = document.get("parent", null);
        
        List<MongoDocument> firstLevelComments = new ArrayList<MongoDocument>();
        
        // 2) First level replies
        //MongoDBQuery flquery = new MongoDBQuery().is( "conversation", getIdentifier() ).is("parent", rootCommentId);
        MongoDBQuery flquery = new MongoDBQuery().is( "conversation", getIdentifier() ).is("parent", rootCommentId);
        logger.debug("QUERY: {}", flquery);
        MongoDocument sort = new MongoDocument().set( "created", (reversed ? -1 : 1) );
        List<MongoDocument> docs = MongoDBCollection.get( Comment.COMMENTS_COLLECTION ).find( flquery, offset, number, sort );
        
        logger.debug("Number of docs: {}", docs.size());
        
        // A list of first level comments identifiers 
        List<String> ids = new ArrayList<String>(docs.size());

        for(MongoDocument d : docs) {
            Comment c = new Comment( core, this, d );
            
            // Place view
            d.set("view", core.getTemplateManager().getRenderer( request ).updateContext("contextResource", resourceId).renderObject( c, "view.vm" ) );
            
            firstLevelComments.add(d);
            
            ids.add(c.getIdentifier());
        }
        
        logger.debug("IDS: {}", ids);
        
        comments.put(rootCommentId, firstLevelComments);

        // 3) Get descendants, but only if there are any
        if(!ids.isEmpty()) {
	        List<String> descendants = getDescendants(ids);
	        logger.debug("Descendants: {}", descendants.size());
	        for(String descendant : descendants) {
	        	MongoDocument d = Comment.getComment(descendant);
	        	Comment c = new Comment(core, this, d);
	        	
	        	// Make sure the parent key is in the map
	            if(!comments.containsKey(c.getCommentParent())) {
	            	comments.put(c.getCommentParent(), new ArrayList<MongoDocument>());
	            }
	            List<MongoDocument> cs = comments.get(c.getCommentParent());
	            
	            // Place view
	            d.set("view", core.getTemplateManager().getRenderer( request ).updateContext("contextResource", resourceId).renderObject( c, "view.vm" ) );
	            //d.set("resource", parent);
	            
	            cs.add(d);
	        }
        } else {
        	logger.debug("NO DESCENDANTS!!!");
        }
        
        //PrintWriter writer = response.getWriter();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        //writer.write( gson.toJson( comments ) );
        return new WebResponse().appendBody(gson.toJson( comments ));
        //writer.write( comments.toString() );
    }
    
    @GetMethod
    public WebResponse doGetNumberOfFirstLevelComments(Request request) throws IOException {
    	//response.setRenderType(RenderType.NONE);
    	
        MongoDBQuery query = new MongoDBQuery().is( "conversation", getIdentifier() ).is("parent", getRootComment().getIdentifier());
        //logger.debug("QUERY: {}", query);
        MongoDocument sort = new MongoDocument().set( "created", 1 );
        //response.getWriter().print(MongoDBCollection.get( Comment.COMMENTS_COLLECTION ).count(query));
        return new WebResponse().appendBody(MongoDBCollection.get( Comment.COMMENTS_COLLECTION ).count(query));
    }
    
    
    /**
     * Given a list comment ids, get their descendants.
     */
    public List<String> getDescendants(List<String> parents) {
    	logger.debug("Getting descendants for {}", parents.toArray());
        MongoDBQuery query = new MongoDBQuery().in("ancestors", parents);
        MongoDocument sort = new MongoDocument().set( "created", 1 );
        
        logger.debug("Query for descendants: {}", query);

        List<MongoDocument> docs = MongoDBCollection.get( Comment.COMMENTS_COLLECTION ).find( query, 0, 0, sort, "_id" );
        logger.debug( "DOCS FOR IDS: {}", docs );
        
        List<String> ids = new ArrayList<String>(docs.size());
        
        for(MongoDocument d : docs) {
        	ids.add(d.getIdentifier());
        }
        
        return ids;
    }
    
    public Comment addComment(Request request) throws ClassNotFoundException, ItemInstantiationException {
        String text = request.getValue( "comment", "" );

        Comment.CommentDescriptor descriptor = core.getDescriptor( Comment.class );
        Comment comment = descriptor.newInstance( request, this );

        JsonObject json = request.getJson();
        comment.updateConfiguration(json);
        
        // Set the correct conversation
        comment.setConversation(this);
        
        comment.save();
        
        	String resourceId = json.get("resource").getAsString();
        	if(json.has("resource") && !json.get("resource").getAsString().isEmpty()) {
        	try {
				Resource<?> r = core.getNodeById(this, resourceId);
				r.touch();
			} catch (NotFoundException e) {
				logger.log(Level.WARN, "Unable to touch " + resourceId, e);
			}
        } else {
	        Resource<?> r = core.find(this.parent, Resource.class);
	        if(r != null) {
	        	r.touch();
	        } else {
	        	logger.warn("Could not touch child resource of {}", this);
	        }
        }
        
        return comment;
    }
    
    public Resource<?> getResource() {
    	Resource<?> r = core.find(this.parent, Resource.class);
    	if(r != null) {
    		return r;
    	} else {
    		return null;
    	}
    }

	@PostMethod
	public WebResponse doIndex(Request request) throws IOException, ClassNotFoundException, ItemInstantiationException, TemplateException {
    	JsonObject json = request.getJson();
        
        if(json == null) {
        	//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print("No data provided");
        	return new WebResponse().setCode(406).setHeader("No data provided");
        }
        
        if(!json.has("comment") || json.get("comment").getAsString().length() < 0) {
        	//response.setStatus(Response.SC_NOT_ACCEPTABLE);
        	//response.getWriter().print("No comment provided");
        	return new WebResponse().setCode(406).setHeader("No comment provided");
        }

    	Comment comment = addComment(request);
        //setUpdatedCall( null );
    	setUpdated(null);
    	save();

        comment.getDocument().set( "view", core.getTemplateManager().getRenderer( request ).renderObject( comment, "view.vm" ) );

        comment.getDocument().set("identifier", comment.getIdentifier());
        //PrintWriter writer = response.getWriter();
        //writer.write( comment.getDocument().toString() );
        
    	//response.setStatus(Response.SC_ACCEPTED);
        return new WebResponse().appendBody(comment.getDocument().toString());
    	//response.getWriter().print("{\"id\":\"" + getIdentifier() + "\"}");
	}

    public static long getNumberOfConversations(Resource<?> resource) {
    	MongoDBQuery query = new MongoDBQuery().is(PARENT_FIELD, resource.getIdentifier()).is("type", TYPE_NAME);
    	return MongoDBCollection.get(Core.NODES_COLLECTION_NAME).count(query);
    }

	public static class ConversationDescriptor extends ResourceDescriptor<Conversation> {

		public ConversationDescriptor(Node parent) {
			super(parent);
		}
		
		@Override
		public String getUrlName() {
			return "conversations";
		}
		
		/*
        @Override
        public Conversation newInstance( CoreRequest request, Node parent ) throws ItemInstantiationException {
            Conversation conversation = super.newInstance( request, parent );

            if(parent instanceof PersistedNode) {
                conversation.getDocument().set( PARENT_FIELD, request.getValue("parent") );
            }

            return conversation;
        }
        */
		
		

		@Override
		public String getType() {
			return "conversation";
		}

		@Override
		protected void onNewInstance(Conversation instance, Core core, Node parent, JsonObject json) {
            if(parent instanceof PersistedNode) {
                instance.getDocument().set( PARENT_FIELD, json.get("parent").getAsString() );
            }
		}

		@Override
		public String getDisplayName() {
			return "Conversation";
		}
		
	}
}
