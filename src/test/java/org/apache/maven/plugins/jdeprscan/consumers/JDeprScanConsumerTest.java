package org.apache.maven.plugins.jdeprscan.consumers;

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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class JDeprScanConsumerTest
{
    private JDeprScanConsumer consumer;
    
    @Before
    public void setUp()
    {
        consumer = new JDeprScanConsumer();
    }

    @Test
    public void testDeprecatedClass()
    {
        consumer.consumeLine( "class o/a/m/p/j/its/Deprecations uses deprecated class java/rmi/RMISecurityManager " );
        
        assertEquals( consumer.getDeprecatedClasses().size(), 1 );
        assertEquals( consumer.getDeprecatedMethods().size(), 0 );
        
        Set<String> classes = consumer.getDeprecatedClasses().get( "o/a/m/p/j/its/Deprecations" );
        assertEquals( Collections.singleton( "java/rmi/RMISecurityManager" ), classes );
    }

    @Test
    public void testDeprecatedMethod()
    {
        consumer.consumeLine( "class o/a/m/p/j/its/Deprecations uses deprecated method java/lang/Boolean::<init>(Z)V" );
        
        assertEquals( consumer.getDeprecatedClasses().size(), 0 );
        assertEquals( consumer.getDeprecatedMethods().size(), 1 );
        
        Set<String> methods = consumer.getDeprecatedMethods().get( "o/a/m/p/j/its/Deprecations" );
        assertEquals( Collections.singleton( "java/lang/Boolean::<init>(Z)V" ), methods );
    }
}
