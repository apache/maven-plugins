package org.apache.maven.plugin.antrun.components;

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

import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.UnknownElement;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Plexus ConfigurationConverter to set up Ant Target component fields.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntTargetConverter
    extends AbstractConfigurationConverter
{
    public static final String MAVEN_EXPRESSION_EVALUATOR_ID = "maven.expressionEvaluator";

    public static final String ROLE = ConfigurationConverter.class.getName();

    /**
     * @see org.codehaus.plexus.component.configurator.converters.ConfigurationConverter#canConvert(java.lang.Class)
     */
    public boolean canConvert( Class type )
    {
        return Target.class.isAssignableFrom( type );
    }

    /**
     * @see org.codehaus.plexus.component.configurator.converters.ConfigurationConverter#fromConfiguration(org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup, org.codehaus.plexus.configuration.PlexusConfiguration, java.lang.Class, java.lang.Class, java.lang.ClassLoader, org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator, org.codehaus.plexus.component.configurator.ConfigurationListener)
     */
    public Object fromConfiguration( ConverterLookup converterLookup, PlexusConfiguration configuration, Class type,
                                    Class baseType, ClassLoader classLoader, ExpressionEvaluator expressionEvaluator,
                                    ConfigurationListener listener )
        throws ComponentConfigurationException
    {
        Object retValue = fromExpression( configuration, expressionEvaluator, type );
        if ( retValue != null )
        {
            return retValue;
        }

        Class implementation = getClassForImplementationHint( type, configuration, classLoader );

        retValue = instantiateObject( implementation );

        if ( !( retValue instanceof Target ) )
        {
            retValue = new Target();
        }

        processConfiguration( (Target) retValue, configuration, expressionEvaluator );

        return retValue;
    }

    private void processConfiguration( Target target, PlexusConfiguration configuration,
                                      ExpressionEvaluator expressionEvaluator )
        throws ComponentConfigurationException
    {
        Project project = new Project();
        project.setName( "DummyProject" );

        target.setName( "" );
        target.setProject( project );
        String[] attributeNames = configuration.getAttributeNames();
        for ( int i = 0; i < attributeNames.length; i++ )
        {
            String attributeName = attributeNames[i];
            String attributeValue = configuration.getAttribute( attributeNames[i], null );

            addAttributes( target, attributeName, attributeValue );
        }
        project.addTarget( target );

        project.addReference( MAVEN_EXPRESSION_EVALUATOR_ID, expressionEvaluator );

        initDefinitions( project, target );

        processConfiguration( null, project, target, configuration );

        project.init();
    }

    private void processConfiguration( RuntimeConfigurable parentWrapper, Project project, Target target,
                                      PlexusConfiguration configuration )
        throws ComponentConfigurationException
    {
        int items = configuration.getChildCount();

        Object parent = parentWrapper == null ? null : parentWrapper.getProxy();

        for ( int i = 0; i < items; i++ )
        {
            PlexusConfiguration childConfiguration = configuration.getChild( i );
            UnknownElement task = new UnknownElement( childConfiguration.getName() );
            task.setProject( project );
            task.setNamespace( "" );
            task.setQName( childConfiguration.getName() );
            task.setTaskType( ProjectHelper.genComponentName( task.getNamespace(), childConfiguration.getName() ) );
            task.setTaskName( childConfiguration.getName() );
            task.setOwningTarget( target );
            task.init();

            if ( parent != null )
            {
                ( (UnknownElement) parent ).addChild( task );
            }
            else
            {
                target.addTask( task );
            }

            RuntimeConfigurable wrapper = new RuntimeConfigurable( task, task.getTaskName() );

            try
            {
                if ( childConfiguration.getValue() != null )
                {
                    wrapper.addText( childConfiguration.getValue() );
                }
            }
            catch ( PlexusConfigurationException e )
            {
                throw new ComponentConfigurationException( "Error reading text value from element '"
                    + childConfiguration.getName() + "'", e );
            }

            String[] attrNames = childConfiguration.getAttributeNames();

            for ( int a = 0; a < attrNames.length; a++ )
            {
                try
                {
                    String v = childConfiguration.getAttribute( attrNames[a] );
                    wrapper.setAttribute( attrNames[a], v );
                }
                catch ( PlexusConfigurationException e )
                {
                    throw new ComponentConfigurationException( "Error getting attribute '" + attrNames[a]
                        + "' of tag '" + childConfiguration.getName() + "'", e );
                }
            }

            if ( parentWrapper != null )
            {
                parentWrapper.addChild( wrapper );
            }

            processConfiguration( wrapper, project, target, childConfiguration );
        }
    }

    protected void initDefinitions( Project project, Target unused )
    {
        ComponentHelper componentHelper = ComponentHelper.getComponentHelper( project );

        componentHelper.initDefaultDefinitions();
    }

    /**
     * Add specific <code>attributeValue</code> to the tasks for given <code>attributeName</code> like
     * <code>"if"</code>, <code>"unless"</code>, <code>"name"</code> and <code>"description"</code>.
     * <br/>
     * <b>Note:</b> <code>"depends"</code> from Ant tasks is not be used.
     *
     * @see <a href="http://ant.apache.org/manual/using.html#targets">Ant targets</a>
     *
     * @param tasks should be not null
     * @param attributeName if empty, skipped
     * @param attributeValue if empty, skipped
     */
    private static void addAttributes( Target tasks, String attributeName, String attributeValue )
    {
        if ( StringUtils.isEmpty( attributeName ) )
        {
            return;
        }
        if ( StringUtils.isEmpty( attributeValue ) )
        {
            return;
        }

        if ( attributeName.toLowerCase().equals( "name" ) )
        {
            tasks.setName( attributeValue );
        }
        if ( attributeName.toLowerCase().equals( "unless" ) )
        {
            tasks.setUnless( attributeValue );
        }
        if ( attributeName.toLowerCase().equals( "description" ) )
        {
            tasks.setDescription( attributeValue );
        }
        if ( attributeName.toLowerCase().equals( "if" ) )
        {
            tasks.setIf( attributeValue );
        }
    }
}
