package org.seventyeight.web.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.utils.GetMethod;
import org.seventyeight.web.Core;
import org.seventyeight.web.model.*;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.responses.WebResponse;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cwolfgang
 */
public class WidgetAction implements Node, Parent {

    private static Logger logger = LogManager.getLogger( WidgetAction.class );

    private ConcurrentHashMap<String, Widget> widgets = new ConcurrentHashMap<String, Widget>(  );

    private GetWidget getWidget = new GetWidget();

    private Core core;

    public WidgetAction( Core core ) {
        this.core = core;
    }

    @Override
    public Node getParent() {
        return core.getRoot();
    }

    @Override
    public String getDisplayName() {
        return "Widgets";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }

    public void addWidget( Widget widget ) {
        logger.debug( "Adding widget {}", widget.getName() );
        widgets.put( widget.getName(), widget );
    }

    @GetMethod
    public WebResponse doGet(Request request) {
        //response.setRenderType( Response.RenderType.NONE );

    	return new WebResponse();
    }

    public Widget get( String token ) throws NotFoundException {
        if(widgets.containsKey( token )) {
            return widgets.get( token );
        } else {
            throw new NotFoundException( "No such widget, " + token );
        }
    }

    @Override
    public Node getChild( String name ) throws NotFoundException {
        return getWidget;
    }

    public class GetWidget implements Node, Parent {

        @Override
        public Node getParent() {
            return WidgetAction.this;
        }

        @Override
        public String getDisplayName() {
            return "Get widget";
        }

        @Override
        public String getMainTemplate() {
            return null;
        }

        @Override
        public Node getChild( String name ) throws NotFoundException {
            return get( name );
        }
    }
}
