package org.apache.maven.plugin.eclipse.writers.rad;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.eclipse.writers.AbstractWtpResourceWriter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates a .settings folder for Eclipse WTP 1.x release and writes out the
 * configuration under it.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 */
public class RadWebsiteConfigWriter
    extends AbstractEclipseWriter
{

    private static final String WEBSITE_CONFIG_FILENAME = ".website-config";

    private static final String WEBSITE_CONFIG_STRUCTURE = "structure";

    private static final String WEBSITE_CONFIG_VERSION = "version";

    private static final String WEBSITE_CONFIG_WEBSITE = "website";

    /**
     * write the website-config file for RAD6 if needed.
     * 
     * @see AbstractWtpResourceWriter#write(EclipseSourceDir[],
     *      ArtifactRepository, File)
     * @param sourceDirs
     *            all eclipse source directorys
     * @param localRepository
     *            the local reposetory
     * @param buildOutputDirectory
     *            build output directory (target)
     * @throws MojoExecutionException
     *             when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        MavenProject project = config.getProject();

        FileWriter w;
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( project.getPackaging() ) )
        {
            try
            {
                w = new FileWriter( new File( config.getEclipseProjectDirectory(), WEBSITE_CONFIG_FILENAME ) );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
            }
            XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
            writeModuleTypeFacetCore( writer );
            IOUtil.close( w );
        }
    }

    /**
     * write the website-config file.
     * 
     * @param writer
     *            wher to write to
     */
    private void writeModuleTypeFacetCore( XMLWriter writer )
    {
        writer.startElement( WEBSITE_CONFIG_WEBSITE );
        writer.addAttribute( WEBSITE_CONFIG_VERSION, "600" );
        writer.startElement( WEBSITE_CONFIG_STRUCTURE );
        writer.endElement();
        writer.endElement();
    }
}
