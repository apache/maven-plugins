package org.apache.maven.plugin.jdeps.consumers;

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

import static org.junit.Assert.*;

import org.apache.maven.plugin.jdeps.consumers.JDepsConsumer;
import org.junit.Test;

public class JDepsConsumerTest
{

    private JDepsConsumer consumer;
    
    @Test
    public void testJDKInterAPI()
    {
        
        consumer = new JDepsConsumer();
        consumer.consumeLine( "test-classes -> java.base" );
        consumer.consumeLine( "   <unnamed> (test-classes)" );
        consumer.consumeLine( "      -> java.io                                            " );
        consumer.consumeLine( "      -> java.lang                                          " );
        consumer.consumeLine( "      -> sun.misc                                           JDK internal API (java.base)" );
        
        assertEquals( 1, consumer.getOffendingPackages().size() );
        assertEquals( "JDK internal API (java.base)", consumer.getOffendingPackages().get( "sun.misc" ) );
        assertEquals( 0, consumer.getProfiles().size() );
    }

    @Test
    public void testProfile()
    {
        consumer = new JDepsConsumer();
        consumer.consumeLine( "E:\\java-workspace\\apache-maven-plugins\\maven-jdeps-plugin\\target\\classes -> "
            + "C:\\Program Files\\Java\\jdk1.8.0\\jre\\lib\\rt.jar (compact1)" );
        consumer.consumeLine( "   <unnamed> (classes)" );
        consumer.consumeLine( "      -> java.io                                            compact1" );
        consumer.consumeLine( "      -> java.lang                                          compact1" );
        consumer.consumeLine( "      -> sun.misc                                           JDK internal API (rt.jar)" );
        
        assertEquals( 1, consumer.getOffendingPackages().size() );
        assertEquals( "JDK internal API (rt.jar)", consumer.getOffendingPackages().get( "sun.misc" ) );
        assertEquals( 2, consumer.getProfiles().size() );
        assertEquals( "compact1", consumer.getProfiles().get( "java.io" ) );
        assertEquals( "compact1", consumer.getProfiles().get( "java.lang" ) );
    }

}
