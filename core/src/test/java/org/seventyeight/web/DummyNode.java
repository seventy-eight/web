package org.seventyeight.web;

import org.seventyeight.web.model.Node;

/**
 * @author cwolfgang
 */
public class DummyNode implements Node {

    protected Node parent;

    public DummyNode( Node parent ) {
        this.parent = parent;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public String getDisplayName() {
        return "Dummy";
    }

    @Override
    public String getMainTemplate() {
        return null;
    }
}
