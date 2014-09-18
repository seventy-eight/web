package org.seventyeight.web.model;

import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

import java.io.IOException;

/**
 * @author cwolfgang
 */
public interface Autonomous {
    public void autonomize( Request request, Response response ) throws IOException, Throwable;
}
