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

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * <code>Unit tests</code> of Antlr plugin
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
}
