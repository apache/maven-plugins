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
package org.apache.maven.plugin.eclipse.writers.rad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.eclipse.writers.AbstractWtpResourceWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

/**
 * This writer creates the application.xml and the .modulemaps files for RAD6
 * in the META-INF directory in the project root. this is where RAD6 requires
 * the files to be. These will be independent of the real application.xml witch
 * will be generated the stad. maven way.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class RadApplicationXMLWriter
    extends AbstractEclipseWriter
{

    private static final String APPLICATION_XML_APPLICATION = "application";

    private static final String APPLICATION_XML_CONTEXT_ROOT = "context-root";

    private static final String APPLICATION_XML_DESCRIPTION = "description";

    private static final String APPLICATION_XML_DISPLAY_NAME = "display-name";

    private static final String APPLICATION_XML_FILENAME = "application.xml";

    private static final String APPLICATION_XML_MODULE = "module";

    private static final String APPLICATION_XML_WEB = "web";

    private static final String APPLICATION_XML_WEB_URI = "web-uri";

    private static final String HREF = "href";

    private static final String ID = "id";

    private static final String MODULEMAP_EARPROJECT_MAP = "modulemap:EARProjectMap";

    private static final String MODULEMAPS_APPLICATION_EJB_MODULE = "application:EjbModule";

    private static final String MODULEMAPS_APPLICATION_WEB_MODULE = "application:WebModule";

    private static final String MODULEMAPS_FILENAME = ".modulemaps";

    private static final String MODULEMAPS_MAPPINGS = "mappings";

    private static final String MODULEMAPS_PROJECT_NAME = "projectName";

    private static final String MODULEMAPS_UTILITY_JARMAPPINGS = "utilityJARMappings";

    private static final String URI = "uri";

    private static final String VERSION = "version";

    private static final String XMI_ID = "xmi:id";

    private static final String XMI_TYPE = "xmi:type";

    private static final String XMI_VERSION = "xmi:version";

    private static final String XMLNS = "xmlns";

    private static final String XMLNS_APPLICATION = "xmlns:application";

    private static final String XMLNS_MODULEMAP = "xmlns:modulemap";

    private static final String XMLNS_SCHEMA_LOCATION = "xmlns:schemaLocation";

    private static final String XMLNS_XMI = "xmlns:xmi";

    private static final String XMLNS_XSI = "xmlns:xsi";

    private Xpp3Dom[] applicationXmlDomChildren;

    private long baseId = System.currentTimeMillis();

    private Xpp3Dom[] modulemapsXmlDomChildren;

    private Xpp3Dom[] webModulesFromPoms;

    /**
     * write the application.xml and the .modulemaps file to the META-INF
     * directory.
     * 
     * @see AbstractWtpResourceWriter#write(EclipseSourceDir[],
     *      ArtifactRepository, File)
     * @param sourceDirs
     *            all eclipse source directorys
     * @param localRepository
     *            the local reposetory
     * @param buildOutputDirectory
     *            build output directory (target)
     * @throws MojoExecutionException
     *             when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        String packaging = config.getProject().getPackaging();
        if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            File applicationXmlFile = new File( config.getEclipseProjectDirectory(), "META-INF" + File.separator
                + APPLICATION_XML_FILENAME );
            Xpp3Dom applicationXmlDom = readXMLFile( applicationXmlFile );
            if ( applicationXmlDom == null )
            {
                applicationXmlDom = createNewApplicationXml();
            }
            this.applicationXmlDomChildren = applicationXmlDom.getChildren( APPLICATION_XML_MODULE );

            File modulemapsXmlFile = new File( config.getEclipseProjectDirectory(), "META-INF" + File.separator
                + MODULEMAPS_FILENAME );
            Xpp3Dom modulemapsXmlDom = readXMLFile( modulemapsXmlFile );
            if ( modulemapsXmlDom == null )
            {
                modulemapsXmlDom = createNewModulemaps();
            }
            this.modulemapsXmlDomChildren = modulemapsXmlDom.getChildren();

            try
            {
                this.webModulesFromPoms = ( (Xpp3Dom) ( (org.apache.maven.model.Plugin) config.getProject().getBuild()
                    .getPluginsAsMap().get( "org.apache.maven.plugins:maven-ear-plugin" ) ).getConfiguration() )
                    .getChild( "modules" ).getChildren( "webModule" );
            }
            catch ( java.lang.NullPointerException ex )
            {
                this.webModulesFromPoms = new Xpp3Dom[0];
            }

            IdeDependency[] deps = config.getDeps();
            for ( int index = 0; index < deps.length; index++ )
            {
                updateApplicationXml( applicationXmlDom, modulemapsXmlDom, deps[index] );
            }

            removeUnusedEntries( applicationXmlDom, modulemapsXmlDom );

            writePrettyXmlFile( applicationXmlFile, applicationXmlDom );
            writePrettyXmlFile( modulemapsXmlFile, modulemapsXmlDom );
        }
    }

    /**
     * there is no existing application.xml file so create a new one.
     * 
     * @return the domtree representing the contents of application.xml
     */
    private Xpp3Dom createNewApplicationXml()
    {
        Xpp3Dom result = new Xpp3Dom( APPLICATION_XML_APPLICATION );
        result.setAttribute( ID, "Application_ID" );
        result.setAttribute( VERSION, "1.4" );
        result.setAttribute( XMLNS, "http://java.sun.com/xml/ns/j2ee" );
        result.setAttribute( XMLNS_XSI, "http://www.w3.org/2001/XMLSchema-instance" );
        result.setAttribute( XMLNS_SCHEMA_LOCATION,
                             "http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd" );
        result.addChild( new Xpp3Dom( APPLICATION_XML_DESCRIPTION ) );
        Xpp3Dom name = new Xpp3Dom( APPLICATION_XML_DISPLAY_NAME );
        name.setValue( config.getProject().getArtifactId() );
        result.addChild( name );
        return result;
    }

    /**
     * there is no existing .modulemaps file so create a new one.
     * 
     * @return the domtree representing the contents of the .modulemaps file
     */
    private Xpp3Dom createNewModulemaps()
    {
        Xpp3Dom result = new Xpp3Dom( MODULEMAP_EARPROJECT_MAP );
        result.setAttribute( XMI_VERSION, "2.0" );
        result.setAttribute( XMLNS_XMI, "http://www.omg.org/XMI" );
        result.setAttribute( XMLNS_APPLICATION, "application.xmi" );
        result.setAttribute( XMLNS_MODULEMAP, "modulemap.xmi" );
        result.setAttribute( XMI_ID, "EARProjectMap_" + ( this.baseId++ ) );
        return result;
    }

    /**
     * find an existing module entry in the application.xml file by looking up
     * the id in the modulemaps file and then using that to locate the entry in
     * the application.xml file.
     * 
     * @param applicationXmlDom
     *            application.xml dom tree
     * @param mapping
     *            .modulemaps dom tree
     * @return dom tree representing the module
     */
    private Xpp3Dom findModuleInApplicationXml( Xpp3Dom applicationXmlDom, Xpp3Dom mapping )
    {
        String id = getIdFromMapping( mapping );
        Xpp3Dom[] children = applicationXmlDom.getChildren();
        for ( int index = 0; index < children.length; index++ )
        {
            String childId = children[index].getAttribute( ID );
            if ( childId != null && childId.equals( id ) )
            {
                return children[index];
            }
        }
        return null;
    }

    /**
     * find an artifact in the modulemaps dom tree, if it is missing create a
     * new entry in the modulemaps dom tree.
     * 
     * @param dependency
     *            dependency to find
     * @param modulemapXmlDom
     *            dom-tree of modulemaps
     * @return dom-tree representing the artifact
     */
    private Xpp3Dom findOrCreateArtifact( IdeDependency dependency, Xpp3Dom modulemapXmlDom )
    {
        // first try to find it
        Xpp3Dom[] children = modulemapXmlDom.getChildren();
        for ( int index = 0; index < children.length; index++ )
        {
            if ( children[index].getAttribute( MODULEMAPS_PROJECT_NAME ).equals( dependency.getArtifactId() ) )
            {
                if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_EJB )
                    && children[index].getName().equals( MODULEMAPS_MAPPINGS )
                    && children[index].getChild( APPLICATION_XML_MODULE ).getAttribute( XMI_TYPE )
                        .equals( MODULEMAPS_APPLICATION_EJB_MODULE ) )
                {
                    return children[index];
                }
                else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_WAR )
                    && children[index].getName().equals( MODULEMAPS_MAPPINGS )
                    && children[index].getChild( APPLICATION_XML_MODULE ).getAttribute( XMI_TYPE )
                        .equals( MODULEMAPS_APPLICATION_WEB_MODULE ) )
                {
                    return children[index];
                }
                else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_JAR )
                    && children[index].getName().equals( MODULEMAPS_UTILITY_JARMAPPINGS ) )
                {
                    return children[index];
                }
                else
                {
                    modulemapXmlDom.removeChild( index );
                    break;
                }
            }
        }
        // ok, its missing (or it changed type). create a new one based on its
        // type
        long id = this.baseId++;
        if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_EJB ) )
        {
            Xpp3Dom mapping = new Xpp3Dom( MODULEMAPS_MAPPINGS );
            mapping.setAttribute( XMI_ID, "ModuleMapping_" + id );
            mapping.setAttribute( MODULEMAPS_PROJECT_NAME, dependency.getArtifactId() );
            Xpp3Dom module = new Xpp3Dom( APPLICATION_XML_MODULE );
            module.setAttribute( XMI_TYPE, MODULEMAPS_APPLICATION_EJB_MODULE );
            module.setAttribute( HREF, "META-INF/application.xml#EjbModule_" + id );
            mapping.addChild( module );
            modulemapXmlDom.addChild( mapping );
            return mapping;
        }
        else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {
            Xpp3Dom mapping = new Xpp3Dom( MODULEMAPS_MAPPINGS );
            mapping.setAttribute( XMI_ID, "ModuleMapping_" + id );
            mapping.setAttribute( MODULEMAPS_PROJECT_NAME, dependency.getArtifactId() );
            Xpp3Dom module = new Xpp3Dom( APPLICATION_XML_MODULE );
            module.setAttribute( XMI_TYPE, MODULEMAPS_APPLICATION_WEB_MODULE );
            module.setAttribute( HREF, "META-INF/application.xml#WebModule_" + id );
            mapping.addChild( module );
            modulemapXmlDom.addChild( mapping );
            return mapping;
        }
        else
        {
            Xpp3Dom utilityJARMapping = new Xpp3Dom( MODULEMAPS_UTILITY_JARMAPPINGS );
            utilityJARMapping.setAttribute( XMI_ID, "UtilityJARMapping_" + id );
            utilityJARMapping.setAttribute( MODULEMAPS_PROJECT_NAME, dependency.getArtifactId() );
            utilityJARMapping.setAttribute( URI, dependency.getArtifactId() + ".jar" );
            modulemapXmlDom.addChild( utilityJARMapping );
            return utilityJARMapping;
        }
    }

    /**
     * get the id from the href of a modulemap.
     * 
     * @param mapping
     *            the dom-tree of modulemaps
     * @return module identifier
     */
    private String getIdFromMapping( Xpp3Dom mapping )
    {
        if ( mapping.getChildCount() < 1 )
        {
            return "";
        }
        String href = mapping.getChild( 0 ).getAttribute( HREF );
        String id = href.substring( href.indexOf( '#' ) + 1 );
        return id;
    }

    /**
     * mark the domtree entry as handled (all not handled ones will be deleted).
     * 
     * @param xpp3Dom
     *            dom element to mark handled
     */
    private void handled( Xpp3Dom xpp3Dom )
    {
        for ( int index = 0; index < this.applicationXmlDomChildren.length; index++ )
        {
            if ( this.applicationXmlDomChildren[index] == xpp3Dom )
            {
                this.applicationXmlDomChildren[index] = null;
            }
        }
        for ( int index = 0; index < this.modulemapsXmlDomChildren.length; index++ )
        {
            if ( this.modulemapsXmlDomChildren[index] == xpp3Dom )
            {
                this.modulemapsXmlDomChildren[index] = null;
            }
        }
    }

    /**
     * read an xml file (application.xml or .modulemaps).
     * 
     * @param xmlFile
     *            an xmlfile
     * @return dom-tree representing the file contents
     */
    private Xpp3Dom readXMLFile( File xmlFile )
    {
        try
        {
            FileReader reader1 = new FileReader( xmlFile );
            Xpp3Dom applicationXmlDom = Xpp3DomBuilder.build( reader1 );
            return applicationXmlDom;
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            log.error( Messages.getString( "EclipsePlugin.cantreadfile", xmlFile.getAbsolutePath() ) );
            // this will trigger creating a new file
            return null;
        }
    }

    /**
     * delete all unused entries from the dom-trees.
     * 
     * @param applicationXmlDom
     *            dom-tree of application.xml
     * @param modulemapsXmlDom
     *            dom-tree of modulemaps
     */
    private void removeUnusedEntries( Xpp3Dom applicationXmlDom, Xpp3Dom modulemapsXmlDom )
    {
        for ( int index = 0; index < this.modulemapsXmlDomChildren.length; index++ )
        {
            if ( this.modulemapsXmlDomChildren[index] != null )
            {
                Xpp3Dom[] newModulemapsXmlDomChildren = modulemapsXmlDom.getChildren();
                for ( int newIndex = 0; newIndex < newModulemapsXmlDomChildren.length; newIndex++ )
                {
                    if ( ( newModulemapsXmlDomChildren[newIndex] != null )
                        && ( newModulemapsXmlDomChildren[newIndex] == this.modulemapsXmlDomChildren[index] ) )
                    {
                        modulemapsXmlDom.removeChild( newIndex );
                        break;
                    }
                }
            }
        }
        for ( int index = 0; index < this.applicationXmlDomChildren.length; index++ )
        {
            if ( this.applicationXmlDomChildren[index] != null )
            {
                Xpp3Dom[] newApplicationXmlDomChildren = applicationXmlDom.getChildren();
                for ( int newIndex = 0; newIndex < newApplicationXmlDomChildren.length; newIndex++ )
                {
                    if ( newApplicationXmlDomChildren[newIndex] == this.applicationXmlDomChildren[index] )
                    {
                        applicationXmlDom.removeChild( newIndex );
                        break;
                    }
                }
            }
        }
    }

    /**
     * update the application.xml and the .modulemaps file for a specified
     * dependency.all WAR an EJB dependencies will go in both files all others
     * only in the modulemaps files. Webapplications contextroots are corrected
     * to the contextRoot specified in the pom.
     * 
     * @param applicationXmlDom
     *            dom-tree of application.xml
     * @param modulemapXmlDom
     *            dom-tree of modulemaps
     * @param dependency
     *            the eclipse dependency to handle
     */
    private void updateApplicationXml( Xpp3Dom applicationXmlDom, Xpp3Dom modulemapXmlDom, IdeDependency dependency )
    {
        boolean isEar = Constants.PROJECT_PACKAGING_EJB.equals( dependency.getType() );
        boolean isWar = Constants.PROJECT_PACKAGING_WAR.equals( dependency.getType() );

        if ( dependency.isReferencedProject() || isEar || isWar )
        {
            Xpp3Dom mapping = findOrCreateArtifact( dependency, modulemapXmlDom );
            handled( mapping );
            if ( isEar )
            {
                Xpp3Dom module = findModuleInApplicationXml( applicationXmlDom, mapping );
                if ( module == null )
                {
                    module = new Xpp3Dom( APPLICATION_XML_MODULE );
                    module.setAttribute( ID, getIdFromMapping( mapping ) );
                    Xpp3Dom ejb = new Xpp3Dom( Constants.PROJECT_PACKAGING_EJB );
                    ejb.setValue( dependency.getArtifactId() + ".jar" );
                    module.addChild( ejb );
                    applicationXmlDom.addChild( module );
                }
                else
                {
                    handled( module );
                    module.getChild( Constants.PROJECT_PACKAGING_EJB ).setValue( dependency.getArtifactId() + ".jar" );
                }
            }
            else if ( isWar )
            {
                String contextRootInPom = getContextRootFor( dependency.getArtifactId() );
                Xpp3Dom module = findModuleInApplicationXml( applicationXmlDom, mapping );
                if ( module == null )
                {
                    module = new Xpp3Dom( APPLICATION_XML_MODULE );
                    module.setAttribute( ID, getIdFromMapping( mapping ) );
                    Xpp3Dom web = new Xpp3Dom( APPLICATION_XML_WEB );
                    Xpp3Dom webUri = new Xpp3Dom( APPLICATION_XML_WEB_URI );
                    webUri.setValue( dependency.getArtifactId() + ".war" );
                    Xpp3Dom contextRoot = new Xpp3Dom( APPLICATION_XML_CONTEXT_ROOT );
                    contextRoot.setValue( contextRootInPom );
                    web.addChild( webUri );
                    web.addChild( contextRoot );
                    module.addChild( web );
                    applicationXmlDom.addChild( module );
                }
                else
                {
                    handled( module );
                    module.getChild( APPLICATION_XML_WEB ).getChild( APPLICATION_XML_WEB_URI )
                        .setValue( dependency.getArtifactId() + ".war" );
                    module.getChild( APPLICATION_XML_WEB ).getChild( APPLICATION_XML_CONTEXT_ROOT )
                        .setValue( contextRootInPom );
                }
            }
        }
    }

    /**
     * Find the contextRoot specified in the pom and convert it into contectroot
     * for the application.xml.
     * 
     * @param artifactId
     *            the artifactid to search
     * @return string with the context root
     */
    private String getContextRootFor( String artifactId )
    {
        for ( int index = 0; index < webModulesFromPoms.length; index++ )
        {
            if ( webModulesFromPoms[index].getChild( "artifactId" ).getValue().equals( artifactId ) )
                return new File( webModulesFromPoms[index].getChild( "contextRoot" ).getValue() ).getName();
        }
        return artifactId;
    }

    /**
     * write back a domtree to a xmlfile and use the pretty print for it so that
     * it is human readable.
     * 
     * @param xmlFile
     *            file to write to
     * @param xmlDomTree
     *            dom-tree to write
     * @throws MojoExecutionException
     *             if the file could not be written
     */
    private void writePrettyXmlFile( File xmlFile, Xpp3Dom xmlDomTree )
        throws MojoExecutionException
    {
        Xpp3Dom original = readXMLFile( xmlFile );
        if ( original != null && original.equals( xmlDomTree ) )
        {
            log.info( Messages.getString( "EclipseCleanMojo.unchanged", xmlFile.getAbsolutePath() ) );
            return;
        }
        FileWriter w = null;
        xmlFile.getParentFile().mkdirs();
        try
        {
            w = new FileWriter( xmlFile );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "Rad6Plugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }
        XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
        Xpp3DomWriter.write( writer, xmlDomTree );
        IOUtil.close( w );
    }
}
