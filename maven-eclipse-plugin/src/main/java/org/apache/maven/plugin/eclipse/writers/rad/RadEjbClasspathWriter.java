package org.apache.maven.plugin.eclipse.writers.rad;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.eclipse.writers.AbstractWtpResourceWriter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Adapts the .classpath file for RAD6 for now write hardcoded:
 * target/websphere/classes future releases could make this varriable.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class RadEjbClasspathWriter
    extends AbstractEclipseWriter
{

    private static final String CLASSPATH = "classpath";

    private static final String CLASSPATH_FILE = ".classpath";

    private static final String CLASSPATHENTRY = "classpathentry";

    private static final String CON = "con";

    private static final String KIND = "kind";

    private static final String LIB = "lib";

    private static final String OUTPUT = "output";

    private static final String PATH = "path";

    private static final String SRC = "src";

    private static final String TARGET_WEBSPHERE_CLASSES = "target/websphere/generated-classes";

    private static final String VAR = "var";

    private static final String WEBSPHERE6CONTAIGNER = "com.ibm.wtp.server.java.core.container/com.ibm.ws.ast.st.runtime.core.runtimeTarget.v60/was.base.v6";

    /**
     * write the .classpath file to the project root directory.
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
        if ( Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging ) )
        {
            new File( config.getEclipseProjectDirectory(), TARGET_WEBSPHERE_CLASSES ).mkdirs();
            File classpathFile = new File( config.getEclipseProjectDirectory(), CLASSPATH_FILE );
            
            if ( !classpathFile.exists() )
            {
                return;
            }
            FileWriter w;
            Xpp3Dom classpath = readXMLFile( classpathFile );
            Xpp3Dom[] children = classpath.getChildren();
            for ( int index = 0; index < children.length; index++ )
            {
                if ( LIB.equals( children[index].getAttribute( KIND ) )
                    && TARGET_WEBSPHERE_CLASSES.equals( children[index].getAttribute( "path" ) ) )
                {
                    return; // nothing to do!
                }
            }

            Xpp3Dom newEntry = new Xpp3Dom( CLASSPATHENTRY );
            newEntry.setAttribute( KIND, LIB );
            newEntry.setAttribute( PATH, TARGET_WEBSPHERE_CLASSES );
            classpath.addChild( newEntry );

            newEntry = new Xpp3Dom( CLASSPATHENTRY );
            newEntry.setAttribute( KIND, CON );
            newEntry.setAttribute( PATH, WEBSPHERE6CONTAIGNER );
            classpath.addChild( newEntry );

            children = classpath.getChildren();
            for ( int index = children.length - 1; index >= 0; index-- )
            {
                if ( children[index].getValue() == null )
                {
                    children[index].setValue( "" );
                }
            }

            removeDupicateWAS6Libs( classpath );
            classpath = orderClasspath( classpath );

            try
            {
                w = new FileWriter( classpathFile );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
            }
            XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
            Xpp3DomWriter.write( writer, classpath );
            IOUtil.close( w );
        }
    }

    /**
     * determinate of witch type this classpath entry is. this is used for
     * sorting them.
     * 
     * @param classpathentry
     *            the classpath entry to sort
     * @return an integer identifieing the type
     * @see RadEjbClasspathWriter#orderClasspath(Xpp3Dom)
     */
    private int detectClasspathEntryType( Xpp3Dom classpathentry )
    {
        String kind = classpathentry.getAttribute( KIND );
        String path = classpathentry.getAttribute( PATH );

        if ( kind == null || path == null )
        {
            return 6;
        }

        boolean absolutePath = path.startsWith( "\\" ) || path.startsWith( "/" );
        boolean windowsAbsolutePath = path.indexOf( ':' ) >= 0;
        boolean anyAbsolutePath = absolutePath || windowsAbsolutePath;

        if ( kind.equals( SRC ) && !absolutePath )
        {
            return 1;
        }
        else if ( kind.equals( LIB ) && !anyAbsolutePath )
        {
            return 2;
        }
        else if ( kind.equals( SRC ) )
        {
            return 3;
        }
        else if ( kind.equals( VAR ) )
        {
            return 4;
        }
        else if ( kind.equals( LIB ) )
        {
            return 5;
        }
        else if ( kind.equals( OUTPUT ) )
        {
            return 7;
        }
        else
        {
            return 6;
        }
    }

    /**
     * Order of classpath this is nessesary for the ejb's the generated classes
     * are elsewise not found.
     * 
     * 1 - kind=src ohne starting '/' oder '\' 2 - kind=lib kein ':' und kein
     * start mit '/' oder '\' 3 - kind=src mit ohne starting '/' oder '\' 4 -
     * kind=var 5 - kind=lib ein ':' oder start mit '/' oder '\' 6 - rest 7 -
     * kind=output
     * 
     * @param classpath
     *            the classpath to sort
     * @return dom-tree representing ordered classpath
     */
    private Xpp3Dom orderClasspath( Xpp3Dom classpath )
    {
        Xpp3Dom[] children = classpath.getChildren();
        Arrays.sort( children, new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                return detectClasspathEntryType( (Xpp3Dom) o1 ) - detectClasspathEntryType( (Xpp3Dom) o2 );
            }
        } );
        Xpp3Dom resultClasspath = new Xpp3Dom( CLASSPATH );
        for ( int index = 0; index < children.length; index++ )
        {
            resultClasspath.addChild( children[index] );
        }
        return resultClasspath;
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
     * Losche alle pfade die nach was6 zeigen diese sind erkennbar an den
     * parrent runtimes/base_v6/lib.
     * 
     * @param classpath
     *            classpath to remove was6 libraries
     */
    private void removeDupicateWAS6Libs( Xpp3Dom classpath )
    {
        Xpp3Dom[] children;
        children = classpath.getChildren();
        for ( int index = children.length - 1; index >= 0; index-- )
        {
            try
            {
                File path = new File( children[index].getAttribute( PATH ) );

                if ( path.exists() && path.getParentFile().getName().equals( LIB )
                    && path.getParentFile().getParentFile().getName().equals( "base_v6" )
                    && path.getParentFile().getParentFile().getParentFile().getName().equals( "runtimes" ) )
                {
                    Xpp3Dom[] currentChildren = classpath.getChildren();
                    for ( int deleteIndex = currentChildren.length - 1; deleteIndex >= 0; deleteIndex-- )
                    {
                        if ( currentChildren[deleteIndex] == children[index] )
                        {
                            classpath.removeChild( deleteIndex );
                            break;
                        }
                    }
                }
            }
            catch ( Exception e )
            {
                log.debug( e );
            }
        }
    }

}
