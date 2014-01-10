package org.apache.maven.plugins.jarsigner;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.jarsigner.JarSignerRequest;
import org.apache.maven.shared.jarsigner.JarSignerSignRequest;
import org.apache.maven.shared.jarsigner.JarSignerUtil;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.cli.Commandline;

import java.io.File;
import java.io.IOException;

/**
 * Signs a project artifact and attachments using jarsigner.
 *
 * @author <a href="cs@schulte.it">Christian Schulte</a>
 * @version $Id$
 * @since 1.0
 */
@Mojo( name = "sign", defaultPhase = LifecyclePhase.PACKAGE )
public class JarsignerSignMojo
    extends AbstractJarsignerMojo
{

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    @Parameter( property = "jarsigner.keypass" )
    private String keypass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    @Parameter( property = "jarsigner.sigfile" )
    private String sigfile;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    @Parameter( property = "jarsigner.providerName" )
    private String providerName;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    @Parameter( property = "jarsigner.providerClass" )
    private String providerClass;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     */
    @Parameter( property = "jarsigner.providerArg" )
    private String providerArg;

    /**
     * Indicates whether existing signatures should be removed from the processed JAR files prior to signing them. If
     * enabled, the resulting JAR will appear as being signed only once.
     *
     * @since 1.1
     */
    @Parameter( property = "jarsigner.removeExistingSignatures", defaultValue = "false" )
    private boolean removeExistingSignatures;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @since 1.3
     */
    @Parameter( property = "jarsigner.tsa" )
    private String tsa;

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @since 1.3
     */
    @Parameter( property = "jarsigner.tsacert" )
    private String tsacert;

    @Override
    protected String getCommandlineInfo( final Commandline commandLine )
    {
        String commandLineInfo = commandLine != null ? commandLine.toString() : null;

        if ( commandLineInfo != null )
        {
            commandLineInfo = StringUtils.replace( commandLineInfo, this.keypass, "'*****'" );
        }

        return commandLineInfo;
    }

    @Override
    protected void preProcessArchive( final File archive )
        throws MojoExecutionException
    {
        if ( removeExistingSignatures )
        {
            try
            {
                JarSignerUtil.unsignArchive( archive );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to unsign archive " + archive + ": " + e.getMessage(), e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected JarSignerRequest createRequest( File archive )
        throws MojoExecutionException
    {
        JarSignerSignRequest request = new JarSignerSignRequest();
        request.setProviderArg( providerArg );
        request.setProviderClass( providerClass );
        request.setProviderName( providerName );
        request.setSigfile( sigfile );
        request.setTsaLocation( tsa );
        request.setTsaAlias( tsacert );

        // Special handling for passwords through the Maven Security Dispatcher
        request.setKeypass( decrypt( keypass ) );
        return request;
    }

}
