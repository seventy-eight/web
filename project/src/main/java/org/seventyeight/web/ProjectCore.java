package org.seventyeight.web;

import org.seventyeight.web.actions.*;
import org.seventyeight.web.extensions.filetype.ImageFileType;
import org.seventyeight.web.model.Menu;
import org.seventyeight.web.nodes.*;
import org.seventyeight.web.project.actions.AddCertificate;
import org.seventyeight.web.project.actions.AddNode;
import org.seventyeight.web.project.actions.CertificateSearch;
import org.seventyeight.web.project.actions.Search;
import org.seventyeight.web.project.model.Certificate;
import org.seventyeight.web.project.model.Profile;
import org.seventyeight.web.project.model.Signature;

import java.io.File;

/**
 * @author cwolfgang
 */
public class ProjectCore extends Core {

    private File signaturePath;

    public ProjectCore( File path, String dbname ) throws CoreException {
        super( path, dbname );

        signaturePath = new File( path, "signatures" );

        /* Mandatory top level Actions */
        children.put( "static", new StaticFiles() );
        children.put( "theme", new ThemeFiles() );
        children.put( "new", new NewContent( this ) );
        children.put( "get", new Get( this ) );
        children.put( "upload", new Upload() );
        children.put( "nodes", new Nodes() );
        children.put( "configuration", new GlobalConfiguration() );
        //children.put( "login", new Login( this ) );

        /* Adding search action */
        Search search = new Search();
        CertificateSearch cs = new CertificateSearch( search );
        NodeSearch ns = new NodeSearch( search );

        search.addAction( Certificate.CERTIFICATE, cs );
        search.addAction( "node", ns );

        children.put( "search", search );

        /* Adders */
        AddNode add = new AddNode();

        AddCertificate ac = new AddCertificate( add );

        add.addAction( Certificate.CERTIFICATE, ac );

        children.put( "add", add );

        /**/
        addDescriptor( new Profile.ProfileDescriptor() );
        addDescriptor( new Group.GroupDescriptor() );
        addDescriptor( new FileNode.FileDescriptor() );
        addDescriptor( new Certificate.CertificateDescriptor() );
        addDescriptor( new Article.ArticleDescriptor() );

        addDescriptor( new ImageFileType.ImageFileTypeDescriptor() );

        addDescriptor( new Signature.SignatureDescriptor() );
        //addDescriptor( new  );

        //addExtension( ImageFileType.class, new ImageFileType(  ) );

        mainMenu.add( new Menu.MenuItem( "New Content", "/new/" ) );
        mainMenu.add( new Menu.MenuItem( "Upload", "/upload/" ) );
        mainMenu.add( new Menu.MenuItem( "Test", "/user/wolle/" ) );
        mainMenu.add( new Menu.MenuItem( "Configure", "/configuration/" ) );
    }

    public File getSignaturePath() {
        return signaturePath;
    }

}
