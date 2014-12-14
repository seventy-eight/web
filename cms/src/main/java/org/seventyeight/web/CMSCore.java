package org.seventyeight.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.web.actions.*;
import org.seventyeight.web.authorization.ACLFiller;
import org.seventyeight.web.authorization.BasicResourceBasedSecurity;
import org.seventyeight.web.authorization.PublicACL;
import org.seventyeight.web.extensions.*;
import org.seventyeight.web.extensions.filetype.ImageFileType;
import org.seventyeight.web.extensions.listernes.ActivityNodeListener;
import org.seventyeight.web.extensions.searchers.TitleSearch;
import org.seventyeight.web.extensions.searchers.TypeSearch;
import org.seventyeight.web.model.Comment;
import org.seventyeight.web.model.RootNode;
import org.seventyeight.web.music.Artist;
import org.seventyeight.web.music.Concert;
import org.seventyeight.web.music.Festival;
import org.seventyeight.web.music.Venue;
import org.seventyeight.web.nodes.*;
import org.seventyeight.web.nodes.listeners.FileTypeListener;
import org.seventyeight.web.nodes.listeners.SearchFormatListener;
import org.seventyeight.web.nodes.listeners.WidgetListener;
import org.seventyeight.web.social.Follow;
import org.seventyeight.web.social.FollowAction;
import org.seventyeight.web.social.FollowLayout;
import org.seventyeight.web.widgets.ActivityWidget;
import org.seventyeight.web.widgets.CurrentUser;
import org.seventyeight.web.widgets.LastUsersWidget;
import org.seventyeight.web.widgets.NextEventWidget;

import java.io.File;

/**
 * @author cwolfgang
 */
public class CMSCore extends Core {

    private static Logger logger = LogManager.getLogger( CMSCore.class );

    private File signaturePath;

    public CMSCore( RootNode root, File path, String dbname ) throws CoreException {
        super( root, path, dbname );

        signaturePath = new File( path, "signatures" );

        /* Mandatory top level Actions */
        root.addNode( "static", new StaticFiles(this) );
        root.addNode( "theme", new ThemeFiles(this) );
        root.addNode( "new", new NewContent( this, root ) );
        //children.put( "get", new Get( this ) );
        root.addNode( "upload", new Upload(this) );
        root.addNode( "configuration", new GlobalConfiguration(this) );
        root.addNode( "search", new org.seventyeight.web.actions.Search(this) );

        root.addNode( "resources", new ResourcesAction(this) );

        root.addNode("clear", new Clear());
        
        WidgetAction widgets = new WidgetAction(this);
        root.addNode( "widgets", widgets );


        root.addNode( "information", new Information(this) );

        root.addNode( "language", new LanguageAction(this) );

        addDescriptor( new Conversation.ConversationDescriptor(root));
        addDescriptor( new Comment.CommentDescriptor(root) );

        addDescriptor( new User.UserDescriptor(root) );

        addDescriptor( new Group.GroupDescriptor(root) );

        addDescriptor( new FileResource.FileDescriptor(root) );

        addDescriptor( new Tags.TagsDescriptor(this) );
        addDescriptor( new Event.EventDescriptor(this)   );
        addDescriptor( new Collection.CollectionDescriptor(root) );

        ImageUploadsWrapper.ImageUploadsWrapperDescriptor wrapper = new ImageUploadsWrapper.ImageUploadsWrapperDescriptor(root);
        logger.debug("-------> {}", Integer.toHexString(wrapper.hashCode()));
        logger.debug("PAPAPPAPAPAPAP: {}", wrapper.getParent());
        addDescriptor( wrapper );

        addDescriptor( new GetAction.GetDescriptor(this) );
        addDescriptor( new Conversations.ConversationsDescriptor(this) );

        addSearchable( new TitleSearch() );
        addSearchable( new TypeSearch() );

        addDescriptor( new GravatarPortrait.GravatarPortraitDescriptor(this) );
        addDescriptor( new UploadablePortrait.UploadablePortraitDescriptor(this) );

        addExtension( new TabbedPartitionedResource() );
        addExtension( new CollectionExtension() );

        addExtension( new ActivityWidget() );

        addExtension( new FileTypeListener(this) );
        addExtension( new ImageFileType() );

        addExtension( new SearchFormatListener(this) );
        //addExtension( new CollectionFormatter() );
        addExtension( new WidgetListener(this) );
        addExtension( new NextEventWidget() );
        addExtension( new LastUsersWidget() );
        addExtension(new CurrentUser());

        addExtension( new ActivityNodeListener() );
        addExtension( new DefaultMenuContributor() );
        addExtension( new DefaultAddMenuContributor() );

        addDescriptor( new BasicResourceBasedSecurity.BasicResourceBasedSecurityDescriptor(this) );
        addDescriptor( new PublicACL.PublicACLDescriptor(this) );

        addDescriptor( new Artist.ArtistDescriptor(root) );
        addDescriptor( new Venue.VenueDescriptor(root) );
        addDescriptor( new Concert.ConcertDescriptor(root) );
        addDescriptor( new Festival.FestivalDescriptor(root) );
       
        //addDescriptor( new Follow.FollowDescriptor(this) );
        addDescriptor( new FollowAction.FollowActionDescriptor(this) );
        addExtension(new FollowLayout());
        
        addExtension(new ACLFiller());

        //addDescriptor( new  );

        //addExtension( ImageFileType.class, new ImageFileType(  ) );

        /*
        mainMenu.addItem( "Main", new Menu.MenuItem( "New Content", "/new/", ACL.Permission.READ ) );
        mainMenu.addItem( "Main", new Menu.MenuItem( "Upload", "/upload/", ACL.Permission.READ ) );
        mainMenu.addItem( "Main", new Menu.MenuItem( "Configure", "/configuration/", ACL.Permission.ADMIN ) );
        mainMenu.addItem( "Main", new Menu.MenuItem( "Search", "/search2/", ACL.Permission.ALL ) );
        */

        /*
        MongoDBQuery query = new MongoDBQuery().is( "username", "anonymous" );
        MongoDocument d = MongoDBCollection.get( Core.RESOURCES_COLLECTION_NAME ).findOne( query );
        Profile a = getNode( this, d );
        setAnonymous( a );
        */
    }

    public File getSignaturePath() {
        return signaturePath;
    }

    public String getSignatureURL() {
        return "signatures/";
    }

    @Override
    public String getDefaultTemplate() {
        return "org/seventyeight/web/main.vm";
    }
}
