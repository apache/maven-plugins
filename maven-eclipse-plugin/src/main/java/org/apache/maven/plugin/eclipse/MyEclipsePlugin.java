package org.apache.maven.plugin.eclipse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.EclipseWriterConfig;
import org.apache.maven.plugin.eclipse.writers.myeclipse.MyEclipseHibernateWriter;
import org.apache.maven.plugin.eclipse.writers.myeclipse.MyEclipseMetadataWriter;
import org.apache.maven.plugin.eclipse.writers.myeclipse.MyEclipseSpringBeansWriter;
import org.apache.maven.plugin.eclipse.writers.myeclipse.MyEclipseStrutsDataWriter;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;

/**
 * Generates MyEclipse configuration files
 * 
 * @author <a href="mailto:olivier.jacob@gmail.com">Olivier Jacob</a>
 * @goal myeclipse
 * @since 2.5
 * @execute phase="generate-resources"
 */
public class MyEclipsePlugin
    extends EclipsePlugin
{
    /* MyEclipse project natures */
    private static final String MYECLIPSE_EAR_NATURE = "com.genuitec.eclipse.j2eedt.core.earnature";

    private static final String MYECLIPSE_WEB_NATURE = "com.genuitec.eclipse.j2eedt.core.webnature";

    private static final String MYECLISPE_SPRING_NATURE = "com.genuitec.eclipse.springframework.springnature";

    private static final String MYECLIPSE_STRUTS_NATURE =
        "com.genuitec.eclipse.cross.easystruts.eclipse.easystrutsnature";

    private static final String MYECLIPSE_HIBERNATE_NATURE = "com.genuitec.eclipse.hibernate.hibernatenature";

    /* MyEclipse builders */
    private static final String MYECLIPSE_DEPLOYMENT_DESCRIPTOR_VALIDATOR_BUILDER =
        "com.genuitec.eclipse.j2eedt.core.DeploymentDescriptorValidator";

    private static final String MYECLIPSE_WEB_CLASSPATH_BUILDER =
        "com.genuitec.eclipse.j2eedt.core.WebClasspathBuilder";

    private static final String MYECLIPSE_J2EE_PROJECT_VALIDATOR_BUILDER =
        "com.genuitec.eclipse.j2eedt.core.J2EEProjectValidator";

    private static final String MYECLIPSE_SPRING_BUILDER = "com.genuitec.eclipse.springframework.springbuilder";

    private static final String MYECLIPSE_HIBERNATE_BUILDER = "com.genuitec.eclipse.hibernate.HibernateBuilder";

    private static final String MYECLIPSE_J2EE_14_CLASSPATH_CONTAINER =
        "com.genuitec.eclipse.j2eedt.core.J2EE14_CONTAINER";

    private static final String MYECLIPSE_J2EE_13_CLASSPATH_CONTAINER =
        "com.genuitec.eclipse.j2eedt.core.J2EE13_CONTAINER";

    private static final String MYECLIPSE_DEFAULT_HIBERNATE_CFG_XML = "src/main/resources/applicationContext.xml";

    /**
     * Spring configuration placeholder
     * <p/>
     * 
     * <pre>
     *   &lt;spring&gt;
     *     &lt;version&gt;1.0/2.0&lt;/version&gt;
     *     &lt;file-pattern&gt;applicationContext-*.xml&lt;/file-pattern&gt;
     *     &lt;basedir&gt;src/main/resources&lt;/basedir&gt;
     *   &lt;/spring&gt;
     * </pre>
     * 
     * @parameter
     */
    private Map spring;

    /**
     * Hibernate configuration placeholder
     * <p/>
     * 
     * <pre>
     *   &lt;hibernate&gt;
     *     &lt;config-file&gt;src/main/resources/applicationContext-persistence.xml&lt;/config-file&gt;
     *     &lt;session-factory-id&gt;mySessionFactory&lt;/session-factory-id&gt;
     *   &lt;/hibernate&gt;
     * </pre>
     * 
     * @parameter
     */
    private Map hibernate;

    /**
     * Allow declaration of struts properties for MyEclipse
     * <p/>
     * 
     * <pre>
     *   &lt;struts&gt;
     *     &lt;version&gt;1.2.9&lt;/version&gt;
     *     &lt;servlet-name&gt;action&lt;/servlet-name&gt;
     *     &lt;pattern&gt;*.do&lt;/pattern&gt;
     *     &lt;base-package&gt;1.2.9&lt;/base-package&gt;
     *   &lt;/struts&gt;
     * </pre>
     * 
     * @parameter
     */
    private Map struts;

    /**
     * {@inheritDoc}
     */
    protected void writeConfigurationExtras( EclipseWriterConfig config )
        throws MojoExecutionException
    {
        super.writeConfigurationExtras( config );
        if ( isJavaProject() )
        {
            // If the project is a Web Project, make it compile in WEB-INF/classes
            if ( Constants.PROJECT_PACKAGING_WAR.equals( project.getPackaging() ) )
            {
                String warSourceDirectory =
                    IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                               "warSourceDirectory",//$NON-NLS-1$
                                               "/src/main/webapp" ); //$NON-NLS-1$

                EclipseSourceDir[] sourceDirs = config.getSourceDirs();
                for ( int i = 0; i < sourceDirs.length; i++ )
                {
                    if ( !sourceDirs[i].isTest() )
                    {
                        sourceDirs[i].setOutput( warSourceDirectory + "/WEB-INF/classes" );
                    }
                }
            }
        }

        // the MyEclipse part ...

        new MyEclipseMetadataWriter().init( getLog(), config ).write();

        if ( getStruts() != null )
        {
            new MyEclipseStrutsDataWriter( getStruts() ).init( getLog(), config ).write();
        }
        if ( getSpring() != null )
        {
            new MyEclipseSpringBeansWriter( getSpring() ).init( getLog(), config ).write();
        }
        if ( getHibernate() != null )
        {
            // Only Spring configuration file is currently supported
            String hbmCfgFile = (String) getHibernate().get( "config-file" );

            if ( "".equals( hbmCfgFile ) )
            {
                hbmCfgFile = MYECLIPSE_DEFAULT_HIBERNATE_CFG_XML;
            }

            new MyEclipseHibernateWriter( getHibernate() ).init( getLog(), config ).write();
        }
    }

    /**
     * Override the default builders with the builders used by MyEclipse
     * 
     * @param packaging packaging-type (jar,war,ejb,ear)
     */
    protected void fillDefaultBuilders( String packaging )
    {
        List commands = new ArrayList();

        super.fillDefaultBuilders( packaging );

        if ( Constants.PROJECT_PACKAGING_EAR.equals( packaging ) )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "EAR packaging does not need specific builders" );
            }
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            commands.add( MYECLIPSE_DEPLOYMENT_DESCRIPTOR_VALIDATOR_BUILDER );
            commands.add( MYECLIPSE_J2EE_PROJECT_VALIDATOR_BUILDER );
            commands.add( MYECLIPSE_WEB_CLASSPATH_BUILDER );

            // WST Validation Builder : may be added by super.fillDefaultBuilders so check before adding it
            if ( !getBuildcommands().contains( new BuildCommand( BUILDER_WST_VALIDATION ) ) )
            {
                commands.add( BUILDER_WST_VALIDATION );
            }
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equals( packaging ) )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "EJB packaging is not implemented yet" );
            }
        }
        else if ( isJavaProject() )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "JAR packaging does not need specific builders" );
            }
        }

        if ( getSpring() != null )
        {
            commands.add( MYECLIPSE_SPRING_BUILDER );
        }
        if ( getHibernate() != null )
        {
            commands.add( MYECLIPSE_HIBERNATE_BUILDER );
        }

        convertBuildCommandList( commands );
        getBuildcommands().addAll( commands );
    }

    /**
     * Override the default natures with the natures used by MyEclipse
     * 
     * @param packaging packaging-type (jar,war,ejb,ear)
     */
    protected void fillDefaultNatures( String packaging )
    {
        List natures = new ArrayList();

        super.fillDefaultNatures( packaging );

        if ( Constants.PROJECT_PACKAGING_EAR.equals( packaging ) )
        {
            natures.add( MYECLIPSE_EAR_NATURE );
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            natures.add( MYECLIPSE_WEB_NATURE );
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equals( packaging ) )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "EJB packaging is not implemented yet" );
            }
        }
        else if ( isJavaProject() )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "JAR projects does not need specific natures" );
            }
        }

        // Spring
        if ( getSpring() != null )
        {
            natures.add( MYECLISPE_SPRING_NATURE );
        }
        // Struts
        if ( getStruts() != null )
        {
            natures.add( MYECLIPSE_STRUTS_NATURE );
        }

        // Hibernate
        if ( getHibernate() != null )
        {
            natures.add( MYECLIPSE_HIBERNATE_NATURE );
        }

        getProjectnatures().addAll( natures );
    }

    protected void fillDefaultClasspathContainers( String packaging )
    {
        super.fillDefaultClasspathContainers( packaging );

        if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            String j2eeVersion;
            if ( this.jeeversion != null )
            {
                j2eeVersion = JeeUtils.getJeeDescriptorFromJeeVersion( this.jeeversion ).getJeeVersion();
            }
            else
            {
                j2eeVersion =
                    JeeUtils.getJeeDescriptorFromServletVersion( JeeUtils.resolveServletVersion( project ) ).getJeeVersion();
            }

            if ( "1.3".equals( j2eeVersion ) )
            {
                getClasspathContainers().add( MYECLIPSE_J2EE_13_CLASSPATH_CONTAINER );
            }
            else if ( "1.4".equals( j2eeVersion ) )
            {
                getClasspathContainers().add( MYECLIPSE_J2EE_14_CLASSPATH_CONTAINER );
            }
        }
    }

    public Map getSpring()
    {
        return spring;
    }

    public void setSpring( Map spring )
    {
        this.spring = spring;
    }

    public Map getHibernate()
    {
        return hibernate;
    }

    public void setHibernate( Map hibernate )
    {
        this.hibernate = hibernate;
    }

    public Map getStruts()
    {
        return struts;
    }

    public void setStruts( Map struts )
    {
        this.struts = struts;
    }

}
