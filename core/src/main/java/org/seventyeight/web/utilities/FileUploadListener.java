package org.seventyeight.web.utilities;

import org.apache.commons.fileupload.ProgressListener;

/**
 * @author cwolfgang
 */
public class FileUploadListener implements ProgressListener {

    private double percent = 0.0;

    @Override
    public void update( long bytesRead, long totalBytes, int item ) {
        if( totalBytes > 0) {
            percent = (double)bytesRead / totalBytes;
        }
    }

    public double getPercent() {
        return percent;
    }
}
