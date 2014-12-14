package org.seventyeight.web;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seventyeight.TokenList;
import org.seventyeight.cache.SessionCache;
import org.seventyeight.database.mongodb.*;
import org.seventyeight.loader.Loader;
import org.seventyeight.utils.ClassUtils;
import org.seventyeight.utils.FileUtilities;
import org.seventyeight.utils.PostMethod;
import org.seventyeight.web.authentication.*;
import org.seventyeight.web.authorization.ACL;
import org.seventyeight.web.authorization.AccessControlled;
import org.seventyeight.web.extensions.ExtensionGroup;
import org.seventyeight.web.handlers.template.TemplateManager;
import org.seventyeight.web.model.*;
import org.seventyeight.web.model.AbstractExtension.ExtensionDescriptor;
import org.seventyeight.web.nodes.Group;
import org.seventyeight.web.nodes.User;
import org.seventyeight.web.runners.AutonomousRunner;
import org.seventyeight.web.runners.RedirectRunner;
import org.seventyeight.web.servlet.Request;
import org.seventyeight.web.servlet.Response;
import org.seventyeight.web.themes.DefaultTheme;
import org.seventyeight.web.utilities.ExecuteUtils;
import org.seventyeight.web.utilities.ResourceBundles;

import javax.servlet.http.Cookie;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cwolfgang
 */
public abstract class Core implements CoreSystem {

    private static Logger logger = LogManager.getLogger( Core.class );

    public static final String NAME_FIELD = "title";

    public static final String MAIN_TEMPLATE = "org/seventyeight/web/main.vm";

    public static final String TEMPLATE_PATH_NAME = "templates";
    public static final String THEMES_PATH_NAME = "themes";
    public static final String PLUGINS_PATH_NAME = "plugins";
    public static final String CACHE_PATH_NAME = "cache";


    protected TemplateManager templateManager = new TemplateManager();

    protected ResourceBundles resourceBundles;

    protected Authentication authentication = new SimpleAuthentication( this );
    protected SessionManager sessionManager = new SessionManager( this );

    protected Map<String, Searchable> searchables = new ConcurrentHashMap<String, Searchable>(  );

    protected Map<String, String> searchKeyMap = new ConcurrentHashMap<String, String>(  );

    protected ConcurrentHashMap<String, NaturalSearchable> naturalSearchables = new ConcurrentHashMap<String, NaturalSearchable>(  );

    /** ACL implementation, that serves as a backup if no ACL is defined */
    protected ACL acl;

    /**
     * The default anonymous {@link User}
     */
    protected User anonymous;

    /**
     * The collection name for {@link Node}s
     */
    public static final String NODES_COLLECTION_NAME = "nodes";

    /**
     * The collection name for {@link Descriptor}s
     */
    public static final String DESCRIPTOR_COLLECTION_NAME = "descriptors";

    protected org.seventyeight.loader.ClassLoader classLoader = null;
    protected Loader pluginLoader;

    /* Database */
    protected MongoDBManager dbManager;
    protected MongoDatabase db;

    protected Menu mainMenu = new Menu();

    /** A map of all the extenion groups */
    protected Map<String, ExtensionGroup> extensionGroups = new HashMap<String, ExtensionGroup>(  );

    /**
     * A map of descriptors keyed by their supers class
     */
    protected Map<Class<?>, Descriptor<?>> descriptors = new HashMap<Class<?>, Descriptor<?>>();

    //protected Map<String, Map<String, AbstractExtension.ExtensionDescriptor<?>>> extensionDescriptors = new HashMap<String, Map<String, AbstractExtension.ExtensionDescriptor<?>>>();

    /**
     * A map of interfaces corresponding to specific {@link Descriptor}s<br />
     * This is used to map an extension class/interface to those {@link Describable}s {@link Descriptor}s implementing them.
     */
    protected Map<Class, List<Descriptor>> descriptorList = new HashMap<Class, List<Descriptor>>();

