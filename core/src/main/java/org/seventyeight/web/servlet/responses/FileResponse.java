package org.seventyeight.web.servlet.responses;

import java.io.File;
import java.io.IOException;

import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;

public class FileResponse extends WebResponse {

	private File file;
	
	public FileResponse(File file) {
		this.file = file;
	}

	@Override
	public void writeBody(Request request, Response response) throws IOException {
		response.deliverFile(request, file, true);
	}
}
