package org.codehaus.mojo.shade;

import org.codehaus.mojo.shade.relocation.Relocator;
import org.codehaus.mojo.shade.resource.ResourceTransformer;
import org.codehaus.plexus.util.IOUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author Jason van Zyl
 * @plexus.component
 */
public class DefaultShader
    implements Shader
{
    public void shade( Set jars,
                       File uberJar,
                       List relocators,
                       List resourceTransformers )
        throws IOException
    {
        Set resources = new HashSet();

        MyRemapper remapper = new MyRemapper( relocators );

        JarOutputStream jos = new JarOutputStream( new FileOutputStream( uberJar ) );

        for ( Iterator i = jars.iterator(); i.hasNext(); )
        {
            File jar = (File) i.next();

            JarFile jarFile = new JarFile( jar );

            for ( Enumeration j = jarFile.entries(); j.hasMoreElements(); )
            {
                JarEntry entry = (JarEntry) j.nextElement();

                String name = entry.getName();

                InputStream is = jarFile.getInputStream( entry );

                if ( entry.isDirectory() )
                {
                    IOUtil.copy( is, jos );
                }
                else
                {
                    if ( name.endsWith( ".class" ) )
                    {
                        ClassReader cr = new ClassReader( is );

                        ClassWriter cw = new ClassWriter( cr, 0 );

                        ClassVisitor cv = new RemappingClassAdapter( cw, remapper );

                        cr.accept( cv, 0 );

                        byte[] renamedClass = cw.toByteArray();

                        // Need to take the .class off for remapping evaluation                        
                        String newName = remapper.map( name.substring( 0, name.indexOf( '.' ) ) );

                        // Now we put it back on so the class file is written out with the right extension.
                        jos.putNextEntry( new JarEntry( newName + ".class" ) );

                        IOUtil.copy( renamedClass, jos );
                    }
                    else
                    {
                        boolean resourceTransformed = false;

                        for ( Iterator k = resourceTransformers.iterator(); k.hasNext(); )
                        {
                            ResourceTransformer transformer = (ResourceTransformer) k.next();

                            if ( transformer.canTransformResource( name ) )
                            {
                                transformer.processResource( is );

                                resourceTransformed = true;

                                break;
                            }
                        }

                        if ( !resourceTransformed )
                        {
                            // Avoid duplicates that aren't accounted for by the resource transformers
                            if ( resources.contains( name ) )
                            {
                                continue;
                            }

                            jos.putNextEntry( new JarEntry( name ) );

                            IOUtil.copy( is, jos );

                            resources.add( name );
                        }
                    }
                }

                IOUtil.close( is );
            }
        }

        for ( Iterator i = resourceTransformers.iterator(); i.hasNext(); )
        {
            ResourceTransformer transformer = (ResourceTransformer) i.next();

            if ( transformer.hasTransformedResource() )
            {
                transformer.modifyOutputStream( jos );
            }
        }

        IOUtil.close( jos );
    }

    class MyRemapper
        extends Remapper
    {
        List relocators;

        public MyRemapper( List relocators )
        {
            this.relocators = relocators;
        }

        public Object mapValue( Object object )
        {
            return object;
        }

        public String map( String name )
        {
            for ( Iterator i = relocators.iterator(); i.hasNext(); )
            {
                Relocator r = (Relocator) i.next();

                if ( r.canRelocate( name ) )
                {
                    return r.relocate( name );
                }
            }

            return name;
        }
    }
}
