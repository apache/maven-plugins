package org.apache.maven.plugins.ear;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.util.Set;

/**
 * The {@link EarModule} implementation for a non J2EE module such as third party libraries.
 * <p/>
 * Such module is not incorporated in the generated <tt>application.xml<tt>
 * but some application servers support it. To include it in the generated
 * deployment descriptor anyway, set the <tt>includeInApplicationXml</tt> boolean flag.
 * <p/>
 * This class deprecates {@link org.apache.maven.plugins.ear.JavaModule}.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: JarModule.java 1648046 2014-12-27 11:07:02Z khmarbaise $
 */
public class JarModule
    extends AbstractEarModule
{
    private Boolean includeInApplicationXml = Boolean.FALSE;

    /**
     * Create an instance.
     */
    public JarModule()
    {
        super();
    }

    /**
     * @param a {@link Artifact}
     * @param defaultLibBundleDir The default library bundle directory.
     * @param includeInApplicationXml Include the application xml or not.
     */
    public JarModule( Artifact a, String defaultLibBundleDir, Boolean includeInApplicationXml )
    {
        super( a );
        setLibBundleDir( defaultLibBundleDir );
        this.includeInApplicationXml = includeInApplicationXml;

    }

    /**
     * {@inheritDoc}
     */
    public void appendModule( XMLWriter writer, String version, Boolean generateId )
    {
        // Generates an entry in the application.xml only if
        // includeInApplicationXml is set
        if ( includeInApplicationXml )
        {
            startModuleElement( writer, generateId );
            writer.startElement( JAVA_MODULE );
            writer.writeText( getUri() );
            writer.endElement();

            writeAltDeploymentDescriptor( writer, version );

            writer.endElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resolveArtifact( Set<Artifact> artifacts )
        throws EarPluginException, MojoFailureException
    {
        // Let's resolve the artifact
        super.resolveArtifact( artifacts );

        // If the defaultLibBundleDir is set and no bundle dir is
        // set, set the default as bundle dir
        setLibBundleDir( earExecutionContext.getDefaultLibBundleDir() );
    }

    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return "jar";
    }

    private void setLibBundleDir( String defaultLibBundleDir )
    {
        if ( defaultLibBundleDir != null && bundleDir == null )
        {
            this.bundleDir = defaultLibBundleDir;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean changeManifestClasspath()
    {
        return false;
    }
}
