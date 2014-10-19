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
import org.seventyeight.web.model.FeatureSearch;
import org.seventyeight.web.model.ItemInstantiationException;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.NotFoundException;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.SearchHelper;
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
    public void doSearch( Request request, Response response ) throws IOException, NotFoundException, ItemInstantiationException, TemplateException {
        response.setRenderType( Response.RenderType.NONE );

        SearchHelper sh = new SearchHelper( this, request, response );
        sh.search();
        sh.render();
    }
    
    @GetMethod
    public void doGetMethods(Request request, Response response) throws IOException {
    	response.setContentType(Response.ContentType.JSON.toString());
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

            PrintWriter writer = response.getWriter();
            Gson gson = new Gson();
            writer.print( gson.toJson(methods) );
            
            return;
        }

        if(fullList) {
            PrintWriter writer = response.getWriter();
            Gson gson = new Gson();
            writer.print( gson.toJson(request.getCore().getSearchables().keySet()) );
        	
        	return;
    	}
        
        response.getWriter().write( "{}" );
    }
    
    private static QueryParser queryParser = new QueryParser();
    
    /**
     * Get a complete list of methods
     */
    @GetMethod
    public void doComplete(Request request, Response response) {
    	response.setContentType(Response.ContentType.JSON.toString());
    	
    	String query = request.getValue( "query", null );
    	
        if( query != null && !query.isEmpty() ) {
            //LinkedList<String> tokens = tokenizer.tokenize( query );
            Root root = queryParser.parse( query );
            QueryVisitor visitor = new QueryVisitor( core );
            visitor.visit( root );

        }
    }

    @GetMethod
    public void doShow( Request request, Response response ) {
        logger.debug( "SHOW????!!!!" );
    }



}
