package org.apache.maven.plugin.idea;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.apache.maven.plugin.idea.stubs.TestCounter;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractIdeaTestCase
    extends AbstractMojoTestCase
{
    protected IdeaProjectMojo mojo;

    protected Document executeMojo( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), pluginXml );

        mojo = (IdeaProjectMojo) lookupMojo( "project", pluginXmlFile );

        assertNotNull( "Get project mojo instance using " + pluginXmlFile.getAbsolutePath() , mojo );

        mojo.execute();

        int testCounter = TestCounter.currentCount();

        File iprFile = new File( "target/test-harness/" + testCounter + "/plugin-test-" + testCounter + ".ipr" );

        assertTrue( "Project file was created", iprFile.exists() );

        return readXmlDocument( iprFile );
    }

    protected void testJdkName( Document document, String jdkLevel, String expected )
        throws Exception
    {
        Element root = document.getRootElement();

        Element component = findComponent( root, "ProjectRootManager" );

        String jdkName = component.attributeValue( "project-jdk-name" );

        if ( jdkLevel == null )
        {
            jdkLevel = System.getProperty( "java.specification.version" );
        }

        if ( jdkLevel.startsWith( "1.4" ) )
        {
            assertEquals( "assert-keyword must be true for jdk 1.4",
                          "true", component.attributeValue( "assert-keyword" ) );

            assertEquals( "jdk-15 must be false for jdk 1.4",
                          "false", component.attributeValue( "jdk-15") );
        }
        else if ( jdkLevel.compareTo( "1.5" ) >= 0 )
        {
            assertEquals( "assert-keyword must be true for jdk >= 1.5",
                          "true", component.attributeValue( "assert-keyword" ) );

            assertEquals( "jdk-15 must be true for jdk >= 1.5",
                          "true", component.attributeValue( "jdk-15") );
        }
        else
        {
            assertEquals( "assert-keyword must be true for jdk >= 1.5",
                          "false", component.attributeValue( "assert-keyword" ) );
        }

        if ( expected != null )
        {
            assertEquals( "Expected jdkName test", jdkName, expected );
        }
    }

    protected Document readXmlDocument( File xmlFile )
        throws DocumentException
    {
        SAXReader reader = new SAXReader();

        return reader.read( xmlFile );
    }

    protected Element findComponent( Element module, String name )
        throws Exception
    {
        return findElementByNameAttribute( module, "component", name );
    }

    protected Element findElementByNameAttribute( Element element, String elementName, String nameAttribute )
        throws Exception
    {
        Element e = null;

        for ( Iterator children = element.elementIterator( elementName ); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if ( nameAttribute == null )
            {
                e = child;
            }
            else if ( nameAttribute.equals( child.attributeValue( "name" ) ) )
            {
                e = child;
            }
        }

        if ( e == null)
        {
            if ( nameAttribute == null )
            {
                fail( "Element " + elementName + " not found." );
            }
            else
            {
                fail( "Attribute " + nameAttribute + " not found in elements " + elementName + "." );
            }
        }

        return e;
    }

    protected List findElementsByName( Element element, String elementName )
    {
        return element.elements( elementName );
    }
}
