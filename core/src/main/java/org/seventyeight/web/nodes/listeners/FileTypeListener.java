package org.seventyeight.web.nodes.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.web.Core;
import org.seventyeight.web.extensions.StartupListener;
import org.seventyeight.web.extensions.filetype.FileType;
import org.seventyeight.web.nodes.FileResource;

import java.util.List;

/**
 * @author cwolfgang
 */
public class FileTypeListener implements StartupListener {

    private static Logger logger = LogManager.getLogger( FileTypeListener.class );

    private Core core;

    public FileTypeListener( Core core ) {
        this.core = core;
    }

    @Override
    public void onStartup() {
        logger.debug( "Gathering file types" );
        List<FileType> fileTypes = core.getExtensions( FileType.class );

        FileResource.FileDescriptor fd = core.getDescriptor( FileResource.class );
        for( FileType fileType : fileTypes ) {
            fd.addFileType( fileType );
        }
    }
}
