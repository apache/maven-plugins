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
        testProject( "project-myeclipse-01", null, "myeclipse-clean", "myeclipse" );
    }

    /**
     * Web project, no spring/struts/hibernate capability, J2EE 1.4
     * 
     * @throws Exception
     */
    public void testProject02()
        throws Exception
    {
        testProject( "project-myeclipse-02", null, "myeclipse-clean", "myeclipse" );
    }

    /**
     * Simple project with Spring capability
     * 
     * @throws Exception
     */
    public void testProject03()
        throws Exception
    {
        testProject( "project-myeclipse-03", null, "myeclipse-clean", "myeclipse" );
    }

    /**
     * Simple project with Spring and Hibernate capabilities
     * 
     * @throws Exception
     */
    public void testProject04()
        throws Exception
    {
        testProject( "project-myeclipse-04", null, "myeclipse-clean", "myeclipse" );
    }

    /**
     * Simple project with additionalConfig
     * 
     * @throws Exception
     */
    public void testProject05()
        throws Exception
    {
        testProject( "project-myeclipse-05", null, "myeclipse-clean", "myeclipse" );
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
        testProject( "project-myeclipse-07-MECLIPSE-445", null, "myeclipse-clean", "myeclipse" );
    }

}
