package org.apache.maven.test;

import org.codehaus.surefire.SurefireBooter;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.util.List;

/**
 * @goal test
 *
 * @description Run tests using surefire
 *
 * @prereq compiler:compile
 * @prereq compiler:testCompile
 * @prereq resources:resources
 * @prereq resources:testResources
 *
 * @parameter
 *  name="mavenRepoLocal"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#maven.repo.local"
 *  description=""
 * @parameter
 *  name="basedir"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#basedir"
 *  description=""
 * @parameter
 *  name="classesDirectory"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#project.build.output"
 *  description=""
 * @parameter
 *  name="testClassesDirectory"
 *  type="String"
 *  required="true"
 *  validator="validator"
 *  expression="#project.build.testOutput"
 *  description=""
 * @parameter
 *  name="includes"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.unitTest.includes"
 *  description=""
 * @parameter
 *  name="excludes"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.unitTest.excludes"
 *  description=""
 * @parameter
 *  name="classpathElements"
 *  type="String[]"
 *  required="true"
 *  validator=""
 *  expression="#project.classpathElements"
 *  description=""
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 *
 * @todo make version of junit and surefire configurable
 * @todo make report to be produced configurable
 */
public class SurefirePlugin
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String mavenRepoLocal = (String) request.getParameter( "mavenRepoLocal" );

        String basedir = (String) request.getParameter( "basedir" );

        String classesDirectory = (String) request.getParameter( "classesDirectory" );

        String testClassesDirectory = (String) request.getParameter( "testClassesDirectory" );

        List includes = (List) request.getParameter( "includes" );

        List excludes = (List) request.getParameter( "excludes" );

        String[] classpathElements = (String[]) request.getParameter( "classpathElements" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        System.setProperty( "basedir", basedir );

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{basedir, includes, excludes} );

        // TODO: there should be a better way to construct a classpath
        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( classesDirectory ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory ).getPath() );

        for ( int i = 0; i < classpathElements.length; i++ )
        {
            surefireBooter.addClassPathUrl( classpathElements[i] );
        }

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReport" );

        boolean success = surefireBooter.run();

        if ( !success )
        {
            response.setExecutionFailure( true, new SurefireFailureResponse( null ) );
        }
    }
}
