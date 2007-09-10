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

package org.apache.maven.plugins.shade.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class ApacheNoticeResourceTransformer
    implements ResourceTransformer
{
    Set entries = new HashSet();
    String projectName;
    
    String preamble1 = 
          "// ------------------------------------------------------------------\n"
        + "// NOTICE file corresponding to the section 4d of The Apache License,\n"
        + "// Version 2.0, in this case for ";
    
    String preamble2 = "\n// ------------------------------------------------------------------\n\n";
        
    String preamble3 = "This product includes software developed at\n" +
        "Apache Software Foundation (http://www.apache.org/).\n";

    String copyright;
    
    public boolean canTransformResource( String resource )
    {
        String s = resource.toLowerCase();

        if (s.equals( "meta-inf/notice.txt" ) || s.equals( "meta-inf/notice" ) )
        {
            return true;
        }

        return false;
    }

    public void processResource( InputStream is )
        throws IOException
    {
        copyright = projectName + "\nCopyright 2006-2007 Apache Software Foundation\n";
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        String line = reader.readLine();
        StringBuffer sb = new StringBuffer();
        while (line != null) {
            line = line.trim();
            
            if (!line.startsWith("//")) {
                if (line.length() > 0) {
                    sb.append(line).append("\n");
                } else {
                    entries.add(sb.toString());
                    sb = new StringBuffer();
                }
            }
            
            line = reader.readLine();
        }
        
        entries.remove(preamble3);
        entries.remove(copyright);
    }

    public boolean hasTransformedResource()
    {
        return true;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        jos.putNextEntry( new JarEntry( "META-INF/NOTICE" ) );
        
        OutputStreamWriter writer = new OutputStreamWriter(jos);
        writer.write(preamble1);
        writer.write(projectName);
        writer.write(preamble2);
        
        writer.write(copyright);
        writer.write("\n");
        
        writer.write(preamble3);
        writer.write("\n");
        
        for (Iterator itr = entries.iterator(); itr.hasNext();) {
            String line = (String) itr.next();
            writer.append(line);
            writer.append('\n');
        }
        
        writer.flush();
    }
}
