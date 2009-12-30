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

import org.apache.maven.component.api.*;

import org.codehaus.plexus.*;

public class Main
{

    public static void main( String[] args )
        throws Exception
    {
        DefaultPlexusContainer container = new DefaultPlexusContainer();
        container.initialize();
        container.start();
        
        Component comp = (Component) container.lookup( Component.class.getName(), "test" );
        System.out.println( comp.getId() );
        if ( !"test-default".equals( comp.getId() ) )
        {
            throw new IllegalStateException( "bad component " + comp );
        }
    }

}
