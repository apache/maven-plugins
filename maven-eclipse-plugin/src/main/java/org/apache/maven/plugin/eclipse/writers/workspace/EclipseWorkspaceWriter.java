package org.apache.maven.plugin.eclipse.writers.workspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.WorkspaceConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;

public class EclipseWorkspaceWriter
    implements WorkspaceWriter
{

    /**
     * Path under Eclipse workspace where Eclipse Plugin metadata/config is stored.
     */
    public static final String ECLIPSE_PLUGINS_METADATA_DIR = ".metadata/.plugins"; //$NON-NLS-1$

    /**
     * Path under {@value #ECLIPSE_PLUGINS_METADATA_DIR } folder where Eclipse Workspace Runtime settings are stored.
     */
    public static final String ECLIPSE_CORE_RUNTIME_SETTINGS_DIR =
        ECLIPSE_PLUGINS_METADATA_DIR + "/org.eclipse.core.runtime/.settings"; //$NON-NLS-1$

    /**
     * File that stores the Eclipse JDT Core preferences.
     */
    public static final String ECLIPSE_JDT_CORE_PREFS_FILE = "org.eclipse.jdt.core.prefs"; //$NON-NLS-1$

    /**
     * Property constant under which Variable 'M2_REPO' is setup.
     */
    public static final String CLASSPATH_VARIABLE_M2_REPO = "org.eclipse.jdt.core.classpathVariable.M2_REPO"; //$NON-NLS-1$

    /**
     * File that stores the Eclipse JDT UI preferences.
     */
    public static final String ECLIPSE_JDT_UI_PREFS_FILE = "org.eclipse.jdt.ui.prefs"; //$NON-NLS-1$

    private WorkspaceConfiguration config;

    private Log logger;

    private File workDir;

    public WorkspaceWriter init( Log logger, WorkspaceConfiguration config )
    {
        this.logger = logger;
        this.config = config;

        workDir = new File( config.getWorkspaceDirectory(), ECLIPSE_CORE_RUNTIME_SETTINGS_DIR );
        workDir.mkdirs();

        return this;
    }

    public void write()
        throws MojoExecutionException
    {
        this.writeLocalRepositoryConfiguration();

        if ( config.getCodeStylesURL() != null )
        {
            this.writeCodeStyleConfiguration();
        }
    }

    private void writeCodeStyleConfiguration()
        throws MojoExecutionException
    {
        File f = new File( workDir, ECLIPSE_JDT_UI_PREFS_FILE );

        Properties props = loadProperties( f );

        EclipseCodeFormatterProfile codeFormatter =
            new EclipseCodeFormatterProfile().init( config.getCodeStylesURL(), config.getActiveStyleProfileName() );

        if ( codeFormatter.getProfileName() != null )
        {
            logger.info( "Set active code style profile name: " + codeFormatter.getProfileName() );
            props.setProperty( "formatter_profile", "_" + codeFormatter.getProfileName() );
        }

        props.setProperty( "org.eclipse.jdt.ui.formatterprofiles", codeFormatter.getContent() );

        storeProperties( props, f );
    }

    private void writeLocalRepositoryConfiguration()
        throws MojoExecutionException
    {
        File f = new File( workDir, ECLIPSE_JDT_CORE_PREFS_FILE );

        Properties props = loadProperties( f );

        props.put( CLASSPATH_VARIABLE_M2_REPO, config.getLocalRepository().getBasedir() ); //$NON-NLS-1$  //$NON-NLS-2$

        storeProperties( props, f );
    }

    private static Properties loadProperties( File f )
        throws MojoExecutionException
    {
        Properties props = new Properties();

        // preserve old settings
        if ( f.exists() )
        {
            try
            {
                props.load( new FileInputStream( f ) );
            }
            catch ( FileNotFoundException e )
            {
                throw new MojoExecutionException(
                                                  Messages.getString( "EclipsePlugin.cantreadfile", f.getAbsolutePath() ), e ); //$NON-NLS-1$
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                                                  Messages.getString( "EclipsePlugin.cantreadfile", f.getAbsolutePath() ), e ); //$NON-NLS-1$
            }
        }

        return props;
    }

    private static void storeProperties( Properties props, File f )
        throws MojoExecutionException
    {
        OutputStream os = null;

        try
        {
            os = new FileOutputStream( f );
            props.store( os, null );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantwritetofile", //$NON-NLS-1$
                                                                  f.getAbsolutePath() ) );
        }
        finally
        {
            IOUtil.close( os );
        }
    }
}
