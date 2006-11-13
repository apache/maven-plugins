package org.apache.maven.plugin.eclipse.writers.rad;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
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
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class RadWebSettingsWriter
    extends AbstractEclipseWriter
{

    private static final String COM_IBM_ETOOLS_SITEEDIT_WIZARDS_PROJECTFEATURE_WEB_SITE_FEATURE = "com.ibm.etools.siteedit.wizards.projectfeature.WebSiteFeature";

    private static final String WEBSETTINGS_CONTEXT_ROOT = "context-root";

    private static final String WEBSETTINGS_FEATURE = "feature";

    private static final String WEBSETTINGS_FEATURE_ID = "feature-id";

    private static final String WEBSETTINGS_FEATURES = "features";

    private static final String WEBSETTINGS_FILENAME = ".websettings";

    private static final String WEBSETTINGS_JSP_LEVEL = "jsp-level";

    private static final String WEBSETTINGS_PROJECT_TYPE = "project-type";

    private static final String WEBSETTINGS_TEMPLATEFEATURE = "templatefeature";

    private static final String WEBSETTINGS_VERSION = "version";

    private static final String WEBSETTINGS_WEBCONTENT = "webcontent";

    private static final String WEBSETTINGS_WEBSETTINGS = "websettings";

    /**
     * the context root to use for this project
     */
    private String warContextRoot;

    /**
     * required default constructor.
     * 
     * @param warContextRoot
     *            the context root to use for this project
     */
    public RadWebSettingsWriter( String warContextRoot )
    {
        this.warContextRoot = warContextRoot;
    }

    /**
     * write the websettings file for RAD6 if needed.
     * @throws MojoExecutionException
     *             when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        FileWriter w;
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( config.getProject().getPackaging() ) )
        {
            try
            {
                w = new FileWriter( new File( config.getEclipseProjectDirectory(), WEBSETTINGS_FILENAME ) );
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
     * write the websettings file for RAD6.
     * 
     * @param writer
     *            where to write to
     */
    private void writeModuleTypeFacetCore( XMLWriter writer )
    {
        writer.startElement( WEBSETTINGS_WEBSETTINGS );
        writer.addAttribute( WEBSETTINGS_VERSION, "600" );
        writer.startElement( WEBSETTINGS_WEBCONTENT );
        writer.writeText( "src/main/webapp" );
        writer.endElement();
        writer.startElement( WEBSETTINGS_CONTEXT_ROOT );
        writer.writeText( getContextRoot( warContextRoot ) );
        writer.endElement();
        writer.startElement( WEBSETTINGS_PROJECT_TYPE );
        writer.writeText( "J2EE" );
        writer.endElement();
        writer.startElement( WEBSETTINGS_JSP_LEVEL );
        writer.writeText( "1.3" );
        writer.endElement();
        writer.startElement( WEBSETTINGS_FEATURES );
        writer.startElement( WEBSETTINGS_FEATURE );
        writer.startElement( WEBSETTINGS_FEATURE_ID );
        writer.writeText( WEBSETTINGS_TEMPLATEFEATURE );
        writer.endElement();
        writer.endElement();
        writer.startElement( WEBSETTINGS_FEATURE );
        writer.startElement( WEBSETTINGS_FEATURE_ID );
        writer.writeText( COM_IBM_ETOOLS_SITEEDIT_WIZARDS_PROJECTFEATURE_WEB_SITE_FEATURE );
        writer.endElement();
        writer.endElement();
        writer.endElement();
        writer.endElement();
    }

    /**
     * Create the ContextRoot for this project, the default is the artifact id
     * 
     * @param warContextRoot
     *            set as a configuration property.
     * @return the context root to use
     */
    private String getContextRoot( String warContextRoot )
    {
        if ( warContextRoot == null || warContextRoot.length() == 0 )
        {
            return config.getProject().getArtifactId();
        }
        else
        {
            return warContextRoot;
        }
    }

}
