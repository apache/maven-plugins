package org.apache.maven.plugins.jlink;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
public class AbsractJLinkMojoTest
{
    private AbstractJLinkMojo mojoMock;

    @Before
    public void before()
    {
        this.mojoMock = mock( AbstractJLinkMojo.class, Mockito.CALLS_REAL_METHODS );
    }

    @Test
    public void convertShouldReturnSingleCharacter()
    {
        StringBuilder result = mojoMock.convertSeparatedModulePathToPlatformSeparatedModulePath( "x" );
        assertThat( result.toString() ).isNotEmpty().isEqualTo( "x" );
    }

    @Test
    public void convertShouldReturnTwoCharactersSeparatedByPathSeparator()
    {
        StringBuilder result = mojoMock.convertSeparatedModulePathToPlatformSeparatedModulePath( "x;a" );
        assertThat( result.toString() ).isEqualTo( "x" + File.pathSeparatorChar + "a" );
    }

    @Test
    public void convertUsingDifferentDelimiterShouldReturnTwoCharactersSeparatedByPathSeparator()
    {
        StringBuilder result = mojoMock.convertSeparatedModulePathToPlatformSeparatedModulePath( "x:a" );
        assertThat( result.toString() ).isEqualTo( "x" + File.pathSeparatorChar + "a" );
    }

    @Test
    public void convertUsingMultipleDelimitersShouldReturnTwoCharactersSeparatedByPathSeparator()
    {
        StringBuilder result = mojoMock.convertSeparatedModulePathToPlatformSeparatedModulePath( "x:a::" );
        assertThat( result.toString() ).isEqualTo( "x" + File.pathSeparatorChar + "a" );
    }

    @Test
    public void getPlatformDependSeparateListShouldReturnASingleCharacter()
    {
        String result = mojoMock.getPlatformDependSeparateList( Collections.singletonList( "A" ) );
        assertThat( result ).isEqualTo( "A" );
    }

    @Test
    public void getPlatformDependSeparateListShouldReturnTwoCharactersSeparated()
    {
        String result = mojoMock.getPlatformDependSeparateList( Arrays.asList( "A", "B" ) );
        assertThat( result ).isEqualTo( "A" + File.pathSeparatorChar + "B" );
    }

    @Test
    public void getPlatformDependSeparateListShouldReturnThreeCharactersSeparated()
    {
        String result = mojoMock.getPlatformDependSeparateList( Arrays.asList( "A", "B", "C" ) );
        assertThat( result ).isEqualTo( "A" + File.pathSeparatorChar + "B" + File.pathSeparatorChar + "C" );
    }

    @Test
    public void getCommaSeparatedListShouldReturnASingleCharacter()
    {
        String result = mojoMock.getCommaSeparatedList( Arrays.asList( "A" ) );
        assertThat( result ).isEqualTo( "A" );
    }

    @Test
    public void getCommaSeparatedListShouldReturnTwoCharactersSeparatedByComma()
    {
        String result = mojoMock.getCommaSeparatedList( Arrays.asList( "A", "B" ) );
        assertThat( result ).isEqualTo( "A,B" );
    }

}
