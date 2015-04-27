package org.seventyeight.web.social;

import org.seventyeight.web.model.Layoutable;
import org.seventyeight.web.model.Node;
import org.seventyeight.web.model.Resource;
import org.seventyeight.web.nodes.User;

public class FollowLayout implements Layoutable {
	@Override
	public boolean isApplicable(Node node) {
		return node instanceof Resource;
	}
}
