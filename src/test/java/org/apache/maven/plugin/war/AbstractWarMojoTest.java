package org.apache.maven.plugin.war;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.war.stub.MavenProjectBasicStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractWarMojoTest
    extends AbstractMojoTestCase
{

    protected abstract File getTestDirectory()
        throws Exception;

    /**
     * initialize required parameters
     *
     * @param mojo
     * @param filters
     * @param classesDir
     * @param webAppSource
     * @param webAppDir
     * @param project
     * @throws Exception
     */
    protected void configureMojo( AbstractWarMojo mojo, List filters, File classesDir, File webAppSource,
                                  File webAppDir, MavenProjectBasicStub project )
        throws Exception
    {
        setVariableValueToObject( mojo, "filters", filters );
        mojo.setClassesDirectory( classesDir );
        mojo.setWarSourceDirectory( webAppSource );
        mojo.setWebappDirectory( webAppDir );
        mojo.setProject( project );
    }

    /**
     * create an isolated xml dir
     *
     * @param id
     * @param xmlFiles
     * @return
     * @throws Exception
     */
    protected File createXMLConfigDir( String id, String[] xmlFiles )
        throws Exception
    {
        File xmlConfigDir = new File( getTestDirectory(), "/" + id + "-test-data/xml-config" );
        File XMLFile;

        createDir( xmlConfigDir );

        if ( xmlFiles != null )
        {
            Iterator iterator = Arrays.asList( xmlFiles ).iterator();
            while ( iterator.hasNext() )
            {
                XMLFile = new File( xmlConfigDir, (String) iterator.next() );
                createFile( XMLFile );
            }
        }

        return xmlConfigDir;
    }

    /**
     * create an isolated web source with a sample jsp file
     *
     * @param id
     * @return
     * @throws Exception
     */
    protected File createWebAppSource( String id )
        throws Exception
    {
        File webAppSource = new File( getTestDirectory(), "/" + id + "-test-data/source" );
        File simpleJSP = new File( webAppSource, "pansit.jsp" );
        File jspFile = new File( webAppSource, "org/web/app/last-exile.jsp" );

        createFile( simpleJSP );
        createFile( jspFile );
        return webAppSource;
    }

    /**
     * create a class directory with or without a sample class
     *
     * @param id
     * @param empty
     * @return
     * @throws Exception
     */
    protected File createClassesDir( String id, boolean empty )
        throws Exception
    {
        File classesDir = new File( getTestDirectory() + "/" + id + "-test-data/classes/" );

        createDir( classesDir );

        if ( !empty )
        {
            createFile( new File( classesDir + "/sample-servlet.class" ) );
        }

        return classesDir;
    }

    protected void createDir( File dir )
    {
        if ( !dir.exists() )
        {
            assertTrue( "can not create test dir: " + dir.toString(), dir.mkdirs() );
        }
    }

    protected void createFile( File testFile )
        throws Exception
    {
        createDir( testFile.getParentFile() );
        FileUtils.fileWrite( testFile.toString(), testFile.toString() );

        assertTrue( "could not create file: " + testFile, testFile.exists() );
    }
}
