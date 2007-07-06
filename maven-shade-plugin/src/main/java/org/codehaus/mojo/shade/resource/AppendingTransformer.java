package org.codehaus.mojo.shade.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.codehaus.plexus.util.IOUtil;

public class AppendingTransformer
    implements ResourceTransformer
{
    String resource;
    ByteArrayOutputStream data = new ByteArrayOutputStream();
    
    public boolean canTransformResource( String r )
    {
        r = r.toLowerCase();

        if (resource != null && resource.toLowerCase().equals(r))
        {
            return true;
        }

        return false;
    }

    public void processResource( InputStream is )
        throws IOException
    {
        IOUtil.copy(is, data);
        data.write('\n');
        
        is.close();
    }

    public boolean hasTransformedResource()
    {
        return true;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        jos.putNextEntry( new JarEntry( resource ) );
        
        IOUtil.copy(new ByteArrayInputStream(data.toByteArray()), jos);
    }
}
