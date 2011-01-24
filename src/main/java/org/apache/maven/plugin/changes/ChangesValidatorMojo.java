package org.apache.maven.plugin.changes;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.changes.schema.ChangesSchemaValidator;
import org.apache.maven.plugin.changes.schema.SchemaValidatorException;
import org.apache.maven.plugin.changes.schema.XmlValidationHandler;
import org.xml.sax.SAXException;

/**
 * 
 * Goal which validate the <code>changes.xml</code> file.
 * 
 * @goal changes-validate
 * 
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @version $Id$
 * @since 2.1
 * @threadSafe
 */
public class ChangesValidatorMojo
    extends AbstractMojo
{

    /**
     * @component role="org.apache.maven.plugin.changes.schema.ChangesSchemaValidator" roleHint="default"
     */
    private ChangesSchemaValidator changesSchemaValidator;

    /**
     * The changes xsd version.
     *
     * @parameter expression="${changes.xsdVersion}" default-value="1.0.0"
     */
    private String changesXsdVersion;

    /**
     * Mojo failure if validation failed. If not and validation failed only a warning will be logged.
     *
     * @parameter expression="${changes.validate.failed}" default-value="false"
     */
    private boolean failOnError;

    /**
     * The path of the <code>changes.xml</code> file that will be converted into an HTML report.
     *
     * @parameter expression="${changes.xmlPath}" default-value="src/changes/changes.xml"
     */
    private File xmlPath;

    /** 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( !xmlPath.exists() )
        {
            getLog().warn( "changes.xml file " + xmlPath.getAbsolutePath() + " does not exist." );
            return;
        }

        try
        {
            XmlValidationHandler xmlValidationHandler = changesSchemaValidator
                .validateXmlWithSchema( xmlPath, changesXsdVersion, failOnError );
            boolean hasErrors = !xmlValidationHandler.getErrors().isEmpty();
            if ( hasErrors )
            {
                logSchemaValidation( xmlValidationHandler.getErrors() );
                if ( failOnError )
                {
                    throw new MojoExecutionException( "changes.xml file " + xmlPath.getAbsolutePath()
                        + " is not valid, see previous errors." );
                }
                else
                {
                    getLog().info( " skip previous validation errors due to failOnError=false." );
                }
            }
        }
        catch ( SchemaValidatorException e )
        {
            if ( failOnError )
            {
                throw new MojoExecutionException( "failed to validate changes.xml file " + xmlPath.getAbsolutePath()
                    + ": " + e.getMessage(), e );
            }
        }
    }

    private void logSchemaValidation( List /*SAXException*/errors )
    {
        getLog().warn( "failed to validate changes.xml file " + xmlPath.getAbsolutePath() );
        getLog().warn( "validation errors: " );
        for ( Iterator iterator = errors.iterator(); iterator.hasNext(); )
        {
            SAXException error = (SAXException) iterator.next();
            getLog().warn( error.getMessage() );
        }
    }

}
