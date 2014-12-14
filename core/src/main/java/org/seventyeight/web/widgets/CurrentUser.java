package org.seventyeight.web.widgets;

import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.Widget;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.servlet.Request;

public class CurrentUser extends Widget {

	@Override
	public Node getParent() {
		return null;
	}

	@Override
	public String getMainTemplate() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return "Current user";
	}

	@Override
	public String getName() {
		return "Current user widget";
	}
	
	public User getUser(Request request) {
		return request.getUser();
	}

}
