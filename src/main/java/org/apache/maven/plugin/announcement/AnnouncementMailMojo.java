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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.announcement.mailsender.ProjectJavamailMailSender;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.mailsender.MailMessage;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Goal which sends an announcement through email.
 *
 * @author aramirez@exist.com
 * @version $Id$
 * @goal announcement-mail
 * @execute goal="announcement-generate"
 * @since 2.0-beta-2
 * @threadSafe
 */
public class AnnouncementMailMojo
    extends AbstractAnnouncementMojo
{
    //=========================================
    // announcement-mail goal fields
    //=========================================

    /**
     * Possible senders.
     *
     * @parameter expression="${project.developers}"
     * @required
     * @readonly
     */
    private List from;

    /**
     * The id of the developer sending the announcement mail. Only used if the <tt>mailSender</tt>
     * attribute is not set. In this case, this should match the id of one of the developers in
     * the pom. If a matching developer is not found, then the first developer in the pom will be
     * used.
     *
     * @parameter expression="${changes.fromDeveloperId}"
     */
    private String fromDeveloperId;

    /**
     * Mail content type to use.
     * @parameter default-value="text/plain"
     * @required
     * @since 2.1
     */
    private String mailContentType;

    /**
     * Defines the sender of the announcement email. This takes precedence over the list
     * of developers specified in the POM.
     * if the sender is not a member of the development team. Note that since this is a bean type,
     * you cannot specify it from command level with <pre>-D</pre>. Use 
     * <pre>-Dchanges.sender='Your Name &lt;you@domain>'</pre> instead.
     *
     * @parameter expression="${changes.mailSender}"
     */
    private MailSender mailSender;
    
    /**
     * Defines the sender of the announcement. This takes precedence over both ${changes.mailSender}
     * and the list of developers in the POM. 
     * 
     * This parameter parses an email address in standard RFC822 format, e.g.
     * <pre>-Dchanges.sender='Your Name &lt;you@domain>'</pre>.
     *
     * @parameter expression="${changes.sender}"
     * @since 2.7
     */
    private String senderString;

    /**
     * The password used to send the email.
     *
     * @parameter expression="${changes.password}"
     */
    private String password;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Smtp Server.
     *
     * @parameter expression="${changes.smtpHost}"
     * @required
     */
    private String smtpHost;

    /**
     * Port.
     *
     * @parameter default-value="25" expression="${changes.smtpPort}"
     * @required
     */
    private int smtpPort;

    /**
     * If the email should be sent in SSL mode.
     *
     * @parameter default-value="false" expression="${changes.sslMode}"
     */
    private boolean sslMode;


    /**
     * Subject for the email.
     *
     * @parameter default-value="[ANNOUNCEMENT] - ${project.name} ${project.version} released" expression="${changes.subject}"
     * @required
     */
    private String subject;

    /**
     * The Velocity template used to format the announcement.
     *
     * @parameter default-value="announcement.vm" expression="${changes.template}"
     * @required
     */
    private String template;

    /**
     * Directory which contains the template for announcement email.
     *
     * @parameter expression="${project.build.directory}/announcement"
     * @required
     */
    private File templateOutputDirectory;

    /**
     * Recipient email address.
     *
     * @parameter
     * @required
     */
    private List toAddresses;

    /**
     * Recipient cc email address.
     *
     * @parameter
     * @since 2.5
     */
    private List ccAddresses;

    /**
     * Recipient bcc email address.
     *
     * @parameter
     * @since 2.5
     */
    private List bccAddresses;

    /**
     * The username used to send the email.
     *
     * @parameter expression="${changes.username}"
     */
    private String username;

    private ProjectJavamailMailSender mailer = new ProjectJavamailMailSender();

    public void execute()
        throws MojoExecutionException
    {
        // Run only at the execution root
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the announcement mail in this project because it's not the Execution Root" );
        }
        else
        {
            File templateFile = new File( templateOutputDirectory, template );

            ConsoleLogger logger = new ConsoleLogger( Logger.LEVEL_INFO, "base" );

            if ( getLog().isDebugEnabled() )
            {
                logger.setThreshold( Logger.LEVEL_DEBUG );
            }

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

            if ( templateFile.isFile() )
            {
                getLog().info( "Connecting to Host: " + getSmtpHost() + ":" + getSmtpPort() );

                sendMessage();
            }
            else
            {
                throw new MojoExecutionException( "Announcement template " + templateFile + " not found..." );
            }
        }
    }

    /**
     * Send the email.
     *
     * @throws MojoExecutionException if the mail could not be sent
     */
    protected void sendMessage()
        throws MojoExecutionException
    {
        File templateFile = new File( templateOutputDirectory, template );
        String email = "";
        final MailSender ms = getActualMailSender();
        final String fromName = ms.getName();
        final String fromAddress = ms.getEmail();
        if ( fromAddress == null || fromAddress.equals( "" ) )
        {
            throw new MojoExecutionException( "Invalid mail sender: name and email is mandatory (" + ms + ")." );
        }
        getLog().info( "Using this sender for email announcement: " + fromAddress + " < " + fromName + " > " );
        try
        {
            MailMessage mailMsg = new MailMessage();
            mailMsg.setSubject( getSubject() );
            mailMsg.setContent( IOUtil.toString( readAnnouncement( templateFile ) ) );
            mailMsg.setContentType( this.mailContentType );
            mailMsg.setFrom( fromAddress, fromName );

            final Iterator it = getToAddresses().iterator();
            while ( it.hasNext() )
            {
                email = it.next().toString();
                getLog().info( "Sending mail to " + email + "..." );
                mailMsg.addTo( email, "" );
            }

            if(getCcAddresses() != null)
            {
                final Iterator it2 = getCcAddresses().iterator();
                while ( it2.hasNext() )
                {
                    email = it2.next().toString();
                    getLog().info( "Sending cc mail to " + email + "..." );
                    mailMsg.addCc( email, "" );
                }
            }

            if(getBccAddresses() != null)
            {
                final Iterator it3 = getBccAddresses().iterator();
                while ( it3.hasNext() )
                {
                    email = it3.next().toString();
                    getLog().info( "Sending bcc mail to " + email + "..." );
                    mailMsg.addBcc( email, "" );
                }
            }

            mailer.send( mailMsg );
            getLog().info( "Sent..." );
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

    /**
     * Read the announcement generated file.
     *
     * @param file the file to be read
     * @return fileReader Return the FileReader
     * @throws MojoExecutionException if the file could not be found
     */
    protected FileReader readAnnouncement( File file )
        throws MojoExecutionException
    {
        FileReader fileReader;
        try
        {
            fileReader = new FileReader( file );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( "File not found. " + file );
        }
        return fileReader;
    }

    /**
     * Returns the identify of the mail sender according to the plugin's configuration:
     * <ul>
     * <li>if the <tt>mailSender</tt> parameter is set, it is returned</li>
     * <li>if no <tt>fromDeveloperId</tt> is set, the first developer in the list is returned</li>
     * <li>if a <tt>fromDeveloperId</tt> is set, the developer with that id is returned</li>
     * <li>if the developers list is empty or if the specified id does not exist, an exception is thrown</li>
     * </ul>
     *
     * @return the mail sender to use
     * @throws MojoExecutionException if the mail sender could not be retrieved
     */
    protected MailSender getActualMailSender()
        throws MojoExecutionException
    {
        if (senderString != null) 
        {
            try
            {
                InternetAddress ia = new InternetAddress(senderString, true);
                return new MailSender(ia.getPersonal(), ia.getAddress());
            }
            catch ( AddressException e )
            {
                throw new MojoExecutionException("Invalid value for change.sender: ", e);
            }
        }
        if ( mailSender != null && mailSender.getEmail() != null )
        {
            return mailSender;
        }
        else if ( from == null || from.isEmpty() )
        {
            throw new MojoExecutionException(
                "The <developers> section in your pom should not be empty. Add a <developer> entry or set the "
                    + "mailSender parameter." );
        }
        else if ( fromDeveloperId == null )
        {
            final Developer dev = (Developer) from.get( 0 );
            return new MailSender( dev.getName(), dev.getEmail() );
        }
        else
        {
            final Iterator it = from.iterator();
            while ( it.hasNext() )
            {
                Developer developer = (Developer) it.next();

                if ( fromDeveloperId.equals( developer.getId() ) )
                {
                    return new MailSender( developer.getName(), developer.getEmail() );
                }
            }
            throw new MojoExecutionException(
                "Missing developer with id '" + fromDeveloperId + "' in the <developers> section in your pom." );
        }
    }

    //================================
    // announcement-mail accessors
    //================================

    public List getBccAddresses()
    {
        return bccAddresses;
    }

    public void setBccAddresses( List bccAddresses )
    {
        this.bccAddresses = bccAddresses;
    }

    public List getCcAddresses()
    {
        return ccAddresses;
    }

    public void setCcAddresses( List ccAddresses )
    {
        this.ccAddresses = ccAddresses;
    }

    public List getFrom()
    {
        return from;
    }

    public void setFrom( List from )
    {
        this.from = from;
    }

    public String getFromDeveloperId()
    {
        return fromDeveloperId;
    }

    public void setFromDeveloperId( String fromDeveloperId )
    {
        this.fromDeveloperId = fromDeveloperId;
    }

    public MailSender getMailSender()
    {
        return mailSender;
    }

    public void setMailSender( MailSender mailSender )
    {
        this.mailSender = mailSender;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

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

    public boolean isSslMode()
    {
        return sslMode;
    }

    public void setSslMode( boolean sslMode )
    {
        this.sslMode = sslMode;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    public String getTemplate()
    {
        return template;
    }

    public void setTemplate( String template )
    {
        this.template = template;
    }

    public File getTemplateOutputDirectory()
    {
        return templateOutputDirectory;
    }

    public void setTemplateOutputDirectory( File templateOutputDirectory )
    {
        this.templateOutputDirectory = templateOutputDirectory;
    }

    public List getToAddresses()
    {
        return toAddresses;
    }

    public void setToAddresses( List toAddresses )
    {
        this.toAddresses = toAddresses;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }
}
