package org.apache.maven.plugins.release.config;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.settings.Settings;

import java.io.File;

/**
 * Configuration used for the release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseConfiguration
{
    /**
     * The last completed phase.
     */
    private String completedPhase;

    /**
     * The Maven settings.
     */
    private Settings settings;

    /**
     * Tag base for an SVN repository.
     */
    private String tagBase;

    /**
     * Username for the SCM repository.
     */
    private String username;

    /**
     * Password for the SCM repository.
     */
    private String password;

    /**
     * URL for the SCM repository.
     */
    private String url;

    /**
     * Private key for an SSH based SCM repository.
     */
    private String privateKey;

    /**
     * Passphrase for the private key.
     */
    private String passphrase;

    /**
     * Where the release is executed.
     */
    private File workingDirectory;

    public String getCompletedPhase()
    {
        return completedPhase;
    }

    public void setCompletedPhase( String completedPhase )
    {
        this.completedPhase = completedPhase;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public String getTagBase()
    {
        return tagBase;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getUrl()
    {
        return url;
    }

    public String getPrivateKey()
    {
        return privateKey;
    }

    public String getPassphrase()
    {
        return passphrase;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setSettings( Settings settings )
    {
        this.settings = settings;
    }

    public void setTagBase( String tagBase )
    {
        this.tagBase = tagBase;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public void setPrivateKey( String privateKey )
    {
        this.privateKey = privateKey;
    }

    public void setPassphrase( String passphrase )
    {
        this.passphrase = passphrase;
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Merge two configurations together. All SCM settings are overridden by the merge configuration, as are the
     * <code>settings</code> and <code>workingDirectory</code> fields. The <code>completedPhase</code> field is used as
     * a default from the merge configuration, but not overridden if it exists.
     *
     * @param mergeConfiguration the configuration to merge into this configuration
     * @todo double check if these are the expected behaviours
     */
    public void merge( ReleaseConfiguration mergeConfiguration )
    {
        // Overridden if configured from the caller
        this.url = mergeOverride( this.url, mergeConfiguration.url );
        this.tagBase = mergeOverride( this.tagBase, mergeConfiguration.tagBase );
        this.username = mergeOverride( this.username, mergeConfiguration.username );
        this.password = mergeOverride( this.password, mergeConfiguration.password );
        this.privateKey = mergeOverride( this.privateKey, mergeConfiguration.privateKey );
        this.passphrase = mergeOverride( this.passphrase, mergeConfiguration.passphrase );

        // These must be overridden, as they are generally not stored
        this.settings = mergeOverride( this.settings, mergeConfiguration.settings );
        this.workingDirectory = mergeOverride( this.workingDirectory, mergeConfiguration.workingDirectory );

        // Not overridden - not configured from caller
        this.completedPhase = mergeDefault( this.completedPhase, mergeConfiguration.completedPhase );
    }

    private static File mergeOverride( File thisValue, File mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static String mergeOverride( String thisValue, String mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static Settings mergeOverride( Settings thisValue, Settings mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static String mergeDefault( String thisValue, String mergeValue )
    {
        return thisValue != null ? thisValue : mergeValue;
    }

    public int hashCode()
    {
        int result = completedPhase != null ? completedPhase.hashCode() : 0;
        result = 29 * result + ( settings != null ? settings.hashCode() : 0 );
        result = 29 * result + ( tagBase != null ? tagBase.hashCode() : 0 );
        result = 29 * result + ( username != null ? username.hashCode() : 0 );
        result = 29 * result + ( password != null ? password.hashCode() : 0 );
        result = 29 * result + url.hashCode();
        result = 29 * result + ( privateKey != null ? privateKey.hashCode() : 0 );
        result = 29 * result + ( passphrase != null ? passphrase.hashCode() : 0 );
        result = 29 * result + workingDirectory.hashCode();
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        ReleaseConfiguration that = (ReleaseConfiguration) obj;

        if ( completedPhase != null ? !completedPhase.equals( that.completedPhase ) : that.completedPhase != null )
        {
            return false;
        }
        if ( passphrase != null ? !passphrase.equals( that.passphrase ) : that.passphrase != null )
        {
            return false;
        }
        if ( password != null ? !password.equals( that.password ) : that.password != null )
        {
            return false;
        }
        if ( privateKey != null ? !privateKey.equals( that.privateKey ) : that.privateKey != null )
        {
            return false;
        }
        if ( settings != null ? !settings.equals( that.settings ) : that.settings != null )
        {
            return false;
        }
        if ( tagBase != null ? !tagBase.equals( that.tagBase ) : that.tagBase != null )
        {
            return false;
        }
        if ( !url.equals( that.url ) )
        {
            return false;
        }
        if ( username != null ? !username.equals( that.username ) : that.username != null )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( !workingDirectory.equals( that.workingDirectory ) )
        {
            return false;
        }

        return true;
    }
}
