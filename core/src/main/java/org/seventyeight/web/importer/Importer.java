package org.seventyeight.web.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Importer {
	
	private static Logger logger = LogManager.getLogger(Importer.class);
	
	//@Option(name="-type", required=true)
	private String type;

	private String dbHost = "212.97.132.75:3306/cwolfga_gymnerds";
	
	private String dbUser = "cwolfga_admin", dbPass = "avenger";
	
	private Context context;
	
	
	
	public static JsonArray getReturnJsonArray(CloseableHttpResponse response) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		
		String output;
		StringBuilder sb = new StringBuilder();
		while ((output = br.readLine()) != null) {
			sb.append(output);
		}
		
        JsonParser parser = new JsonParser();
        try {
        	return (JsonArray) parser.parse(sb.toString());
        } catch(Exception e) {
        	return null;
        }
	}
	
	public static JsonObject getReturnJsonObject(CloseableHttpResponse response) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		
		String output;
		StringBuilder sb = new StringBuilder();
		while ((output = br.readLine()) != null) {
			sb.append(output);
		}
		
        JsonParser parser = new JsonParser();
        try {
        	return (JsonObject) parser.parse(sb.toString());
        } catch(Exception e) {
        	return null;
        }
	}
	

	
	public static abstract class Action {
		public abstract void act(Connection connection, CloseableHttpClient httpclient) throws SQLException, IOException, ImportException;
	}
	

	
		

	public class ConversationImport extends Action {
		
		private class Topic {
			public int topicId;
			public int categoryId;
			public String conversation;
			public String title;
			
			
			public Topic(int topicId, int categoryId, String conversation, String title) {
				this.topicId = topicId;
				this.categoryId = categoryId;
				this.conversation = conversation;
				this.title = title;
			}
			
			@Override
			public String toString() {
				return topicId + "/" + categoryId + "/" + conversation + "/" + title;
			}
		}

		@Override
		public void act(Connection connection, CloseableHttpClient httpclient) throws SQLException, IOException, ImportException {
			
			
			// Get all the topics
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM board_topics");
			
			ConversationInserter ci = new ConversationInserter(httpclient, context);
			
			List<Topic> topics = new ArrayList<Topic>();
			
			while(rs.next()) {
				String title = rs.getString("title");
				int ownerId = rs.getInt("start_user_id");
				int topicId = rs.getInt("topic_id");
				int categoryId = rs.getInt("category_id");
				
				// Insert
				ConversationInserter.Arguments args = new ConversationInserter.Arguments(title, ownerId);
				String conversation = ci.act(args);
				
				//conversations.put(topicId, conversation);
				topics.add(new Topic(topicId, categoryId, conversation, title));
				
				//break;
			}
			
			stmt.close();
			
			CommentInserter coi = new CommentInserter(httpclient, context);
			ChangeOwnership co = new ChangeOwnership(httpclient, context);
			
			// Get the posts
			for(Topic topic : topics) {
				logger.debug(topic);
				Statement stmt2 = connection.createStatement();
				ResultSet rs2 = stmt2.executeQuery("SELECT * FROM board_posts WHERE topic_id=" + topic.topicId + " AND category_id=" + topic.categoryId);
				
				boolean first = true;
				String parent = topic.conversation;
				while(rs2.next()) {
					String postText = rs2.getString("post_text");
					int userId = rs2.getInt("user_id");
					long timestamp = rs2.getLong("post_time");
					
					//logger.debug("PARENT: {}", postText);
					
					CommentInserter.Arguments ca = new CommentInserter.Arguments(topic.conversation, context.getUserMap().get(userId), topic.title, postText, parent, timestamp);
					
					String id = coi.act(ca);
					logger.debug("Comment ID={}, user={}, new={}", id, userId, context.getUserMap().get(userId));
					
					ChangeOwnership.Arguments coa = new ChangeOwnership.Arguments(id, context.getUserMap().get(userId));
					
					co.act(coa);
					
					if(first) {
						parent = id;
					}
					
					first = false;
				}
				
				stmt2.close();
				
			}
			
		}
		
	}
		
	public class GroupImport extends Action {

		@Override
		public void act(Connection connection, CloseableHttpClient httpclient) throws SQLException, IOException {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM user_groups");
			
			GroupInserter gi = new GroupInserter(httpclient, context);
			
			while(rs.next()) {
				String groupName = rs.getString("group_name");
				int groupId = rs.getInt("group_id");
				int ownerId = rs.getInt("owner_id");
				
				// Insert
				GroupInserter.Arguments args = new GroupInserter.Arguments(groupName, groupId, ownerId);
				gi.act(args);
			}
		}	
	}
	
		
	public class UserImport extends Action {
		
		public void act(Connection connection, CloseableHttpClient httpclient) throws SQLException, IOException {
			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM users");
			
			UserInserter ui = new UserInserter(httpclient, context);
			
		    while(rs.next()) {
		    	String u = rs.getString("username");
		    	String p = rs.getString("password");
		    	String e = rs.getString("email");
		    	int userId = rs.getInt("user_id");
		    	logger.info("Username: {}", u);
		    	
		    	// Checking
		    	CheckUser.Arguments args = new CheckUser.Arguments(u, userId);
		    	boolean exists = new CheckUser(httpclient, context).act(args);
		    	if(!exists) {
		    		UserInserter.Arguments a = new UserInserter.Arguments(u, userId, p, e);
		    		ui.act(a);
		    	}   	
		    }
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, ClientProtocolException, IOException, ImportException {
		Importer i = new Importer();
		CmdLineParser parser = new CmdLineParser(i);
		try {
			parser.parseArgument(args);
			i.execute();
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
		}
	}
	
	public Importer() {
	}
	
	public void execute() throws ClassNotFoundException, SQLException, ClientProtocolException, IOException, ImportException {
		//UserImport ui = new UserImport("mydb5.surftown.dk:3306/cwolfga_gymnerds", "cwolfga_admin", "avenger");
		
		Class.forName("com.mysql.jdbc.Driver");
		String db = "jdbc:mysql://" + dbHost;
		
		Connection con = DriverManager.getConnection(db, dbUser, dbPass);
		Statement stmt = con.createStatement();
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		context = new Context("http://localhost:8080/");

		new TruncateDatabases(httpclient, context).act("");
		
		new UserImport().act(con, httpclient);
		
		logger.fatal("Users: {}", context.getUserMap());
		
		//new GroupImport().act(con, httpclient);
		
		new ConversationImport().act(con, httpclient);
	}
}
