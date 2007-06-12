package org.apache.maven.plugin.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.profiles.activation.OperatingSystemProfileActivator;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that the OS is allowed by combinations
 * of family, name, version and cpu architecture. The
 * behavior is exactly the same as the Maven Os profile
 * activation so the same values are allowed here.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: RequireOS.java 524303 2007-03-30 22:59:32Z
 *          brianf $
 */
public class RequireOS
    implements EnforcerRule
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
     */
    public String family = null;

    /**
     * Runtime information containing Maven Version.
     */
    public String name = null;

    /**
     * Runtime information containing Maven Version.
     */
    public String version = null;

    /**
     * Runtime information containing Maven Version.
     */
    public String arch = null;

    /**
     * Specify an optional message to the user if the rule
     * fails.
     */
    public String message = "";

    /**
     * Display detected OS information.
     */
    public boolean display = false;

    private Set validFamilies = null;

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String OS_ARCH = System.getProperty( "os.arch" ).toLowerCase( Locale.US );

    public static final String OS_VERSION = System.getProperty( "os.version" ).toLowerCase( Locale.US );

    public RequireOS()
    {
        validFamilies = new HashSet();
        validFamilies.add( "dos" );
        validFamilies.add( "mac" );
        validFamilies.add( "netware" );
        validFamilies.add( "os/2" );
        validFamilies.add( "tandem" );
        validFamilies.add( "unix" );
        validFamilies.add( "windows" );
        validFamilies.add( "win9x" );
        validFamilies.add( "z/os" );
        validFamilies.add( "os/400" );
    }

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        displayOSInfo( helper.getLog(), display );

        if ( allParamsEmpty() )
        {
            throw new EnforcerRuleException(
                                             "All parameters can not be empty. You must pick at least one of (family, name, version, arch) or use -Denforcer.os.display=true to see the current OS information." );
        }

        if ( isValidFamily( this.family ) )
        {
            if ( !isAllowed() )
            {
                if ( StringUtils.isEmpty( message ) )
                {
                    message = ( "OS Arch: " + RequireOS.OS_ARCH + " Family: " + determineOsFamily() + " Name: "
                        + RequireOS.OS_NAME + " Version: " + RequireOS.OS_VERSION + " is not allowed by"
                        + ( arch != null ? " Arch=" + arch : "" ) + ( family != null ? " Family=" + family : "" )
                        + ( name != null ? " Name=" + name : "" ) + ( version != null ? " Version=" + version : "" ) );
                }
                throw new EnforcerRuleException( message );
            }
        }
        else
        {
            StringBuffer buffer = new StringBuffer( 50 );
            Iterator iter = validFamilies.iterator();
            while ( iter.hasNext() )
            {
                buffer.append( iter.next() );
                buffer.append( ", " );
            }
            String help = StringUtils.stripEnd( buffer.toString().trim(), "." );
            throw new EnforcerRuleException( "Invalid Family type used. Valid family types are: " + help );
        }
    }

    /**
     * Log the current OS information
     * 
     * @param log
     */
    public void displayOSInfo( Log log, boolean info )
    {
        String string = "OS Info: Arch: " + RequireOS.OS_ARCH + " Family: " + determineOsFamily() + " Name: "
            + RequireOS.OS_NAME + " Version: " + RequireOS.OS_VERSION;

        if ( !info )
        {
            log.debug( string );
        }
        else
        {
            log.info( string );
        }
    }

    /**
     * Helper method to determine the current OS family.
     * 
     * @return name of current OS family.
     */
    public String determineOsFamily()
    {
        Iterator iter = getValidFamilies().iterator();
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

    /**
     * Helper method to determine if the current OS is
     * allowed based on the injected values for family,
     * name, version and arch.
     * 
     * @return true if the version is allowed.
     */
    public boolean isAllowed()
    {
        OperatingSystemProfileActivator activator = new OperatingSystemProfileActivator();

        return activator.isActive( createProfile() );
    }

    /**
     * Helper method to check that at least one of family,
     * name, version or arch is set.
     * 
     * @return true if all parameters are empty.
     */
    public boolean allParamsEmpty()
    {
        return ( StringUtils.isEmpty( family ) && StringUtils.isEmpty( arch ) && StringUtils.isEmpty( name ) && StringUtils
            .isEmpty( version ) );

    }

    /**
     * Creates a Profile object that contains the activation
     * information
     * 
     * @return a properly populated profile to be used for
     *         OS validation.
     */
    private Profile createProfile()
    {
        Profile profile = new Profile();
        profile.setActivation( createActivation() );
        return profile;
    }

    /**
     * Creates an Activation object that contains the
     * ActivationOS information.
     * 
     * @return a properly populated Activation object.
     */
    private Activation createActivation()
    {
        Activation activation = new Activation();
        activation.setActiveByDefault( false );
        activation.setOs( createOsBean() );
        return activation;
    }

    /**
     * Creates an ActivationOS object containing family,
     * name, version and arch.
     * 
     * @return a properly populated ActivationOS object.
     */
    private ActivationOS createOsBean()
    {
        ActivationOS os = new ActivationOS();

        os.setArch( arch );
        os.setFamily( family );
        os.setName( name );
        os.setVersion( version );

        return os;
    }

    /**
     * Helper method to check if the given family is in the
     * following list:
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
     * Note: '!' is allowed at the beginning of the string
     * and still considered valid.
     * 
     * @param theFamily the family to check.
     * @return true if one of the valid families.
     */
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
     * @param theArch the arch to set
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
     * @param theFamily the family to set
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
     * @param theName the name to set
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
     * @param theVersion the version to set
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
     * @param theValidFamilies the validFamilies to set
     */
    public void setValidFamilies( Set theValidFamilies )
    {
        this.validFamilies = theValidFamilies;
    }

}
