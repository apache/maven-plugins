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
    public void testProject01()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-01" );
    }

    /**
     * Web project, no spring/struts/hibernate capability, J2EE 1.4
     * 
     * @throws Exception
     */
    public void testProject02()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-02" );
    }

    /**
     * Simple project with Spring capability
     * 
     * @throws Exception
     */
    public void testProject03()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-03" );
    }

    /**
     * Simple project with Spring and Hibernate capabilities
     * 
     * @throws Exception
     */
    public void testProject04()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-04" );
    }

    /**
     * Simple project with additionalConfig
     * 
     * @throws Exception
     */
    public void testProject05()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-05" );
    }

    /**
     * Simple project with with spring configuration that points at non-existent directory
     * 
     * @throws Exception
     */
    public void testMyEclipseProject06MECLIPSE427()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-06-MECLIPSE-427" );
    }

    /**
     * Verifies spring files created with sub-projects (modules) module-1 should have spring bean files in the
     * .springBeans file. module-2 should not have spring bean files in the .springBeans file.
     * 
     * @throws Exception
     */
    public void testProject07MECLIPSE445()
        throws Exception
    {
        testMyEclipseProject( "project-myeclipse-07-MECLIPSE-445" );
    }

    public void testMyEclipseProject( String project )
        throws Exception
    {
        testProject( project, null, "myeclipse-clean", "myeclipse" );
    }

}
