package org.codehaus.mojo.shade.resource;

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
