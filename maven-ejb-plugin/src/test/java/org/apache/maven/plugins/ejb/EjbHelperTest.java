package org.apache.maven.plugins.ejb;

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

import java.io.File;

import org.junit.Test;

public class EjbHelperTest
{

    @Test
    public void validClassifier()
    {
        assertThat( EjbHelper.isClassifierValid( "anton" ) ).isTrue();
    }

    @Test
    public void anOtherValidClassifier()
    {
        assertThat( EjbHelper.isClassifierValid( "jdk15" ) ).isTrue();
    }

    @Test
    public void moreValidClassifier()
    {
        assertThat( EjbHelper.isClassifierValid( "client-classifier" ) ).isTrue();
    }

    @Test
    public void isClassifierValidShouldReturnFalseIfClassifierIsPrefixedByDash()
    {
        assertThat( EjbHelper.isClassifierValid( "-anton" ) ).isFalse();
    }

    @Test
    public void isClassifierValidShouldReturnFalseIfClassifierIsNull()
    {
        assertThat( EjbHelper.isClassifierValid( null ) ).isFalse();
    }

    @Test
    public void hasClassifierShouldReturnFalseForNull()
    {
        assertThat( EjbHelper.hasClassifier( null ) ).isFalse();
    }

    @Test
    public void hasClassifierShouldReturnFalseForEmptyString()
    {
        assertThat( EjbHelper.hasClassifier( "" ) ).isFalse();
    }

    @Test
    public void hasClassifierShouldReturnTrueForNonEmptyString()
    {
        assertThat( EjbHelper.hasClassifier( "x" ) ).isTrue();
    }

    @Test
    public void getJarFileNameShouldReturnFileNameWithoutClassifier()
    {
        assertThat( EjbHelper.getJarFileName( new File( "base" ), "test",
                                              null ) ).isEqualTo( new File( "base/test.jar" ) );
    }

    @Test
    public void getJarFileNameShouldReturnFileNameWithClassifier()
    {
        assertThat( EjbHelper.getJarFileName( new File( "base" ), "test",
                                              "alpha" ) ).isEqualTo( new File( "base/test-alpha.jar" ) );
    }
}
