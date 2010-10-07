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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.util.Properties;

import junit.framework.TestCase;

public class AssemblyFormatUtilsTest
    extends TestCase
{

    private final MockManager mockManager = new MockManager();

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
        verifyOutputDirUsingArtifactProject( "${artifact.groupId}", null, "group", null, null, null, null, "group/" );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingArtifactProject( "${artifact.artifactId}", null, null, "artifact", null, null, null,
                                             "artifact/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingArtifactProject( "${artifact.version}", null, null, null, "version", null, null, "version/" );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingArtifactProject( "${artifact.build.finalName}", null, null, null, null, "finalName", null,
                                             "finalName/" );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseModuleInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingModuleProject( "${module.groupId}", null, "group", null, null, null, null, "group/" );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseModuleInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingModuleProject( "${module.artifactId}", null, null, "artifact", null, null, null,
                                           "artifact/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseModuleInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingModuleProject( "${module.version}", null, null, null, "version", null, null, "version/" );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseModuleInfo()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingModuleProject( "${module.build.finalName}", null, null, null, null, "finalName", null,
                                           "finalName/" );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${pom.groupId}", null, "group", null, null, null, null, "group/" );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${pom.artifactId}", null, null, "artifact", null, null, null, "artifact/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${pom.version}", null, null, null, "version", null, null, "version/" );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${pom.build.finalName}", null, null, null, null, "finalName", null,
                                         "finalName/" );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${project.groupId}", null, "group", null, null, null, null, "group/" );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${project.artifactId}", null, null, "artifact", null, null, null, "artifact/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${project.version}", null, null, null, "version", null, null, "version/" );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "${project.build.finalName}", null, "finalName", "finalName/" );
    }

    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "dir/", "finalName", null, "dir/" );
    }

    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions_CheckWithBackslash()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "dir\\", "finalName", null, "dir\\" );
    }

    public void testGetOutputDir_ShouldAppendSlashToOutDirWhenMissingAndIncludeBaseFalseAndNoExpressions()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "dir", "finalName", null, "dir/" );
    }

    public void testGetOutputDir_ShouldResolveGroupIdInOutDir()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${groupId}", "finalName", "group", null, null, null, null, "group/" );
    }

    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${artifactId}", "finalName", null, "artifact", null, null, null, "artifact/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInOutDir()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "${version}", "finalName", null, null, "version", null, null, "version/" );
    }

    public void testGetOutputDir_ShouldResolveVersionInLargerOutDirExpr()
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( "my-special-${version}", "finalName", null, null, "99", null, null,
                                         "my-special-99/" );
    }

    public void testGetOutputDir_ShouldResolveFinalNameInOutDir()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "${finalName}", "finalName", null, "finalName/" );
    }

    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir()
        throws AssemblyFormattingException
    {
        verifyOutputDir( "${build.finalName}", "finalName", null, "finalName/" );
    }

    public void testGetOutputDir_ShouldReturnEmptyPathWhenAllInputIsEmptyAndIncludeBaseFalse()
        throws AssemblyFormattingException
    {
        verifyOutputDir( null, null, null, "" );
    }

    public void testGetOutputDir_ShouldResolveProjectProperty()
        throws AssemblyFormattingException
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyOutputDirUsingMainProject( "file.${myProperty}", null, null, null, null, null, props, "file.value/" );
    }

    public void testGetOutputDir_ShouldResolveProjectPropertyAltExpr()
        throws AssemblyFormattingException
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyOutputDirUsingMainProject( "file.${pom.properties.myProperty}", null, null, null, null, null, props,
                                         "file.value/" );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdAndBaseVersionInOutDir_UseArtifactInfo_WithValidMainProject()
        throws AssemblyFormattingException
    {
        final MavenProject mainProject = createProject( "group", "main", "1", null );

        final String artifactVersion = "2-20070807.112233-1";
        final String artifactBaseVersion = "2-SNAPSHOT";
        final MavenProject artifactProject = createProject( "group", "artifact", artifactVersion, null );
        final ArtifactMock artifactMock =
            new ArtifactMock( mockManager, "group", "artifact", artifactVersion, "jar", true, artifactBaseVersion );

        artifactProject.setArtifact( artifactMock.getArtifact() );

        final MockControl sessionCtl = MockClassControl.createControl( MavenSession.class );
        mockManager.add( sessionCtl );

        final MavenSession session = (MavenSession) sessionCtl.getMock();
        session.getExecutionProperties();
        sessionCtl.setReturnValue( null, MockControl.ZERO_OR_MORE );

        session.getUserProperties();
        sessionCtl.setReturnValue( new Properties(), MockControl.ZERO_OR_MORE );

        final MockControl csCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( csCtl );

        final AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csCtl.getMock();
        cs.getMavenSession();
        csCtl.setReturnValue( session, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        final String result =
            AssemblyFormatUtils.evaluateFileNameMapping( "${artifact.artifactId}-${artifact.baseVersion}",
                                                         artifactMock.getArtifact(), mainProject, artifactProject, cs );

        assertEquals( "artifact-2-SNAPSHOT", result );

        mockManager.verifyAll();

        // clear out for next call.
        mockManager.clear();
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingArtifactProject( "${artifact.groupId}", null, "group", null, null, null, "group",
                                                       null );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingArtifactProject( "${artifact.artifactId}", null, null, "artifact", null, null,
                                                       "artifact", null );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseArtifactInfo()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingArtifactProject( "${artifact.version}", null, null, null, "version", null,
                                                       "version", null );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseArtifactInfoAndModulePrefix()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingModuleProject( "${module.groupId}", null, "group", null, null, null, "group",
                                                     null );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseArtifactInfoAndModulePrefix()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingModuleProject( "${module.artifactId}", null, null, "artifact", null, null,
                                                     "artifact", null );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseArtifactInfoAndModulePrefix()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingModuleProject( "${module.version}", null, null, null, "version", null, "version",
                                                     null );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${pom.groupId}", null, "group", null, null, null, "group", null );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${pom.artifactId}", null, null, "artifact", null, null, "artifact",
                                                   null );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseExplicitMainProject()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${pom.version}", null, null, null, "version", null, "version", null );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${project.groupId}", null, "group", null, null, null, "group", null );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${project.artifactId}", null, null, "artifact", null, null,
                                                   "artifact", null );
    }

    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseExplicitMainProject_projectRef()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${project.version}", null, null, null, "version", null, "version",
                                                   null );
    }

    public void testEvalFileNameMapping_ShouldPassExpressionThroughUnchanged()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename", null, null, "filename", null );
    }

    public void testEvalFileNameMapping_ShouldInsertClassifierAheadOfExtension()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename-${artifact.classifier}.ext", "classifier", null,
                                   "filename-classifier.ext", null );
    }

    public void testEvalFileNameMapping_ShouldAppendDashClassifierWhenClassifierPresent()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", "classifier", null, "filename-classifier", null );
    }

    public void testEvalFileNameMapping_ShouldNotAppendDashClassifierWhenClassifierMissing()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", null, null, "filename", null );
    }

    public void testEvalFileNameMapping_ShouldResolveGroupId()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${groupId}", null, "group", null, null, null, "group", null );
    }

    public void testEvalFileNameMapping_ShouldResolveArtifactId()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${artifactId}", null, null, "artifact", null, null, "artifact",
                                                   null );
    }

    public void testEvalFileNameMapping_ShouldResolveVersion()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( "${version}", null, null, null, "version", null, "version", null );
    }

    public void testEvalFileNameMapping_ShouldResolveExtension()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "file.${artifact.extension}", null, "ext", "file.ext", null );
    }

    public void testEvalFileNameMapping_ShouldResolveProjectProperty()
        throws AssemblyFormattingException
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyEvalFileNameMapping( "file.${myProperty}", null, null, "file.value", props );
    }

    public void testEvalFileNameMapping_ShouldResolveProjectPropertyAltExpr()
        throws AssemblyFormattingException
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyEvalFileNameMapping( "file.${pom.properties.myProperty}", null, null, "file.value", props );
    }

    public void testEvalFileNameMapping_ShouldResolveSystemPropertyWithoutMainProjectPresent()
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMapping( "file.${java.version}", null, null, "file." + System.getProperty( "java.version" ),
                                   null );
    }

    private void verifyEvalFileNameMapping( final String expression, final String classifier, final String extension,
                                            final String checkValue, final Properties projectProperties )
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( expression, classifier, null, null, null, extension, checkValue,
                                                   projectProperties );
    }

    private void verifyEvalFileNameMappingUsingMainProject( final String expression, final String classifier,
                                                            final String groupId, final String artifactId,
                                                            final String version, final String extension,
                                                            final String checkValue, final Properties projectProperties )
        throws AssemblyFormattingException
    {
        final MavenProject mainProject = createProject( groupId, artifactId, version, projectProperties );

        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );

        verifyEvalFileNameMapping( expression, classifier, extension, mainProject, moduleProject, artifactProject,
                                   checkValue );
    }

    private void verifyEvalFileNameMappingUsingArtifactProject( final String expression, final String classifier,
                                                                final String groupId, final String artifactId,
                                                                final String version, final String extension,
                                                                final String checkValue,
                                                                final Properties projectProperties )
        throws AssemblyFormattingException
    {
        final MavenProject artifactProject = createProject( groupId, artifactId, version, projectProperties );

        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );

        verifyEvalFileNameMapping( expression, classifier, extension, mainProject, moduleProject, artifactProject,
                                   checkValue );
    }

    private void verifyEvalFileNameMappingUsingModuleProject( final String expression, final String classifier,
                                                              final String groupId, final String artifactId,
                                                              final String version, final String extension,
                                                              final String checkValue,
                                                              final Properties projectProperties )
        throws AssemblyFormattingException
    {
        final MavenProject moduleProject = createProject( groupId, artifactId, version, projectProperties );

        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );

        verifyEvalFileNameMapping( expression, classifier, extension, mainProject, moduleProject, artifactProject,
                                   checkValue );
    }

    private MavenProject createProject( String groupId, String artifactId, String version,
                                        final Properties projectProperties )
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

        final Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        model.setProperties( projectProperties );

        return new MavenProject( model );
    }

    private void verifyEvalFileNameMapping( final String expression, final String classifier, final String extension,
                                            final MavenProject mainProject, final MavenProject moduleProject,
                                            final MavenProject artifactProject, final String checkValue )
        throws AssemblyFormattingException
    {
        final ArtifactMock artifactMock =
            new ArtifactMock( mockManager, artifactProject.getGroupId(), artifactProject.getArtifactId(),
                              artifactProject.getVersion(), extension, classifier, false, null );

        final ArtifactMock moduleArtifactMock =
            new ArtifactMock( mockManager, moduleProject.getGroupId(), moduleProject.getArtifactId(),
                              moduleProject.getVersion(), "jar", false, null );

        final MockControl sessionCtl = MockClassControl.createControl( MavenSession.class );
        mockManager.add( sessionCtl );

        final MavenSession session = (MavenSession) sessionCtl.getMock();
        session.getExecutionProperties();
        sessionCtl.setReturnValue( System.getProperties(), MockControl.ZERO_OR_MORE );

        session.getUserProperties();
        sessionCtl.setReturnValue( new Properties(), MockControl.ZERO_OR_MORE );

        final MockControl csCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( csCtl );

        final AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csCtl.getMock();
        cs.getMavenSession();
        csCtl.setReturnValue( session, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        final String result =
            AssemblyFormatUtils.evaluateFileNameMapping( expression, artifactMock.getArtifact(), mainProject,
                                                         moduleProject, moduleArtifactMock.getArtifact(),
                                                         artifactProject, cs );

        assertEquals( checkValue, result );

        mockManager.verifyAll();

        // clear out for next call.
        mockManager.clear();
    }

    private void verifyOutputDir( final String outDir, final String finalName, final String projectFinalName,
                                  final String checkValue )
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( outDir, finalName, null, null, null, projectFinalName, null, checkValue );
    }

    private void verifyOutputDirUsingMainProject( final String outDir, final String finalName, final String groupId,
                                                  final String artifactId, final String version,
                                                  final String projectFinalName, final Properties properties,
                                                  final String checkValue )
        throws AssemblyFormattingException
    {
        final MavenProject project = createProject( groupId, artifactId, version, properties );

        if ( projectFinalName != null )
        {
            final Build build = new Build();
            build.setFinalName( projectFinalName );

            project.getModel().setBuild( build );
        }

        final MavenProject mainProject = project;
        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );

        verifyOutputDir( outDir, finalName, mainProject, moduleProject, artifactProject, checkValue );
    }

    private void verifyOutputDirUsingModuleProject( final String outDir, final String finalName, final String groupId,
                                                    final String artifactId, final String version,
                                                    final String projectFinalName, final Properties properties,
                                                    final String checkValue )
        throws AssemblyFormattingException
    {
        final MavenProject project = createProject( groupId, artifactId, version, properties );

        if ( projectFinalName != null )
        {
            final Build build = new Build();
            build.setFinalName( projectFinalName );

            project.getModel().setBuild( build );
        }

        final MavenProject moduleProject = project;
        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );

        verifyOutputDir( outDir, finalName, mainProject, moduleProject, artifactProject, checkValue );
    }

    private void verifyOutputDirUsingArtifactProject( final String outDir, final String finalName,
                                                      final String groupId, final String artifactId,
                                                      final String version, final String projectFinalName,
                                                      final Properties properties, final String checkValue )
        throws AssemblyFormattingException
    {
        final MavenProject project = createProject( groupId, artifactId, version, properties );

        if ( projectFinalName != null )
        {
            final Build build = new Build();
            build.setFinalName( projectFinalName );

            project.getModel().setBuild( build );
        }

        final MavenProject artifactProject = project;
        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );

        verifyOutputDir( outDir, finalName, mainProject, moduleProject, artifactProject, checkValue );
    }

    private void verifyOutputDir( final String outDir, final String finalName, final MavenProject mainProject,
                                  final MavenProject moduleProject, final MavenProject artifactProject,
                                  final String checkValue )
        throws AssemblyFormattingException
    {
        final MockControl sessionCtl = MockClassControl.createControl( MavenSession.class );
        mockManager.add( sessionCtl );

        final MavenSession session = (MavenSession) sessionCtl.getMock();
        session.getExecutionProperties();
        sessionCtl.setReturnValue( System.getProperties(), MockControl.ZERO_OR_MORE );

        session.getUserProperties();
        sessionCtl.setReturnValue( new Properties(), MockControl.ZERO_OR_MORE );

        final MockControl csCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( csCtl );

        final AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csCtl.getMock();
        cs.getMavenSession();
        csCtl.setReturnValue( session, MockControl.ZERO_OR_MORE );

        String result;

        mockManager.replayAll();
        result =
            AssemblyFormatUtils.getOutputDirectory( outDir, mainProject, moduleProject, artifactProject, finalName, cs );

        assertEquals( checkValue, result );

        mockManager.verifyAll();

        mockManager.clear();
    }

    private void verifyDistroName( final String assemblyId, final String classifier, final String finalName,
                                   final boolean appendAssemblyId, final String checkValue )
    {
        final MockAndControlForGetDistroName mac =
            new MockAndControlForGetDistroName( finalName, appendAssemblyId, classifier );

        mockManager.replayAll();

        final Assembly assembly = new Assembly();
        assembly.setId( assemblyId );

        final String result = AssemblyFormatUtils.getDistributionName( assembly, mac.configSource );

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

        public MockAndControlForGetDistroName( final String finalName, final boolean isAssemblyIdAppended,
                                               final String classifier )
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
