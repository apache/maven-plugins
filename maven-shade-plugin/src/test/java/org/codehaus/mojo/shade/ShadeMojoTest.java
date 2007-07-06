package org.codehaus.mojo.shade;

import org.codehaus.mojo.shade.Shader;
import org.codehaus.mojo.shade.relocation.SimpleRelocator;
import org.codehaus.mojo.shade.resource.ComponentsXmlResourceTransformer;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;


/** @author Jason van Zyl */
public class ShadeMojoTest
    extends PlexusTestCase
{
    public void testShader()
        throws Exception
    {
        Shader s = (Shader) lookup( Shader.ROLE );

        Set set = new HashSet();

        set.add( new File( getBasedir(), "src/test/jars/test-project-1.0-SNAPSHOT.jar" ) );

        set.add( new File( getBasedir(), "src/test/jars/plexus-utils-1.4.1.jar" ) );

        File jar = new File( "target/foo.jar" );

        List relocators = new ArrayList();

        relocators.add( new SimpleRelocator( "org/codehaus/plexus/util", Arrays.asList(
            new String[]{"org/codehaus/plexus/util/xml/Xpp3Dom", "org/codehaus/plexus/util/xml/pull.*"} ) ) );

        List resourceTransformers = new ArrayList();

        resourceTransformers.add( new ComponentsXmlResourceTransformer() );

        s.shade( set, jar, relocators, resourceTransformers );
    }
}
