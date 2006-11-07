package org.apache.maven.plugin.eclipse.writers;

import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Writes an external ant launch file.
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 *
 */
public class EclipseAntExternalLaunchConfigurationWriter
    extends EclipseLaunchConfigurationWriter
{
    private String buildfilePath;

    protected String getLaunchConfigurationType()
    {
        return "org.eclipse.ant.AntBuilderLaunchConfigurationType";
    }

    protected String getBuilderLocation()
    {
        return "${build_project}/" + buildfilePath;
    }

    /**
     *
     * @param launcherName Name of the launch file, for instance 'AntBuilder.launch'
     * @param buildfilePath Project relative path to the ant build file, for instance
     *            'eclipse-build.xml'
     * @return this
     */
    public EclipseWriter init( Log log, EclipseWriterConfig config, String launcherName, String buildfilePath )
    {
        this.buildfilePath = buildfilePath;
        return super.init( log, config, launcherName );
    }

    protected void addAttributes( XMLWriter writer )
    {
        // ant specific
        writeAttribute( writer, "process_factory_id", "org.eclipse.ant.ui.remoteAntProcessFactory" );
        writeAttribute( writer, "org.eclipse.ant.ui.DEFAULT_VM_INSTALL", false );
        writeAttribute( writer, "org.eclipse.jdt.launching.DEFAULT_CLASSPATH", true );
        writeAttribute( writer, "org.eclipse.debug.ui.ATTR_CONSOLE_OUTPUT_ON", false );

        writer.startElement( "listAttribute" );
        writer.addAttribute( "key", "org.eclipse.debug.core.MAPPED_RESOURCE_TYPES" );
        writer.endElement();

        writeAttribute( writer, "org.eclipse.ant.ui.ATTR_TARGETS_UPDATED", true );

        writeAttribute(
            writer,
            "org.eclipse.jdt.launching.CLASSPATH_PROVIDER",
            "org.eclipse.ant.ui.AntClasspathProvider" );

        // writeAttribute( writer, "org.eclipse.jdt.launching.PROJECT_ATTR", "" );

        // writer.startElement( "listAttribute" );
        // writer.addAttribute( "key", "org.eclipse.debug.core.MAPPED_RESOURCE_PATHS" );
        // writer.endElement();

    }

    protected List getMonitoredResources()
    {
        // TODO: return a list of MonitoredResources that encapsulate
        // the resource locations - includes/excludes aren't supported
        // so we need to just add the directories.
        return super.getMonitoredResources();
    }
}
