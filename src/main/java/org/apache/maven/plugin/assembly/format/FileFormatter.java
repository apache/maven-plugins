package org.apache.maven.plugin.assembly.format;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.PropertyUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.interpolation.MapBasedValueSource;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class FileFormatter
{

    private static final String LS = System.getProperty( "line.separator", "\n" );;

    private final Logger logger;

    private final AssemblerConfigurationSource configSource;

    private Properties filterProperties;

    public FileFormatter( AssemblerConfigurationSource configSource, Logger logger )
    {
        this.configSource = configSource;
        this.logger = logger;
    }

    public File format( final File source, final boolean filter, final String lineEnding )
        throws AssemblyFormattingException
    {
        File result = source;

        String lineEndingChars;
        if ( lineEnding != null )
        {
            lineEndingChars = AssemblyFileUtils.getLineEndingCharacters( lineEnding );
        }
        else
        {
            lineEndingChars = LS;
        }

        initializeFiltering();

        File tempRoot = configSource.getTemporaryRootDirectory();

        String sourceName = source.getName();

        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( source );
            StringWriter contentWriter = new StringWriter();

            IOUtil.copy( fileReader, contentWriter );

            // support ${token}
            RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
            interpolator.addValueSource( new MapBasedValueSource( filterProperties ) );
            interpolator.addValueSource( new ObjectBasedValueSource( configSource.getProject() ) );

            String contents = contentWriter.toString();

            contents = interpolator.interpolate( contents, "project" );

            BufferedReader contentReader = new BufferedReader( new StringReader( contents ) );

            File tempFilterFile = File.createTempFile( sourceName + ".", ".filtered", tempRoot );
            
            AssemblyFileUtils.convertLineEndings( contentReader, tempFilterFile, lineEndingChars );

        }
        catch ( FileNotFoundException e )
        {
            throw new AssemblyFormattingException( "File to filter not found: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Error filtering file '" + source + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        return result;
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
                        Properties properties = PropertyUtils.getInterpolatedPropertiesFromFile( new File( filtersfile ), true, true );

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