    protected Map<Class<?>, List> extensionsList = new HashMap<Class<?>, List>();

    //protected ConcurrentMap<String, N>

    /**
     * A {@link Map} of top level actions, given by its name
     */
    //private ConcurrentMap<String, TopLevelGizmo> topLevelGizmos = new ConcurrentHashMap<String, TopLevelGizmo>();



    /**
     * Default {@link Group} that has no one in it
     */
    protected Group noneGroup;

    /**
     * Default {@link Group} that has all in it
     */
    protected Group addGroup;

    /**
     * Path to the ...
     */
    protected File path;
    protected File orientdbPath;
    protected File pluginsPath;
    protected File uploadPath;
    protected File cachePath;

    protected SessionCache documentCache;
    protected SessionCache sessionCache;

    protected File portraitPath;

    /**
     * This path contains the themes. Each theme in a sub directory
     */
    protected File themesPath;


    protected Theme defaultTheme = new DefaultTheme();

    /** Root node */
    protected RootNode root;

    public static class Relations {
        public static final String EXTENSIONS = "extensions";
    }

    public Core( RootNode root, File path, String dbname ) throws CoreException {

        /* Initialize database */
        try {
            dbManager = new MongoDBManager( dbname );
        } catch( UnknownHostException e ) {
            throw new IllegalArgumentException( e );
        }
        db = dbManager.getDatabase();

        /* Initialize paths */
        this.path = path;
        this.uploadPath = new File( path, "upload" );

        // Caching
        SessionCache.reset( 1000 );
        this.documentCache = new SessionCache( new MongoDatabaseStrategy( NODES_COLLECTION_NAME ) );
        this.documentCache.setAutoFlush( true );
        this.sessionCache = new SessionCache( new MongoDatabaseStrategy( SessionManager.SESSIONS_COLLECTION_NAME ) );
        this.sessionCache.setAutoFlush( true );
        this.cachePath = new File( path, CACHE_PATH_NAME );
        this.portraitPath = new File( path, "portraits" );

        addDescriptor( new Session.SessionsDescriptor(this) );

        /* Default groups */
        // Group.de Maybe not???!?!?!?!

        /* Class loader */
        classLoader = new org.seventyeight.loader.ClassLoader( Thread.currentThread().getContextClassLoader() );
        this.pluginLoader = new Loader( classLoader );

        resourceBundles = new ResourceBundles( classLoader );

        searchKeyMap.put( "title", "title" );

        this.root = root;
    }

    public Core initialize() {

        /* TODO something with install */

        /* Other stuff */

        return this;
    }

    public MongoDatabase getDatabase() {
        return db;
    }

    public SessionCache getDocumentCache() {
        return documentCache;
    }

    public SessionCache getSessionCache() {
        return sessionCache;
    }

    public RootNode getRoot() {
        return root;
    }

    /**
     * Get an object given a {@link MongoDocument}
     * @param document
     * @param <T>
     * @return
     * @throws ItemInstantiationException
     */
    public <T extends PersistedNode> T getNode( Node parent, MongoDocument document ) throws ItemInstantiationException {
        String clazz = null;
        try {
            clazz = document.get( "class" );
        } catch( Exception e ) {
            throw new ItemInstantiationException( "Field \"class\" not found.", e );
        }

        if( clazz == null ) {
            logger.warn( "Class property not found" );
            throw new ItemInstantiationException( "\"class\" property not found for " + document );
        }
        //logger.debug( "ModelObject class: " + clazz );
        //logger.debug( "PARENT: " + parent );

        try {
            Class<PersistedNode> eclass = (Class<PersistedNode>) Class.forName(clazz, true, classLoader );
            Constructor<?> c = eclass.getConstructor( Core.class, Node.class, MongoDocument.class );
            return (T) c.newInstance( this, parent, document );
        } catch( Exception e ) {
            logger.error( "Unable to get the class " + clazz );
            throw new ItemInstantiationException( "Unable to get the class " + clazz, e );
        }
    }

