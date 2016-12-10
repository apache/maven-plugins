package org.apache.maven.plugins.toolchains.its.custom;

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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcher;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Custom toolchain factory, declared as <code>ToolchainFactory</code> Plexus component with <code>custom</code>
 * hint.
 *
 * @author Hervé Boutemy
 */
@Component( role = ToolchainFactory.class, hint = "custom" )
public class CustomToolchainFactory
    implements ToolchainFactory
{

    @Requirement
    private Logger logger;

    public ToolchainPrivate createToolchain( ToolchainModel model )
        throws MisconfiguredToolchainException
    {
        if ( model == null )
        {
            return null;
        }

        CustomToolchainImpl customToolchain = new CustomToolchainImpl( model, logger );

        // populate the provides section
        Properties provides = getProvidesProperties( model );

        for ( Map.Entry<Object, Object> provide : provides.entrySet() )
        {
            String key = (String) provide.getKey();
            String value = (String) provide.getValue();

            if ( value == null )
            {
                throw new MisconfiguredToolchainException(
                    "Provides token '" + key + "' doesn't have any value configured." );
            }

            RequirementMatcher matcher;
            if ( "version".equals( key ) )
            {
                matcher = RequirementMatcherFactory.createVersionMatcher( value );
            }
            else
            {
                matcher = RequirementMatcherFactory.createExactMatcher( value );
            }

            customToolchain.addProvideToken( key, matcher );
        }

        // populate the configuration section
        Properties configuration = toProperties( (Xpp3Dom) model.getConfiguration() );

        String toolHome = configuration.getProperty( CustomToolchainImpl.KEY_TOOLHOME );
        if ( toolHome == null )
        {
            throw new MisconfiguredToolchainException( "Custom toolchain without the "
                + CustomToolchainImpl.KEY_TOOLHOME + " configuration element." );
        }

        toolHome = FileUtils.normalize( toolHome );
        customToolchain.setToolHome( toolHome );

        if ( !new File( toolHome ).exists() )
        {
            throw new MisconfiguredToolchainException( "Non-existing tool home configuration at "
                                                        + new File( toolHome ).getAbsolutePath() );
        }

        return customToolchain;
    }

    public ToolchainPrivate createDefaultToolchain()
    {
        // not sure it's necessary to provide a default toolchain here.
        return null;
    }

    protected Logger getLogger()
    {
        return logger;
    }

    /**
     * Get <code>provides</code> properties in in a way compatible with toolchains descriptor version 1.0
     * (Maven 2.0.9 to 3.2.3, where it is represented as Object/DOM) and descriptor version 1.1
     * (Maven 3.2.4 and later, where it is represented as Properties).
     * 
     * @param model the toolchain model as read from XML
     * @return the properties defined in the <code>provides</code> element
     * @see <a href="https://issues.apache.org/jira/browse/MNG-5718">MNG-5718</a> 
     */
    protected Properties getProvidesProperties( ToolchainModel model )
    {
        Object value = getBeanProperty( model, "provides" );

        return ( value instanceof Properties ) ? (Properties) value : toProperties( (Xpp3Dom) value );
    }

    protected Properties toProperties( Xpp3Dom dom )
    {
        Properties props = new Properties();

        Xpp3Dom[] children = dom.getChildren();
        for ( Xpp3Dom child : children )
        {
            props.put( child.getName(), child.getValue() );
        }

        return props;
    }

    protected Object getBeanProperty( Object obj, String property )
    {
        try
        {
            Method method = new PropertyDescriptor( property, obj.getClass() ).getReadMethod();

            return method.invoke( obj );
        }
        catch ( IntrospectionException e )
        {
            throw new RuntimeException( "Incompatible toolchain API", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( "Incompatible toolchain API", e );
        }
        catch ( InvocationTargetException e )
        {
            Throwable cause = e.getCause();

            if ( cause instanceof RuntimeException )
            {
                throw (RuntimeException) cause;
            }

            throw new RuntimeException( "Incompatible toolchain API", e );
        }
    }
}
