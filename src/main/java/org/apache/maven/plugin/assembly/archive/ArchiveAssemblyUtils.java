package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ArchiveAssemblyUtils
{

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private ArchiveAssemblyUtils()
    {
    }

    public static void addDirectory( Archiver archiver, File directory, String output, List includes,
                                     List fileSetExcludes, ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException
    {
        // TODO Handle this in the archiver!
        List excludes = new ArrayList( fileSetExcludes );
        excludes.addAll( FileUtils.getDefaultExcludesAsList() );
        
        if ( directory.exists() )
        {
            List adaptedExcludes = excludes;

            // TODO: more robust set of filters on added files in the archiver
            File componentsXml = new File( directory, ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH );
            if ( componentsXml.exists() )
            {
                try
                {
                    componentsXmlFilter.addComponentsXml( componentsXml );
                }
                catch ( IOException e )
                {
                    throw new ArchiveCreationException( "Error reading components.xml to merge: " + e.getMessage(), e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new ArchiveCreationException( "Error reading components.xml to merge: " + e.getMessage(), e );
                }
                adaptedExcludes = new ArrayList( excludes );
                adaptedExcludes.add( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH );
            }

            try
            {
                String[] includesArray = (String[]) includes.toArray( EMPTY_STRING_ARRAY );
                String[] excludesArray = (String[]) adaptedExcludes.toArray( EMPTY_STRING_ARRAY );
                
                archiver.addDirectory( directory, output, includesArray, excludesArray );
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding directory to archive: " + e.getMessage(), e );
            }
        }
    }

    /**
     * Unpacks the archive file.
     * 
     * @param file
     *            File to be unpacked.
     * @param location
     *            Location where to put the unpacked files.
     */
    public static void unpack( File file, File location, ArchiverManager archiverManager )
        throws ArchiveExpansionException, NoSuchArchiverException
    {
        try
        {
            UnArchiver unArchiver = archiverManager.getUnArchiver( file );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveExpansionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

}
