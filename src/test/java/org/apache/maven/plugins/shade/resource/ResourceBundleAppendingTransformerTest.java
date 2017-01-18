package org.apache.maven.plugins.shade.resource;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ResourceBundleAppendingTransformerTest
{
    private ResourceBundleAppendingTransformer transformer;
    
    @Before
    public void setUp()
    {
        transformer = new ResourceBundleAppendingTransformer();
    }
    
    @Test
    public void testCanTransformResource()
    {
        transformer.setBasename( "a/b/c/ButtonLabel" );
        
        assertTrue( transformer.canTransformResource( "a/b/c/ButtonLabel.properties" ) );
        assertTrue( transformer.canTransformResource( "a/b/c/ButtonLabel_en.properties" ) );
        assertTrue( transformer.canTransformResource( "a/b/c/ButtonLabel_en_US.properties" ) );
        assertTrue( transformer.canTransformResource( "a/b/c/ButtonLabel_fr.properties" ) );
        assertTrue( transformer.canTransformResource( "a/b/c/ButtonLabel_fr_CA.properties" ) );
        assertTrue( transformer.canTransformResource( "a/b/c/ButtonLabel_fr_CA_UNIX.properties" ) );

        assertFalse( transformer.canTransformResource( "a/b/c/ButtonLabel.class" ) );
        assertFalse( transformer.canTransformResource( "c/ButtonLabel.properties" ) );
        assertFalse( transformer.canTransformResource( "ButtonLabel.properties" ) );
    }

}
