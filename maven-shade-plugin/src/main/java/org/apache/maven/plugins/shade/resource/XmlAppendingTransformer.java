package org.apache.maven.plugins.shade.resource;

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
        if ( resource != null && resource.equalsIgnoreCase( r ) )
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
            r = new SAXBuilder().build( is );
        }
        catch ( JDOMException e )
        {
            throw new RuntimeException( e );
        }

        if ( doc == null )
        {
            doc = r;
        }
        else
        {
            Element root = r.getRootElement();

            for ( Iterator itr = root.getAttributes().iterator(); itr.hasNext(); )
            {
                Attribute a = (Attribute) itr.next();
                itr.remove();

                Element mergedEl = doc.getRootElement();
                Attribute mergedAtt = mergedEl.getAttribute( a.getName(), a.getNamespace() );
                if ( mergedAtt == null )
                {
                    mergedEl.setAttribute( a );
                }
            }

            for ( Iterator itr = root.getChildren().iterator(); itr.hasNext(); )
            {
                Content n = (Content) itr.next();
                itr.remove();

                doc.getRootElement().addContent( n );
            }
        }
    }

    public boolean hasTransformedResource()
    {
        return doc != null;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        jos.putNextEntry( new JarEntry( resource ) );

        new XMLOutputter( Format.getPrettyFormat() ).output( doc, jos );

        doc = null;
    }
}