    public <T extends Node> T getNodeById( Node parent, String id ) throws ItemInstantiationException, NotFoundException {
        //logger.debug( "Getting node by id: " + id );
        if(id == null) {
            throw new IllegalArgumentException( "Id not provided" );
        }
        //MongoDocument d = MongoDBCollection.get( NODES_COLLECTION_NAME ).getDocumentById( id );
        MongoDocument d = documentCache.get( id );

        if(d != null) {
            PersistedNode obj = getNode( parent, d );

            return (T) obj;
        } else {
            throw new NotFoundException( "Could not find node with id " + id );
        }
    }
    
    /*
    public <T extends Node> T getNode(String title, String type) {
    	MongoDBQuery query = new MongoDBQuery().is("title", title).is("type", type);
    	List<MongoDocument> docs = MongoDBCollection.get(NODES_COLLECTION_NAME).find(query);
    	if() {
    		
    	}
    }
    */

    public void saveNode(AbstractNode<?> node) {
    	logger.debug("Saving {} to cache", this);
        documentCache.save( node.getDocument(), node.getIdentifier() );
    }

    /*
    public Object resolve( String path ) {

        return null;
    }
    */

    public MongoDocument getId(MongoDBQuery query) {
        MongoDocument docs = MongoDBCollection.get( NODES_COLLECTION_NAME ).findOne( query, (MongoDocument) new MongoDocument().set( "_id", true ) );
        logger.debug( "DOCS FOR IDS: {}", docs );
        return docs;
    }

    public List<String> getIds(MongoDBQuery query, int offset, int number, MongoDocument sort) {
        List<MongoDocument> docs = MongoDBCollection.get( NODES_COLLECTION_NAME ).find( query, offset, number, sort, "_id" );
        logger.debug( "DOCS FOR IDS: {}", docs );
        
        List<String> ids = new ArrayList<String>(docs.size());
        
        for(MongoDocument d : docs) {
        	ids.add(d.getIdentifier());
        }
        
        return ids;
    }

    /**
     * Render the path from the URL
     * @throws Throwable 
     */
    public Runner render( CallContext request ) throws Throwable {

        request.getStopWatch().start( "Resolve node" );
        TokenList tokens = new TokenList( request.getUri() );
        Node node = null;
        Exception exception = null;
        try {
            node = resolveNode( tokens, request.getUser() );
            logger.debug("Found node {}", node );
            if( !tokens.isEndsWithSlash() && tokens.isEmpty() ) {
                return new RedirectRunner(request.getUri() + "/");
            }
        } catch( NotFoundException e ) {
            logger.debug( "Exception is set to {}", e );
            exception = e;
        } catch( Exception e ) {
            throw new CoreException( e );
        }

        if( node instanceof Autonomous ) {
            logger.debug( "{} is autonomous", node );
            // Make autonomous runner
			//((Autonomous)node).autonomize( request, response );
			return new AutonomousRunner((Autonomous)node);
        }

        request.getStopWatch().stop( "Resolve node" );

        request.getStopWatch().start( "Rendering" );

        /* Only try to find a valid view if there was a valid node found on the path */
        if( node != null ) {
                try {
                    if( tokens.isEmpty() && exception == null ) {
                        logger.debug( "Executing last node" );
                        return ExecuteUtils.getRunner( request, node, "index" );
                    }

                    return renderObject( node, exception, request, tokens );
                } catch( CoreException ce ) {
                    throw ce;
                } catch( Exception e ) {
                    throw new CoreException( e.getMessage(), e );
                }
        } else {
            //Response.NOT_FOUND_404.render( request, response, exception );
            if( exception instanceof CoreException ) {
                throw (CoreException)exception;
            } else {
                throw new HttpException( exception ).setCode( 404 );
            }
        }

    }

