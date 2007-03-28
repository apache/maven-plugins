package org.apache.maven.plugin.enforcer;

import java.io.File;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.DefaultRuntimeInformation;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.composition.CompositionException;
import org.codehaus.plexus.component.composition.UndefinedComponentComposerException;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.factory.ComponentInstantiationException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfigurationResourceException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class MockPlexusContainer
    implements PlexusContainer
{

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#addComponentDescriptor(org.codehaus.plexus.component.repository.ComponentDescriptor)
     */
    public void addComponentDescriptor( ComponentDescriptor theComponentDescriptor )
        throws ComponentRepositoryException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#addContextValue(java.lang.Object,
     *      java.lang.Object)
     */
    public void addContextValue( Object theKey, Object theValue )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#addJarRepository(java.io.File)
     */
    public void addJarRepository( File theRepository )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#addJarResource(java.io.File)
     */
    public void addJarResource( File theResource )
        throws PlexusContainerException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#composeComponent(java.lang.Object,
     *      org.codehaus.plexus.component.repository.ComponentDescriptor)
     */
    public void composeComponent( Object theComponent, ComponentDescriptor theComponentDescriptor )
        throws CompositionException, UndefinedComponentComposerException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#createChildContainer(java.lang.String,
     *      java.util.List, java.util.Map)
     */
    public PlexusContainer createChildContainer( String theName, List theClasspathJars, Map theContext )
        throws PlexusContainerException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#createChildContainer(java.lang.String,
     *      java.util.List, java.util.Map, java.util.List)
     */
    public PlexusContainer createChildContainer( String theName, List theClasspathJars, Map theContext,
                                                 List theDiscoveryListeners )
        throws PlexusContainerException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#createComponentInstance(org.codehaus.plexus.component.repository.ComponentDescriptor)
     */
    public Object createComponentInstance( ComponentDescriptor theComponentDescriptor )
        throws ComponentInstantiationException, ComponentLifecycleException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#dispose()
     */
    public void dispose()
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getChildContainer(java.lang.String)
     */
    public PlexusContainer getChildContainer( String theName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptor(java.lang.String)
     */
    public ComponentDescriptor getComponentDescriptor( String theComponentKey )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptorList(java.lang.String)
     */
    public List getComponentDescriptorList( String theRole )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptorMap(java.lang.String)
     */
    public Map getComponentDescriptorMap( String theRole )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getComponentRealm(java.lang.String)
     */
    public ClassRealm getComponentRealm( String theComponentKey )
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getContainerRealm()
     */
    public ClassRealm getContainerRealm()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getContext()
     */
    public Context getContext()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getCreationDate()
     */
    public Date getCreationDate()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getLogger()
     */
    public Logger getLogger()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#getLoggerManager()
     */
    public LoggerManager getLoggerManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#hasChildContainer(java.lang.String)
     */
    public boolean hasChildContainer( String theName )
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#hasComponent(java.lang.String)
     */
    public boolean hasComponent( String theComponentKey )
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#hasComponent(java.lang.String,
     *      java.lang.String)
     */
    public boolean hasComponent( String theRole, String theRoleHint )
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#initialize()
     */
    public void initialize()
        throws PlexusContainerException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#isInitialized()
     */
    public boolean isInitialized()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#isStarted()
     */
    public boolean isStarted()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.String)
     */
    public Object lookup( String theComponentKey )
        throws ComponentLookupException
    {
        if ( theComponentKey.equals( MavenProject.class.getName() ) )
        {
            return new MavenProject();
        }
        else if ( theComponentKey.equals( RuntimeInformation.class.getName() ) )
        {
            return new DefaultRuntimeInformation();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.String,
     *      java.lang.String)
     */
    public Object lookup( String theRole, String theRoleHint )
        throws ComponentLookupException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#lookupList(java.lang.String)
     */
    public List lookupList( String theRole )
        throws ComponentLookupException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#lookupMap(java.lang.String)
     */
    public Map lookupMap( String theRole )
        throws ComponentLookupException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#registerComponentDiscoveryListener(org.codehaus.plexus.component.discovery.ComponentDiscoveryListener)
     */
    public void registerComponentDiscoveryListener( ComponentDiscoveryListener theListener )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#release(java.lang.Object)
     */
    public void release( Object theComponent )
        throws ComponentLifecycleException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#releaseAll(java.util.Map)
     */
    public void releaseAll( Map theComponents )
        throws ComponentLifecycleException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#releaseAll(java.util.List)
     */
    public void releaseAll( List theComponents )
        throws ComponentLifecycleException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#removeChildContainer(java.lang.String)
     */
    public void removeChildContainer( String theName )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#removeComponentDiscoveryListener(org.codehaus.plexus.component.discovery.ComponentDiscoveryListener)
     */
    public void removeComponentDiscoveryListener( ComponentDiscoveryListener theListener )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#resume(java.lang.Object)
     */
    public void resume( Object theComponent )
        throws ComponentLifecycleException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#setConfigurationResource(java.io.Reader)
     */
    public void setConfigurationResource( Reader theConfiguration )
        throws PlexusConfigurationResourceException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#setLoggerManager(org.codehaus.plexus.logging.LoggerManager)
     */
    public void setLoggerManager( LoggerManager theLoggerManager )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#setParentPlexusContainer(org.codehaus.plexus.PlexusContainer)
     */
    public void setParentPlexusContainer( PlexusContainer theParentContainer )
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#start()
     */
    public void start()
        throws PlexusContainerException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.plexus.PlexusContainer#suspend(java.lang.Object)
     */
    public void suspend( Object theComponent )
        throws ComponentLifecycleException
    {
        // TODO Auto-generated method stub

    }

}
