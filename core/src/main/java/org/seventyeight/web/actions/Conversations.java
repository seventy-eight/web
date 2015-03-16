package org.seventyeight.web.actions;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.AbstractNode;
import org.seventyeight.web.model.Action;
import org.seventyeight.web.model.Comment;
import org.seventyeight.web.model.DeletingParent;
import org.seventyeight.web.model.Getable;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.model.Parent;
import org.seventyeight.web.model.Resource;
import org.seventyeight.web.nodes.Conversation;
import org.seventyeight.web.nodes.Group;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class Conversations extends Action<Conversations> implements Getable<Conversation>, DeletingParent {
	
	private static Logger logger = LogManager.getLogger(Conversations.class);

	public Conversations(Core core, Node parent, MongoDocument document) {
		super(core, parent, document);
	}

	@Override
	public String getMainTemplate() {
		return null;
	}

	@Override
	public void updateNode(JsonObject jsonData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDisplayName() {
		return "Conversations()";
	}
	
	@Override
	public void deleteChild(Node node) {
		if(parent != null && parent instanceof DeletingParent) {
            ( (DeletingParent) parent ).deleteChild( node );
        } else {
            logger.debug( "No deleting operation for {}", this );
        }	
	}

	/*
	@Override
	public Node getChild(String name) throws NotFoundException {
		try {
			return core.getNodeById(this, name);
		} catch (ItemInstantiationException e) {
			throw new NotFoundException(e.getMessage());
		}
	}
	*/

	@Override
	public Conversation get(Core core, String token) throws NotFoundException {
		try {
			return core.getNodeById(this, token);
		} catch (ItemInstantiationException e) {
			throw new NotFoundException(e.getMessage());
		}
	}
	
	public long getNumberOfConversations() {
    	MongoDBQuery query = new MongoDBQuery().is(Conversation.PARENT_FIELD, ((AbstractNode<?>) parent).getIdentifier()).is("type", Conversation.TYPE_NAME);
    	return MongoDBCollection.get(Core.NODES_COLLECTION_NAME).count(query);
    }
	
	@GetMethod
	public WebResponse doGetNumber(Request request) {
		return new WebResponse().appendBody(getNumberOfConversations());
	}
	
    @PostMethod
    public WebResponse doAdd(Request request) throws ItemInstantiationException, ClassNotFoundException, TemplateException, IOException {
    	//response.setRenderType( Response.RenderType.NONE );
    	
    	logger.debug("Adding conversation to {}", this);
    	
        String text = request.getValue( "comment", "" );

        if(text.length() > 1) {
            Conversation.ConversationDescriptor descriptor = core.getDescriptor( Conversation.class );
            Conversation conversation = descriptor.newInstance( request, this );
            logger.debug("Conversation is {}", conversation);
            if(conversation != null) {
                JsonObject json = request.getJson();
                conversation.updateConfiguration(json);
                logger.debug("OBJECT IS {}", conversation.getDocument());
                conversation.save();
                
                ((Resource<?>) parent).setUpdatedCall( null );
                
                Comment comment = conversation.addComment(request);
                // The special case, needs to set the parent for the comment
                comment.setConversationParent(conversation.getIdentifier());
                comment.setResource(conversation.getResource());
                comment.save();
                logger.debug("Comment is {}", comment);

                //comment.getDocument().set( "view", core.getTemplateManager().getRenderer( request ).renderObject( comment, "view.vm" ) );

                conversation.getDocument().set("view", core.getTemplateManager().getRenderer( request ).renderObject( conversation, "view.vm" ));
                
                //PrintWriter writer = response.getWriter();
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                //writer.write( gson.toJson( conversation.getDocument() ) );
                //writer.write( core.getTemplateManager().getRenderer( request ).renderObject( conversation, "view.vm" ) );
                return new WebResponse().appendBody(gson.toJson( conversation.getDocument() ));
            }
            return new WebResponse().serverError().setHeader("Conversation not created");
        } else {
            //throw new IllegalStateException( "No text provided!" );
        	return new WebResponse().badRequest().setHeader("No Text provided");
        }
    }

    @GetMethod
    public WebResponse doGetAll(Request request) throws IOException, TemplateException {
        //response.setRenderType( Response.RenderType.NONE );

        int number = request.getInteger( "number", 10 );
        int offset = request.getInteger( "offset", 0 );
        
        List<String> groupIds = Group.getGroupIds(request.getUser());
    	groupIds.add(request.getUser().getIdentifier());
    	groupIds.add(ACL.ALL);

        //MongoDBQuery query = new MongoDBQuery().is( "parent", ((AbstractNode<?>) parent).getIdentifier() ).is( "type", "conversation" ).in("ACL.read", groupIds);
    	MongoDBQuery query = new MongoDBQuery().is( "parent", ((AbstractNode<?>) parent).getIdentifier() ).is( "type", "conversation" ).in(AbstractNode.fullAclReadField, groupIds);
        MongoDocument sort = new MongoDocument().set( "created", 1 );
        logger.fatal("--->{}", query);
        List<MongoDocument> docs = MongoDBCollection.get( Core.NODES_COLLECTION_NAME ).find( query, offset, number, sort );
        logger.fatal("RES:{}", docs);
        //PrintWriter writer = response.getWriter();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        //writer.write( gson.toJson( docs ) );
        return WebResponse.makeJsonResponse().appendBody(gson.toJson( docs ));
    }

	public static class ConversationsDescriptor extends Action.ActionDescriptor<Conversations> {

		public ConversationsDescriptor(Core core) {
			super(core);
		}

		@Override
		public String getExtensionName() {
			return "conversations";
		}

		@Override
		public Class<Conversations> getExtensionClass() {
			return Conversations.class;
		}

		@Override
		public String getDisplayName() {
			return "Conversations";
		}

        @Override
        public boolean isOmnipresent() {
            return true;
        }
	}
}
