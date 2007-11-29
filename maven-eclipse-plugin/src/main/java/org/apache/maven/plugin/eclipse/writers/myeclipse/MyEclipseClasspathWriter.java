package org.apache.maven.plugin.eclipse.writers.myeclipse;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.EclipseClasspathWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * @author <a href="mailto:olivier.jacob@gmail.com">Olivier Jacob</a>
 */
public class MyEclipseClasspathWriter
    extends EclipseClasspathWriter
{
    /**
     * Write the dependency only if scope is <b>not</b> provided
     * 
     * @param writer the XmlWriter to write the config with
     * @param dep the dependency to be added to configuration
     * @throws MojoExecutionException
     */
    protected void addDependency( XMLWriter writer, IdeDependency dep )
        throws MojoExecutionException
    {

        if ( log.isDebugEnabled() )
        {
            log.debug( "Currently processing " + dep.getArtifactId() + " dependency" );
        }

        if ( !dep.isProvided() )
        {
            super.addDependency( writer, dep );
        }
    }
}