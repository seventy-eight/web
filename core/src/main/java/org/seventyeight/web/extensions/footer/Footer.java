package org.seventyeight.web.extensions.footer;

import com.google.gson.JsonObject;
import org.seventyeight.database.mongodb.MongoDocument;
import org.seventyeight.web.extensions.NodeExtension;
import org.seventyeight.web.model.Action;
import org.seventyeight.web.model.CoreRequest;
import org.seventyeight.web.model.Descriptor;
import org.seventyeight.web.model.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author cwolfgang
 *         Date: 06-03-13
 *         Time: 19:50
 */
public class Footer extends NodeExtension {

    public Footer( MongoDocument document ) {
        super( document );
    }

    @Override
    public void save( CoreRequest request, JsonObject jsonData ) {
    }

    public String getFooter() {
        return "MY FOOT!ER!";
    }

    @Override
    public List<Action> getActions() {
        List<Action> actions = new ArrayList<Action>( 1 );
        actions.add( new FooterAction() );
        return actions;
    }

    public class FooterAction implements Action {

        @Override
        public Node getParent() {
            return null;
        }

        @Override
        public String getUrlName() {
            return "footer";
        }

        @Override
        public String getName() {
            return "Footer";
        }
    }

    public static class FooterDescriptor extends Descriptor<Footer> {

        @Override
        public String getDisplayName() {
            return "Footer";
        }
    }
}
