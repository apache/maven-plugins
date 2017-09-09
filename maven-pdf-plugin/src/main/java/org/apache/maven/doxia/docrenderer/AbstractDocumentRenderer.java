package org.apache.maven.doxia.docrenderer;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.Doxia;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Reader;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.parser.manager.ParserNotFoundException;
import org.apache.maven.doxia.logging.PlexusLoggerWrapper;
import org.apache.maven.doxia.parser.module.ParserModule;
import org.apache.maven.doxia.parser.module.ParserModuleManager;
import org.apache.maven.doxia.util.XmlValidator;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.velocity.SiteResourceLoader;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 * Abstract <code>document</code> renderer.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author ltheussl
 * @version $Id: AbstractDocumentRenderer.java 1726406 2016-01-23 15:06:45Z hboutemy $
 * @since 1.1
 */
public abstract class AbstractDocumentRenderer
    extends AbstractLogEnabled
    implements DocumentRenderer
{
    @Requirement
    protected ParserModuleManager parserModuleManager;

    @Requirement
    protected Doxia doxia;

    @Requirement
    private VelocityComponent velocity;

    /**
     * The common base directory of source files.
     */
    private String baseDir;

      //--------------------------------------------
     //
    //--------------------------------------------

    /**
     * Render an aggregate document from the files found in a Map.
     *
     * @param filesToProcess the Map of Files to process. The Map should contain as keys the paths of the
     *      source files (relative to {@link #getBaseDir() baseDir}), and the corresponding ParserModule as values.
     * @param outputDirectory the output directory where the aggregate document should be generated.
     * @param documentModel the document model, containing all the metadata, etc.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     * @deprecated since 1.1.2, use {@link #render(Map, File, DocumentModel, DocumentRendererContext)}
     */
    public abstract void render( Map<String, ParserModule> filesToProcess, File outputDirectory,
                                 DocumentModel documentModel )
        throws DocumentRendererException, IOException;

      //--------------------------------------------
     //
    //--------------------------------------------

    /** {@inheritDoc} */
    public void render( Collection<String> files, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException
    {
        render( getFilesToProcess( files ), outputDirectory, documentModel, null );
    }

    /** {@inheritDoc} */
    public void render( File baseDirectory, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException
    {
        render( baseDirectory, outputDirectory, documentModel, null );
    }

    /**
     * Render an aggregate document from the files found in a Map.
     *
     * @param filesToProcess the Map of Files to process. The Map should contain as keys the paths of the
     *      source files (relative to {@link #getBaseDir() baseDir}), and the corresponding ParserModule as values.
     * @param outputDirectory the output directory where the aggregate document should be generated.
     * @param documentModel the document model, containing all the metadata, etc.
     * @param context the rendering context when processing files.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     */
    public void render( Map<String, ParserModule> filesToProcess, File outputDirectory, DocumentModel documentModel,
                        DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        // nop
    }

    /**
     * Render a document from the files found in a source directory, depending on a rendering context.
     *
     * @param baseDirectory the directory containing the source files.
     *              This should follow the standard Maven convention, ie containing all the site modules.
     * @param outputDirectory the output directory where the document should be generated.
     * @param documentModel the document model, containing all the metadata, etc.
     *              If the model contains a TOC, only the files found in this TOC are rendered,
     *              otherwise all files found under baseDirectory will be processed.
     *              If the model is null, render all files from baseDirectory individually.
     * @param context the rendering context when processing files.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     * @since 1.1.2
     */
    public void render( File baseDirectory, File outputDirectory, DocumentModel documentModel,
                        DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        render( getFilesToProcess( baseDirectory ), outputDirectory, documentModel, context );
    }

    /**
     * Render a document from the files found in baseDirectory. This just forwards to
     *              {@link #render(File,File,DocumentModel)} with a new DocumentModel.
     *
     * @param baseDirectory the directory containing the source files.
     *              This should follow the standard Maven convention, ie containing all the site modules.
     * @param outputDirectory the output directory where the document should be generated.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     * @see #render(File, File, DocumentModel)
     */
    public void render( File baseDirectory, File outputDirectory )
        throws DocumentRendererException, IOException
    {
        render( baseDirectory, outputDirectory, (DocumentModel) null );
    }

    /**
     * Render a document from the files found in baseDirectory.
     *
     * @param baseDirectory the directory containing the source files.
     *              This should follow the standard Maven convention, ie containing all the site modules.
     * @param outputDirectory the output directory where the document should be generated.
     * @param documentDescriptor a file containing the document model.
     *              If this file does not exist or is null, some default settings will be used.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     * @see #render(File, File) if documentDescriptor does not exist or is null
     * @see #render(Map, File, DocumentModel) otherwise
     */
    public void render( File baseDirectory, File outputDirectory, File documentDescriptor )
        throws DocumentRendererException, IOException
    {
        if ( ( documentDescriptor == null ) || ( !documentDescriptor.exists() ) )
        {
            getLogger().warn( "No documentDescriptor found: using default settings!" );

            render( baseDirectory, outputDirectory );
        }
        else
        {
            render( getFilesToProcess( baseDirectory ), outputDirectory, readDocumentModel( documentDescriptor ),
                    null );
        }
    }

    /**
     * Render documents separately for each file found in a Map.
     *
     * @param filesToProcess the Map of Files to process. The Map should contain as keys the paths of the
     *      source files (relative to {@link #getBaseDir() baseDir}), and the corresponding ParserModule as values.
     * @param outputDirectory the output directory where the documents should be generated.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     * @since 1.1.1
     * @deprecated since 1.1.2, use {@link #renderIndividual(Map, File, DocumentRendererContext)}
     */
    public void renderIndividual( Map<String, ParserModule> filesToProcess, File outputDirectory )
        throws DocumentRendererException, IOException
    {
        // nop
    }

    /**
     * Render documents separately for each file found in a Map.
     *
     * @param filesToProcess the Map of Files to process. The Map should contain as keys the paths of the
     *      source files (relative to {@link #getBaseDir() baseDir}), and the corresponding ParserModule as values.
     * @param outputDirectory the output directory where the documents should be generated.
     * @param context the rendering context.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     * @since 1.1.2
     */
    public void renderIndividual( Map<String, ParserModule> filesToProcess, File outputDirectory,
                                  DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        // nop
    }

    /**
     * Returns a Map of files to process. The Map contains as keys the paths of the source files
     *      (relative to {@link #getBaseDir() baseDir}), and the corresponding ParserModule as values.
     *
     * @param baseDirectory the directory containing the source files.
     *              This should follow the standard Maven convention, ie containing all the site modules.
     * @return a Map of files to process.
     * @throws java.io.IOException in case of a problem reading the files under baseDirectory.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     */
    public Map<String, ParserModule> getFilesToProcess( File baseDirectory )
        throws IOException, DocumentRendererException
    {
        if ( !baseDirectory.isDirectory() )
        {
            getLogger().warn( "No files found to process!" );

            return new HashMap<String, ParserModule>();
        }

        setBaseDir( baseDirectory.getAbsolutePath() );

        Map<String, ParserModule> filesToProcess = new LinkedHashMap<String, ParserModule>();
        Map<String, String> duplicatesFiles = new LinkedHashMap<String, String>();

        Collection<ParserModule> modules = parserModuleManager.getParserModules();
        for ( ParserModule module : modules )
        {
            File moduleBasedir = new File( baseDirectory, module.getSourceDirectory() );

            if ( moduleBasedir.exists() )
            {
                // TODO: handle in/excludes
                List<String> allFiles = FileUtils.getFileNames( moduleBasedir, "**/*.*", null, false );

                String[] extensions = getExtensions( module );
                List<String> docs = new LinkedList<String>( allFiles );
                // Take care of extension case
                for ( Iterator<String> it = docs.iterator(); it.hasNext(); )
                {
                    String name = it.next().trim();

                    if ( !endsWithIgnoreCase( name, extensions ) )
                    {
                        it.remove();
                    }
                }

                String[] vmExtensions = new String[extensions.length];
                for ( int i = 0; i < extensions.length; i++ )
                {
                    vmExtensions[i] = extensions[i] + ".vm";
                }
                List<String> velocityFiles = new LinkedList<String>( allFiles );
                // *.xml.vm
                for ( Iterator<String> it = velocityFiles.iterator(); it.hasNext(); )
                {
                    String name = it.next().trim();

                    if ( !endsWithIgnoreCase( name, vmExtensions ) )
                    {
                        it.remove();
                    }
                }
                docs.addAll( velocityFiles );

                for ( String filePath : docs )
                {
                    filePath = filePath.trim();

                    if ( filePath.lastIndexOf( '.' ) > 0 )
                    {
                        String key = filePath.substring( 0, filePath.lastIndexOf( '.' ) );

                        if ( duplicatesFiles.containsKey( key ) )
                        {
                            throw new DocumentRendererException( "Files '" + module.getSourceDirectory()
                                + File.separator + filePath + "' clashes with existing '"
                                + duplicatesFiles.get( key ) + "'." );
                        }

                        duplicatesFiles.put( key, module.getSourceDirectory() + File.separator + filePath );
                    }

                    filesToProcess.put( filePath, module );
                }
            }
        }

        return filesToProcess;
    }

    protected static String[] getExtensions( ParserModule module )
    {
        String[] extensions = new String[module.getExtensions().length];
        for ( int i = module.getExtensions().length - 1; i >= 0; i-- )
        {
            extensions[i] = '.' + module.getExtensions()[i];
        }
        return extensions;
    }

    // TODO replace with StringUtils.endsWithIgnoreCase() from maven-shared-utils 0.7
    protected static boolean endsWithIgnoreCase( String str, String searchStr )
    {
        if ( str.length() < searchStr.length() )
        {
            return false;
        }

        return str.regionMatches( true, str.length() - searchStr.length(), searchStr, 0, searchStr.length() );
    }

    protected static boolean endsWithIgnoreCase( String str, String[] searchStrs )
    {
        for ( String searchStr : searchStrs )
        {
            if ( endsWithIgnoreCase( str, searchStr ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a Map of files to process. The Map contains as keys the paths of the source files
     *      (relative to {@link #getBaseDir() baseDir}), and the corresponding ParserModule as values.
     *
     * @param files The Collection of source files.
     * @return a Map of files to process.
     */
    public Map<String, ParserModule> getFilesToProcess( Collection<String> files )
    {
        // ----------------------------------------------------------------------
        // Map all the file names to parser ids
        // ----------------------------------------------------------------------

        Map<String, ParserModule> filesToProcess = new HashMap<String, ParserModule>();

        Collection<ParserModule> modules = parserModuleManager.getParserModules();
        for ( ParserModule module : modules )
        {
            String[] extensions = getExtensions( module );

            String sourceDirectory = File.separator + module.getSourceDirectory() + File.separator;

            for ( String file : files )
            {
                // first check if the file path contains one of the recognized source dir identifiers
                // (there's trouble if a pathname contains 2 identifiers), then match file extensions (not unique).

                if ( file.indexOf( sourceDirectory ) != -1 )
                {
                    filesToProcess.put( file, module );
                }
                else
                {
                    // don't overwrite if it's there already
                    if ( endsWithIgnoreCase( file, extensions ) && !filesToProcess.containsKey( file ) )
                    {
                        filesToProcess.put( file, module );
                    }
                }
            }
        }

        return filesToProcess;
    }

    /** {@inheritDoc} */
    public DocumentModel readDocumentModel( File documentDescriptor )
        throws DocumentRendererException, IOException
    {
        DocumentModel documentModel;

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( documentDescriptor );
            documentModel = new DocumentXpp3Reader().read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new DocumentRendererException( "Error parsing document descriptor", e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return documentModel;
    }

    /**
     * Sets the current base directory.
     *
     * @param newDir the absolute path to the base directory to set.
     */
    public void setBaseDir( String newDir )
    {
        this.baseDir = newDir;
    }

    /**
     * Return the current base directory.
     *
     * @return the current base directory.
     */
    public String getBaseDir()
    {
        return this.baseDir;
    }

      //--------------------------------------------
     //
    //--------------------------------------------

    /**
     * Parse a source document into a sink.
     *
     * @param fullDocPath absolute path to the source document.
     * @param parserId determines the parser to use.
     * @param sink the sink to receive the events.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException in case of a parsing error.
     * @throws java.io.IOException if the source document cannot be opened.
     * @deprecated since 1.1.2, use {@link #parse(String, String, Sink, DocumentRendererContext)}
     */
    protected void parse( String fullDocPath, String parserId, Sink sink )
        throws DocumentRendererException, IOException
    {
        parse( fullDocPath, parserId, sink, null );
    }

    /**
     * Parse a source document into a sink.
     *
     * @param fullDocPath absolute path to the source document.
     * @param parserId determines the parser to use.
     * @param sink the sink to receive the events.
     * @param context the rendering context.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException in case of a parsing error.
     * @throws java.io.IOException if the source document cannot be opened.
     */
    protected void parse( String fullDocPath, String parserId, Sink sink, DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Parsing file " + fullDocPath );
        }

        Reader reader = null;
        try
        {
            File f = new File( fullDocPath );

            Parser parser = doxia.getParser( parserId );
            switch ( parser.getType() )
            {
                case Parser.XML_TYPE:
                    reader = ReaderFactory.newXmlReader( f );

                    if ( isVelocityFile( f ) )
                    {
                        reader = getVelocityReader( f, ( (XmlStreamReader) reader ).getEncoding(), context );
                    }
                    if ( context != null && Boolean.TRUE.equals( (Boolean) context.get( "validate" ) ) )
                    {
                        reader = validate( reader, fullDocPath );
                    }
                    break;

                case Parser.TXT_TYPE:
                case Parser.UNKNOWN_TYPE:
                default:
                    if ( isVelocityFile( f ) )
                    {
                        reader =
                            getVelocityReader( f, ( context == null ? ReaderFactory.FILE_ENCODING
                                            : context.getInputEncoding() ), context );
                    }
                    else
                    {
                        if ( context == null )
                        {
                            reader = ReaderFactory.newPlatformReader( f );
                        }
                        else
                        {
                            reader = ReaderFactory.newReader( f, context.getInputEncoding() );
                        }
                    }
            }

            sink.enableLogging( new PlexusLoggerWrapper( getLogger() ) );

            doxia.parse( reader, parserId, sink );
        }
        catch ( ParserNotFoundException e )
        {
            throw new DocumentRendererException( "No parser '" + parserId
                        + "' found for " + fullDocPath + ": " + e.getMessage(), e );
        }
        catch ( ParseException e )
        {
            throw new DocumentRendererException( "Error parsing " + fullDocPath + ": " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );

            sink.flush();
        }
    }

    /**
     * Copies the contents of the resource directory to an output folder.
     *
     * @param outputDirectory the destination folder.
     * @throws java.io.IOException if any.
     */
    protected void copyResources( File outputDirectory )
            throws IOException
    {
        File resourcesDirectory = new File( getBaseDir(), "resources" );

        if ( !resourcesDirectory.isDirectory() )
        {
            return;
        }

        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }

        copyDirectory( resourcesDirectory, outputDirectory );
    }

    /**
     * Copy content of a directory, excluding scm-specific files.
     *
     * @param source directory that contains the files and sub-directories to be copied.
     * @param destination destination folder.
     * @throws java.io.IOException if any.
     */
    protected void copyDirectory( File source, File destination )
            throws IOException
    {
        if ( source.isDirectory() && destination.isDirectory() )
        {
            DirectoryScanner scanner = new DirectoryScanner();

            String[] includedResources = {"**/**"};

            scanner.setIncludes( includedResources );

            scanner.addDefaultExcludes();

            scanner.setBasedir( source );

            scanner.scan();

            List<String> includedFiles = Arrays.asList( scanner.getIncludedFiles() );

            for ( String name : includedFiles )
            {
                File sourceFile = new File( source, name );

                File destinationFile = new File( destination, name );

                FileUtils.copyFile( sourceFile, destinationFile );
            }
        }
    }

    /**
     * @param documentModel not null
     * @return the output name defined in the documentModel without the output extension. If the output name is not
     * defined, return target by default.
     * @since 1.1.1
     * @see org.apache.maven.doxia.document.DocumentModel#getOutputName()
     * @see #getOutputExtension()
     */
    protected String getOutputName( DocumentModel documentModel )
    {
        String outputName = documentModel.getOutputName();
        if ( outputName == null )
        {
            getLogger().info( "No outputName is defined in the document descriptor. Using 'target'" );

            documentModel.setOutputName( "target" );
        }

        outputName = outputName.trim();
        if ( outputName.toLowerCase( Locale.ENGLISH ).endsWith( "." + getOutputExtension() ) )
        {
            outputName =
                outputName.substring( 0, outputName.toLowerCase( Locale.ENGLISH )
                                                   .lastIndexOf( "." + getOutputExtension() ) );
        }
        documentModel.setOutputName( outputName );

        return documentModel.getOutputName();
    }

    /**
     * TODO: DOXIA-111: we need a general filter here that knows how to alter the context
     *
     * @param f the file to process, not null
     * @param encoding the wanted encoding, not null
     * @param context the current render document context not null
     * @return a reader with
     * @throws DocumentRendererException
     */
    private Reader getVelocityReader( File f, String encoding, DocumentRendererContext context )
        throws DocumentRendererException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Velocity render for " + f.getAbsolutePath() );
        }

        SiteResourceLoader.setResource( f.getAbsolutePath() );

        Context velocityContext = new VelocityContext();

        if ( context.getKeys() != null )
        {
            for ( int i = 0; i < context.getKeys().length; i++ )
            {
                String key = (String) context.getKeys()[i];

                velocityContext.put( key, context.get( key ) );
            }
        }

        StringWriter sw = new StringWriter();
        try
        {
            velocity.getEngine().mergeTemplate( f.getAbsolutePath(), encoding, velocityContext, sw );
        }
        catch ( Exception e )
        {
            throw new DocumentRendererException( "Error whenn parsing Velocity file " + f.getAbsolutePath() + ": "
                + e.getMessage(), e );
        }

        return new StringReader( sw.toString() );
    }

    /**
     * @param f not null
     * @return <code>true</code> if file has a vm extension, <code>false</false> otherwise.
     */
    private static boolean isVelocityFile( File f )
    {
        return FileUtils.getExtension( f.getAbsolutePath() ).toLowerCase( Locale.ENGLISH ).endsWith( "vm" );
    }

    private Reader validate( Reader source, String resource )
            throws ParseException, IOException
    {
        getLogger().debug( "Validating: " + resource );

        try
        {
            String content = IOUtil.toString( new BufferedReader( source ) );

            new XmlValidator( new PlexusLoggerWrapper( getLogger() ) ).validate( content );

            return new StringReader( content );
        }
        finally
        {
            IOUtil.close( source );
        }
    }
}
