package org.apache.maven.plugin.assembly.filter;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import junit.framework.TestCase;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class ComponentsXmlArchiverFileFilterTest
    extends TestCase
{
    private ComponentsXmlArchiverFileFilter filter;

    private final TestFileManager fileManager = new TestFileManager( "componentsXmlArchiverFileFilter.test", ".zip" );

    @Override
    public void setUp()
    {
        filter = new ComponentsXmlArchiverFileFilter();
    }

    @Override
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testAddComponentsXml_ShouldAddComponentWithoutRoleHint()
        throws IOException, XmlPullParserException
    {
        final Reader reader =
            writeComponentsXml( Collections.singletonList( new ComponentDef( "role", null, "org.apache.maven.Impl" ) ) );

        filter.addComponentsXml( reader );

        assertFalse( filter.components.isEmpty() );

        final Xpp3Dom componentDom = filter.components.get( "role" );

        assertEquals( "role", componentDom.getChild( "role" )
                                          .getValue() );
        assertNull( componentDom.getChild( "role-hint" ) );
        assertEquals( "org.apache.maven.Impl", componentDom.getChild( "implementation" )
                                                           .getValue() );
    }

    public void testAddComponentsXml_ShouldAddComponentWithRoleHint()
        throws IOException, XmlPullParserException
    {
        final Reader reader =
            writeComponentsXml( Collections.singletonList( new ComponentDef( "role", "hint", "org.apache.maven.Impl" ) ) );

        filter.addComponentsXml( reader );

        assertFalse( filter.components.isEmpty() );

        final Xpp3Dom componentDom = filter.components.get( "rolehint" );

        assertEquals( "role", componentDom.getChild( "role" )
                                          .getValue() );
        assertEquals( "hint", componentDom.getChild( "role-hint" )
                                          .getValue() );
        assertEquals( "org.apache.maven.Impl", componentDom.getChild( "implementation" )
                                                           .getValue() );
    }

    public void testAddComponentsXml_ShouldAddTwoComponentsWithRoleHints()
        throws IOException, XmlPullParserException
    {
        final List<ComponentDef> defs = new ArrayList<ComponentDef>();

        defs.add( new ComponentDef( "role", "hint", "org.apache.maven.Impl" ) );
        defs.add( new ComponentDef( "role", "hint2", "org.apache.maven.Impl2" ) );

        final Reader reader = writeComponentsXml( defs );

        filter.addComponentsXml( reader );

        assertFalse( filter.components.isEmpty() );

        Xpp3Dom componentDom = filter.components.get( "rolehint" );

        assertEquals( "role", componentDom.getChild( "role" )
                                          .getValue() );
        assertEquals( "hint", componentDom.getChild( "role-hint" )
                                          .getValue() );
        assertEquals( "org.apache.maven.Impl", componentDom.getChild( "implementation" )
                                                           .getValue() );

        componentDom = filter.components.get( "rolehint2" );

        assertEquals( "role", componentDom.getChild( "role" )
                                          .getValue() );
        assertEquals( "hint2", componentDom.getChild( "role-hint" )
                                           .getValue() );
        assertEquals( "org.apache.maven.Impl2", componentDom.getChild( "implementation" )
                                                            .getValue() );
    }

    public void testAddToArchive_ShouldWriteComponentWithoutHintToFile()
        throws IOException, ArchiverException, JDOMException
    {
        final Xpp3Dom dom = createComponentDom( new ComponentDef( "role", null, "impl" ) );

        filter.components = new LinkedHashMap<String, Xpp3Dom>();
        filter.components.put( "role", dom );

        final FileCatchingArchiver fca = new FileCatchingArchiver();

        filter.finalizeArchiveCreation( fca );

        assertEquals( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH, fca.getDestFileName() );

        final SAXBuilder builder = new SAXBuilder( false );

        final Document doc = builder.build( fca.getFile() );

        final XPath role = XPath.newInstance( "//component[position()=1]/role/text()" );
        final XPath hint = XPath.newInstance( "//component[position()=1]/role-hint/text()" );
        final XPath implementation = XPath.newInstance( "//component[position()=1]/implementation/text()" );

        assertEquals( "role", ( (Text) role.selectSingleNode( doc ) ).getText() );
        assertNull( hint.selectSingleNode( doc ) );
        assertEquals( "impl", ( (Text) implementation.selectSingleNode( doc ) ).getText() );
    }

    public void testAddToArchive_ShouldWriteComponentWithHintToFile()
        throws IOException, ArchiverException, JDOMException
    {
        final Xpp3Dom dom = createComponentDom( new ComponentDef( "role", "hint", "impl" ) );

        filter.components = new LinkedHashMap<String, Xpp3Dom>();
        filter.components.put( "rolehint", dom );

        final FileCatchingArchiver fca = new FileCatchingArchiver();

        filter.finalizeArchiveCreation( fca );

        assertEquals( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH, fca.getDestFileName() );

        final SAXBuilder builder = new SAXBuilder( false );

        final Document doc = builder.build( fca.getFile() );

        final XPath role = XPath.newInstance( "//component[position()=1]/role/text()" );
        final XPath hint = XPath.newInstance( "//component[position()=1]/role-hint/text()" );
        final XPath implementation = XPath.newInstance( "//component[position()=1]/implementation/text()" );

        assertEquals( "role", ( (Text) role.selectSingleNode( doc ) ).getText() );
        assertEquals( "hint", ( (Text) hint.selectSingleNode( doc ) ).getText() );
        assertEquals( "impl", ( (Text) implementation.selectSingleNode( doc ) ).getText() );
    }

    public void testAddToArchive_ShouldWriteTwoComponentToFile()
        throws IOException, ArchiverException, JDOMException
    {
        filter.components = new LinkedHashMap<String, Xpp3Dom>();

        final Xpp3Dom dom = createComponentDom( new ComponentDef( "role", "hint", "impl" ) );

        filter.components.put( "rolehint", dom );

        final Xpp3Dom dom2 = createComponentDom( new ComponentDef( "role", "hint2", "impl" ) );

        filter.components.put( "rolehint2", dom2 );

        final FileCatchingArchiver fca = new FileCatchingArchiver();

        filter.finalizeArchiveCreation( fca );

        assertEquals( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH, fca.getDestFileName() );

        final SAXBuilder builder = new SAXBuilder( false );

        final Document doc = builder.build( fca.getFile() );

        final XPath role = XPath.newInstance( "//component[position()=1]/role/text()" );
        final XPath hint = XPath.newInstance( "//component[position()=1]/role-hint/text()" );
        final XPath implementation = XPath.newInstance( "//component[position()=1]/implementation/text()" );

        assertEquals( "role", ( (Text) role.selectSingleNode( doc ) ).getText() );
        assertEquals( "hint", ( (Text) hint.selectSingleNode( doc ) ).getText() );
        assertEquals( "impl", ( (Text) implementation.selectSingleNode( doc ) ).getText() );

        final XPath role2 = XPath.newInstance( "//component[position()=2]/role/text()" );
        final XPath hint2 = XPath.newInstance( "//component[position()=2]/role-hint/text()" );
        final XPath implementation2 = XPath.newInstance( "//component[position()=2]/implementation/text()" );

        assertEquals( "role", ( (Text) role2.selectSingleNode( doc ) ).getText() );
        assertEquals( "hint2", ( (Text) hint2.selectSingleNode( doc ) ).getText() );
        assertEquals( "impl", ( (Text) implementation2.selectSingleNode( doc ) ).getText() );

    }

    public void testAddToArchive_ShouldWriteTwoComponentToArchivedFile()
        throws IOException, ArchiverException, JDOMException
    {
        filter.components = new LinkedHashMap<String, Xpp3Dom>();

        final Xpp3Dom dom = createComponentDom( new ComponentDef( "role", "hint", "impl" ) );

        filter.components.put( "rolehint", dom );

        final Xpp3Dom dom2 = createComponentDom( new ComponentDef( "role", "hint2", "impl" ) );

        filter.components.put( "rolehint2", dom2 );

        final ZipArchiver archiver = new ZipArchiver();

        final File archiveFile = fileManager.createTempFile();

        archiver.setDestFile( archiveFile );

        final File descriptorFile = fileManager.createTempFile();

        archiver.setArchiveFinalizers( Collections.<ArchiveFinalizer>singletonList( filter ) );

        archiver.createArchive();

        final ZipFile zf = new ZipFile( archiveFile );

        final ZipEntry ze = zf.getEntry( ComponentsXmlArchiverFileFilter.COMPONENTS_XML_PATH );

        assertNotNull( ze );

        final FileOutputStream fileStream = new FileOutputStream( descriptorFile );

        IOUtil.copy( zf.getInputStream( ze ), fileStream );
        IOUtil.close( fileStream );

        final SAXBuilder builder = new SAXBuilder( false );

        final Document doc = builder.build( descriptorFile );

        final XPath role = XPath.newInstance( "//component[position()=1]/role/text()" );
        final XPath hint = XPath.newInstance( "//component[position()=1]/role-hint/text()" );
        final XPath implementation = XPath.newInstance( "//component[position()=1]/implementation/text()" );

        assertEquals( "role", ( (Text) role.selectSingleNode( doc ) ).getText() );
        assertEquals( "hint", ( (Text) hint.selectSingleNode( doc ) ).getText() );
        assertEquals( "impl", ( (Text) implementation.selectSingleNode( doc ) ).getText() );

        final XPath role2 = XPath.newInstance( "//component[position()=2]/role/text()" );
        final XPath hint2 = XPath.newInstance( "//component[position()=2]/role-hint/text()" );
        final XPath implementation2 = XPath.newInstance( "//component[position()=2]/implementation/text()" );

        assertEquals( "role", ( (Text) role2.selectSingleNode( doc ) ).getText() );
        assertEquals( "hint2", ( (Text) hint2.selectSingleNode( doc ) ).getText() );
        assertEquals( "impl", ( (Text) implementation2.selectSingleNode( doc ) ).getText() );

    }

    private Xpp3Dom createComponentDom( final ComponentDef def )
    {
        final Xpp3Dom dom = new Xpp3Dom( "component" );

        final Xpp3Dom role = new Xpp3Dom( "role" );
        role.setValue( def.role );
        dom.addChild( role );

        final String hint = def.roleHint;
        if ( hint != null )
        {
            final Xpp3Dom roleHint = new Xpp3Dom( "role-hint" );
            roleHint.setValue( hint );
            dom.addChild( roleHint );
        }

        final Xpp3Dom impl = new Xpp3Dom( "implementation" );
        impl.setValue( def.implementation );
        dom.addChild( impl );

        return dom;
    }

    private Reader writeComponentsXml( final List<ComponentDef> componentDefs )
        throws IOException
    {
        final StringWriter writer = new StringWriter();

        final PrettyPrintXMLWriter xmlWriter = new PrettyPrintXMLWriter( writer );

        xmlWriter.startElement( "component-set" );
        xmlWriter.startElement( "components" );

        for ( final ComponentDef def : componentDefs )
        {
            xmlWriter.startElement( "component" );

            xmlWriter.startElement( "role" );
            xmlWriter.writeText( def.role );
            xmlWriter.endElement();

            final String roleHint = def.roleHint;
            if ( roleHint != null )
            {
                xmlWriter.startElement( "role-hint" );
                xmlWriter.writeText( roleHint );
                xmlWriter.endElement();
            }

            xmlWriter.startElement( "implementation" );
            xmlWriter.writeText( def.implementation );
            xmlWriter.endElement();

            xmlWriter.endElement();
        }

        xmlWriter.endElement();
        xmlWriter.endElement();

        return new StringReader( writer.toString() );
    }

    private static final class ComponentDef
    {
        String role;

        String roleHint;

        String implementation;

        ComponentDef( final String role, final String roleHint, final String implementation )
        {
            this.role = role;
            this.roleHint = roleHint;
            this.implementation = implementation;

        }
    }

    private static final class FileCatchingArchiver
        implements Archiver
    {

        private File inputFile;

        private String destFileName;

        private boolean useJvmChmod;

        private boolean ignorePermissions;

        public void addDirectory( final File directory )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addDirectory( final File directory, final String prefix )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addDirectory( final File directory, final String[] includes, final String[] excludes )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addDirectory( final File directory, final String prefix, final String[] includes,
                                  final String[] excludes )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addFile( final File inputFile, final String destFileName )
            throws ArchiverException
        {
            this.inputFile = inputFile;
            this.destFileName = destFileName;
        }

        File getFile()
        {
            return inputFile;
        }

        String getDestFileName()
        {
            return destFileName;
        }

        public void addFile( final File inputFile, final String destFileName, final int permissions )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void createArchive()
            throws ArchiverException, IOException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public int getDefaultDirectoryMode()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public int getDefaultFileMode()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public File getDestFile()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        @SuppressWarnings( "rawtypes" )
        public Map getFiles()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public boolean getIncludeEmptyDirs()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setDefaultDirectoryMode( final int mode )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setDefaultFileMode( final int mode )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setDestFile( final File destFile )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setIncludeEmptyDirs( final boolean includeEmptyDirs )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addArchivedFileSet( final File archiveFile )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addArchivedFileSet( final File archiveFile, final String prefix )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addArchivedFileSet( final File archiveFile, final String[] includes, final String[] excludes )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addArchivedFileSet( final File archiveFile, final String prefix, final String[] includes,
                                        final String[] excludes )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setForced( final boolean forced )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public boolean isForced()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public boolean isSupportingForced()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setDotFileDirectory( final File dotFileDirectory )
        {
        }

        public void addArchivedFileSet( final ArchivedFileSet fileSet )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addFileSet( final FileSet fileSet )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addResource( final PlexusIoResource resource, final String destFileName, final int permissions )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void addResources( final PlexusIoResourceCollection resources )
            throws ArchiverException
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public ResourceIterator getResources()
            throws ArchiverException
        {
            return new ResourceIterator()
            {

                public boolean hasNext()
                    throws ArchiverException
                {
                    return false;
                }

                public ArchiveEntry next()
                    throws ArchiverException
                {
                    throw new NoSuchElementException();
                }

                public void remove()
                {
                    throw new NoSuchElementException();
                }
            };
        }

        public String getDuplicateBehavior()
        {
            return Archiver.DUPLICATES_ADD;
        }

        public void setDuplicateBehavior( final String duplicate )
        {
        }

        public int getDirectoryMode()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public int getFileMode()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public int getOverrideDirectoryMode()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public int getOverrideFileMode()
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setDirectoryMode( final int mode )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public void setFileMode( final int mode )
        {
            throw new UnsupportedOperationException( "not supported" );
        }

        public boolean isUseJvmChmod()
        {
            return useJvmChmod;
        }

        public void setUseJvmChmod( final boolean useJvmChmod )
        {
            this.useJvmChmod = useJvmChmod;
        }

        public boolean isIgnorePermissions()
        {
            return ignorePermissions;
        }

        public void setIgnorePermissions( final boolean ignorePermissions )
        {
            this.ignorePermissions = ignorePermissions;
        }
    }

}
