package org.apache.maven.plugin.checkstyle.rss;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.checkstyle.CheckstyleReport;
import org.apache.maven.plugin.checkstyle.CheckstyleResults;
import org.apache.maven.plugin.checkstyle.VelocityTemplate;
import org.apache.maven.reporting.MavenReportException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/**
 * @author Olivier Lamy
 * @since 2.4
 * @plexus.component role="org.apache.maven.plugin.checkstyle.rss.CheckstyleRssGenerator"
 *                   role-hint="default"
 */
public class DefaultCheckstyleRssGenerator
    implements CheckstyleRssGenerator
{
   
    /**
     * @plexus.requirement
     */
    private VelocityComponent velocityComponent;

    
    /**
     * @see org.apache.maven.plugin.checkstyle.rss.CheckstyleRssGenerator#generateRSS(org.apache.maven.plugin.checkstyle.CheckstyleResults)
     */
    public void generateRSS( CheckstyleResults results, CheckstyleRssGeneratorRequest checkstyleRssGeneratorRequest )
        throws MavenReportException
    {

        VelocityTemplate vtemplate = new VelocityTemplate( velocityComponent, CheckstyleReport.PLUGIN_RESOURCES );
        vtemplate.setLog( checkstyleRssGeneratorRequest.getLog() );

        Context context = new VelocityContext();
        context.put( "results", results );
        context.put( "project", checkstyleRssGeneratorRequest.getMavenProject() );
        context.put( "copyright", checkstyleRssGeneratorRequest.getCopyright() );
        context.put( "levelInfo", SeverityLevel.INFO );
        context.put( "levelWarning", SeverityLevel.WARNING );
        context.put( "levelError", SeverityLevel.ERROR );
        context.put( "stringutils", new StringUtils() );

        try
        {
            vtemplate.generate( checkstyleRssGeneratorRequest.getOutputDirectory().getPath() + "/checkstyle.rss",
                                "checkstyle-rss.vm", context );
        }
        catch ( ResourceNotFoundException e )
        {
            throw new MavenReportException( "Unable to find checkstyle-rss.vm resource.", e );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Unable to generate checkstyle.rss.", e );
        }
        catch ( VelocityException e )
        {
            throw new MavenReportException( "Unable to generate checkstyle.rss.", e );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to generate checkstyle.rss.", e );
        }
    }
    
}
