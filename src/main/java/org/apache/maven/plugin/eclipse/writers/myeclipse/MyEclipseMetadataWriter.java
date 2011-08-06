package org.apache.maven.plugin.eclipse.writers.myeclipse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeDescriptor;
import org.apache.maven.plugin.ide.JeeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * MyEclipse .mymetadata configuration file writer
 * 
 * @author Olivier Jacob
 */
public class MyEclipseMetadataWriter
    extends AbstractEclipseWriter
{

    private static final String MYECLIPSE_MYMETADATA_FILENAME = ".mymetadata";

    private static final String MYECLIPSE_METADATA_PROJECT = "project-module";

    private static final String MYECLIPSE_METADATA_PROJECT_TYPE = "type";

    private static final String MYECLIPSE_METADATA_PROJECT_NAME = "name";

    private static final String MYECLIPSE_METADATA_PROJECT_ID = "id";

    private static final String MYECLIPSE_METADATA_PROJECT_CONTEXT_ROOT = "context-root";

    private static final String MYECLIPSE_METADATA_PROJECT_J2EE_SPEC = "j2ee-spec";

    private static final String MYECLIPSE_METADATA_PROJECT_ARCHIVE = "archive";

    private static final String MYECLIPSE_METADATA_PROJECT_TYPE_WAR = "WEB";

    private static final String MYECLIPSE_METADATA_PROJECT_TYPE_EAR = "EAR";

    private static final String MYECLIPSE_METADATA_PROJECT_TYPE_EJB = "EJB";

    private static final String MYECLIPSE_METADATA_PROJECT_ATTRIBUTES = "attributes";

    private static final String MYECLIPSE_METADATA_PROJECT_ATTRIBUTE = "attribute";

    /**
     * Writer entry point
     * 
     * @throws MojoExecutionException
     */
    public void write()
        throws MojoExecutionException
    {
        String packaging = config.getProject().getPackaging();

        if ( !Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging )
            && !Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging )
            && !Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging ) )
        {
            return;
        }

        FileWriter w;

        try
        {
            w = new FileWriter( new File( config.getEclipseProjectDirectory(), MYECLIPSE_MYMETADATA_FILENAME ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );

        writer.startElement( MYECLIPSE_METADATA_PROJECT );
        writer.addAttribute( MYECLIPSE_METADATA_PROJECT_TYPE, getMyEclipseProjectType( packaging ) );
        writer.addAttribute( MYECLIPSE_METADATA_PROJECT_NAME, config.getEclipseProjectName() );
        writer.addAttribute( MYECLIPSE_METADATA_PROJECT_ID, config.getEclipseProjectName() );

        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) )
        {
            // Find web application context root from maven-war-plugin configuration.
            // ArtifactId is used as the default value
            String warContextRoot =
                IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN, "warContextRoot",//$NON-NLS-1$
                                           "/" + config.getProject().getArtifactId() );

            writer.addAttribute( MYECLIPSE_METADATA_PROJECT_CONTEXT_ROOT, warContextRoot );

            writer.addAttribute( MYECLIPSE_METADATA_PROJECT_J2EE_SPEC, getJeeVersion() );
            // TODO : use maven final name
            writer.addAttribute( MYECLIPSE_METADATA_PROJECT_ARCHIVE, config.getEclipseProjectName() + ".war" );
        }

        if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            // TODO : use maven final name
            writer.addAttribute( MYECLIPSE_METADATA_PROJECT_ARCHIVE, config.getEclipseProjectName() + ".ear" );
        }

        writer.startElement( MYECLIPSE_METADATA_PROJECT_ATTRIBUTES );
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) )
        {
            writer.startElement( MYECLIPSE_METADATA_PROJECT_ATTRIBUTE );
            writer.addAttribute( "name", "webrootdir" );
            // TODO : retrieve this from project configuration
            writer.addAttribute( "value", "src/main/webapp" );
            writer.endElement();
        }
        // Close <attributes>
        writer.endElement();

        // Close <project-module>
        writer.endElement();

        IOUtil.close( w );
    }

    /**
     * @param packaging maven project packaging
     * @return MyEclipse project type (EAR, WAR, EJB)
     */
    private String getMyEclipseProjectType( String packaging )
    {
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) )
        {
            return MYECLIPSE_METADATA_PROJECT_TYPE_WAR;
        }
        if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            return MYECLIPSE_METADATA_PROJECT_TYPE_EAR;
        }
        if ( Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging ) )
        {
            return MYECLIPSE_METADATA_PROJECT_TYPE_EJB;
        }
        // Should never be reached
        return null;
    }

    /**
     * Find JEE version from the project dependencies : find version from 'j2ee.jar' artifact or from 'servlet-api'
     * 
     * @return the JEE version for the project (1.2, 1.3, 1.4, 1.5)
     * @see org.apache.maven.plugin.ide.JeeUtils#resolveJeeVersion(org.apache.maven.project.MavenProject)
     */
    private String getJeeVersion()
    {
        String jeeVersion;
        if ( config.getJeeVersion() != null )
        {
            jeeVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getJeeVersion();
        }
        else
        {
            jeeVersion =
                JeeUtils.getJeeDescriptorFromServletVersion( JeeUtils.resolveServletVersion( config.getProject() ) ).getJeeVersion();
        }

        if ( jeeVersion == null )
        {
            return JeeDescriptor.JEE_1_4;
        }

        return jeeVersion;
    }
}