package org.apache.maven.plugin.announcement;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.announcement.mailsender.ProjectJavamailMailSender;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Goal which sends an announcement through email.
 *
 * @goal announcement-mail
 * @execute goal="announcement-generate"
 * @author aramirez@exist.com
 * @version $Id$
 */
public class AnnouncementMailMojo
    extends AbstractMojo
{
    //=========================================
    // announcement-mail goal fields
    //=========================================

    /**
     * @parameter expression=${project}
     * @readonly
     */
    private MavenProject project;

    /**
     * Smtp Server.
     *
     * @parameter
     * @required
     */
    private String smtpHost;

    /**
     * Port.
     *
     * @parameter default-value="25";
     * @required
     */
    private int smtpPort;

    /**
     * The username used to send the email.
     *
     * @parameter
     */
    private String username;

    /**
     * The password used to send the email.
     *
     * @parameter
     */
    private String password;

    /**
     * If the email should be sent in SSL mode.
     *
     * @parameter default-value="false"
     */
    private boolean sslMode;

    /**
     * Subject for the email.
     *
     * @parameter default-value="[ANNOUNCEMENT] - ${project.artifactId} ${project.version} release!"
     * @required
     */
    private String subject;

    /**
     * The id of the developer sending the announcement mail. This should match
     * the id of one of the developers in the pom. If a matching developer is
     * not found, then the first developer in the pom will be used.
     *
     * @parameter expression="${changes.fromDeveloperId}"
     */
    private String fromDeveloperId;

    /**
     * Recipient email address.
     *
     * @parameter 
     * @required
     */
    private List toAddresses;

    /**
     * Possible senders.
     *
     * @parameter expression="${project.developers}"
     * @required
     * @readonly
     */
    private List from;

    /**
     * Directory which contains the template for announcement email.
     *
     * @parameter expression="${project.build.directory}/announcement"
     * @required
     */
    private String templateOutputDirectory;

    /**
     * The Velocity template used to format the announcement.
     *
     * @parameter default-value="announcement.vm"
     * @required
     */
    private String template;

    private ProjectJavamailMailSender mailer = new ProjectJavamailMailSender();

    public void execute()
        throws MojoExecutionException
    {
        template = templateOutputDirectory + "/" + template;

        ConsoleLogger logger = new ConsoleLogger( 0, "base" );

        mailer.enableLogging( logger );

        mailer.setSmtpHost( getSmtpHost() );

        mailer.setSmtpPort( getSmtpPort() );

        mailer.setSslMode( sslMode );

        if ( username != null )
        {
            mailer.setUsername( username );
        }

        if ( password != null )
        {
            mailer.setPassword( password );
        }
        mailer.initialize();

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "fromDeveloperId: " + getFromDeveloperId() );
        }

        if ( isTextFileExisting( template ) )
        {
            getLog().info( "Connecting to Host: " + getSmtpHost() + ":" + getSmtpPort() );

            sendMessage();
        }
        else
        {
            if ( template != null )
            {
                if ( isTextFileExisting( template ) )
                {
                    getLog().info( "Connecting to Host: " + getSmtpHost() + " : " + getSmtpPort() );

                    sendMessage();
                }
                else
                {
                    throw new MojoExecutionException( "Announcement template " + template + " not found..." );
                }
            }
            else
            {
                throw new MojoExecutionException( "Announcement template " + template + " not found..." );
            }
        }
    }

    /**
     * Send the email.
     *
     * @throws MojoExecutionException
     */
    protected void sendMessage()
        throws MojoExecutionException
    {
        String email = "";

        try
        {
            int i = 0;

            Developer sendingDeveloper;
            if ( getFromDeveloperId() != null )
            {
                sendingDeveloper = getDeveloperById( getFromDeveloperId(), getFrom() );
            }
            else
            {
                sendingDeveloper = getFirstDeveloper( getFrom() );
            }

            String fromName = sendingDeveloper.getName();
            String fromAddress = sendingDeveloper.getEmail();

            getLog().info( "Using this sender for email announcement: " + fromAddress + " < " + fromName + " > " );

            if ( fromAddress == null || fromAddress.equals( "" ) )
            {
                throw new MojoExecutionException( "Email address in <developers> section is required." );
            }


            while ( i < getToAddresses().size() )
            {
                email = getToAddresses().get( i ).toString();

                getLog().info( "Sending mail... " + email );

                mailer.send( getSubject(), IOUtil.toString( readAnnouncement( template ) ),
                             email, "", fromAddress, fromName );

                getLog().info( "Sent..." );

                i++;
            }
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Failed to send email.", ioe );
        }
        catch ( MailSenderException e )
        {
            throw new MojoExecutionException( "Failed to send email < " + email + " >", e );
        }
    }

    protected boolean isTextFileExisting( String fileName )
    {
        boolean found = false;

        File f = new File( fileName );

        if ( f.exists() )
        {
            found = true;
        }
        return found;
    }

    /**
     * Read the announcement generated file.
     * 
     * @param  fileName         Accepts filename to be read.
     * @return  fileReader      Return the FileReader.
     */
    public FileReader readAnnouncement( String fileName )
        throws MojoExecutionException
    {
        FileReader fileReader = null;

        try
        {
            File file = new File( fileName );

            fileReader = new FileReader( file );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( "File not found. " + fileName );
        }
        return fileReader;
    }

    /**
     * Retrieve the first name and email address found in the developers list.
     *
     * @param developers A List of developers
     * @return The first developer in the list
     */
    protected Developer getFirstDeveloper( List developers )
        throws MojoExecutionException
    {
        if ( developers.size() > 0 )
        {
            return (Developer) developers.get( 0 );
        }
        else
        {
            throw new MojoExecutionException( "Email address is required in the <developers> section in your pom." );
        }
    }

    /**
     * Retrieve the developer with the given id.
     *
     * @param developers A List of developers
     * @return The developer in the list with the specified id
     */
    protected Developer getDeveloperById( String id, List developers )
        throws MojoExecutionException
    {
        Iterator it = developers.iterator();
        while ( it.hasNext() )
        {
            Developer developer = (Developer) it.next();

            if ( id.equals( developer.getId() ) )
            {
                return developer;
            }

        }

        throw new MojoExecutionException( "Missing developer with id '"  + id
            + "' in the <developers> section in your pom." );
    }

    //================================
    // announcement-mail accessors
    //================================

    public String getSmtpHost()
    {
        return smtpHost;
    }

    public void setSmtpHost( String smtpHost )
    {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort()
    {
        return smtpPort;
    }

    public void setSmtpPort( int smtpPort )
    {
        this.smtpPort = smtpPort;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    public List getFrom()
    {
        return from;
    }

    public void setFrom( List from )
    {
        this.from = from;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public List getToAddresses()
    {
        return toAddresses;
    }

    public void setToAddresses( List toAddresses )
    {
        this.toAddresses = toAddresses;
    }

    public String getFromDeveloperId()
    {
        return fromDeveloperId;
    }

    public void setFromDeveloperId( String fromDeveloperId )
    {
        this.fromDeveloperId = fromDeveloperId;
    }
}
