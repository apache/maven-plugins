package org.apache.maven.plugin.antrun;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.antrun.components.AntTargetConverter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 */
public abstract class AbstractAntMojo
    extends AbstractMojo
{
    
    /**
     * The plugin dependencies.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List artifacts;
    
    protected void executeTasks( Target antTasks, MavenProject mavenProject )
        throws MojoExecutionException
    {
        try
        {
            //TODO refactor - place the manipulation of the expressionEvaluator into a separated class.
            ExpressionEvaluator exprEvaluator = (ExpressionEvaluator) antTasks.getProject().getReference(AntTargetConverter.MAVEN_EXPRESSION_EVALUATOR_ID);

            Project antProject = antTasks.getProject();

            PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper( antProject );

            propertyHelper.setNext( new AntPropertyHelper( exprEvaluator, getLog() ) );

            DefaultLogger antLogger = new DefaultLogger();
            antLogger.setOutputPrintStream( System.out );
            antLogger.setErrorPrintStream( System.err );
            antLogger.setMessageOutputLevel( Project.MSG_INFO );

            antProject.addBuildListener( antLogger );
            antProject.setBaseDir( mavenProject.getBasedir() );

            Path p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getArtifacts().iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.dependency.classpath", p );
            p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getCompileClasspathElements().iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.compile.classpath", p );
            p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getRuntimeClasspathElements().iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.runtime.classpath", p );
            p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getTestClasspathElements().iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.test.classpath", p );

            /* set maven.plugin.classpath with plugin dependencies */
            List list = new ArrayList( artifacts.size() );

            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();
                File file = a.getFile();
                if ( file == null )
                {
                    throw new DependencyResolutionRequiredException( a );
                }
                list.add( file.getPath() );
            }
            p = new Path( antProject );
            p.setPath( StringUtils.join( list.iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.plugin.classpath", p );

            getLog().info( "Executing tasks" );

            antTasks.execute();

            getLog().info( "Executed tasks" );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing ant tasks", e );
        }
    }
}