    private Runner renderObject( Object obj, Exception exception, CallContext request, TokenList tokens ) throws Throwable {
        logger.debug( "Rendering object {}, tokens: {}", obj, tokens );
        switch( tokens.left() ) {
                /* If the last token on the path is a valid node */
            case 0:
                return ExecuteUtils.getRunner( request, obj, "index" );

                /* Typically, this happens if a node has an action, either as a view or doSomething */
            case 1:
                String view = tokens.next();
                return ExecuteUtils.getRunner( request, obj, view );

                /* Generate a 404 if there are more tokens, because this means, that a valid node was not found */
            default:
                //Response.NOT_FOUND_404.render( request, response, exception );
                if( exception != null ) {
                    throw new CoreException( exception );
                } else {
                    throw new NotFoundException( request.getUri() );
                }
        }
    }


    /**
     * Resolve the {@link org.seventyeight.web.model.Node}s from a path. <br />
     * @param tokens
     * @return The last valid {@link Node} on the path, adding the extra tokens, if any, to the the token list.
     */
    public Node resolveNode( TokenList tokens, User user ) throws NotFoundException, UnsupportedEncodingException, ItemInstantiationException, NoAuthorizationException {
        //logger.debug( "Resolving " + path );
        //StringTokenizer tokenizer = new StringTokenizer( URLDecoder.decode( path, "ISO-8859-1" ), "/" );
        //StringTokenizer tokenizer = new StringTokenizer( path, "/" );

        Node current = root;
        Node next = null;
        Node last = root;

        while( tokens.hasMore() ) {
            String token = tokens.next();
            logger.debug( "Current: {}", current );
            logger.debug( "Token  : {}", token );

            /* Find a child node */

            // TEST
            if(current instanceof PersistedNode) {
                logger.fatal( "CURRENT NODES CORE IS {}", ( (PersistedNode) current ).getCore() );
            }

            next = null;
            if( current instanceof Parent ) {
                next = ((Parent)current).getChild( token );
            }
            logger.debug( "Found node is {}", next );

            /* If there's no child, try an action */
            if( next == null ) {

                //AbstractExtension.ExtensionDescriptor<?> d = extensionDescriptors.get( "action" ).get( token );
                //List<AbstractExtension.ExtensionDescriptor> ds = getExtensionDescriptors( AbstractExtension.ExtensionDescriptor.class );
                //List<AbstractExtension.ExtensionDescriptor> ds = getExtensionDescriptors( Action.class );
            	List<Action.ActionDescriptor<?>> ds = getExtensionDescriptors( Action.class );
                logger.debug( "DS: " + ds );
                for( Action.ActionDescriptor<?> d : ds) {
                    logger.debug( "Found descriptor {} for {}", d, token );
                    if( d != null && d.getExtensionName().equals( token ) ) {
                        if( d.isApplicable( current ) ) {
                            if( current instanceof PersistedNode ) {
                                next = d.getExtension( this, (PersistedNode) current );
                                logger.debug( "Found action is {}", next );
                            } else if( current instanceof Descriptor ) {
                                next = d.getExtension( this, (Descriptor) current );
                            }
                        }

                        break;
                    }
                }

                /* If there ain't any actions */
                if( next == null ) {
                    tokens.backup();
                    break;
                }
            }

            if( next instanceof Autonomous ) {
                logger.debug( "{} is autonomous", current );
                return next;
            }

            /* TODO Something about authorization? */
            if(next instanceof AccessControlled) {
                logger.debug("ACL: {}", ((AccessControlled)next).getACL());
                logger.debug("ACL: {}", ((AccessControlled)next).getACL().getPermission( user ));
                if( ((AccessControlled)next).getACL().getPermission( user ).ordinal() < ACL.Permission.READ.ordinal() ) {
                    throw new NoAuthorizationException( user + " was not authorized to " + next );
                }
            }

            /*
            if( current instanceof Parent ) {
            } else {
                tokens.backup();
                break;
            }
            */

            current = next;
            last = next;
        }

        return last;
    }

