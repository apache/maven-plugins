package fr.husta.test.assembly;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class JarWithDependenciesTest
{

    @Test
    public void checkMetaInfContent() throws IOException
    {
        // check META-INF/services/java.sql.Driver exists
        JarFile jar = new JarFile("target/issue-mvn-assembly-plugin-730-jar-with-dependencies.jar");
        JarEntry entry = jar.getJarEntry("META-INF/services/java.sql.Driver");
        if (entry == null)
        {
            fail("the file 'META-INF/services/java.sql.Driver' should exist in jar-with-dependencies");
        }

        // Content should be "org.postgresql.Driver"
        InputStream is = jar.getInputStream(entry);
        String content = IOUtils.toString(is, "UTF-8");
        System.out.println("JDBC Driver found : " + content.substring(0, content.indexOf("\n")));
        assertEquals("org.postgresql.Driver", content.substring(0, content.indexOf("\n")));

        // if test fails and content == "sun.jdbc.odbc.JdbcOdbcDriver",
        // it means it comes from jre/lib/resources.jar!/META-INF/services/java.sql.Driver (which is unwanted)

    }

}
