package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Base class for writing external launch configuration files.
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 *
 */
public abstract class EclipseLaunchConfigurationWriter
    extends AbstractEclipseWriter
{
    public static final String FILE_DOT_EXTERNAL_TOOL_BUILDERS = ".externalToolBuilders/";

    private String filename;

    private boolean initialized;

    /**
     * Filename including .launch
     *
     * @param filename
     */
    protected EclipseWriter init( Log log, EclipseWriterConfig config, String filename )
    {
        this.filename = filename;
        initialized = true;
        return super.init( log, config );
    }

    public void write()
        throws MojoExecutionException
    {
        if ( !initialized )
        {
            throw new MojoExecutionException( "Not initialized" );
        }

        FileWriter w;

        try
        {
            File extToolsDir = new File( config.getEclipseProjectDirectory(), FILE_DOT_EXTERNAL_TOOL_BUILDERS );
            if ( !extToolsDir.exists() && !extToolsDir.mkdir() )
            {
                throw new MojoExecutionException( "Error creating directory " + extToolsDir );
            }
            w = new FileWriter( new File( extToolsDir, filename ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "launchConfiguration" );
        writer.addAttribute( "type", getLaunchConfigurationType() );

        writeAttribute( writer, "org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", isLaunchInBackground() );

        writeAttribute( writer, "org.eclipse.ui.externaltools.ATTR_RUN_BUILD_KINDS", StringUtils.join(
            getRunBuildKinds(),
            "," ) );

        // i think this one means if the ATTR_RUN_BUILD_KINDS is not default.
        writeAttribute( writer, "org.eclipse.ui.externaltools.ATTR_TRIGGERS_CONFIGURED", true );

        writeAttribute( writer, "org.eclipse.debug.core.appendEnvironmentVariables", isAppendEnvironmentVariables() );

        writeAttribute( writer, "org.eclipse.ui.externaltools.ATTR_LOCATION", getBuilderLocation() );

        if ( getWorkingDirectory() != null )
        {
            writeAttribute( writer, "org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY", getWorkingDirectory() );
        }

        if ( getRefreshScope() != null )
        {
            writeAttribute( writer, "org.eclipse.debug.core.ATTR_REFRESH_SCOPE", getRefreshScope() );
        }

        writeAttribute( writer, "org.eclipse.debug.core.capture_output", isCaptureOutput() );

        String workingSet =  "<?xml version='1.0'?>"
        + "<launchConfigurationWorkingSet editPageId='org.eclipse.ui.resourceWorkingSetPage'"
        + " factoryID='org.eclipse.ui.internal.WorkingSetFactory'"
        + " label='workingSet'"
        + " name='workingSet'>";

        for ( Iterator it = getMonitoredResources().iterator(); it.hasNext(); )
        {
            MonitoredResource monitoredResource = (MonitoredResource) it.next();

            workingSet+=monitoredResource.print();
        }

        workingSet+="</launchConfigurationWorkingSet>";


        writeAttribute(
            writer,
            "org.eclipse.ui.externaltools.ATTR_BUILD_SCOPE",
            "${working_set:" + workingSet
                + "}" );

        addAttributes( writer );

        writer.endElement();

        IOUtil.close( w );
    }

    protected List getMonitoredResources()
    {
        return Collections.singletonList( new MonitoredResource( config.getEclipseProjectName(), MonitoredResource.PROJECT ) );
    }

    protected abstract void addAttributes( XMLWriter writer );

    /**
     * Wheter to allocate a console.
     */
    private boolean isCaptureOutput()
    {
        return false;
    }

    private String getWorkingDirectory()
    {
        return "${build_project}";
    }

    protected String getRefreshScope()
    {
        return "${project}";
    }

    protected abstract String getBuilderLocation();

    protected String[] getRunBuildKinds()
    {
        return new String[] { "full", "incremental", "auto", "clean" };
    }

    protected boolean isAppendEnvironmentVariables()
    {
        return true;
    }

    protected boolean isLaunchInBackground()
    {
        return false;
    }

    protected abstract String getLaunchConfigurationType();

    protected static void writeAttribute( XMLWriter writer, String key, String value )
    {
        writer.startElement( "stringAttribute" );
        writer.addAttribute( "key", key );
        writer.addAttribute( "value", value );
        writer.endElement();
    }

    protected static void writeAttribute( XMLWriter writer, String key, boolean value )
    {
        writer.startElement( "booleanAttribute" );
        writer.addAttribute( "key", key );
        writer.addAttribute( "value", "" + value );
        writer.endElement();
    }

}
