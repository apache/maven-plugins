package org.codehaus.mojo.shade.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XmlAppendingTransformer
    implements ResourceTransformer
{
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    
    String resource;
    Document doc;
    
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
        Document r;
        try 
        {
            r = new SAXBuilder().build(is);
        } 
        catch (JDOMException e) 
        {
            throw new RuntimeException(e);
        }
        
        if (doc == null) 
        {
            doc = r;
        } 
        else 
        {
            Element root = r.getRootElement();
            
            for (Iterator itr = root.getAttributes().iterator(); itr.hasNext();)
            {
                Attribute a = (Attribute) itr.next();
                itr.remove();
                
                Element mergedEl = doc.getRootElement();
                Attribute mergedAtt = mergedEl.getAttribute(a.getName(), a.getNamespace());
                if (mergedAtt == null) 
                {
                    mergedEl.setAttribute(a);
                }
            }
            
            for (Iterator itr = root.getChildren().iterator(); itr.hasNext();)
            {
                Content n = (Content) itr.next();
                itr.remove();
                
                doc.getRootElement().addContent(n);
            }
        }
    }

    public boolean hasTransformedResource()
    {
        return true;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        jos.putNextEntry( new JarEntry( resource ) );
        
        new XMLOutputter(Format.getPrettyFormat()).output(doc, jos);
    }
}