    /*
    public Object resolveObject( Actionable parent, LinkedList<String> tokens ) {
        Action action = null;
        Object lastObject = parent;
        //for( String token : tokens ) {
        while( !tokens.isEmpty() ) {
            String token = tokens.peek();
            logger.debug( "Popped " + token );

            action = parent.getAction( token );

            if( action == null ) {
                return lastObject;
            }

            if( action instanceof Actionable ) {
                parent = (Actionable) action;
            }

            lastObject = action;

            tokens.pop();
        }

        return lastObject;
    }
    */

    public void addDescriptor( Descriptor<?> descriptor ) throws CoreException {
    	logger.info("----====> {} <====----", descriptor.getClazz());
        logger.debug( "Adding {}, {}", descriptor, descriptor.getClazz() );
        this.descriptors.put( descriptor.getClazz(), descriptor );

        /* Determine if the descriptor has something to be loaded */
        descriptor.loadConfiguration();

        List<Class<?>> interfaces = ClassUtils.getInterfaces( descriptor.getClazz() );
        interfaces.addAll( ClassUtils.getClasses( descriptor.getClazz() ) );
        for( Class<?> i : interfaces ) {
            //logger.debug( "INTERFACE: " + i );
            List<Descriptor> list = null;
            if( !descriptorList.containsKey( i ) ) {
                descriptorList.put( i, new ArrayList<Descriptor>() );
            }
            list = descriptorList.get( i );

            list.add( descriptor );
        }

        // Add instances of node descriptors to the root
        if( descriptor instanceof NodeDescriptor ) {
            NodeDescriptor nd = (NodeDescriptor) descriptor;
            root.addNode( nd.getUrlName(), nd );
        }

        /* Find searchables */
        List<Searchable> ss = descriptor.getSearchables();
        for( Searchable s : ss ) {
            //searchables.put( s.getMethodName(), s );
        	addSearchable(s);
        }

        // Search key maps
        searchKeyMap.putAll( descriptor.getSearchKeyMap() );

        /**/
        if( descriptor instanceof NaturalSearchable ) {
            naturalSearchables.put( ( (NaturalSearchable) descriptor ).getType(), ( NaturalSearchable )descriptor );
        }

        /*
        if( descriptor instanceof AbstractExtension.ExtensionDescriptor ) {
            addExtensionsGroup( (AbstractExtension.ExtensionDescriptor) descriptor );
        }
        */

        
        addExtension( descriptor );


        descriptor.initialize();
        /**/
        //descriptor.configureIndex( db );
    }
    
    /**
     * Get an {@link Action} for a given node given the name.
     */
    public <T extends Action<T>> Action<T> getActionByName(PersistedNode node, String name) throws ItemInstantiationException {
    	List<Action.ActionDescriptor<T>> ds = getExtensionDescriptors( Action.class );
    	for(Action.ActionDescriptor<T> d : ds) {
    		if(d.getExtensionName().equalsIgnoreCase(name)) {
    			Action<T> next = (Action<T>) d.getExtension( this, node );
    			return next;
    		}
    	}
    	
    	return null;
    }

    public void addExtensionsGroup(AbstractExtension.ExtensionDescriptor<?> descriptor) {
        if(!extensionGroups.containsKey( descriptor.getExtensionClass().getName() )) {
            ExtensionGroup extensionGroup = descriptor.getExtensionGroup();
            logger.debug( "Adding extension group, {}", extensionGroup );
            extensionGroups.put( extensionGroup.getClazz().getName(), extensionGroup );
        }

        logger.debug( "Adding {} to extensions groups", descriptor.getClazz() );
        ExtensionGroup group = extensionGroups.get( descriptor.getExtensionClass().getName() );
        logger.debug( "GROUT: {}", group );
        group.addDescriptor( descriptor );
    }

    public ExtensionGroup getExtensionGroup(String clazz) {
        return extensionGroups.get( clazz );
    }

    public NaturalSearchable getNaturalSearchable( String type ) {
        return naturalSearchables.get( type );
    }

    public Map<String, NaturalSearchable> getNaturalSearchables() {
        return naturalSearchables;
    }

