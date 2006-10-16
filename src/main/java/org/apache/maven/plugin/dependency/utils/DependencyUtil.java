package org.apache.maven.plugin.dependency.utils;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility class with static helper methods
 * 
 * @author brianf
 * 
 */
public final class DependencyUtil
{
    /**
     * Builds the file name. If removeVersion is set, then the file name must be
     * reconstructed from the artifactId, Classifier (if used) and Type.
     * Otherwise, this method returns the artifact file name.
     * 
     * @param artifact
     *            File to be formatted.
     * @param removeVersion
     *            Specifies if the version should be removed from the file name.
     * @return Formatted file name in the format
     *         artifactId-[classifier-][version].[type]
     */
    public static String getFormattedFileName( Artifact artifact, boolean removeVersion )
    {
        String destFileName = null;
        if ( !removeVersion )
        {
            File file = artifact.getFile();
            if ( file != null )
            {
                destFileName = file.getName();
            }
            // so it can be used offline
            else
            {
                if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
                {
                    destFileName = artifact.getArtifactId() + "-" + artifact.getClassifier() + "-"
                        + artifact.getVersion() + "." + artifact.getType();
                }
                else
                {
                    destFileName = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType();
                }
            }

        }
        else
        {
            if ( artifact.getClassifier() != null )
            {
                destFileName = artifact.getArtifactId() + "-" + artifact.getClassifier() + "." + artifact.getType();
            }
            else
            {
                destFileName = artifact.getArtifactId() + "." + artifact.getType();
            }
        }
        return destFileName;
    }

    /**
     * Formats the outputDirectory based on type.
     * 
     * @param useSubdirsPerType
     *            if a new sub directory should be used for each type.
     * @param useSubdirsPerArtifact
     *            if a new sub directory should be used for each artifact.
     * @param outputDirectory
     *            base outputDirectory.
     * @param artifact
     *            information about the artifact.
     * 
     * @return a formatted File object to use for output.
     */
    public static File getFormattedOutputDirectory( boolean useSubdirsPerType, boolean useSubdirPerArtifact,
                                                   File outputDirectory, Artifact artifact )
    {
        File result = null;

        // get id but convert the chars so it's safe as a folder name.
        String artifactId = artifact.getId().replace( ':', '-' );
        if ( !useSubdirsPerType )
        {
            if ( useSubdirPerArtifact )
            {

                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifactId
                    + File.separatorChar );
            }
            else
            {
                result = outputDirectory;
            }
        }
        else
        {
            if ( useSubdirPerArtifact )
            {
                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifact.getType() + "s"
                    + File.separatorChar + artifactId + File.separatorChar );
            }
            else
            {
                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifact.getType() + "s"
                    + File.separatorChar );
            }
        }

        return result;

    }
}
