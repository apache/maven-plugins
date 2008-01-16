package org.apache.maven.plugins.shade;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.plugins.shade.filter.Filter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

/**
 * @author Jason van Zyl
 * @plexus.component
 */
public class DefaultShader
    extends AbstractLogEnabled
    implements Shader
{
    public void shade( Set jars, File uberJar, List filters, List relocators, List resourceTransformers )
    throws IOException
    {
        Set resources = new HashSet();

        RelocatorRemapper remapper = new RelocatorRemapper( relocators );

        JarOutputStream jos = new JarOutputStream( new FileOutputStream( uberJar ) );

        for ( Iterator i = jars.iterator(); i.hasNext(); )
        {
            File jar = (File) i.next();

            List jarFilters = getFilters( jar, filters );

            JarFile jarFile = new JarFile( jar );

            for ( Enumeration j = jarFile.entries(); j.hasMoreElements(); )
            {
                JarEntry entry = (JarEntry) j.nextElement();

                String name = entry.getName();
                if ( "META-INF/INDEX.LIST".equals( name ) ) 
                {
                    //we cannot allow the jar indexes to be copied over or the 
                    //jar is useless.   Ideally, we could create a new one
                    //later
                    continue;
                }
                
                String mappedName = remapper.map( name );

                InputStream is = jarFile.getInputStream( entry );
                if ( !entry.isDirectory() && !isFiltered( jarFilters, name ) )
                {
                    int idx = mappedName.lastIndexOf('/');
                    if ( idx != -1 )
                    {
                        //make sure dirs are created
                        String dir = mappedName.substring(0, idx);
                        if ( !resources.contains( dir ) )
                        {
                            addDirectory( resources, jos, dir );
                        }
                    }

                    if ( name.endsWith( ".class" ) )
                    {
                        addRemappedClass( remapper, jos, jar, name, is );
                    }
                    else
                    {
                        if ( !resourceTransformed( resourceTransformers, mappedName, is ) )
                        {
                            // Avoid duplicates that aren't accounted for by the resource transformers
                            if ( resources.contains( mappedName ) )
                            {
                                continue;
                            }

                            addResource( resources, jos, mappedName, is );
                        }
                    }
                }

                IOUtil.close( is );
            }

            jarFile.close();
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

    private List getFilters(File jar, List filters)
    {
        List list = new ArrayList();

        for ( int i = 0; i < filters.size(); i++ )
        {
            Filter filter = (Filter) filters.get( i );

            if ( filter.canFilter( jar ) )
            {
                list.add( filter );
            }

        }

        return list;
    }

    private void addDirectory( Set resources, JarOutputStream jos, String name )
        throws IOException
    {
        if ( name.lastIndexOf( '/' ) > 0 )
        {
            String parent = name.substring( 0, name.lastIndexOf( '/' ) );
            if ( !resources.contains( parent ) )
            {
                addDirectory( resources, jos, parent );
            }
        }
        //directory entries must end in "/"
        JarEntry entry = new JarEntry( name + "/" );
        entry.setMethod( JarEntry.STORED );
        entry.setSize(0);
        entry.setCompressedSize(0);
        entry.setCrc(0);
        jos.putNextEntry( entry );

        resources.add( name );
    }

    private void addRemappedClass( RelocatorRemapper remapper, JarOutputStream jos, File jar, String name, InputStream is )
        throws IOException
    {
        if ( !remapper.hasRelocators() )
        {
            jos.putNextEntry( new JarEntry( name ) );

            IOUtil.copy( is, jos );
            return;
        }

        ClassReader cr = new ClassReader( is );

        ClassWriter cw = new ClassWriter( cr, 0 );

        ClassVisitor cv = new RemappingClassAdapter( cw, remapper );

        cr.accept( cv, ClassReader.EXPAND_FRAMES );

        byte[] renamedClass = cw.toByteArray();

        // Need to take the .class off for remapping evaluation
        String mappedName = remapper.map( name.substring( 0, name.indexOf( '.' ) ) );

        try
        {
            // Now we put it back on so the class file is written out with the right extension.
            jos.putNextEntry( new JarEntry( mappedName + ".class" ) );

            IOUtil.copy( renamedClass, jos );
        }
        catch ( ZipException e )
        {
            getLogger().warn( "We have a duplicate " + mappedName + " in " + jar );
        }
    }

    private boolean isFiltered( List filters, String name )
    {
        for ( int i = 0; i < filters.size(); i++ )
        {
            Filter filter = (Filter) filters.get( i );

            if ( filter.isFiltered( name ) )
            {
                return true;
            }
        }

        return false;
    }

    private boolean resourceTransformed( List resourceTransformers, String name, InputStream is )
        throws IOException
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
        return resourceTransformed;
    }

    private void addResource( Set resources, JarOutputStream jos, String name, InputStream is )
        throws IOException
    {
        jos.putNextEntry( new JarEntry( name ) );

        IOUtil.copy( is, jos );

        resources.add( name );
    }

    class RelocatorRemapper
        extends Remapper
    {
        List relocators;

        public RelocatorRemapper( List relocators )
        {
            this.relocators = relocators;
        }

        public boolean hasRelocators()
        {
            return !relocators.isEmpty();
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
