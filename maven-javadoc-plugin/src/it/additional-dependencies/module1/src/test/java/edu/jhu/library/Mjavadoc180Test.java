package edu.jhu.library;

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

import junit.framework.TestCase;
import org.dbunit.database.IDatabaseConnection;

/**
 * Created by IntelliJ IDEA.
 * User: esm
 * Date: May 17, 2008
 * Time: 11:28:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class Mjavadoc180Test
    extends TestCase
{
    /**
     * This is some test javadoc.  This test method has a phony dependency on DB Unit.
     */
    public void testMJAVADOC180()
    {
        IDatabaseConnection phony = null;
        final HelloWorld hw = new HelloWorld();
        assertTrue( "Hello World".equals( hw.hello( "Hello World" ) ) );
    }
}
