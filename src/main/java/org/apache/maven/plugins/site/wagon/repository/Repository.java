package org.apache.maven.plugins.site.wagon.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugins.site.wagon.PathUtils;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.util.StringUtils;

import java.util.Properties;

/**
 * This class is an abstraction of the location from/to resources
 * can be transfered.
 *
 * <strong>Note: </strong> This is a copy of a file from Wagon. It was copied here to be able to work around WAGON-307.
 * This class can be removed when the prerequisite Maven version uses wagon-provider-api:1.0-beta-7.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 * @todo [BP] some things are specific to certain wagons (eg key stuff in authInfo, permissions)
 */
public class Repository
    extends org.apache.maven.wagon.repository.Repository
{
    private static final long serialVersionUID = 1312227676322136247L;

    private String id;

    private String name;

    private String host;

    private int port = WagonConstants.UNKNOWN_PORT;

    private String basedir;

    private String protocol;

    private String url;

    private RepositoryPermissions permissions;

    /**
     * Properties influencing wagon behaviour
     * which are very specific to particular wagon.
     */
    private Properties parameters = new Properties();

    // Username/password are sometimes encoded in the URL
    private String username = null;

    private String password = null;

    /**
     * @deprecated use {@link #Repository(String, String)}
     */
    public Repository()
    {
        // no op
    }

    public Repository( String id, String url )
    {
        if ( id == null )
        {
            throw new NullPointerException( "id can not be null" );
        }
        
        setId( id );

        if ( url == null )
        {
            throw new NullPointerException( "url can not be null" );
        }
        
        setUrl( url );
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    /**
     * Retrieve the base directory of the repository. This is derived from the full repository URL, and
     * contains the entire path component.
     * 
     * @return the base directory
     */
    public String getBasedir()
    {
        return basedir;
    }

    public void setBasedir( String basedir )
    {
        this.basedir = basedir;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    public void setUrl( String url )
    {
        this.url = url;

        // TODO [BP]: refactor out the PathUtils URL stuff into a class like java.net.URL, so you only parse once
        //  can't use URL class as is because it won't recognise our protocols, though perhaps we could attempt to
        //  register handlers for scp, etc?

        this.protocol = PathUtils.protocol( url );

        this.host = PathUtils.host( url );

        this.port = PathUtils.port( url );

        this.basedir = PathUtils.basedir( url );

        String username = PathUtils.user( url );
        this.username = username;

        if ( username != null )
        {
            String password = PathUtils.password( url );

            if ( password != null )
            {
                this.password = password;

                username += ":" + password;
            }

            username += "@";

            int index = url.indexOf( username );
            this.url = url.substring( 0, index ) + url.substring( index + username.length() );
        }
    }

    public String getUrl()
    {
        if ( url != null )
        {
            return url;
        }

        StringBuffer sb = new StringBuffer();

        sb.append( protocol );

        sb.append( "://" );

        sb.append( host );

        if ( port != WagonConstants.UNKNOWN_PORT )
        {
            sb.append( ":" );

            sb.append( port );
        }

        sb.append( basedir );

        return sb.toString();
    }

    public String getHost()
    {
        if ( host == null )
        {
            return "localhost";
        }
        return host;
    }

    public String getName()
    {
        if ( name == null )
        {
            return getId();
        }
        return name;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "Repository[" );

        if ( StringUtils.isNotEmpty( getName() ) )
        {
            sb.append( getName() ).append( "|" );
        }

        sb.append( getUrl() );
        sb.append( "]" );

        return sb.toString();
    }

    public String getProtocol()
    {
        return protocol;
    }

    public RepositoryPermissions getPermissions()
    {
        return permissions;
    }

    public void setPermissions( RepositoryPermissions permissions )
    {
        this.permissions = permissions;
    }

    public String getParameter( String key )
    {
        return parameters.getProperty( key );
    }

    public void setParameters( Properties parameters )
    {
        this.parameters = parameters;
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final Repository other = (Repository) obj;
        if ( id == null )
        {
            if ( other.id != null )
            {
                return false;
            }
        }
        else if ( !id.equals( other.id ) )
        {
            return false;
        }
        return true;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    }

}
