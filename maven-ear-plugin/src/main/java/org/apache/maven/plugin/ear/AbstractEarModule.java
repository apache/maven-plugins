package org.apache.maven.plugin.ear;

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
import org.apache.maven.plugin.ear.util.ArtifactRepository;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.util.Set;

/**
 * A base implementation of an {@link EarModule}.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public abstract class AbstractEarModule
    implements EarModule
{

    protected static final String MODULE_ELEMENT = "module";

    protected static final String JAVA_MODULE = "java";

    protected static final String ALT_DD = "alt-dd";

    private String uri;

    private Artifact artifact;

    // Those are set by the configuration

    private String groupId;

    private String artifactId;

    private String classifier;

    protected String bundleDir;

    protected String bundleFileName;

    protected Boolean excluded = Boolean.FALSE;

    protected Boolean unpack = null;

    protected String altDeploymentDescriptor;

    protected EarExecutionContext earExecutionContext;

    /**
     * Empty constructor to be used when the module
     * is built based on the configuration.
     */
    public AbstractEarModule()
    {
    }

    /**
     * Creates an ear module from the artifact.
     *
     * @param a the artifact
     */
    public AbstractEarModule( Artifact a )
    {
        this.artifact = a;
        this.groupId = a.getGroupId();
        this.artifactId = a.getArtifactId();
        this.classifier = a.getClassifier();
        this.bundleDir = null;
    }

    public void setEarExecutionContext( EarExecutionContext earExecutionContext )
    {
        this.earExecutionContext = earExecutionContext;
    }

    public void resolveArtifact( Set artifacts )
        throws EarPluginException, MojoFailureException
    {
        // If the artifact is already set no need to resolve it
        if ( artifact == null )
        {
            // Make sure that at least the groupId and the artifactId are specified
            if ( groupId == null || artifactId == null )
            {
                throw new MojoFailureException(
                    "Could not resolve artifact[" + groupId + ":" + artifactId + ":" + getType() + "]" );
            }
            final ArtifactRepository ar = earExecutionContext.getArtifactRepository();
            artifact = ar.getUniqueArtifact( groupId, artifactId, getType(), classifier );
            // Artifact has not been found
            if ( artifact == null )
            {
                Set candidates = ar.getArtifacts( groupId, artifactId, getType() );
                if ( candidates.size() > 1 )
                {
                    throw new MojoFailureException( "Artifact[" + this + "] has " + candidates.size() +
                                                        " candidates, please provide a classifier." );
                }
                else
                {
                    throw new MojoFailureException( "Artifact[" + this + "] " + "is not a dependency of the project." );
                }
            }
        }
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public String getUri()
    {
        if ( uri == null )
        {
            if ( getBundleDir() == null )
            {
                uri = getBundleFileName();
            }
            else
            {
                uri = getBundleDir() + getBundleFileName();
            }
        }
        return uri;
    }

    /**
     * Returns the artifact's groupId.
     *
     * @return the group Id
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * Returns the artifact's Id.
     *
     * @return the artifact Id
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * Returns the artifact's classifier.
     *
     * @return the artifact classifier
     */
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * Returns the bundle directory. If null, the module
     * is bundled in the root of the EAR.
     *
     * @return the custom bundle directory
     */
    public String getBundleDir()
    {
        if ( bundleDir != null )
        {
            bundleDir = cleanBundleDir( bundleDir );
        }
        return bundleDir;
    }

    /**
     * Returns the bundle file name. If null, the artifact's
     * file name is returned.
     *
     * @return the bundle file name
     */
    public String getBundleFileName()
    {
        if ( bundleFileName == null )
        {
            bundleFileName = earExecutionContext.getFileNameMapping().mapFileName( artifact );
        }
        return bundleFileName;
    }


    /**
     * The alt-dd element specifies an optional URI to the post-assembly version
     * of the deployment descriptor file for a particular Java EE module. The URI
     * must specify the full pathname of the deployment descriptor file relative
     * to the application's root directory.
     *
     * @return the alternative deployment descriptor for this module
     */
    public String getAltDeploymentDescriptor()
    {
        return altDeploymentDescriptor;
    }

    /**
     * Specify whether this module should be excluded or not.
     *
     * @return true if this module should be skipped, false otherwise
     */
    public boolean isExcluded()
    {
        return excluded.booleanValue();
    }

    public Boolean shouldUnpack()
    {
        return unpack;
    }

    /**
     * Writes the alternative deployment descriptor if necessary.
     *
     * @param writer  the writer to use
     * @param version the java EE version in use
     */
    protected void writeAltDeploymentDescriptor( XMLWriter writer, String version )
    {
        if ( getAltDeploymentDescriptor() != null )
        {
            writer.startElement( ALT_DD );
            writer.writeText( getAltDeploymentDescriptor() );
            writer.endElement();
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( getType() ).append( ":" ).append( groupId ).append( ":" ).append( artifactId );
        if ( classifier != null )
        {
            sb.append( ":" ).append( classifier );
        }
        if ( artifact != null )
        {
            sb.append( ":" ).append( artifact.getVersion() );
        }
        return sb.toString();
    }

    /**
     * Cleans the bundle directory so that it might be used
     * properly.
     *
     * @param bundleDir the bundle directory to clean
     * @return the cleaned bundle directory
     */
    static String cleanBundleDir( String bundleDir )
    {
        if ( bundleDir == null )
        {
            return bundleDir;
        }

        // Using slashes
        bundleDir = bundleDir.replace( '\\', '/' );

        // Remove '/' prefix if any so that directory is a relative path
        if ( bundleDir.startsWith( "/" ) )
        {
            bundleDir = bundleDir.substring( 1, bundleDir.length() );
        }

        if ( bundleDir.length() > 0 && !bundleDir.endsWith( "/" ) )
        {
            // Adding '/' suffix to specify a directory structure if it is not empty
            bundleDir = bundleDir + "/";
        }

        return bundleDir;
    }

    /**
     * Specify if the objects are both null or both equal.
     *
     * @param first  the first object
     * @param second the second object
     * @return true if parameters are either both null or equal
     */
    static boolean areNullOrEqual( Object first, Object second )
    {
        if ( first != null )
        {
            return first.equals( second );
        }
        else
        {
            return second == null;
        }
    }

    /**
     * Sets the URI of the module explicitely for testing purposes.
     *
     * @param uri the uri
     */
    void setUri( String uri )
    {
        this.uri = uri;

    }
}
