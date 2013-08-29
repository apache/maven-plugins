package org.apache.maven.plugin.install;

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

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

public class DualDigesterTest
    extends TestCase
{
    public void testGetMd5()
        throws Exception
    {
        DualDigester dualDigester = new DualDigester();
        dualDigester.calculate( new ByteArrayInputStream( "A Dog And A Cat".getBytes() ) );
        Assert.assertEquals( "39bc6b34be719cab3a3dc922445aae7c", dualDigester.getMd5() );
        Assert.assertEquals( "d07b1e7ecc7986b3f1126ddf1b67e3601ec362a9", dualDigester.getSha1() );
        dualDigester.calculate( new ByteArrayInputStream( "Yep, we do it again".getBytes() ) );
        Assert.assertEquals( "8cd83a9cbbd7076f668c2bcc0379ed49", dualDigester.getMd5() );
        Assert.assertEquals( "194ebcb8d168cffdc25c3b854d6187b568cf6273", dualDigester.getSha1() );
    }

}
