package org.apache.maven.plugins.dependency;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

public class TestSkip
    extends AbstractDependencyMojoTestCase
{
    public void testSkipAnalyze()
        throws Exception
    {
        doTest( "analyze" );
    }

    public void testSkipAnalyzeDepMgt()
        throws Exception
    {
        doTest( "analyze-dep-mgt" );
    }

    public void testSkipAnalyzeOnly()
        throws Exception
    {
        doTest( "analyze-only" );
    }

    public void testSkipAnalyzeReport()
        throws Exception
    {
        doSpecialTest( "analyze-report" );
    }

    public void testSkipAnalyzeDuplicate()
        throws Exception
    {
        doTest( "analyze-duplicate" );
    }

    public void testSkipBuildClasspath()
        throws Exception
    {
        doTest( "build-classpath" );
    }

    public void testSkipCopy()
        throws Exception
    {
        doTest( "copy" );
    }

    public void testSkipCopyDependencies()
        throws Exception
    {
        doTest( "copy-dependencies" );
    }

    public void testSkipGet()
        throws Exception
    {
        doSpecialTest( "get" );
    }

    public void testSkipGoOffline()
        throws Exception
    {
        doTest( "go-offline" );
    }

    public void testSkipList()
        throws Exception
    {
        doTest( "list" );
    }

    public void testSkipProperties()
        throws Exception
    {
        doTest( "properties" );
    }

    public void testSkipPurgeLocalRepository()
        throws Exception
    {
        doSpecialTest( "purge-local-repository" );
    }

    public void testSkipResolve()
        throws Exception
    {
        doTest( "resolve" );
    }

    public void testSkipResolvePlugins()
        throws Exception
    {
        doTest( "resolve-plugins" );
    }

    public void testSkipSources()
        throws Exception
    {
        doTest( "sources" );
    }

    public void testSkipTree()
        throws Exception
    {
        doTest( "tree" );
    }

    public void testSkipUnpack()
        throws Exception
    {
        doTest( "unpack" );
    }

    public void testSkipUnpackDependencies()
        throws Exception
    {
        doTest( "unpack-dependencies" );
    }

    protected void doTest( String mojoName )
        throws Exception
    {
        doConfigTest( mojoName, "plugin-config.xml" );
    }

    protected void doSpecialTest( String mojoName )
        throws Exception
    {
        doConfigTest( mojoName, "plugin-" + mojoName + "-config.xml" );
    }

    private void doConfigTest( String mojoName, String configFile )
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/skip-test/" + configFile );
        Mojo mojo = lookupMojo( mojoName, testPom );
        assertNotNull( mojo );
        CapturingLog log = new CapturingLog();
        mojo.setLog( log );
        mojo.execute();

        assertTrue( log.getContent().contains( "Skipping plugin execution" ) );
    }

    class CapturingLog
        implements Log
    {
        StringBuilder sb = new StringBuilder();

        /** {@inheritDoc} */
        public void debug( CharSequence content )
        {
            print( "debug", content );
        }

        /** {@inheritDoc} */
        public void debug( CharSequence content, Throwable error )
        {
            print( "debug", content, error );
        }

        /** {@inheritDoc} */
        public void debug( Throwable error )
        {
            print( "debug", error );
        }

        /** {@inheritDoc} */
        public void info( CharSequence content )
        {
            print( "info", content );
        }

        /** {@inheritDoc} */
        public void info( CharSequence content, Throwable error )
        {
            print( "info", content, error );
        }

        /** {@inheritDoc} */
        public void info( Throwable error )
        {
            print( "info", error );
        }

        /** {@inheritDoc} */
        public void warn( CharSequence content )
        {
            print( "warn", content );
        }

        /** {@inheritDoc} */
        public void warn( CharSequence content, Throwable error )
        {
            print( "warn", content, error );
        }

        /** {@inheritDoc} */
        public void warn( Throwable error )
        {
            print( "warn", error );
        }

        /** {@inheritDoc} */
        public void error( CharSequence content )
        {
            System.err.println( "[error] " + content.toString() );
        }

        /** {@inheritDoc} */
        public void error( CharSequence content, Throwable error )
        {
            StringWriter sWriter = new StringWriter();
            PrintWriter pWriter = new PrintWriter( sWriter );

            error.printStackTrace( pWriter );

            System.err.println( "[error] " + content.toString() + "\n\n" + sWriter.toString() );
        }

        /**
         * @see org.apache.maven.plugin.logging.Log#error(java.lang.Throwable)
         */
        public void error( Throwable error )
        {
            StringWriter sWriter = new StringWriter();
            PrintWriter pWriter = new PrintWriter( sWriter );

            error.printStackTrace( pWriter );

            System.err.println( "[error] " + sWriter.toString() );
        }

        /**
         * @see org.apache.maven.plugin.logging.Log#isDebugEnabled()
         */
        public boolean isDebugEnabled()
        {
            // TODO: Not sure how best to set these for this implementation...
            return false;
        }

        /**
         * @see org.apache.maven.plugin.logging.Log#isInfoEnabled()
         */
        public boolean isInfoEnabled()
        {
            return true;
        }

        /**
         * @see org.apache.maven.plugin.logging.Log#isWarnEnabled()
         */
        public boolean isWarnEnabled()
        {
            return true;
        }

        /**
         * @see org.apache.maven.plugin.logging.Log#isErrorEnabled()
         */
        public boolean isErrorEnabled()
        {
            return true;
        }

        private void print( String prefix, CharSequence content )
        {
            sb.append( "[" ).append( prefix ).append( "] " ).append( content.toString() ).append( "\n" );
        }

        private void print( String prefix, Throwable error )
        {
            StringWriter sWriter = new StringWriter();
            PrintWriter pWriter = new PrintWriter( sWriter );

            error.printStackTrace( pWriter );

            sb.append( "[" ).append( prefix ).append( "] " ).append( sWriter.toString() ).append( "\n" );
        }

        private void print( String prefix, CharSequence content, Throwable error )
        {
            StringWriter sWriter = new StringWriter();
            PrintWriter pWriter = new PrintWriter( sWriter );

            error.printStackTrace( pWriter );

            sb.append( "[" ).append( prefix ).append( "] " ).append( content.toString() ).append( "\n\n" ).append( sWriter.toString() ).append( "\n" );
        }

        protected String getContent()
        {
            return sb.toString();
        }
    }

}
