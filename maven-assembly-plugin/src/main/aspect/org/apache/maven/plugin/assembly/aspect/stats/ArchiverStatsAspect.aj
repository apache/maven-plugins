package org.apache.maven.plugin.assembly.aspect.stats;

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.plugin.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugin.assembly.archive.archiver.AssemblyProxyArchiver;
import org.apache.maven.plugin.assembly.model.Assembly;

public aspect ArchiverStatsAspect
{

    private pointcut proxyArchiver_AddArchivedFileSet(): execution( * AssemblyProxyArchiver.addArchivedFileSet(..) );

    private pointcut proxyArchiver_AddFileSet(): execution( * AssemblyProxyArchiver.addFileSet(..) );

    private pointcut proxyArchiver_AddFile(): execution( * AssemblyProxyArchiver.addDirectory(..) );

    private pointcut proxyArchiver_AddDirectory(): execution( * AssemblyProxyArchiver.addFile(..) );

    private pointcut assemblyArchiver_createArchive( Assembly assembly ): execution( * AssemblyArchiver.createArchive(Assembly, ..) ) && args(assembly, ..);

    private int archivedFSCount = 0;
    private int fsCount = 0;
    private int fileCount = 0;
    private int dirCount = 0;

    private long start;

    after() returning: proxyArchiver_AddArchivedFileSet()
    {
        archivedFSCount++;
    }

    after() returning: proxyArchiver_AddFileSet()
    {
        fsCount++;
    }

    after() returning: proxyArchiver_AddFile()
    {
        fileCount++;
    }

    after() returning: proxyArchiver_AddDirectory()
    {
        dirCount++;
    }

    before( Assembly assembly ): assemblyArchiver_createArchive( assembly )
    {
        start = System.currentTimeMillis();
    }

    after( Assembly assembly ) returning: assemblyArchiver_createArchive( assembly )
    {
        if ( "true".equals( System.getProperty( "assembly.showTimings", "false" ) ) )
        {
            long stop = System.currentTimeMillis();

            StringBuffer summary = new StringBuffer();

            summary.append( "\n" );
            summary.append( "\n*****************************************************" );
            summary.append( "\nSummary for Assembly: " ).append( assembly.getId() );
            summary.append( "\n*****************************************************" );
            summary.append( "\n" );
            summary.append( "\nArtifacts unpacked: " ).append( archivedFSCount );
            summary.append( "\nFile-Sets added: " ).append( fsCount );
            summary.append( "\nDirectories added: " ).append( dirCount );
            summary.append( "\nIndividual files added: " ).append( fileCount );
            summary.append( "\nOutput formats: " ).append( StringUtils.join( assembly.getFormats().iterator(), ", " ) );
            summary.append( "\nTotal time elapsed: " ).append( stop - start ).append( " ms" );
            summary.append( "\n" );
            summary.append( "\n*****************************************************" );
            summary.append( "\n" );

            System.out.println( summary.toString() );
        }
    }

}
