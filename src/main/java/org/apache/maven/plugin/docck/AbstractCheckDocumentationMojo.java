package org.apache.maven.plugin.docck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.IOUtil;

/**
 * Performs the heavy lifting for documentation checks. This is designed to be
 * reused for other types of projects, too.
 * 
 * @author jdcasey
 *
 */
public abstract class AbstractCheckDocumentationMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${reactorProjects}"
     * @readonly
     * @required
     */
    private List reactorProjects;

    /**
     * An optional location where the results should be written.
     * 
     * @parameter expression="${output}"
     */
    private File output;

    private HttpClient httpClient;

    private FileSetManager fileSetManager = new FileSetManager();

    protected AbstractCheckDocumentationMojo()
    {
        httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 5000 );
    }

    protected List getReactorProjects()
    {
        return reactorProjects;
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( output != null )
        {
            getLog().info( "Writing documentation survey results to: " + output );
        }

        Map errors = new LinkedHashMap();
        boolean hasErrors = false;

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( !approveProjectPackaging( project.getPackaging() ) )
            {
                getLog().info( "Skipping non-plugin project: " + project.getName() );
                continue;
            }

            List projectErrors = checkProject( project );

            hasErrors = hasErrors || !projectErrors.isEmpty();

            errors.put( project, projectErrors );
        }

        String messages;

        if ( hasErrors )
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append( "\nThe following documentation problems were found:\n" );

            for ( Iterator it = errors.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                MavenProject project = (MavenProject) entry.getKey();
                List projectErrors = (List) entry.getValue();

                if ( !projectErrors.isEmpty() )
                {
                    buffer.append( "\no " ).append( project.getName() );
                    buffer.append( " (" ).append( projectErrors.size() ).append( " errors)" );

                    for ( Iterator errorIterator = projectErrors.iterator(); errorIterator.hasNext(); )
                    {
                        String error = (String) errorIterator.next();

                        buffer.append( "\n\t- " ).append( error );
                    }

                    buffer.append( "\n" );
                }
            }

            messages = buffer.toString();
        }
        else
        {
            messages = "No documentation errors were found.";
        }

        try
        {
            writeMessages( messages );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing results to output file: " + output );
        }

        if ( hasErrors )
        {
            String logLocation;
            if ( output == null )
            {
                logLocation = "Please see the console output above for more information.";
            }
            else
            {
                logLocation = "Please see \'" + output + "\' for more information.";
            }

            throw new MojoFailureException( "documentation check", "Documentation errors were found.", logLocation );
        }
    }

    protected abstract boolean approveProjectPackaging( String packaging );

    private void writeMessages( String messages )
        throws IOException
    {
        if ( output != null )
        {
            FileWriter writer = null;

            try
            {
                writer = new FileWriter( output );
                writer.write( messages );
                writer.flush();
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        else
        {
            getLog().info( messages );
        }
    }

    private List checkProject( MavenProject project )
    {
        getLog().info( "Checking project: " + project.getName() );
        
        List errors = new ArrayList();

        // check for licenses
        List licenses = project.getLicenses();

        if ( licenses == null || licenses.isEmpty() )
        {
            errors.add( "No license(s) specified." );
        }
        else
        {
            for ( Iterator it = licenses.iterator(); it.hasNext(); )
            {
                License license = (License) it.next();

                String url = license.getUrl();
                
                String protocol = null;
                
                try
                {
                    URL licenseUrl = new URL( url );
                    
                    protocol = licenseUrl.getProtocol();
                    
                    if ( protocol != null )
                    {
                        protocol = protocol.toLowerCase();
                    }
                }
                catch ( MalformedURLException e )
                {
                    getLog().debug( "License: " + license.getName() + " with appears to have an invalid URL: \'" + url + "\'.\nError: "
                                + e.getMessage() + "\n\nTrying to access it as a file instead." );
                }

                if ( protocol != null && protocol.startsWith( "http" ) )
                {
                    HeadMethod headMethod = new HeadMethod( url );
                    headMethod.setFollowRedirects( true );
                    headMethod.setDoAuthentication( false );
                    
                    try
                    {
                        if ( httpClient.executeMethod( headMethod ) != 200 )
                        {
                            errors.add( "Cannot reach license: " + license.getName() + " with URL: \'" + url + "\'." );
                        }
                    }
                    catch ( HttpException e )
                    {
                        errors.add( "Cannot reach license: " + license.getName() + " with URL: \'" + url
                            + "\'.\nError: " + e.getMessage() );
                    }
                    catch ( IOException e )
                    {
                        errors.add( "Cannot reach license: " + license.getName() + " with URL: \'" + url
                            + "\'.\nError: " + e.getMessage() );
                    }
                    finally
                    {
                        if ( headMethod != null )
                        {
                            headMethod.releaseConnection();
                        }
                    }
                }
                else
                {
                    // try looking for the file.
                    File licenseFile = new File( url );
                    if ( !licenseFile.exists() )
                    {
                        errors.add( "License file: \'" + licenseFile.getPath() + " does not exist." );
                    }
                }
            }
        }

        File siteDirectory = new File( project.getBasedir(), "src/site" );

        // check for site.xml
        File siteXml = new File( siteDirectory, "site.xml" );

        if ( !siteXml.exists() )
        {
            errors.add( "site.xml is missing." );
        }

        // check for index.(xml|apt|html)
        if ( !findFiles( siteDirectory, "index" ) )
        {
            errors.add( "Missing site index.(html|xml|apt)." );
        }

        // check for usage.(xml|apt|html)
        if ( !findFiles( siteDirectory, "usage" ) )
        {
            errors.add( "Missing base usage.(html|xml|apt)." );
        }

        // check for **/examples/**.(xml|apt|html)
        if ( !findFiles( siteDirectory, "**/examples/*" ) && !findFiles( siteDirectory, "**/example*" ) )
        {
            errors.add( "Missing examples." );
        }

        checkPackagingSpecificDocumentation( project, errors, siteDirectory );

        return errors;
    }

    protected abstract void checkPackagingSpecificDocumentation( MavenProject project, List errors, File siteDirectory );

    private boolean findFiles( File siteDirectory, String pattern )
    {
        FileSet fs = new FileSet();
        fs.setDirectory( siteDirectory.getAbsolutePath() );
        fs.setFollowSymlinks( false );

        fs.addInclude( "apt/" + pattern + ".apt" );
        fs.addInclude( "xdoc/" + pattern + ".xml" );
        fs.addInclude( "resources/" + pattern + ".html" );

        String[] includedFiles = fileSetManager.getIncludedFiles( fs );

        return includedFiles != null && includedFiles.length > 0;
    }
}