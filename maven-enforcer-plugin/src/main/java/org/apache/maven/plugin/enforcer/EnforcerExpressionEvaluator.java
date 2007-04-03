package org.apache.maven.plugin.enforcer;

import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * This class wraps the PluginParameterExpressionEvaluator because it can't be accessed directly in 2.0.x so we muct
 * create a new one.
 */
public class EnforcerExpressionEvaluator
    extends PluginParameterExpressionEvaluator
{

    /**
     * @param theContext
     * @param theMojoExecution
     * @param thePathTranslator
     * @param theProject
     */
    public EnforcerExpressionEvaluator( MavenSession theContext, PathTranslator thePathTranslator, MavenProject theProject )
    {
        super( theContext, new MojoExecution(new MojoDescriptor()), thePathTranslator, null, theProject, theContext.getExecutionProperties() );
    }

}
