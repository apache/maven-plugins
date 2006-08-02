package org.apache.maven.plugin.assembly.archive.phase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.FileFormatter;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.AbstractLogEnabled;


/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="file-items"
 */
public class FileItemAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List fileList = assembly.getFiles();
        File basedir = configSource.getBasedir();

        FileFormatter fileFormatter = new FileFormatter( configSource, getLogger() );
        for ( Iterator i = fileList.iterator(); i.hasNext(); )
        {
            FileItem fileItem = (FileItem) i.next();

            String sourcePath = fileItem.getSource();

            // ensure source file is in absolute path for reactor build to work
            File source = new File( sourcePath );

            // save the original sourcefile's name, because filtration may
            // create a temp file with a different name.
            String sourceName = source.getName();

            if ( !source.isAbsolute() )
            {
                source = new File( basedir, sourcePath );
            }

            fileFormatter.format( source, fileItem.isFiltered(), fileItem.getLineEnding() );

            String destName = fileItem.getDestName();

            if ( destName == null )
            {
                destName = sourceName;
            }

            String outputDirectory = AssemblyFormatUtils.getOutputDirectory( fileItem.getOutputDirectory(),
                configSource.getProject(), configSource.getFinalName(), assembly.isIncludeBaseDirectory() );

            String target;

            // omit the last char if ends with / or \\
            if ( outputDirectory.endsWith( "/" ) || outputDirectory.endsWith( "\\" ) )
            {
                target = outputDirectory + destName;
            }
            else
            {
                target = outputDirectory + "/" + destName;
            }

            try
            {
                archiver.addFile( source, target, Integer.parseInt( fileItem.getFileMode(), 8 ) );
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
            }
        }
    }

}
