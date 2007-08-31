package org.apache.maven.plugin.assembly.aspect.perf;

import org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.archive.archiver.AssemblyProxyArchiver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.FileSet;

import java.io.File;
import java.util.Set;

public aspect TimingAspect
{

    private pointcut resolveDependencies( MavenProject project, String scope, boolean transitive ): execution( Set DependencyResolver+.resolveDependencies( MavenProject, String, .., boolean ) ) && args( project, scope, .., transitive );

    private boolean timingsEnabled()
    {
        return "true".equals( System.getProperty( "assembly.showTimings", "false" ) );
    }

    Set around( MavenProject project, String scope, boolean transitive ): resolveDependencies( project, scope, transitive )
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            Set result = proceed( project, scope, transitive );

            System.out.println( "\n\n" + thisJoinPointStaticPart.getSignature().getName() + "\nfor project: " + project.getId() + "\nwith scope: " + scope + "\ntransitively? " + transitive + "\ntook " + ( System.currentTimeMillis() - start ) + " ms.\n" );

            return result;
        }
        else
        {
            return proceed( project, scope, transitive );
        }
    }

    private pointcut phaseExecution(): execution( * AssemblyArchiverPhase+.execute( .. ) );

    void around(): phaseExecution()
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed();

            Class phaseClass = thisJoinPointStaticPart.getSignature().getDeclaringType();
            String phaseName = phaseClass.getName().substring( phaseClass.getPackage().getName().length() + 1 );

            System.out.println( "Execution of phase: " + phaseName + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed();
        }
    }

    private pointcut addArchivedFileSet_FileSet( ArchivedFileSet fs ): execution( * AssemblyProxyArchiver.addArchivedFileSet( ArchivedFileSet ) ) && args( fs );

    private pointcut addArchivedFileSet_File( File file ): execution( * AssemblyProxyArchiver.addArchivedFileSet( File, .. ) ) && args( file, .. );

    private pointcut addFileSet( FileSet fs ): execution( * AssemblyProxyArchiver.addFileSet( FileSet ) ) && args( fs );

    private pointcut addFile( File file ): execution( * AssemblyProxyArchiver.addDirectory( File, .. ) ) && args( file, .. );

    private pointcut addDirectory( File file ): execution( * AssemblyProxyArchiver.addFile( File, .. ) ) && args( file, .. );

    private pointcut createArchive(): execution( * AssemblyProxyArchiver.createArchive(..) );

    void around( ArchivedFileSet fs ): addArchivedFileSet_FileSet( fs )
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed( fs );

            System.out.println( thisJoinPointStaticPart.getSignature().getName() + " for artifact: " + fs.getArchive() + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed( fs );
        }
    }

    void around( File file ): addArchivedFileSet_File( file )
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed( file );

            System.out.println( thisJoinPointStaticPart.getSignature().getName() + " for artifact: " + file + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed( file );
        }
    }

    void around( FileSet fs ): addFileSet( fs )
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed( fs );

            System.out.println( thisJoinPointStaticPart.getSignature().getName() + " for file-set: " + fs.getDirectory() + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed( fs );
        }
    }

    void around( File file ): addFile( file )
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed( file );

            System.out.println( thisJoinPointStaticPart.getSignature().getName() + " for file: " + file + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed( file );
        }
    }

    void around( File file ): addDirectory( file )
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed( file );

            System.out.println( thisJoinPointStaticPart.getSignature().getName() + " for dir: " + file + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed( file );
        }
    }

    void around(): createArchive()
    {
        if ( timingsEnabled() )
        {
            long start = System.currentTimeMillis();

            proceed();

            System.out.println( thisJoinPointStaticPart.getSignature().getName() + " took " + ( System.currentTimeMillis() - start ) + " ms." );
        }
        else
        {
            proceed();
        }
    }

}
