package org.seventyeight.web.model;

import org.apache.log4j.Logger;
import org.seventyeight.database.Database;
import org.seventyeight.database.EdgeType;
import org.seventyeight.utils.Date;
import org.seventyeight.web.Core;
import org.seventyeight.web.SeventyEight;
import org.seventyeight.web.exceptions.UnableToInstantiateObjectException;
import org.seventyeight.web.model.extensions.ResourceExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class EntityDescriptor<T extends Entity> extends Descriptor<T> {

	private static Logger logger = Logger.getLogger( EntityDescriptor.class );


    public List<String> getRequiredJavascripts() {
        return Collections.EMPTY_LIST;
    }

	public T newInstance( ) throws ItemInstantiationException {
		logger.debug( "New instance of resource" );
		T instance = super.newInstance();

        Core.getInstance().setIdentifier( instance );
		instance.setCreated( new Date() );
        instance.getNode().set( "type", getType() );

        SeventyEight.getInstance().setIdentifier( instance );

        instance.getNode().save();

        /* Update index */
        instance.updateIndexes();

		return instance;
	}

    public boolean inputTitle() {
        return true;
    }

    public abstract String getType();

    @Override
    public EdgeType getRelationType() {
        return ResourceDescriptorRelation.resource;
    }


    @Override
    public List<Class> getExtensionClasses() {
        List<Class> classes = new ArrayList<Class>(1);

        classes.add( ResourceExtension.class );

        return classes;
    }
}
