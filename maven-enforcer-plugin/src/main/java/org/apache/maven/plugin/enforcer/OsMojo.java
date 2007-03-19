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

import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.profiles.activation.OperatingSystemProfileActivator;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which fails the build if the os isn't the correct version
 * 
 * @goal os
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @phase process-sources
 */
public class OsMojo
    extends AbstractEnforcer
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
     * @parameter expression="${enforcer.os.family}" default-value=null
     * 
     */
    private String family;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${enforcer.os.name}" default-value=null
     * 
     */
    private String name;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${enforcer.os.version}" default-value=null
     * 
     */
    private String version;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${enforcer.os.arch}" default-value=null
     * 
     */
    private String arch;

    /**
     * Runtime information containing Maven Version.
     * 
     * @parameter expression="${component.org.apache.maven.execution.RuntimeInformation}"
     * @required
     * @readonly
     */
    protected RuntimeInformation rti;

    /**
     * Display detected OS information
     * 
     * @parameter expression="${enforcer.os.display}" default-value=false
     * 
     */
    private boolean display = false;

    private Set validFamilies = null;

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String OS_ARCH = System.getProperty( "os.arch" ).toLowerCase( Locale.US );

    public static final String OS_VERSION = System.getProperty( "os.version" ).toLowerCase( Locale.US );

    public OsMojo()
    {
        Set families = new HashSet();
        families.add( "dos" );
        families.add( "mac" );
        families.add( "netware" );
        families.add( "os/2" );
        families.add( "tandem" );
        families.add( "unix" );
        families.add( "windows" );
        families.add( "win9x" );
        families.add( "z/os" );
        families.add( "os/400" );

        validFamilies = families;
    }

    public void execute()
        throws MojoExecutionException
    {
        if ( !skip )
        {
            if (display)
            {
                displayOSInfo();
            }
            
            if ( isValidFamily( this.family ) )
            {
                this.getLog().info( "Is Allowed:" + isAllowed() );
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

    public void displayOSInfo()
    {
        this.getLog().info( "OS Info: Arch: "+OsMojo.OS_ARCH+" Family: "+determineOsFamily()+" Name: "+OsMojo.OS_NAME+" Version: "+OsMojo.OS_VERSION );
    }

    public boolean isAllowed()
    {
        OperatingSystemProfileActivator activator = new OperatingSystemProfileActivator();

        return activator.isActive( createProfile() );
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

        return ( theFamily == null || validFamilies.contains( theFamily ) );
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
