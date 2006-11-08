package org.apache.maven.plugin.assembly.format;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.PropertyUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.interpolation.MapBasedValueSource;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class FileFormatter
{

    private final Logger logger;

    private final AssemblerConfigurationSource configSource;

    private Properties filterProperties;

    public FileFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    // used for unit testing currently.
    protected FileFormatter( Properties filterProperties, AssemblerConfigurationSource configSource, Logger logger )
    {
        this.filterProperties = filterProperties;
        this.configSource = configSource;
        this.logger = logger;
    }

    public File format( File source, boolean filter, String lineEnding )
        throws AssemblyFormattingException
    {
        File result = source;

        File tempRoot = configSource.getTemporaryRootDirectory();
        
        AssemblyFileUtils.verifyTempDirectoryAvailability( tempRoot, logger );

        String sourceName = source.getName();

        try
        {
            boolean contentIsChanged = false;

            String rawContents = readFile( source );
            
            String contents = rawContents;
            
            if ( filter )
            {
                contents = filter( contents );
            }

            contentIsChanged = !contents.equals( rawContents );

            BufferedReader contentReader = new BufferedReader( new StringReader( contents ) );

            File tempFilterFile = FileUtils.createTempFile( sourceName + ".", ".filtered", tempRoot );

            boolean fileWritten = formatLineEndings( contentReader, tempFilterFile, lineEnding, contentIsChanged );
            
            if ( fileWritten )
            {
                result = tempFilterFile;
            }
        }
        catch ( FileNotFoundException e )
        {
            throw new AssemblyFormattingException( "File to filter not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Error filtering file '" + source + "': " + e.getMessage(), e );
        }

        return result;
    }

    private String readFile( File source )
        throws IOException
    {
        FileReader fileReader = null;

        StringWriter contentWriter = new StringWriter();

        try
        {
            fileReader = new FileReader( source );

            IOUtil.copy( fileReader, contentWriter );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        return contentWriter.toString();
    }

    private boolean formatLineEndings( BufferedReader contentReader, File tempFilterFile, String lineEnding,
                                    boolean contentIsChanged )
        throws IOException, AssemblyFormattingException
    {
        boolean fileWritten = false;
        
        String lineEndingChars = AssemblyFileUtils.getLineEndingCharacters( lineEnding );

        if ( lineEndingChars != null )
        {
            AssemblyFileUtils.convertLineEndings( contentReader, tempFilterFile, lineEndingChars );
            
            fileWritten = true;
        }
        else if ( contentIsChanged )
        {
            FileWriter fileWriter = null;

            try
            {
                fileWriter = new FileWriter( tempFilterFile );

                IOUtil.copy( contentReader, fileWriter );
                
                fileWritten = true;
            }
            finally
            {
                IOUtil.close( fileWriter );
            }
        }
        
        return fileWritten;
    }

    private String filter( String rawContents )
        throws AssemblyFormattingException
    {
        initializeFiltering();

        String contents = rawContents;

        // support ${token}
        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
        interpolator.addValueSource( new MapBasedValueSource( filterProperties ) );
        interpolator.addValueSource( new ObjectBasedValueSource( configSource.getProject() ) );

        contents = interpolator.interpolate( contents, "project" );

        return contents;
    }

    private void initializeFiltering()
        throws AssemblyFormattingException
    {
        logger.info( "Initializing assembly filters..." );

        if ( filterProperties == null )
        {
            // System properties
            filterProperties = new Properties( System.getProperties() );

            // Project properties
            MavenProject project = configSource.getProject();
            filterProperties.putAll( project.getProperties() );

            List filters = configSource.getFilters();

            if ( filters != null && !filters.isEmpty() )
            {
                for ( Iterator i = filters.iterator(); i.hasNext(); )
                {
                    String filtersfile = (String) i.next();

                    try
                    {
                        Properties properties = PropertyUtils
                            .getInterpolatedPropertiesFromFile( new File( filtersfile ), true, true );

                        filterProperties.putAll( properties );
                    }
                    catch ( IOException e )
                    {
                        throw new AssemblyFormattingException( "Error loading property file '" + filtersfile + "'", e );
                    }
                }
            }
        }
    }

}
