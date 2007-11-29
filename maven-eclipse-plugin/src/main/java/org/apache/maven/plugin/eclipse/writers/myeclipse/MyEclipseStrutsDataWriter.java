package org.apache.maven.plugin.eclipse.writers.myeclipse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.ide.IdeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * MyEclipse .mystrutsdata configuration file writer
 * 
 * @author Olivier Jacob
 */
public class MyEclipseStrutsDataWriter
    extends AbstractEclipseWriter
{
    private static final String MYECLIPSE_MYSTRUTSDATA_FILENAME = ".mystrutsdata";

    private static final String MYECLIPSE_STRUTS_PROPERTIES = "MyEclipseStrutsProperties";

    private static final String MYECLIPSE_STRUTS_VERSION = "strutsVersion";

    private static final String MYECLIPSE_STRUTS_BASE_PACKAGE = "basePackage";

    private static final String MYECLIPSE_STRUTS_PATTERN = "strutsPattern";

    private static final String MYECLIPSE_STRUTS_SERVLET_NAME = "servletName";

    private static final String MYECLIPSE_STRUTS_DEFAULT_PATTERN = "*.do";

    private static final String MYECLIPSE_STRUTS_SERVLET_DEFAULT_NAME = "action";

    private static Map strutsPatterns;

    private Map strutsProps;

    /**
     * Receive struts properties map from plugin
     * 
     * @param strutsProps
     * @see org.apache.maven.plugin.eclipse.MyEclipsePlugin#struts
     */
    public MyEclipseStrutsDataWriter( Map strutsProps )
    {
        this.strutsProps = strutsProps;

        strutsPatterns = new HashMap();
        strutsPatterns.put( "*.do", "0" );
        strutsPatterns.put( "/do/*", "1" );
    }

    /**
     * Write MyEclipse .mystrutsdata configuration file
     * 
     * @throws MojoExecutionException
     */
    public void write()
        throws MojoExecutionException
    {
        String packaging = config.getProject().getPackaging();

        if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            FileWriter w;
            try
            {
                w = new FileWriter( new File( config.getEclipseProjectDirectory(), MYECLIPSE_MYSTRUTSDATA_FILENAME ) );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
            }

            XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );

            writer.startElement( MYECLIPSE_STRUTS_PROPERTIES );

            writer.startElement( MYECLIPSE_STRUTS_VERSION );
            writer.writeText( getStrutsVersion() );
            writer.endElement();

            writer.startElement( MYECLIPSE_STRUTS_BASE_PACKAGE );
            writer.writeText( getBasePackage() );
            writer.endElement();

            writer.startElement( MYECLIPSE_STRUTS_PATTERN );
            writer.writeText( getStrutsPattern() );
            writer.endElement();

            writer.startElement( MYECLIPSE_STRUTS_SERVLET_NAME );
            writer.writeText( getStrutsServletName() );
            writer.endElement();

            // Close <MyEclipseStrutsProperties>
            writer.endElement();

            IOUtil.close( w );
        }
    }

    /**
     * Retrieve Struts version from plugin configuration or if not specified from project dependencies. If none is
     * specified, use 1.2.9 as default
     * 
     * @return my eclipse struts version code
     */
    private String getStrutsVersion()
    {
        String version;

        if ( strutsProps != null && strutsProps.get( "version" ) != null )
        {
            version = (String) strutsProps.get( "version" );
        }
        else
        {
            version =
                IdeUtils.getArtifactVersion( new String[] { "struts", "struts-core" },
                                             config.getProject().getDependencies(), 5 );

            // Newest version supported by MyEclipse is Struts 1.2.x
            if ( version == null )
            {
                version = "1.2.9";
            }
        }

        int firstDotIndex = version.indexOf( '.' );
        int secondDotIndex = version.indexOf( '.', firstDotIndex + 1 );
        String majorVersion = version.substring( firstDotIndex + 1, secondDotIndex );

        int v = Integer.parseInt( majorVersion );

        return v > 2 ? "2" : majorVersion;
    }

    /**
     * Retrieve struts actions base package name from plugin configuration or use project groupId if not set
     * 
     * @return String
     */
    private String getBasePackage()
    {
        if ( strutsProps != null && strutsProps.get( "base-package" ) != null )
        {
            return (String) strutsProps.get( "base-package" );
        }
        return config.getProject().getGroupId();
    }

    /**
     * Retrieve Struts servlet url-pattern from plugin configuration and convert it to the code MyEclipse uses. If not
     * set, use "*.do" as default
     * 
     * @return String
     */
    private String getStrutsPattern()
    {
        if ( strutsProps != null && strutsProps.get( "pattern" ) != null )
        {
            String pattern = (String) strutsPatterns.get( strutsProps.get( "pattern" ) );
            return pattern != null ? pattern : (String) strutsPatterns.get( MYECLIPSE_STRUTS_DEFAULT_PATTERN );
        }
        return (String) strutsPatterns.get( MYECLIPSE_STRUTS_DEFAULT_PATTERN );
    }

    /**
     * Retrieve Struts servlet name from plugin configuration. Use "action" as default
     * 
     * @return
     */
    private String getStrutsServletName()
    {
        if ( strutsProps != null && strutsProps.get( "servlet-name" ) != null )
        {
            return (String) strutsProps.get( "servlet-name" );
        }
        return MYECLIPSE_STRUTS_SERVLET_DEFAULT_NAME;
    }
}