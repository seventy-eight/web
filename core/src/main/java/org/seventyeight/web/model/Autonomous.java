package org.seventyeight.web.model;

import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.responses.WebResponse;

/**
 * @author cwolfgang
 */
public interface Autonomous {
    public WebResponse autonomize( Request request ) throws Exception;
}
