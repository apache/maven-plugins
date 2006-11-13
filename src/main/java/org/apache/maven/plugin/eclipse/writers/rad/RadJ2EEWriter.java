package org.apache.maven.plugin.eclipse.writers.rad;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.eclipse.writers.AbstractWtpResourceWriter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates the .j2ee file for RAD6 for now write hardcoded: EJB version 2.1 WAR
 * version 2.4 EAR version 1.4 future releases could make these varriable.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 */
public class RadJ2EEWriter
    extends AbstractEclipseWriter
{

    private static final String J2EE_FILENAME = ".j2ee";

    private static final String J2EE_J2EESETTINGS = "j2eesettings";

    private static final String J2EE_MODULEVERSION = "moduleversion";

    private static final String J2EE_VERSION = "version";

    /**
     * write the .j2ee file to the project root directory.
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
        FileWriter w;
        String packaging = config.getProject().getPackaging();
        
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging )
            || Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging )
            || Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            try
            {
                w = new FileWriter( new File( config.getEclipseProjectDirectory(), J2EE_FILENAME ) );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
            }
            
            XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
            writeModuleTypeFacetCore( writer, packaging );
            IOUtil.close( w );
        }
    }

    /**
     * Writes out the facet info for a faceted-project based on the packaging.
     * 
     * @param writer
     *            where to write to
     * @param packaging
     *            packaging type
     */
    private void writeModuleTypeFacetCore( XMLWriter writer, String packaging )
    {
        writer.startElement( J2EE_J2EESETTINGS );
        writer.addAttribute( J2EE_VERSION, "600" );
        writer.startElement( J2EE_MODULEVERSION );
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) )
        {
            writer.writeText( "24" );
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging ) )
        {
            writer.writeText( "21" );
        }
        else if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            writer.writeText( "14" );
        }
        writer.endElement();
        writer.endElement();
    }

}
