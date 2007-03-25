/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.profiles.activation.OperatingSystemProfileActivator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author brianf
 * 
 */
public class RequireOS
    implements EnforcementRule
{

    /**
     * The OS family type desired<br />
     * Possible values:<br />
     * <ul>
     * <li>dos</li>
     * <li>mac</li>
     * <li>netware</li>
     * <li>os/2</li>
     * <li>tandem</li>
     * <li>unix</li>
     * <li>windows</li>
     * <li>win9x</li>
     * <li>z/os</li>
     * <li>os/400</li>
     * </ul>
     * 
     * @parameter expression="${enforcer.os.family}"
     * 
     */
    private String family = null;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${enforcer.os.name}"
     * 
     */
    private String name = null;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${enforcer.os.version}"
     * 
     */
    private String version = null;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${enforcer.os.arch}"
     * 
     */
    private String arch = null;

    /**
     * Display detected OS information.
     * 
     * @parameter expression="${enforcer.os.display}" default-value=false
     * 
     */
    private boolean display = false;

    private Set validFamilies = null;

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String OS_ARCH = System.getProperty( "os.arch" ).toLowerCase( Locale.US );

    public static final String OS_VERSION = System.getProperty( "os.version" ).toLowerCase( Locale.US );

    public void execute( EnforcementRuleHelper helper )
        throws MojoExecutionException
    {        
        if ( isValidFamily( this.family ) )
        {
            if ( !isAllowed() )
            {
                String msg = ( "OS Arch: " + RequireOS.OS_ARCH + " Family: " + determineOsFamily() + " Name: "
                    + RequireOS.OS_NAME + " Version: " + RequireOS.OS_VERSION + " is not allowed by"
                    + ( arch != null ? " Arch=" + arch : "" ) + ( family != null ? " Family=" + family : "" )
                    + ( name != null ? " Name=" + name : "" ) + ( version != null ? " Version=" + version : "" ) );

                throw new MojoExecutionException( msg );
            }
        }
        else
        {
            // if display was chosen, don't complain about the family.
            if ( !display )
            {
                StringBuffer buffer = new StringBuffer( 50 );
                Iterator iter = validFamilies.iterator();
                while ( iter.hasNext() )
                {
                    buffer.append( iter.next() );
                    buffer.append( ", " );
                }
                String help = StringUtils.stripEnd( buffer.toString().trim(), "." );
                throw new MojoExecutionException( "Invalid Family type used. Valid family types are: " + help );
            }
        }
    }

    public String determineOsFamily()
    {
        Iterator iter = this.getValidFamilies().iterator();
        while ( iter.hasNext() )
        {
            String fam = (String) iter.next();
            if ( Os.isFamily( fam ) )
            {
                return fam;
            }
        }
        return null;
    }

    public boolean isAllowed()
        throws MojoExecutionException
    {
        if ( !allParamsEmpty() )
        {
            OperatingSystemProfileActivator activator = new OperatingSystemProfileActivator();

            return activator.isActive( createProfile() );
        }
        else
        {
            throw new MojoExecutionException(
                                              "All parameters can not be empty. You must pick at least one of (family, name, version, arch) or use -Denforcer.os.display=true to see the current OS information." );
        }
    }

    public boolean allParamsEmpty()
    {
        return ( StringUtils.isEmpty( family ) && StringUtils.isEmpty( arch ) && StringUtils.isEmpty( name ) && StringUtils
            .isEmpty( version ) );

    }

    private Profile createProfile()
    {
        Profile profile = new Profile();
        profile.setActivation( createActivation() );
        return profile;
    }

    private Activation createActivation()
    {
        Activation activation = new Activation();
        activation.setActiveByDefault( false );
        activation.setOs( createOsBean() );
        return activation;
    }

    private ActivationOS createOsBean()
    {
        ActivationOS os = new ActivationOS();

        os.setArch( arch );
        os.setFamily( family );
        os.setName( name );
        os.setVersion( version );

        return os;
    }

    public boolean isValidFamily( String theFamily )
    {

        // in case they are checking !family
        theFamily = StringUtils.stripStart( theFamily, "!" );

        return ( StringUtils.isEmpty( theFamily ) || validFamilies.contains( theFamily ) );
    }

    /**
     * @return the arch
     */
    public String getArch()
    {
        return this.arch;
    }

    /**
     * @param theArch
     *            the arch to set
     */
    public void setArch( String theArch )
    {
        this.arch = theArch;
    }

    /**
     * @return the family
     */
    public String getFamily()
    {
        return this.family;
    }

    /**
     * @param theFamily
     *            the family to set
     */
    public void setFamily( String theFamily )
    {
        this.family = theFamily;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param theName
     *            the name to set
     */
    public void setName( String theName )
    {
        this.name = theName;
    }

    /**
     * @return the version
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param theVersion
     *            the version to set
     */
    public void setVersion( String theVersion )
    {
        this.version = theVersion;
    }

    /**
     * @return the validFamilies
     */
    public Set getValidFamilies()
    {
        return this.validFamilies;
    }

    /**
     * @param theValidFamilies
     *            the validFamilies to set
     */
    public void setValidFamilies( Set theValidFamilies )
    {
        this.validFamilies = theValidFamilies;
    }

}
