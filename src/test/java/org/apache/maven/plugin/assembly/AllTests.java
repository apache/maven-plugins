package org.apache.maven.plugin.assembly;

import org.apache.maven.plugin.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugin.assembly.archive.ManifestCreationFinalizerTest;
import org.apache.maven.plugin.assembly.archive.phase.DependencySetAssemblyPhaseTest;
import org.apache.maven.plugin.assembly.archive.phase.FileItemAssemblyPhaseTest;
import org.apache.maven.plugin.assembly.archive.phase.FileSetAssemblyPhaseTest;
import org.apache.maven.plugin.assembly.archive.phase.ModuleSetAssemblyPhaseTest;
import org.apache.maven.plugin.assembly.archive.phase.RepositoryAssemblyPhaseTest;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTaskTest;
import org.apache.maven.plugin.assembly.archive.task.AddDependencySetsTaskTest;
import org.apache.maven.plugin.assembly.archive.task.AddDirectoryTaskTest;
import org.apache.maven.plugin.assembly.archive.task.AddFileSetsTaskTest;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilterTest;
import org.apache.maven.plugin.assembly.format.FileFormatterTest;
import org.apache.maven.plugin.assembly.format.FileSetFormatterTest;
import org.apache.maven.plugin.assembly.interpolation.AssemblyInterpolatorTest;
import org.apache.maven.plugin.assembly.io.DefaultAssemblyReaderTest;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtilsTest;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtilsTest;
import org.apache.maven.plugin.assembly.utils.FilterUtilsTest;
import org.apache.maven.plugin.assembly.utils.ProjectUtilsTest;
import org.apache.maven.plugin.assembly.utils.PropertiesInterpolationValueSourceTest;
import org.apache.maven.plugin.assembly.utils.PropertyUtilsTest;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtilsTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Test for org.apache.maven.plugin.assembly.archive.phase" );
        //$JUnit-BEGIN$
        suite.addTestSuite( FileSetAssemblyPhaseTest.class );
        suite.addTestSuite( RepositoryAssemblyPhaseTest.class );
        suite.addTestSuite( DependencySetAssemblyPhaseTest.class );
        suite.addTestSuite( ModuleSetAssemblyPhaseTest.class );
        suite.addTestSuite( FileItemAssemblyPhaseTest.class );
        suite.addTestSuite( AddArtifactTaskTest.class );
        suite.addTestSuite( AddDependencySetsTaskTest.class );
        suite.addTestSuite( AddDirectoryTaskTest.class );
        suite.addTestSuite( AddFileSetsTaskTest.class );
        suite.addTestSuite( DefaultAssemblyArchiverTest.class );
        suite.addTestSuite( ManifestCreationFinalizerTest.class );
        suite.addTestSuite( ComponentsXmlArchiverFileFilterTest.class );
        suite.addTestSuite( FileFormatterTest.class );
        suite.addTestSuite( FileSetFormatterTest.class );
        suite.addTestSuite( AssemblyInterpolatorTest.class );
        suite.addTestSuite( FilterUtilsTest.class );
        suite.addTestSuite( PropertiesInterpolationValueSourceTest.class );
        suite.addTestSuite( ProjectUtilsTest.class );
        suite.addTestSuite( AssemblyFileUtilsTest.class );
        suite.addTestSuite( PropertyUtilsTest.class );
        suite.addTestSuite( AssemblyFormatUtilsTest.class );
        suite.addTestSuite( DefaultAssemblyReaderTest.class );
        suite.addTestSuite( TypeConversionUtilsTest.class );
        //$JUnit-END$
        return suite;
    }

}
