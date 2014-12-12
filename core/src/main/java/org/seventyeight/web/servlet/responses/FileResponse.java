package org.seventyeight.web.servlet.responses;

import java.io.File;
import java.io.IOException;

import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.servlet.WebResponse;

public class FileResponse extends ResponseAction {

	private File file;
	
	public FileResponse(File file) {
		this.file = file;
	}

	@Override
	public void respond(Request request, WebResponse response) throws IOException {
		response.deliverFile(request, file, true);
	}
}
