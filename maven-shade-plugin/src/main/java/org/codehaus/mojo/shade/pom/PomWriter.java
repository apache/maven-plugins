package org.codehaus.mojo.shade.pom;

import org.apache.maven.model.Model;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.Element;
import org.jdom.output.Format;
import org.codehaus.mojo.shade.pom.MavenJDOMWriter;

import java.io.Writer;
import java.io.IOException;

/** @author Jason van Zyl */
public class PomWriter
{
    public static void write( Writer w,
                              Model newModel )
        throws IOException
        {
        write( w, newModel, false );
    }

    public static void write( Writer w,
                              Model newModel,
                              boolean namespaceDeclaration )
        throws IOException
    {
        Element root = new Element( "project" );

        if ( namespaceDeclaration )
        {
            String modelVersion = newModel.getModelVersion();

            Namespace pomNamespace = Namespace.getNamespace( "", "http://maven.apache.org/POM/" + modelVersion );

            root.setNamespace( pomNamespace );

            Namespace xsiNamespace = Namespace.getNamespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );

            root.addNamespaceDeclaration( xsiNamespace );

            if ( root.getAttribute( "schemaLocation", xsiNamespace ) == null )
            {
                root.setAttribute( "schemaLocation", "http://maven.apache.org/POM/" + modelVersion +
                    " http://maven.apache.org/maven-v" + modelVersion.replace( '.', '_' ) + ".xsd", xsiNamespace );
            }
        }

        Document doc = new Document( root );

        MavenJDOMWriter writer = new MavenJDOMWriter();

        String encoding = newModel.getModelEncoding() != null ? newModel.getModelEncoding() : "UTF-8";

        Format format = Format.getPrettyFormat().setEncoding( encoding );

        writer.write( newModel, doc, w, format );
    }    
}
