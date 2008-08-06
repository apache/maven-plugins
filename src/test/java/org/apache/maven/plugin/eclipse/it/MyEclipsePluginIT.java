package org.apache.maven.plugin.eclipse.it;

/**
 * Unit Tests for MyEclipse plugin
 * 
 * @author <a href="mailto:olivier.jacob@gmail.com">Olivier Jacob</a>
 */
public class MyEclipsePluginIT
    extends AbstractEclipsePluginIT
{
    /**
     * Web project, no spring/struts/hibernate capability, J2EE 1.3
     * 
     * @throws Exception
     */
    public void testMyEclipseProject01()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-01" );
    }

    /**
     * Web project, no spring/struts/hibernate capability, J2EE 1.4
     * 
     * @throws Exception
     */
    public void testMyEclipseProject02()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-02" );
    }

    /**
     * Simple project with Spring capability
     * 
     * @throws Exception
     */
    public void testMyEclipseProject03()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-03" );
    }

    /**
     * Simple project with Spring and Hibernate capabilities
     * 
     * @throws Exception
     */
    public void testMyEclipseProject04()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-04" );
    }

    /**
     * Simple project with additionalConfig
     *
     * @throws Exception
     */
    public void testMyEclipseProject05()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-05" );
    }

    private void testMyEclipseProject( String project )
        throws Exception
    {
        testProject( project, null, "myeclipse-clean", "myeclipse" );
    }
}
