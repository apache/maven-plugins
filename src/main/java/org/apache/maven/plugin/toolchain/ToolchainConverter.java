package org.apache.maven.plugin.toolchain;

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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

/**
 * Plexus ConfigurationConverter
 *
 * @author mkleint
 */
public class ToolchainConverter
    extends AbstractConfigurationConverter
{

    public static final String ROLE = ConfigurationConverter.class.getName();

    /**
     * @see org.codehaus.plexus.component.configurator.converters.ConfigurationConverter#canConvert(java.lang.Class)
     */
    public boolean canConvert( Class type )
    {
        return Toolchains.class.isAssignableFrom( type );
    }

    /**
     * @see org.codehaus.plexus.component.configurator.converters.ConfigurationConverter#fromConfiguration(org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup, org.codehaus.plexus.configuration.PlexusConfiguration, java.lang.Class, java.lang.Class, java.lang.ClassLoader, org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator, org.codehaus.plexus.component.configurator.ConfigurationListener)
     */
    public Object fromConfiguration( ConverterLookup converterLookup,
                                     PlexusConfiguration configuration,
                                     Class type, Class baseType,
                                     ClassLoader classLoader,
                                     ExpressionEvaluator expressionEvaluator,
                                     ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        Toolchains retValue = new Toolchains();
        processConfiguration( retValue, configuration, expressionEvaluator );

        return retValue;
    }

    private void processConfiguration( Toolchains chain,
                                       PlexusConfiguration configuration,
                                       ExpressionEvaluator expressionEvaluator )
        throws ComponentConfigurationException
    {
        Map map = new HashMap();
        PlexusConfiguration[] tools = configuration.getChildren();
        for (PlexusConfiguration tool : tools) {
            String type = tool.getName();
            PlexusConfiguration[] params = tool.getChildren();
            Map parameters = new HashMap();
            for (PlexusConfiguration param : params) {
                try {
                    String name = param.getName();
                    String val = param.getValue();
                    parameters.put(name, val);
                } catch (PlexusConfigurationException ex) {
                    throw new ComponentConfigurationException(ex);
                }
            }
            map.put(type, parameters);
        }
        chain.toolchains = map;
    }
}