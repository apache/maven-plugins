package org.apache.maven.plugin.antlr;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/**
 * Class to test Antlr plugin
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntlrPluginTest
    extends AbstractMojoTestCase
{
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        // nop
    }

    /**
     * Method to test Antlr generation
     *
     * @throws Exception
     */
    public void testJavaGrammar()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/java-grammar-test/java-grammar-test-plugin-config.xml" );
        AntlrPlugin mojo = (AntlrPlugin) lookupMojo( "generate", testPom );
        mojo.execute();

        File outputDir = new File( getBasedir(),
                                   "target/test/unit/java-grammar-test/target/generated-sources/antlr/" );
        assertTrue( new File( outputDir, "JavaLexer.java" ).exists() );
        assertTrue( new File( outputDir, "JavaRecognizer.java" ).exists() );
        assertTrue( new File( outputDir, "JavaTokenTypes.java" ).exists() );
        assertTrue( new File( outputDir, "JavaTokenTypes.txt" ).exists() );
    }

    /**
     * Method to test Antlr generation
     *
     * @throws Exception
     */
    public void testJavaGrammarInheritance()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/java-grammar-inheritance-test/java-grammar-inheritance-test-plugin-config.xml" );
        AntlrPlugin mojo = (AntlrPlugin) lookupMojo( "generate", testPom );
        mojo.execute();

        File outputDir = new File( getBasedir(),
                                   "target/test/unit/java-grammar-inheritance-test/target/generated-sources/antlr/" );
        assertTrue( outputDir.exists() );
        assertTrue( new File( outputDir, "GnuCEmitter.java" ).exists() );
        assertTrue( new File( outputDir, "GnuCEmitterTokenTypes.java" ).exists() );
        assertTrue( new File( outputDir, "GnuCLexer.java" ).exists() );
        assertTrue( new File( outputDir, "GnuCLexerTokenTypes.java" ).exists() );
        assertTrue( new File( outputDir, "GnuCParser.java" ).exists() );
        assertTrue( new File( outputDir, "GNUCTokenTypes.java" ).exists() );
        assertTrue( new File( outputDir, "GnuCTreeParser.java" ).exists() );
        assertTrue( new File( outputDir, "GnuCTreeParserTokenTypes.java" ).exists() );
        assertTrue( new File( outputDir, "StdCLexer.java" ).exists() );
        assertTrue( new File( outputDir, "StdCParser.java" ).exists() );
        assertTrue( new File( outputDir, "STDCTokenTypes.java" ).exists() );
    }

    /**
     * Method to test Antlr Html report
     *
     * @throws Exception
     */
    public void testJavaGrammarReport()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/java-grammar-report-test/java-grammar-report-test-plugin-config.xml" );

        Commandline cmd = new Commandline();

        cmd.setWorkingDirectory( testPom.getParentFile().getAbsolutePath() );

        if ( System.getProperty( "os.name" ).indexOf( "Windows" ) != -1 )
        {
            // To know ExitCode
            cmd.setExecutable( "call mvn" );
        }
        else
        {
            cmd.setExecutable( "mvn" );
        }

        // Full trace
        cmd.createArgument().setValue( "-e" );
        cmd.createArgument().setValue( "-X" );

        cmd.createArgument().setValue( "clean" );
        cmd.createArgument().setValue( "site" );

        cmd.addArguments( new String[] { "-f", testPom.getName() } );

        if ( getContainer().getLogger().isDebugEnabled() )
        {
            getContainer().getLogger().debug( cmd.toString() );
        }

        int exitCode;

        Writer output = new StringWriter();
        StreamConsumer consumer = new WriterStreamConsumer( output );
        exitCode = CommandLineUtils.executeCommandLine( cmd, consumer, consumer );

        if ( getContainer().getLogger().isDebugEnabled() )
        {
            getContainer().getLogger().debug( output.toString() );
        }

        if ( exitCode != 0 )
        {
            // ----------------------------------------------------------------------
            // Calling mvn from command line could throw an exception if
            // maven-antlr-plugin is not already deployed
            // ----------------------------------------------------------------------

            if ( output.toString()
                .indexOf( "POM 'org.apache.maven.plugins:maven-antlr-plugin' not found in repository" ) != -1 )
            {
                getContainer().getLogger().info(
                                                 "org.apache.maven.plugins:maven-antlr-plugin not already "
                                                     + "deployed. AntlrTest#testJavaGrammarReport() skipped." );
                return;
            }

            // ----------------------------------------------------------------------
            // Calling mvn from command line could throw an exception if
            // maven-antlr-plugin could not be configured
            // ----------------------------------------------------------------------

            if ( output.toString()
                .indexOf( "Failed to configure plugin parameters for: org.apache.maven.plugins:maven-antlr-plugin" ) != -1 )
            {
                getContainer().getLogger().info(
                                                 "org.apache.maven.plugins:maven-antlr-plugin could not be " +
                                                 "configured. AntlrTest#testJavaGrammarReport() skipped." );
                return;
            }

            // ----------------------------------------------------------------------
            // Calling mvn from command line could throw an exception if
            // java.lang.NoClassDefFoundError: org/apache/maven/doxia/sink/SinkFactory
            // ----------------------------------------------------------------------

            if ( output.toString()
                .indexOf( "java.lang.NoClassDefFoundError: org/apache/maven/doxia/sink/SinkFactory" ) != -1 )
            {
                getContainer().getLogger().info(
                                                 "java.lang.NoClassDefFoundError: org/apache/maven/doxia/sink/SinkFactory." +
                                                 " AntlrTest#testJavaGrammarReport() skipped." );
                return;
            }

            StringBuffer msg = new StringBuffer();
            msg.append( "The command line failed. Exit code: " + exitCode ).append( "\n= call mvn output =\n" );
            msg.append( output.toString() ).append( "\n" );
            msg.append( "= End output =\n" );

            throw new CommandLineException( msg.toString() );
        }

        // ----------------------------------------------------------------------
        // Old beta version
        // ----------------------------------------------------------------------

        File outputDir = new File( getBasedir(), "target/test/unit/java-grammar-report-test/target/site/antlr" );
        if (!outputDir.exists())
        {
              return;
        }

        File index = new File( outputDir, "index.html" );
        assertNotNull( index );
        assertTrue( readFile( index ).indexOf( "<a href=\"JavaRecognizer.html\">JavaRecognizer</a>" ) != -1 );
        assertTrue( new File( outputDir, "index.html" ).exists() );
        assertTrue( new File( outputDir, "JavaRecognizer.html" ).exists() );
    }

    /**
     * Read the contents of the specified file object into a string
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private String readFile( File file )
        throws IOException
    {
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        return IOUtil.toString( in );
    }
}
