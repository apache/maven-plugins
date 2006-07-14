package org.apache.maven.announcement;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.announcement.mailsender.ProjectJavamailMailSender;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Goal which sends an announcement through email.
 *
 * @goal announcement-mail
 * @execute goal="announcement-generate"
 * @author aramirez@exist.com
 * @version $Id$
 */
public class AnnouncementMailMojo extends AbstractMojo
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
     * Recipient email address.
     *
     * @parameter 
     * @required
     */
    private List toAddresses;
    
    /**
     * Sender.
     *
     * @parameter expression="${project.developers}"
     * @required
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
    
    
    public void execute() throws MojoExecutionException
    {              
        template = templateOutputDirectory + "/" + template;
        
        ConsoleLogger logger = new ConsoleLogger( 0, "base" );      
        
        mailer.enableLogging( logger );        
        
        mailer.setSmtpHost( getSmtpHost() );
        
        mailer.setSmtpPort( getSmtpPort() );
        
        mailer.setSslMode( sslMode );
        
        if( username != null )
        {
            mailer.setUsername( username );
        }
        
        if( password != null )
        {
            mailer.setPassword( password );
        }
        mailer.initialize();
        
        if( isTextFileExisting( template ) )
        {  
            getLog().info( "Connecting to Host: " + getSmtpHost() + ":" + getSmtpPort() );

            sendMessage(  );
        }
        else
        {               
            if( template != null )
            {
                if( isTextFileExisting( template ) )
                {
                    getLog().info( "Connecting to Host: " + getSmtpHost() + " : " + getSmtpPort() );

                    sendMessage(  );
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
     * Send the email 
     *
     * @throws MojoExecutionException
     */
    protected void sendMessage() throws MojoExecutionException
    {
        String email = "";
        
        try
        {
            int i=0;
            
            String[] from = getFirstDevInfo( getFrom() );
            
            while( i < getToAddresses().size() )
            {   
                email = getToAddresses().get( i ).toString();

                getLog().info( "Sending mail... " + email );

                mailer.send( getSubject(), IOUtil.toString( readAnnouncement( template ) ), email, "", from[0], from[1] );

                getLog().info("Sent...");                
                
                i++;
            }      
        }
        catch( IOException ioe )
        {
            throw new MojoExecutionException( "Failed to send email.", ioe );
        }
        catch( MailSenderException e )
        {
            throw new MojoExecutionException( "Failed to send email < " + email + " >", e );
        }        
    }
    
    protected boolean isTextFileExisting( String fileName )
    {
        boolean found = false;
        
        File f = new File( fileName );

        if( f.exists() )
        {
            found = true;
        }
        return found;
    }
    
    /**
     * Read the announcement generated file
     * @param  fileName         Accepts filename to be read.
     * @return  fileReader      Return the FileReader.
     */
    public FileReader readAnnouncement( String fileName ) throws MojoExecutionException
    {   
        FileReader fileReader = null;
        
        try
        {
            File file = new File( fileName );
            
            fileReader = new FileReader( file );
        }
        catch( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( "File not found. " + fileName );
        }
        return fileReader;
    }
    
    /**
     * Retrieve the 1st name and email address found in the developers list
     * @param fromNames         Accepts List of developers.
     * @return fromAddress      Returns the 1st email address found in the list.
     */
    public String[] getFirstDevInfo( List fromNames ) throws MojoExecutionException
    {
        String fromAddress = "";
        
        String fromName = "";
        
        String[] info = new String[2];
        
        if( fromNames.size() > 0 )
        {
            Developer developer = ( Developer ) fromNames.get( 0 );

            fromAddress = developer.getEmail();
            
            fromName = developer.getName();

            info[0] = fromAddress;
            
            info[1] = fromName;
            
            getLog().info( "email retrieved. " + fromAddress + " < " + fromName + " > " );

            if( fromAddress == null  || fromAddress.equals( "" ) )
            {
                throw new MojoExecutionException( "Email address in <developers> section is required." );
            }
        }
        else
        {
            throw new MojoExecutionException( "Email address in <developers> section is required." );
        }
        return info;
    }
        
    //================================
    // announcement-mail accessors
    //================================
    
    public String getSmtpHost() 
    {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) 
    {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() 
    {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) 
    {
        this.smtpPort = smtpPort;
    }

    public String getSubject() 
    {
        return subject;
    }

    public void setSubject(String subject) 
    {
        this.subject = subject;
    }

    public List getFrom() 
    {
        return from;
    }

    public void setFrom(List from) 
    {
        this.from = from;
    }

    public MavenProject getProject() 
    {
        return project;
    }

    public void setProject(MavenProject project) 
    {
        this.project = project;
    }

    public List getToAddresses() 
    {
        return toAddresses;
    }

    public void setToAddresses(List toAddresses) 
    {
        this.toAddresses = toAddresses;
    }
}
