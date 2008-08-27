package org.apache.maven.plugins.help;

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

import junit.framework.Assert;
import junit.framework.TestCase;
import junitx.util.PrivateAccessor;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DescribeMojoTest
    extends TestCase
{
    /**
     * Test method for {@link org.apache.maven.plugins.help.DescribeMojo#toLines(java.lang.String, int, int, int)}.
     *
     * @throws Exception if any
     */
    public void testGetExpressionsRoot()
        throws Exception
    {
        try
        {
            PrivateAccessor.invoke( DescribeMojo.class, "toLines", new Class[] { String.class, Integer.TYPE,
                Integer.TYPE, Integer.TYPE }, new Object[] { "", new Integer( 2 ), new Integer( 2 ),
                new Integer( 80 ) } );
            assertTrue( true );
        }
        catch ( Throwable e )
        {
            Assert.fail( "The API changes" );
        }
    }
}
