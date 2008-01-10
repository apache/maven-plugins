package org.apache.maven.plugin.assembly.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
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

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "${artifact.groupId}", null, "group", null, null, null, "group/", false );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseArtifactInfo()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${artifact.artifactId}", null, null, "artifact", null, null, "artifact/", false );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseArtifactInfo()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${artifact.version}", null, null, null, "version", null, "version/", false );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseArtifactInfo()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${artifact.build.finalName}", null, null, null, null, "finalName", "finalName/", false );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${module.groupId}", null, "group", null, null, null, "group/", "module.", false );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${module.artifactId}", null, null, "artifact", null, null, "artifact/", "module.", false );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${module.version}", null, null, null, "version", null, "version/", "module.", false );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${module.build.finalName}", null, null, null, null, "finalName", "finalName/", "module.", false );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${pom.groupId}", null, "group", null, null, null, "group/", true );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${pom.artifactId}", null, null, "artifact", null, null, "artifact/", true );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${pom.version}", null, null, null, "version", null, "version/", true );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${pom.build.finalName}", null, null, null, null, "finalName", "finalName/", true );
    }

    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "dir/", "finalName", null, null, null, "dir/" );
    }

    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions_CheckWithBackslash()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "dir\\", "finalName", null, null, null, "dir\\" );
    }

    public void testGetOutputDir_ShouldAppendSlashToOutDirWhenMissingAndIncludeBaseFalseAndNoExpressions()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "dir", "finalName", null, null, null, "dir/" );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${groupId}", "finalName", "group", null, null, "group/" );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${artifactId}", "finalName", null, "artifact", null, "artifact/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${version}", "finalName", null, null, "version", "version/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInLargerOutDirExpr()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "my-special-${version}", "finalName", null, null, "99", "my-special-99/" );
    }

    public void testGetOutputDir_ShouldResolveFinalNameInOutDir()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${finalName}", "finalName", null, null, null, "finalName/" );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir()
    throws AssemblyFormattingException
    {
        verifyOutputDir( "${build.finalName}", "finalName", null, null, null, "finalName/" );
    }

    public void testGetOutputDir_ShouldReturnEmptyPathWhenAllInputIsEmptyAndIncludeBaseFalse()
    throws AssemblyFormattingException
    {
        verifyOutputDir( null, null, null, null, null, "" );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdAndBaseVersionInOutDir_UseArtifactInfo_WithValidMainProject()
    throws AssemblyFormattingException
    {
        MavenProject mainProject = newMavenProject( "group", "main", "1" );

        String artifactVersion = "2-20070807.112233-1";
        String artifactBaseVersion = "2-SNAPSHOT";
        MavenProject artifactProject = newMavenProject( "group", "artifact", artifactVersion );
        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", artifactVersion, "jar", true, artifactBaseVersion );

        artifactProject.setArtifact( artifactMock.getArtifact() );

        mockManager.replayAll();

        String result = AssemblyFormatUtils.evaluateFileNameMapping( "${artifact.artifactId}-${artifact.baseVersion}", artifactMock.getArtifact(), mainProject, artifactProject, "artifact." );

        assertEquals( "artifact-2-SNAPSHOT", result );

        mockManager.verifyAll();

        // clear out for next call.
        mockManager.clear();
    }

    private MavenProject newMavenProject( String groupId, String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        return new MavenProject( model );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseArtifactInfo()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${artifact.groupId}", null, "group", null, null, null, "group", false );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseArtifactInfo()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${artifact.artifactId}", null, null, "artifact", null, null, "artifact", false );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseArtifactInfo()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${artifact.version}", null, null, null, "version", null, "version", false );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${module.groupId}", null, "group", null, null, null, "group", "module.", false );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${module.artifactId}", null, null, "artifact", null, null, "artifact", "module.", false );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseArtifactInfoAndModulePrefix()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${module.version}", null, null, null, "version", null, "version", "module.", false );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${pom.groupId}", null, "group", null, null, null, "group", true );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${pom.artifactId}", null, null, "artifact", null, null, "artifact", true );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseExplicitMainProject()
    throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "${pom.version}", null, null, null, "version", null, "version", true );
    }

    public void testEvalFileNameMapping_ShouldPassExpressionThroughUnchanged() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename", null, null, null, null, null, "filename" );
    }

    public void testEvalFileNameMapping_ShouldInsertClassifierAheadOfExtension() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename-${artifact.classifier}.ext", "classifier", null, null, null, null, "filename-classifier.ext" );
    }

    public void testEvalFileNameMapping_ShouldAppendDashClassifierWhenClassifierPresent() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", "classifier", null, null, null, null, "filename-classifier" );
    }

    public void testEvalFileNameMapping_ShouldNotAppendDashClassifierWhenClassifierMissing() throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", null, null, null, null, null, "filename" );
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
        verifyEvalFileNameMapping( "file.${artifact.extension}", null, null, null, null, "ext", "file.ext" );
    }

    private void verifyEvalFileNameMapping( String expression, String classifier, String groupId, String artifactId,
                                            String version, String extension, String checkValue )
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( expression, classifier, groupId, artifactId, version, extension, checkValue, true );
    }

    private void verifyEvalFileNameMapping( String expression, String classifier, String groupId, String artifactId,
                                            String version, String extension, String checkValue, boolean usingMainProject )
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( expression, classifier, groupId, artifactId, version, extension, checkValue, null, usingMainProject );
    }

    private void verifyEvalFileNameMapping( String expression, String classifier, String groupId, String artifactId,
                                            String version, String extension, String checkValue, String prefix, boolean usingMainProject )
        throws AssemblyFormattingException
    {
        if ( artifactId == null )
        {
            artifactId = "artifact";
        }

        if ( groupId == null )
        {
            groupId = "group";
        }

        if ( version == null )
        {
            version = "version";
        }

        if ( extension == null )
        {
            extension = "jar";
        }

        MavenProject project = null;
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        project = new MavenProject( model );

        MavenProject mainProject;
        MavenProject artifactProject = null;

        if ( usingMainProject )
        {
            mainProject = project;
        }
        else
        {
            artifactProject = project;
            mainProject = new MavenProject( new Model() );
        }

        ArtifactMock artifactMock = new ArtifactMock( mockManager, groupId, artifactId, version, extension, classifier, false );

        mockManager.replayAll();

        String result;
        if ( prefix == null )
        {
            result = AssemblyFormatUtils.evaluateFileNameMapping( expression, artifactMock.getArtifact(), mainProject, artifactProject );
        }
        else
        {
            result = AssemblyFormatUtils.evaluateFileNameMapping( expression, artifactMock.getArtifact(), mainProject, artifactProject, prefix );
        }

        assertEquals( checkValue, result );

        mockManager.verifyAll();

        // clear out for next call.
        mockManager.clear();
    }

    private void verifyOutputDir( String outDir, String finalName, String groupId, String artifactId, String version,
                                  String checkValue )
        throws AssemblyFormattingException
    {
        verifyOutputDir( outDir, finalName, groupId, artifactId, version, null, checkValue, true );
    }

    private void verifyOutputDir( String outDir, String finalName, String groupId, String artifactId, String version,
                                  String projectFinalName, String checkValue, boolean usingMainProject )
        throws AssemblyFormattingException
    {
        verifyOutputDir( outDir, finalName, groupId, artifactId, version, projectFinalName, checkValue, null, usingMainProject );
    }

    private void verifyOutputDir( String outDir, String finalName, String groupId, String artifactId, String version,
                                  String projectFinalName, String checkValue, String prefix, boolean usingMainProject )
        throws AssemblyFormattingException
    {
        MavenProject project = null;
        if ( ( groupId != null ) || ( artifactId != null ) || ( version != null ) || ( projectFinalName != null ) )
        {
            Model model = new Model();
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );

            if ( projectFinalName != null )
            {
                Build build = new Build();
                build.setFinalName( projectFinalName );

                model.setBuild( build );
            }

            project = new MavenProject( model );
        }

        MavenProject mainProject;
        MavenProject artifactProject = null;

        if ( usingMainProject )
        {
            mainProject = project;
        }
        else
        {
            artifactProject = project;
            mainProject = new MavenProject( new Model() );
        }

        String result;
        if ( prefix == null )
        {
            result = AssemblyFormatUtils.getOutputDirectory( outDir, mainProject, artifactProject, finalName );
        }
        else
        {
            result = AssemblyFormatUtils.getOutputDirectory( outDir, mainProject, artifactProject, finalName, prefix );
        }

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

}
