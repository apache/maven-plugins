package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.easymock.MockControl;

import junit.framework.TestCase;


public class AssemblyFormatUtilsTest
    extends TestCase
{
    
    private MockManager mockManager = new MockManager();
    
    public void testGetDistroName_ShouldUseJustFinalNameWithNoAppendAssemblyIdOrClassifier()
    {
        verifyDistroName( "assembly", null, "finalName", false, "finalName" );
    }
    
    public void testGetDistroName_ShouldUseFinalNamePlusClassifierWhenAppendAssemblyIdIsNull()
    {
        verifyDistroName( "assembly", "classifier", "finalName", false, "finalName-classifier" );
    }
    
    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions()
    {
        verifyOutputDir( "dir/", "finalName", null, null, null, "dir/" );
    }
    
    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions_CheckWithBackslash()
    {
        verifyOutputDir( "dir\\", "finalName", null, null, null, "dir\\" );
    }
    
    public void testGetOutputDir_ShouldAppendSlashToOutDirWhenMissingAndIncludeBaseFalseAndNoExpressions()
    {
        verifyOutputDir( "dir", "finalName", null, null, null, "dir/" );
    }
    
    public void testGetOutputDir_ShouldResolveGroupIdInOutDir()
    {
        verifyOutputDir( "${groupId}", "finalName", "group", null, null, "group/" );
    }
    
    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir()
    {
        verifyOutputDir( "${artifactId}", "finalName", null, "artifact", null, "artifact/" );
    }
    
    public void testGetOutputDir_ShouldResolveVersionInOutDir()
    {
        verifyOutputDir( "${version}", "finalName", null, null, "version", "version/" );
    }
    
    public void testGetOutputDir_ShouldResolveFinalNameInOutDir()
    {
        verifyOutputDir( "${finalName}", "finalName", null, null, null, "finalName/" );
    }
    
    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir()
    {
        verifyOutputDir( "${build.finalName}", "finalName", null, null, null, "finalName/" );
    }
    
    public void testGetOutputDir_ShouldReturnEmptyPathWhenAllInputIsEmptyAndIncludeBaseFalse()
    {
        verifyOutputDir( null, null, null, null, null, "" );
    }
    
    public void testEvalFileNameMapping_ShouldPassExpressionThroughUnchanged() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename", null, null, null, null, null, "filename" );
    }
    
    public void testEvalFileNameMapping_ShouldInsertClassifierAheadOfExtension() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename-${classifier}.ext", "classifier", null, null, null, null, "filename-classifier.ext" );
    }
    
    public void testEvalFileNameMapping_ShouldAppendClassifier() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename", null, null, null, null, null, "filename" );
    }
    
    public void testEvalFileNameMapping_ShouldResolveGroupId() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${groupId}", null, "group", null, null, null, "group" );
    }
    
    public void testEvalFileNameMapping_ShouldResolveArtifactId() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${artifactId}", null, null, "artifact", null, null, "artifact" );
    }
    
    public void testEvalFileNameMapping_ShouldResolveVersion() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${version}", null, null, null, "version", null, "version" );
    }
    
    public void testEvalFileNameMapping_ShouldResolveExtension() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "file.${extension}", null, null, null, null, "ext", "file.ext" );
    }
    
    private void verifyEvalFileNameMapping( String expression, String classifier, String groupId, String artifactId,
                                            String version, String extension, String checkValue )
        throws AssemblyFormattingException
    {
        MockAndControlForEvalFileNameMapping mac = new MockAndControlForEvalFileNameMapping( groupId, artifactId, version, classifier, extension );
        
        mockManager.replayAll();
        
        String result = AssemblyFormatUtils.evaluateFileNameMapping( expression, mac.artifact );
        
        assertEquals( checkValue, result );
        
        mockManager.verifyAll();
        
        // clear out for next call.
        mockManager.clear();
    }
    
    private void verifyOutputDir( String outDir, String finalName, String groupId, String artifactId, String version, String checkValue )
    {
        MavenProject project = null;
        
        if ( groupId != null || artifactId != null || version != null )
        {
            Model model = new Model();
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );
            
            project = new MavenProject( model );
        }
        
        String result = AssemblyFormatUtils.getOutputDirectory( outDir, project, finalName );
        
        assertEquals( checkValue, result );
    }
    
    private void verifyDistroName( String assemblyId, String classifier, String finalName, boolean appendAssemblyId, String checkValue )
    {
        MockAndControlForGetDistroName mac = new MockAndControlForGetDistroName( finalName, appendAssemblyId, classifier );
        
        mockManager.replayAll();
        
        Assembly assembly = new Assembly();
        assembly.setId( assemblyId );
        
        String result = AssemblyFormatUtils.getDistributionName( assembly, mac.configSource );
        
        assertEquals( checkValue, result );
        
        mockManager.verifyAll();
        
        // clear it out for the next call.
        mockManager.clear();
    }
    
    private final class MockAndControlForGetDistroName
    {
        MockControl control;
        AssemblerConfigurationSource configSource;
        
        private final String classifier;
        private final boolean isAssemblyIdAppended;
        private final String finalName;
        
        public MockAndControlForGetDistroName( String finalName, boolean isAssemblyIdAppended, String classifier )
        {
            this.finalName = finalName;
            this.isAssemblyIdAppended = isAssemblyIdAppended;
            this.classifier = classifier;
            
            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );
            
            configSource = (AssemblerConfigurationSource) control.getMock();
            
            enableExpectations();
        }
        
        private void enableExpectations()
        {
            configSource.getClassifier();
            control.setReturnValue( classifier, MockControl.ONE_OR_MORE );
            
            configSource.isAssemblyIdAppended();
            control.setReturnValue( isAssemblyIdAppended, MockControl.ONE_OR_MORE );
            
            configSource.getFinalName();
            control.setReturnValue( finalName, MockControl.ONE_OR_MORE );
        }
        
    }
    
    private final class MockAndControlForEvalFileNameMapping
    {
        MockControl artifactControl;
        Artifact artifact;
        
        MockControl handlerControl;
        ArtifactHandler handler;
        
        private final String classifier;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String extension;
        
        public MockAndControlForEvalFileNameMapping( String groupId, String artifactId, String version, String classifier, String extension )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.extension = extension;
            
            artifactControl = MockControl.createControl( Artifact.class );
            mockManager.add( artifactControl );
            
            artifact = (Artifact) artifactControl.getMock();
            
            handlerControl = MockControl.createControl( ArtifactHandler.class );
            mockManager.add( handlerControl );
            
            handler = (ArtifactHandler) handlerControl.getMock();
            
            enableExpectations();
        }
        
        private void enableExpectations()
        {
            if ( groupId != null )
            {
                artifact.getGroupId();
                artifactControl.setReturnValue( groupId, MockControl.ONE_OR_MORE );
            }
            
            if ( artifactId != null )
            {
                artifact.getArtifactId();
                artifactControl.setReturnValue( artifactId, MockControl.ONE_OR_MORE );
            }
            
            if ( version != null )
            {
                artifact.getVersion();
                artifactControl.setReturnValue( version, MockControl.ONE_OR_MORE );
            }
            
            if ( extension != null )
            {
                handler.getExtension();
                handlerControl.setReturnValue( extension, MockControl.ONE_OR_MORE );
            }
            
            artifact.isSnapshot();
            artifactControl.setReturnValue( true, MockControl.ONE_OR_MORE );
            
            if ( classifier != null )
            {
                artifact.getClassifier();
                artifactControl.setReturnValue( classifier, MockControl.ONE_OR_MORE );
            }
            
            artifact.getArtifactHandler();
            artifactControl.setReturnValue( handler, MockControl.ONE_OR_MORE );
        }
        
    }
}
