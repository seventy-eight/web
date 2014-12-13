package org.seventyeight.web.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.ast.Root;
import org.seventyeight.database.mongodb.MongoDBCollection;
import org.seventyeight.database.mongodb.MongoDBQuery;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.handlers.template.TemplateException;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.SearchHelper;
import org.seventyeight.web.servlet.responses.WebResponse;
import org.seventyeight.web.utilities.QueryParser;
import org.seventyeight.web.utilities.QueryVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author cwolfgang
 */
public class Search implements Node {

    private static Logger logger = LogManager.getLogger( Search.class );

    private Core core;

    public Search( Core core ) {
        this.core = core;
    }

    @Override
    public Node getParent() {
        return core.getRoot();
    }

    @Override
    public String getDisplayName() {
        return "Search";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    @GetMethod
    public WebResponse doSearch( Request request ) throws IOException, NotFoundException, ItemInstantiationException, TemplateException {
        //response.setRenderType( Response.RenderType.NONE );

        SearchHelper sh = new SearchHelper( this, request );
        sh.search();
        return sh.render();
    }
    
    @GetMethod
    public WebResponse doGetMethods(Request request) throws IOException {
    	//response.setContentType(Response.ContentType.JSON.toString());
        String term = request.getValue( "term", "" );
        boolean fullList = request.getValue("full", 0) > 0;

        if( term.length() > 0 ) {
            Set<String> set = request.getCore().getSearchables().keySet();
            List<String> methods = new ArrayList<String>();
            for(String s : set) {
            	if(StringUtils.containsIgnoreCase(s, term)) {
            		methods.add(s);
            	}
            }

            //PrintWriter writer = response.getWriter();
            Gson gson = new Gson();
            //writer.print( gson.toJson(methods) );
            
            return WebResponse.makeJsonResponse().appendBody(gson.toJson(methods));
        }

        if(fullList) {
            //PrintWriter writer = response.getWriter();
            Gson gson = new Gson();
            //writer.print( gson.toJson(request.getCore().getSearchables().keySet()) );
        	
            return WebResponse.makeJsonResponse().appendBody(gson.toJson(request.getCore().getSearchables().keySet()));
    	}
        
        return WebResponse.makeEmptyJsonResponse();
    }
    
    private static QueryParser queryParser = new QueryParser();
    
    /**
     * Get a complete list of methods
     */
    @GetMethod
    public WebResponse doComplete(Request request) {
    	//response.setContentType(Response.ContentType.JSON.toString());
    	
    	String query = request.getValue( "query", null );
    	
        if( query != null && !query.isEmpty() ) {
            //LinkedList<String> tokens = tokenizer.tokenize( query );
            Root root = queryParser.parse( query );
            QueryVisitor visitor = new QueryVisitor( core );
            visitor.visit( root );

        }
        
        return new WebResponse();
    }

    @GetMethod
    public WebResponse doShow( Request request) {
        logger.debug( "SHOW????!!!!" );
        return new WebResponse();
    }



}
