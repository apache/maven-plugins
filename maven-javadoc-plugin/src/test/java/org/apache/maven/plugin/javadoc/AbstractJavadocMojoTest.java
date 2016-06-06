
package org.apache.maven.plugin.javadoc;

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

import junit.framework.Assert;
import junitx.util.PrivateAccessor;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * @author <a href="mailto:vincent.zurczak@linagora.com">Vincent Zurczak</a>
 * @version $Id$
 */
public class AbstractJavadocMojoTest extends AbstractMojoTestCase {

	private static final String GET_STYLE_SHEET_METHOD = "getStylesheetFile";
	private File outputDirectory;


	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.outputDirectory = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString());
		Assert.assertTrue( this.outputDirectory.mkdirs());
	}


	@Override
	protected void tearDown() throws Exception {

		FileUtils.deleteQuietly( this.outputDirectory );
		super.tearDown();
	}


	/**
     * @throws Throwable if any
     */
    public void testGetStylesheetFile() throws Throwable
    {
    	AbstractJavadocMojo mojo = new AbstractJavadocMojo() {
			public void execute() throws MojoExecutionException, MojoFailureException {
				// nothing
			}
		};

		// Java style sheet
        PrivateAccessor.setField( mojo, "stylesheet", "java" );
        assertNull( PrivateAccessor.invoke(
        		mojo, GET_STYLE_SHEET_METHOD,
        		new Class[] { File.class },
        		new Object[] { this.outputDirectory }));

        // Default style sheet
        PrivateAccessor.setField( mojo, "stylesheet", null );
        String s = new File( this.outputDirectory, AbstractJavadocMojo.DEFAULT_CSS_NAME ).getAbsolutePath();
        assertEquals( s, PrivateAccessor.invoke(
        		mojo, GET_STYLE_SHEET_METHOD,
        		new Class[] { File.class },
        		new Object[] { this.outputDirectory }));

        // Absolute file path
        File newFile = File.createTempFile( "random_", ".css" );
        PrivateAccessor.setField( mojo, "stylesheetfile", newFile.getAbsolutePath());
        assertEquals( newFile.getAbsolutePath(), PrivateAccessor.invoke(
        		mojo, GET_STYLE_SHEET_METHOD,
        		new Class[] { File.class },
        		new Object[] { this.outputDirectory }));

        // URL
        newFile = File.createTempFile( "random_", ".css" );
        PrivateAccessor.setField( mojo, "stylesheetfile", newFile.toURI().toURL().toString());
        String importedFile = (String) PrivateAccessor.invoke(
        		mojo, GET_STYLE_SHEET_METHOD,
        		new Class[] { File.class },
        		new Object[] { this.outputDirectory });

        Assert.assertNotNull( importedFile );
        Assert.assertTrue( importedFile.startsWith( System.getProperty( "java.io.tmpdir" )));
        Assert.assertNotSame( importedFile, newFile.getAbsolutePath());
    }
}
