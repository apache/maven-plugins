package org.apache.maven.plugin.pmd;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import net.sourceforge.pmd.IRuleViolation;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.SourceType;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

/**
 * Implement the PMD report.
 *
 * @author Brett Porter
 * @version $Id: PmdReport.java,v 1.3 2005/02/23 00:08:53 brett Exp $
 * @goal pmd
 * @todo needs to support the multiple source roots
 */
public class PmdReport
    extends AbstractPmdReport
{

    /**
     * The target JDK to analyse based on. Should match the target used in the compiler plugin. Valid values are
     * currently <code>1.3</code>, <code>1.4</code>, <code>1.5</code>.
     *
     * @parameter expression="${targetJdk}"
     */
    private String targetJdk;

    /**
     * The rule priority threshold; rules with lower priority
     * than this will not be evaluated.
     *
     * @parameter expression="${minimumPriority}" default-value="5"
     */
    private int minimumPriority = 5;

    /**
     * Skip the PMD report generation.  Most useful on the command line
     * via "-Dmaven.pmd.skip=true".
     *
     * @parameter expression="${maven.pmd.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * The PMD rulesets to use. See the <a href="http://pmd.sourceforge.net/rules/index.html">Stock Rulesets</a> for a
     * list of some included. Defaults to the basic, imports and unusedcode rulesets.
     *
     * @parameter
     */
    private String[] rulesets = new String[]{"rulesets/basic.xml", "rulesets/unusedcode.xml", "rulesets/imports.xml",};

    /**
     * The file encoding to use when reading the java source.
     *
     * @parameter
     */
    private String sourceEncoding;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !skip && canGenerateReport() )
        {
            Sink sink = getSink();

            PMD pmd = getPMD();
            RuleContext ruleContext = new RuleContext();
            Report report = new Report();
            // TODO: use source roots instead
            String sourceDirectory = project.getBuild().getSourceDirectory();
            PmdReportListener reportSink = new PmdReportListener( sink, sourceDirectory, getBundle( locale ) );
            String location = constructXRefLocation();
            if ( location != null )
            {
                reportSink.setXrefLocation( location );
            }

            report.addListener( reportSink );
            ruleContext.setReport( report );
            reportSink.beginDocument();

            Locator locator = new Locator( getLog() );
            RuleSetFactory ruleSetFactory = new RuleSetFactory();
            ruleSetFactory.setMinimumPriority( this.minimumPriority );
            RuleSet[] sets = new RuleSet[rulesets.length];
            try
            {
                for ( int idx = 0; idx < rulesets.length; idx++ )
                {
                    String set = rulesets[idx];
                    getLog().debug( "Preparing ruleset: " + set );
                    File ruleset = locator.resolveLocation( set, getLocationTemp( set ) );
                    InputStream rulesInput = new FileInputStream( ruleset );
                    sets[idx] = ruleSetFactory.createRuleSet( rulesInput );
                }
            }
            catch ( IOException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }

            boolean hasEncoding = sourceEncoding != null;

            List files;
            try
            {
                files = getFilesToProcess( "**/*.java" );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Can't parse " + sourceDirectory, e );
            }

            for ( Iterator i = files.iterator(); i.hasNext(); )
            {
                File file = (File) i.next();

                
                // TODO: lazily call beginFile in case there are no rules

                reportSink.beginFile( file );
                ruleContext.setSourceCodeFilename( file.getAbsolutePath() );
                for ( int idx = 0; idx < rulesets.length; idx++ )
                {
                    try
                    {
                        // PMD closes this Reader even though it did not open it so we have
                        // to open a new one with every call to processFile().
                        Reader reader = hasEncoding ? new InputStreamReader( new FileInputStream( file ),
                                                                             sourceEncoding )
                            : new FileReader( file );
                        pmd.processFile( reader, sets[idx], ruleContext );
                    }
                    catch ( UnsupportedEncodingException e1 )
                    {
                        throw new MavenReportException( "Encoding '" + sourceEncoding + "' is not supported.", e1 );
                    }
                    catch ( FileNotFoundException e2 )
                    {
                    	getLog().warn("Error opening source file: " + file);
                    	reportSink.ruleViolationAdded(new ProcessingErrorRuleViolation(file, e2.getLocalizedMessage()) );
                    }
                    catch ( Exception e3 )
                    {
                        getLog().warn( "Failure executing PMD for: " + file, e3 );
                        reportSink.ruleViolationAdded(new ProcessingErrorRuleViolation(file, e3.getLocalizedMessage()) );
                    }
                }
                reportSink.endFile( file );
            }
         
            reportSink.endDocument();

            if ( !isHtml() )
            {
                // Use the PMD renderers to render in any format aside from HTML.
                Renderer r = createRenderer();
                String buffer = r.render( report );
                try
                {
                    Writer writer = new FileWriter( new File( targetDirectory, "pmd." + format ) );
                    writer.write( buffer, 0, buffer.length() );
                    writer.close();
                }
                catch ( IOException ioe )
                {
                    throw new MavenReportException( ioe.getMessage(), ioe );
                }
            }
        }
    }

    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @return a String that contains the absolute file name of the file
     */
    private String getLocationTemp( String name )
    {
        String loc = name;
        if ( loc.indexOf( '/' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '/' ) + 1 );
        }
        if ( loc.indexOf( '\\' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '\\' ) + 1 );
        }
        getLog().debug( "Before: " + name + " After: " + loc );
        return project.getBuild().getDirectory() + File.separator + loc;
    }

    /**
     * Constructs the PMD class, passing it an argument
     * that configures the target JDK.
     *
     * @return the resulting PMD
     */
    public PMD getPMD()
    {
        PMD pmd = new PMD();
        
        if ( "1.5".equals( targetJdk ) )
        {
            pmd.setJavaVersion(SourceType.JAVA_15);
        }
        else if ( "1.4".equals( targetJdk ) )
        {
        	pmd.setJavaVersion(SourceType.JAVA_14);
        }
        else if ( "1.3".equals( targetJdk ) )
        {
        	pmd.setJavaVersion(SourceType.JAVA_13);
        }
        
        return pmd;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "pmd";
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "pmd-report", locale, PmdReport.class.getClassLoader() );
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException
     *          if no renderer found for the output type
     */
    public final Renderer createRenderer()
        throws MavenReportException
    {
        Renderer renderer = null;
        if ( "xml".equals( format ) )
        {
            renderer = new XMLRenderer();
        }
        else if ( "txt".equals( format ) )
        {
            renderer = new TextRenderer();
        }
        else if ( "csv".equals( format ) )
        {
            renderer = new CSVRenderer();
        }
        else if ( "html".equals( format ) )
        {
            renderer = new HTMLRenderer();
        }
        else if ( !"".equals( format ) && !"none".equals( format ) )
        {
            try
            {
                renderer = (Renderer) Class.forName( format ).newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException(
                    "Can't find the custom format " + format + ": " + e.getClass().getName() );
            }
        }

        if ( renderer == null )
        {
            throw new MavenReportException( "Can't create report with format of " + format );
        }

        return renderer;
    }
    
    /**
     * @author <a href="mailto:douglass.doug@gmail.com">Doug Douglass</a>
     */
    private static class ProcessingErrorRuleViolation implements IRuleViolation {
    	
    	private String filename;
    	
    	private String description;
    	
    	public ProcessingErrorRuleViolation(File file, String description) {
    		filename = file.getPath();
    		this.description = description;
    	}

		public String getFilename() {
			return this.filename;
		}

		public int getBeginLine() {
			return 0;
		}

		public int getBeginColumn() {
			return 0;
		}

		public int getEndLine() {
			return 0;
		}

		public int getEndColumn() {
			return 0;
		}

		public Rule getRule() {
			return null;
		}

		public String getDescription() {			
			return this.description;
		}

		public String getPackageName() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public String getClassName() {
			return null;
		}

		public boolean isSuppressed() {
			return false;
		}

		public String getVariableName() {
			return null;
		}
    }
}
