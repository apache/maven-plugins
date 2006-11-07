package org.apache.maven.plugin.eclipse.writers;

/**
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 *
 */
public class MonitoredResource
{
    public static final int PROJECT = 4;

    public static final int DIRECTORY = 2;

    private String path;

    private int type;

    public MonitoredResource( String path, int type )
    {
        this.path = path;
        this.type = type;
    }

    public String print()
    {
        return "<item factoryID='org.eclipse.ui.internal.model.ResourceFactory' path='" + path + "' type='" + type
            + "'/>";
    }
}