    public void addSearchable( Searchable s ) {
        searchables.put( s.getMethodName(), s );
    }

    public Map<String, String> getSearchKeyMap() {
        return searchKeyMap;
    }

    public Map<String, Searchable> getSearchables() {
        return searchables;
    }

    public Descriptor<?> getDescriptor( String className ) throws ClassNotFoundException {
        return getDescriptor( Class.forName( className ) );
    }

    /**
     * Get the mapping from  the {@link Describable} class to a {@link Descriptor} instance.
     * @param clazz
     * @param <T>
     * @return
     */
    public <T extends Descriptor> T getDescriptor( Class<?> clazz ) {
        logger.debug( "Getting descriptor for " + clazz );

        if( descriptors.containsKey( clazz ) ) {
            return (T) descriptors.get( clazz );
        } else {
            return null;
        }
    }

    public List<Descriptor> getExtensionDescriptors( String clazz ) throws ClassNotFoundException {
        return getExtensionDescriptors( Class.forName( clazz ) );
    }

    /**
     * Get a list of {@link Descriptor}s whose {@link Describable} implements the given interface
     *
     *
     * @param clazz The interface in question
     * @return
     */
    public <T extends Descriptor> List<T> getExtensionDescriptors( Class clazz ) {

        if( descriptorList.containsKey( clazz ) ) {
            return (List<T>) descriptorList.get( clazz );
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<Descriptor<?>> getAllDescriptors() {
        return descriptors.values();
    }

    public List<Descriptor> getCreatableDescriptors() {
        return getExtensionDescriptors( CreatableNode.class );
    }

    /**
     * Add an extension implementing a certain interface.
     */
    public <T> void addExtension( Object extension ) {
        logger.debug( "Adding " + extension );

        List<Class<?>> interfaces = ClassUtils.getInterfaces( extension.getClass() );
        interfaces.addAll( ClassUtils.getClasses( extension.getClass() ) );
        
        for( Class<?> i : interfaces ) {
            if( !extensionsList.containsKey( i ) ) {
                extensionsList.put( i, new ArrayList<T>() );
            }

            extensionsList.get( i ).add( extension );
        }

        // Verify document if documented
        if( extension instanceof Configurable ) {
            try {
                ( (Configurable) extension ).loadConfiguration();
            } catch( CoreException e ) {
                logger.log( Level.ERROR, "Unable to load the configuration for {}.", extension, e );
            }
        }
    }

    /**
     * Get a list of extensions implementing a certain interface.
     */
    public <T> List<T> getExtensions( Class<T> type ) {
        if( extensionsList.containsKey( type ) ) {
            return extensionsList.get( type );
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Save the {@link TopLevelNode} given a {@link Node}.
     */
    public static void superSave( Node node ) {
        logger.debug( "Saving node " + node );

        while( node != null ) {
            if( node instanceof TopLevelNode ) {
                logger.debug( "Saving top level node " + node );
                ((TopLevelNode)node).save();
                return;
            }

            node = node.getParent();
        }

        throw new IllegalStateException( "No top level node to save" );
    }

    public static <T extends TopLevelNode> T superGet( Node node ) {
        logger.debug( "Super getting node " + node );

        while( node != null ) {
            if( node instanceof TopLevelNode ) {
                return (T) node;
            }

            node = node.getParent();
        }

        throw new IllegalStateException( "No top level node" );
    }

    public List<Descriptor<?>> getACLs() {
        //return getExtensions( ACL.class );
        return getExtensionDescriptors( ACL.class );
    }

    public ACL getACL() {
        return acl;
    }

    public Theme getDefaultTheme() {
        return defaultTheme;
    }

    public String getDefaultTemplate() {
        return "org/seventyeight/web/main.vm";
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ResourceBundles getMessages() {
        return resourceBundles;
    }

    public void setAnonymous( User anonymous ) {
        this.anonymous = anonymous;
    }

    public User getAnonymousUser() {
        return anonymous;
    }

    public static final String NUMBERS_COLLECTION = "numbers";

    /**
     * Given a {@link org.seventyeight.web.model.NodeDescriptor} a unique name is returned.
     * @param d NodeDescriptor
     * @return
     */
    public synchronized String getUniqueName( NodeDescriptor d ) {
        logger.debug( "Getting unique name for " + d.getType() );
        MongoDocument doc = MongoDBCollection.get( NUMBERS_COLLECTION ).findOne( new MongoDBQuery().getId( d.getType() ) );
        if( doc.isNull() ) {
            logger.debug( "Numbers for " + d.getType() + " was null, creating document" );
            doc = new MongoDocument(  ).set( "_id", d.getType() ).set( "number", 1 );
            //MongoDBCollection.get( NUMBERS_COLLECTION ).save( doc );
        }

        int i = doc.get( "number" );
        String name = d.getType() + "-" + i;
        doc.set( "number", ++i );

        MongoDBCollection.get( NUMBERS_COLLECTION ).save( doc );

        return name;
    }

    /**
     * From the given path, get all jars and extract them to their directories
     * @param basePath
     * @return
     * @throws IOException
     */
    public static List<File> extractPlugins( File basePath ) throws IOException {
        logger.debug( "Extracting plugins to " + PLUGINS_PATH_NAME );
        File path = new File( basePath, PLUGINS_PATH_NAME );
        File themes = new File( basePath, THEMES_PATH_NAME );

        FileUtils.deleteDirectory( themes );
        themes.mkdir();

        File[] files = path.listFiles( FileUtilities.getExtension( "jar" ) );

        List<File> plugins = new ArrayList<File>();
        for( File f : files ) {
            logger.debug( "Extracting plugin " + f );
            String p = f.getName();
            p = p.substring( 0, ( p.length() - 4 ) );
            File op = new File( path, p );
            FileUtils.deleteDirectory( op );

            FileUtilities.extractArchive( f, op );
            plugins.add( op );

            /* Copy any theme directory to path */
            File pthemes = new File( op, "themes" );
            if( pthemes.exists() ) {
                logger.debug( "Copying themes from " + pthemes + " to " + themes );
                FileUtils.copyDirectory( pthemes, themes );
            }
        }

        return plugins;
    }

    public File getThemeFile( Theme theme, Theme.Platform platform, String filename ) throws IOException {
        logger.debug( "Getting theme file {} for {}/{}", filename, theme, platform );

        File themePath = new File( new File( themesPath, theme.getName() ), platform.getName() );
        File themeFile = new File( themePath, filename );

        logger.debug( "Getting theme file " + themeFile );

        if( themeFile.exists() ) {
            return themeFile;
        }

        throw new IOException( "Theme file " + themeFile + " does not exist" );
    }

    public void setThemesPath( File path ) {
        this.themesPath = path;
    }

    public File getCachePath() {
        return cachePath;
    }

    public org.seventyeight.loader.ClassLoader getClassLoader() {
        return classLoader;
    }

    public void getPlugins( List<File> plugins ) {
        for( File p : plugins ) {
            logger.debug( "Plugin " + p );
            try {
                /* Maybe check for classes directory */
                //pluginLoader.load( p, "" );
                pluginLoader.load( p );
            } catch( Exception e ) {
                logger.error( "Unable to load " + p, e );
            }
        }
    }

    public File getPath() {
        return path;
    }

    public File getUploadPath() {
        return uploadPath;
    }

    public File getPortrataitPath() {
        return portraitPath;
    }

    public Menu getMainMenu() {
        return mainMenu;
    }
    
    public <T extends Node> T find(Node node, Class<T> find) {
    	logger.debug("Finding {} with class {}", node, find);
    	while(node != null) {
    		logger.debug("NODE: {}", node);
    		if(find.isInstance(node)) {
    			logger.debug("WHOOP, node is {}", node);
    			return (T) node;
    		}
    		node = node.getParent();
    	}
    	
    	return null;
    }

}
