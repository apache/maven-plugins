package org.codehaus.mojo.shade;

import java.util.Set;
import java.util.List;
import java.io.IOException;
import java.io.File;

/** @author Jason van Zyl */
public interface Shader
{
    String ROLE = Shader.class.getName();

    public void shade( Set jars,
                       File uberJar,
                       List relocators,
                       List resourceTransformers )
        throws IOException;
}
