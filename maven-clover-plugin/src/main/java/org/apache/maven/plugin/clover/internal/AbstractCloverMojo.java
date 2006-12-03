/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.plugin.clover.internal;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Taskdef;
import org.codehaus.plexus.resource.ResourceManager;

import java.io.File;

/**
 * Common code for all Clover plugin build Mojos.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public abstract class AbstractCloverMojo extends AbstractMojo
{
    /**
     * The location of the <a href="http://cenqua.com/clover/doc/adv/database.html">Clover database</a>.
     *
     * @parameter expression="${project.build.directory}/clover/clover.db"
     * @required
     */
    private String cloverDatabase;

    /**
     * The location of the merged clover database to create when running a report in a multimodule build.
     *
     * @parameter expression="${project.build.directory}/clover/cloverMerge.db"
     * @required
     */
    private String cloverMergeDatabase;

    /**
     * A Clover license file to be used by the plugin. If not specified, the Clover plugin uses a default evaluation
     * license.
     *
     * @parameter
     * @deprecated As of Clover plugin v2.1, use licenseLocation instead
     */
    private String licenseFile;

    /**
     * A Clover license file to be used by the plugin. The plugin tries to resolve this parameter first as a resource,
     * then as a URL, and then as a file location on the filesystem.
     *
     * @parameter
     */
    private String licenseLocation;

    /**
     * The <a href="http://cenqua.com/clover/doc/adv/flushpolicies.html">Clover flush policy</a> to use.
     * Valid values are <code>directed</code>, <code>interval</code> and <code>threaded</code>.
     *  
     * @parameter default-value="threaded"
     */
    private String flushPolicy;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum period between flush
     * operations (in milliseconds).
     *
     * @parameter default-value="500"
     */
    private int flushInterval;

    /**
     * If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover database before running
     * any query on it.
     * 
     * <p>Note: The only use case where you would want to turn this off is if you're running your tests in a separate
     * JVM. In that case the coverage data will be flushed by default upon the JVM shutdown and there would be no need
     * to wait for the data to be flushed. As we can't control whether users want to fork their tests or not, we're
     * offering this parameter to them.</p>
     * 
     * @parameter default-value="true"
     */
    private boolean waitForFlush;

    /**
     * Whether the Clover instrumentation should use the Clover <code>jdk14</code> or <code>jdk15</code> flags to
     * parse sources.
     *
     * @parameter
     */
    private String jdk;

    /**
     * The Maven project instance for the executing project.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Resource manager used to locate any Clover license file provided by the user.
     * @component
     */
    private ResourceManager resourceManager;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {
        registerLicenseFile();
    }

    public void setResourceManager(ResourceManager resourceManager)
    {
        this.resourceManager = resourceManager;
    }

    public ResourceManager getResourceManager()
    {
        return this.resourceManager;
    }

    /**
     * Registers the license file for Clover runtime by setting the <code>clover.license.path</code> system property.
     * If the user has configured the <code>licenseLocation</code> parameter the plugin tries to resolve it first as a
     * resource, then as a URL, and then as a file location on the filesystem. If the <code>licenseLocation</code>
     * parameter has not been defined by the user we look up a default Clover license in the classpath in
     * <code>/clover.license</code>.
     *
     * @throws MojoExecutionException when the license file cannot be found
     */
    protected void registerLicenseFile() throws MojoExecutionException
    {
        String license;

        if (this.licenseLocation != null)
        {
            try
            {
                license = getResourceManager().getResourceAsFile(this.licenseLocation).getPath();
                getLog().debug("Loading license from classpath [" + license + "]");
            }
            catch (Exception e)
            {
                throw new MojoExecutionException("Failed to load license file [" + this.licenseLocation + "]", e);
            }
        }
        else if (this.licenseFile != null)
        {
            getLog().warn("Deprecation warning: please use licenseLocation instead of LicenseFile");
            license = this.licenseFile;
        }
        else
        {
            license = getClass().getResource("/clover.license").getFile();
        }

        getLog().debug("Using license file [" + license + "]");
        System.setProperty("clover.license.path", license);
    }

    /**
     * Register the Clover Ant tasks against a fake Ant {{@link Project}} object so that we can the tasks later on.
     * This is the Java equivalent of the <code>taskdef</code> call that you would need in your Ant
     * <code>build.xml</code> file if you wanted to use the Clover Ant tasks from Ant.
     * </p>
     * Note: We're defining this method as static because it is also required in the report mojo and reporting mojos
     * and main mojos cannot share anything right now. See http://jira.codehaus.org/browse/MNG-1886.
     *
     * @return A {{@link Project}} instance with the Clover Ant tasks registered in it
     */
    public static Project registerCloverAntTasks()
    {
        Project antProject = new Project();
        antProject.init();

        Taskdef taskdef = (Taskdef) antProject.createTask( "taskdef" );
        taskdef.setResource( "clovertasks" );
        taskdef.execute();

        return antProject;
    }

    /**
     * Wait 2*'flush interval' milliseconds to ensure that the coverage data have been flushed to the Clover database.
     * 
     * TODO: This method should not be static but we need it static here because we cannot share code
     * between non report mojos and main build mojos. See http://jira.codehaus.org/browse/MNG-1886 
     */
    public static void waitForFlush(boolean waitForFlush, int flushInterval)
    {
        if ( waitForFlush )
        {
            try
            {
                Thread.sleep( 2 * flushInterval );
            }
            catch ( InterruptedException e )
            {
                // Nothing to do... Just go on and try to check for coverage.
            }
        }
    }

    /**
     * Check if a Clover database exists (either a single module Clover database or an aggregated one).
     * @return true if a Clover database exists.
     */
    protected boolean areCloverDatabasesAvailable()
    {
        boolean shouldRun = false;

        File singleModuleCloverDatabase = new File( this.cloverDatabase );
        File mergedCloverDatabase = new File ( this.cloverMergeDatabase );

        if (singleModuleCloverDatabase.exists() || mergedCloverDatabase.exists() )
        {
            shouldRun = true;
        }

        return shouldRun;
    }

    protected void setLicenseLocation(String licenseLocation)
    {
        this.licenseLocation = licenseLocation;
    }  

    protected MavenProject getProject()
    {
        return this.project;
    }

    protected boolean getWaitForFlush()
    {
        return this.waitForFlush;
    }

    protected String getJdk()
    {
        return this.jdk;
    }

    protected String getCloverDatabase()
    {
        return this.cloverDatabase;
    }

    protected String getCloverMergeDatabase()
    {
        return this.cloverMergeDatabase;
    }

    protected int getFlushInterval()
    {
        return this.flushInterval;
    }

    protected String getFlushPolicy()
    {
        return this.flushPolicy;
    }
}
