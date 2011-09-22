package org.apache.maven.plugin.eclipse.writers.wtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

/**
 * This writer creates the application.xml and the .modulemaps files for RAD6 the the META-INF directory in the project
 * root. this is where RAD6 requires the files to be. These will be independent of the real application.xml witch will
 * be generated the stad. maven way.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 */
public class EclipseWtpApplicationXMLWriter
    extends AbstractWtpResourceWriter
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

    private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation";

    private static final String XMLNS_XMI = "xmlns:xmi";

    private static final String XMLNS_XSI = "xmlns:xsi";

    private Xpp3Dom[] applicationXmlDomChildren;

    private Xpp3Dom[] modulemapsXmlDomChildren;

    private Xpp3Dom[] webModulesFromPoms;

    /**
     * write the application.xml and the .modulemaps file to the META-INF directory.
     * 
     * @see AbstractWtpResourceWriter#write(EclipseSourceDir[], ArtifactRepository, File)
     * @throws MojoExecutionException when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        String packaging = this.config.getProject().getPackaging();
        if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            File applicationXmlFile =
                new File( this.config.getEclipseProjectDirectory(), "target" + File.separator + "eclipseEar"
                    + File.separator + "META-INF" + File.separator
                    + EclipseWtpApplicationXMLWriter.APPLICATION_XML_FILENAME );
            // create the directory structiure for eclipse deployment
            applicationXmlFile.getParentFile().mkdirs();
            // copy all deployment files to the eclipse deployment
            copyApplicationFiles();
            // delete any existing application.xml so that it will be
            // overwritten.
            applicationXmlFile.delete();

            Xpp3Dom applicationXmlDom = readXMLFile( applicationXmlFile );
            if ( applicationXmlDom == null )
            {
                applicationXmlDom = createNewApplicationXml();
            }
            this.applicationXmlDomChildren =
                applicationXmlDom.getChildren( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE );

            File modulemapsXmlFile =
                new File( this.config.getEclipseProjectDirectory(), "target" + File.separator + "eclipseEar"
                    + File.separator + "META-INF" + File.separator + EclipseWtpApplicationXMLWriter.MODULEMAPS_FILENAME );
            Xpp3Dom modulemapsXmlDom = readXMLFile( modulemapsXmlFile );
            if ( modulemapsXmlDom == null )
            {
                modulemapsXmlDom = createNewModulemaps();
            }
            this.modulemapsXmlDomChildren = modulemapsXmlDom.getChildren();

            this.webModulesFromPoms =
                IdeUtils.getPluginConfigurationDom( config.getProject(), JeeUtils.ARTIFACT_MAVEN_EAR_PLUGIN,
                                                    new String[] { "modules", "webModule" } );

            IdeDependency[] deps = this.config.getDeps();
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
     * Copy all files from application directory to the target eclipseEar directory.
     * 
     * @throws MojoExecutionException wenn an error occures during file copieing
     */
    private void copyApplicationFiles()
        throws MojoExecutionException
    {
        try
        {
            File applicationDirectory =
                new File( this.config.getEclipseProjectDirectory(), "src" + File.separator + "main" + File.separator
                    + "application" );
            File eclipseApplicationDirectory =
                new File( this.config.getEclipseProjectDirectory(), "target" + File.separator + "eclipseEar" );
            copyDirectoryStructure( applicationDirectory, eclipseApplicationDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "could not copy files the the eclipseEar directory", e );
        }
    }

    /**
     * Copies a entire directory structure without scm files. Note:
     * <ul>
     * <li>It will include empty directories.
     * <li>The <code>sourceDirectory</code> must exists.
     * </ul>
     * 
     * @param sourceDirectory
     * @param destinationDirectory
     * @throws IOException
     */
    public static void copyDirectoryStructure( File sourceDirectory, File destinationDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            return;
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destinationDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() && !file.getName().equals( ".svn" ) && !file.getName().equals( "CVS" ) )
            {
                if ( !destination.exists() && !destination.mkdirs() )
                {
                    throw new IOException( "Could not create destination directory '" + destination.getAbsolutePath()
                        + "'." );
                }

                copyDirectoryStructure( file, destination );
            }
        }
    }

    /**
     * there is no existing application.xml file so create a new one.
     * 
     * @return the domtree representing the contents of application.xml
     */
    private Xpp3Dom createNewApplicationXml()
    {
        Xpp3Dom result = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_APPLICATION );
        result.setAttribute( EclipseWtpApplicationXMLWriter.ID, "Application_ID" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.VERSION, "1.4" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMLNS, "http://java.sun.com/xml/ns/j2ee" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMLNS_XSI, "http://www.w3.org/2001/XMLSchema-instance" );

        // special case for development websphere's ....
        String locationAttribute;
        if ( this.config.getWorkspaceConfiguration().getWebsphereVersion() != null )
        {
            locationAttribute = EclipseWtpApplicationXMLWriter.XSI_SCHEMA_LOCATION;
        }
        else
        {
            locationAttribute = EclipseWtpApplicationXMLWriter.XMLNS_SCHEMA_LOCATION;
        }
        result.setAttribute( locationAttribute,
                             "http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd" );
        result.addChild( new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_DESCRIPTION ) );
        Xpp3Dom name = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_DISPLAY_NAME );
        name.setValue( this.config.getEclipseProjectName() );
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
        Xpp3Dom result = new Xpp3Dom( EclipseWtpApplicationXMLWriter.MODULEMAP_EARPROJECT_MAP );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMI_VERSION, "2.0" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMLNS_XMI, "http://www.omg.org/XMI" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMLNS_APPLICATION, "application.xmi" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMLNS_MODULEMAP, "modulemap.xmi" );
        result.setAttribute( EclipseWtpApplicationXMLWriter.XMI_ID, "EARProjectMap_" + System.identityHashCode( this ) );
        return result;
    }

    /**
     * find an existing module entry in the application.xml file by looking up the id in the modulemaps file and then
     * using that to locate the entry in the application.xml file.
     * 
     * @param applicationXmlDom application.xml dom tree
     * @param mapping .modulemaps dom tree
     * @return dom tree representing the module
     */
    private Xpp3Dom findModuleInApplicationXml( Xpp3Dom applicationXmlDom, Xpp3Dom mapping )
    {
        String id = getIdFromMapping( mapping );
        Xpp3Dom[] children = applicationXmlDom.getChildren();
        for ( int index = 0; index < children.length; index++ )
        {
            String childId = children[index].getAttribute( EclipseWtpApplicationXMLWriter.ID );
            if ( childId != null && childId.equals( id ) )
            {
                return children[index];
            }
        }
        return null;
    }

    /**
     * find an artifact in the modulemaps dom tree, if it is missing create a new entry in the modulemaps dom tree.
     * 
     * @param dependency dependency to find
     * @param modulemapXmlDom dom-tree of modulemaps
     * @return dom-tree representing the artifact
     */
    private Xpp3Dom findOrCreateArtifact( IdeDependency dependency, Xpp3Dom modulemapXmlDom )
    {
        // first try to find it
        Xpp3Dom[] children = modulemapXmlDom.getChildren();
        for ( int index = 0; index < children.length; index++ )
        {
            if ( children[index].getAttribute( EclipseWtpApplicationXMLWriter.MODULEMAPS_PROJECT_NAME ).equals(
                                                                                                                dependency.getEclipseProjectName() ) )
            {
                if ( ( dependency.getType().equals( Constants.PROJECT_PACKAGING_EJB ) || dependency.getType().equals(
                                                                                                                      "ejb3" ) )
                    && children[index].getName().equals( EclipseWtpApplicationXMLWriter.MODULEMAPS_MAPPINGS )
                    && children[index].getChild( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE ).getAttribute(
                                                                                                                       EclipseWtpApplicationXMLWriter.XMI_TYPE ).equals(
                                                                                                                                                                         EclipseWtpApplicationXMLWriter.MODULEMAPS_APPLICATION_EJB_MODULE ) )
                {
                    return children[index];
                }
                else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_WAR )
                    && children[index].getName().equals( EclipseWtpApplicationXMLWriter.MODULEMAPS_MAPPINGS )
                    && children[index].getChild( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE ).getAttribute(
                                                                                                                       EclipseWtpApplicationXMLWriter.XMI_TYPE ).equals(
                                                                                                                                                                         EclipseWtpApplicationXMLWriter.MODULEMAPS_APPLICATION_WEB_MODULE ) )
                {
                    return children[index];
                }
                else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_JAR )
                    && children[index].getName().equals( EclipseWtpApplicationXMLWriter.MODULEMAPS_UTILITY_JARMAPPINGS ) )
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
        long id = System.identityHashCode( dependency );
        if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_EJB ) || dependency.getType().equals( "ejb3" ) )
        {
            Xpp3Dom mapping = new Xpp3Dom( EclipseWtpApplicationXMLWriter.MODULEMAPS_MAPPINGS );
            mapping.setAttribute( EclipseWtpApplicationXMLWriter.XMI_ID, "ModuleMapping_" + id );
            mapping.setAttribute( EclipseWtpApplicationXMLWriter.MODULEMAPS_PROJECT_NAME,
                                  dependency.getEclipseProjectName() );
            Xpp3Dom module = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE );
            module.setAttribute( EclipseWtpApplicationXMLWriter.XMI_TYPE,
                                 EclipseWtpApplicationXMLWriter.MODULEMAPS_APPLICATION_EJB_MODULE );
            module.setAttribute( EclipseWtpApplicationXMLWriter.HREF, "META-INF/application.xml#EjbModule_" + id );
            mapping.addChild( module );
            modulemapXmlDom.addChild( mapping );
            return mapping;
        }
        else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {
            Xpp3Dom mapping = new Xpp3Dom( EclipseWtpApplicationXMLWriter.MODULEMAPS_MAPPINGS );
            mapping.setAttribute( EclipseWtpApplicationXMLWriter.XMI_ID, "ModuleMapping_" + id );
            mapping.setAttribute( EclipseWtpApplicationXMLWriter.MODULEMAPS_PROJECT_NAME,
                                  dependency.getEclipseProjectName() );
            Xpp3Dom module = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE );
            module.setAttribute( EclipseWtpApplicationXMLWriter.XMI_TYPE,
                                 EclipseWtpApplicationXMLWriter.MODULEMAPS_APPLICATION_WEB_MODULE );
            module.setAttribute( EclipseWtpApplicationXMLWriter.HREF, "META-INF/application.xml#WebModule_" + id );
            mapping.addChild( module );
            modulemapXmlDom.addChild( mapping );
            return mapping;
        }
        else
        {
            Xpp3Dom utilityJARMapping = new Xpp3Dom( EclipseWtpApplicationXMLWriter.MODULEMAPS_UTILITY_JARMAPPINGS );
            utilityJARMapping.setAttribute( EclipseWtpApplicationXMLWriter.XMI_ID, "UtilityJARMapping_" + id );
            utilityJARMapping.setAttribute( EclipseWtpApplicationXMLWriter.MODULEMAPS_PROJECT_NAME,
                                            dependency.getEclipseProjectName() );
            utilityJARMapping.setAttribute( EclipseWtpApplicationXMLWriter.URI, dependency.getEclipseProjectName()
                + ".jar" );
            modulemapXmlDom.addChild( utilityJARMapping );
            return utilityJARMapping;
        }
    }

    /**
     * get the id from the href of a modulemap.
     * 
     * @param mapping the dom-tree of modulemaps
     * @return module identifier
     */
    private String getIdFromMapping( Xpp3Dom mapping )
    {
        if ( mapping.getChildCount() < 1 )
        {
            return "";
        }
        String href = mapping.getChild( 0 ).getAttribute( EclipseWtpApplicationXMLWriter.HREF );
        String id = href.substring( href.indexOf( '#' ) + 1 );
        return id;
    }

    /**
     * read an xml file (application.xml or .modulemaps).
     * 
     * @param xmlFile an xmlfile
     * @return dom-tree representing the file contents
     */
    private Xpp3Dom readXMLFile( File xmlFile )
    {
        try
        {
            Reader reader = new InputStreamReader( new FileInputStream( xmlFile ), "UTF-8" );
            Xpp3Dom applicationXmlDom = Xpp3DomBuilder.build( reader );
            return applicationXmlDom;
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
        catch ( Exception e )
        {
            this.log.error( "cantreadfile" + xmlFile.getAbsolutePath() );
            // this will trigger creating a new file
            return null;
        }
    }

    /**
     * mark the domtree entry as handled (all not handled ones will be deleted).
     * 
     * @param xpp3Dom dom element to mark handled
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
     * delete all unused entries from the dom-trees.
     * 
     * @param applicationXmlDom dom-tree of application.xml
     * @param modulemapsXmlDom dom-tree of modulemaps
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
                    if ( newModulemapsXmlDomChildren[newIndex] == this.modulemapsXmlDomChildren[index] )
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
     * update the application.xml and the .modulemaps file for a specified dependency.all WAR an EJB dependencies will
     * go in both files all others only in the modulemaps files. Webapplications contextroots are corrected to the
     * contextRoot specified in the pom.
     * 
     * @param applicationXmlDom dom-tree of application.xml
     * @param modulemapXmlDom dom-tree of modulemaps
     * @param dependency the eclipse dependency to handle
     */
    private void updateApplicationXml( Xpp3Dom applicationXmlDom, Xpp3Dom modulemapXmlDom, IdeDependency dependency )
    {
        if ( dependency.isTestDependency() || dependency.isProvided()
            || dependency.isSystemScopedOutsideProject( this.config.getProject() ) )
        {
            return;
        }
        Xpp3Dom mapping = findOrCreateArtifact( dependency, modulemapXmlDom );
        handled( mapping );
        if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_EJB ) || dependency.getType().equals( "ejb3" ) )
        {
            Xpp3Dom module = findModuleInApplicationXml( applicationXmlDom, mapping );
            if ( module == null )
            {
                module = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE );
                module.setAttribute( EclipseWtpApplicationXMLWriter.ID, getIdFromMapping( mapping ) );
                Xpp3Dom ejb = new Xpp3Dom( "ejb" );
                ejb.setValue( dependency.getEclipseProjectName() + ".jar" );
                module.addChild( ejb );
                applicationXmlDom.addChild( module );
            }
            else
            {
                handled( module );
                module.getChild( "ejb" ).setValue( dependency.getEclipseProjectName() + ".jar" );
            }
        }
        else if ( dependency.getType().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {
            String contextRootInPom = getContextRootFor( dependency );
            Xpp3Dom module = findModuleInApplicationXml( applicationXmlDom, mapping );
            if ( module == null )
            {
                module = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_MODULE );
                module.setAttribute( EclipseWtpApplicationXMLWriter.ID, getIdFromMapping( mapping ) );
                Xpp3Dom web = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_WEB );
                Xpp3Dom webUri = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_WEB_URI );
                webUri.setValue( dependency.getEclipseProjectName() + ".war" );
                Xpp3Dom contextRoot = new Xpp3Dom( EclipseWtpApplicationXMLWriter.APPLICATION_XML_CONTEXT_ROOT );
                contextRoot.setValue( contextRootInPom );
                web.addChild( webUri );
                web.addChild( contextRoot );
                module.addChild( web );
                applicationXmlDom.addChild( module );
            }
            else
            {
                handled( module );
                module.getChild( EclipseWtpApplicationXMLWriter.APPLICATION_XML_WEB ).getChild(
                                                                                                EclipseWtpApplicationXMLWriter.APPLICATION_XML_WEB_URI ).setValue(
                                                                                                                                                                   dependency.getEclipseProjectName()
                                                                                                                                                                       + ".war" );
                module.getChild( EclipseWtpApplicationXMLWriter.APPLICATION_XML_WEB ).getChild(
                                                                                                EclipseWtpApplicationXMLWriter.APPLICATION_XML_CONTEXT_ROOT ).setValue(
                                                                                                                                                                        contextRootInPom );
            }
        }
    }

    /**
     * Find the contextRoot specified in the pom and convert it into contectroot for the application.xml.
     * 
     * @param dependency the artifact to search
     * @return string with the context root
     */
    private String getContextRootFor( IdeDependency dependency )
    {
        String artifactId = dependency.getArtifactId();
        String groupId = dependency.getGroupId();
        for ( int index = 0; index < this.webModulesFromPoms.length; index++ )
        {
            Xpp3Dom webGroupId = this.webModulesFromPoms[index].getChild( "groupId" );
            Xpp3Dom webArtifactId = this.webModulesFromPoms[index].getChild( "artifactId" );
            Xpp3Dom webContextRoot = this.webModulesFromPoms[index].getChild( "contextRoot" );

            if ( webContextRoot != null && webArtifactId != null && webArtifactId.getValue().equals( artifactId )
                && webGroupId != null && webGroupId.getValue().equals( groupId ) )
            {
                return webContextRoot.getValue();
            }
        }
        // no configuration found back to maven-ear-plugin default
        return dependency.getArtifactId();
    }

    /**
     * write back a domtree to a xmlfile and use the pretty print for it so that it is human readable.
     * 
     * @param xmlFile file to write to
     * @param xmlDomTree dom-tree to write
     * @throws MojoExecutionException if the file could not be written
     */
    private void writePrettyXmlFile( File xmlFile, Xpp3Dom xmlDomTree )
        throws MojoExecutionException
    {
        Xpp3Dom original = readXMLFile( xmlFile );
        if ( original != null && original.equals( xmlDomTree ) )
        {
            this.log.info( "Rad6CleanMojo.unchanged" + xmlFile.getAbsolutePath() );
            return;
        }
        Writer w = null;
        xmlFile.getParentFile().mkdirs();
        try
        {
            w = new OutputStreamWriter( new FileOutputStream( xmlFile ), "UTF-8" );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( "Rad6Plugin.erroropeningfile", ex ); //$NON-NLS-1$
        }
        XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
        Xpp3DomWriter.write( writer, xmlDomTree );
        IOUtil.close( w );
    }

}
