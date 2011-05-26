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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ManifestResourceTransformer;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.plugins.shade.filter.Filter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;

/**
 * @author Jason van Zyl
 * @plexus.component instantiation-strategy="per-lookup"
 */
public class DefaultShader
    extends AbstractLogEnabled
    implements Shader
{

    public void shade( Set jars, File uberJar, List filters, List relocators, List resourceTransformers )
        throws IOException, MojoExecutionException
    {
        Set resources = new HashSet();

        ResourceTransformer manifestTransformer = null;
        List transformers = new ArrayList( resourceTransformers );
        for ( Iterator it = transformers.iterator(); it.hasNext(); )
        {
            ResourceTransformer transformer = (ResourceTransformer) it.next();
            if ( transformer instanceof ManifestResourceTransformer )
            {
                manifestTransformer = transformer;
                it.remove();
            }
        }

        RelocatorRemapper remapper = new RelocatorRemapper( relocators );

        uberJar.getParentFile().mkdirs();
        JarOutputStream jos = new JarOutputStream( new FileOutputStream( uberJar ) );

        if ( manifestTransformer != null )
        {
            for ( Iterator it = jars.iterator(); it.hasNext(); )
            {
                File jar = (File) it.next();
                JarFile jarFile = newJarFile( jar );
                for ( Enumeration en = jarFile.entries(); en.hasMoreElements(); )
                {
                    JarEntry entry = (JarEntry) en.nextElement();
                    String resource = entry.getName();
                    if ( manifestTransformer.canTransformResource( resource ) )
                    {
                        resources.add( resource );
                        manifestTransformer.processResource( resource, jarFile.getInputStream( entry ), relocators );
                        break;
                    }
                }
            }
            if ( manifestTransformer.hasTransformedResource() )
            {
                manifestTransformer.modifyOutputStream( jos );
            }
        }

        for ( Iterator i = jars.iterator(); i.hasNext(); )
        {
            File jar = (File) i.next();

            getLogger().debug( "Processing JAR " + jar );

            List jarFilters = getFilters( jar, filters );

            JarFile jarFile = newJarFile( jar );

            for ( Enumeration j = jarFile.entries(); j.hasMoreElements(); )
            {
                JarEntry entry = (JarEntry) j.nextElement();

                String name = entry.getName();

                if ( "META-INF/INDEX.LIST".equals( name ) )
                {
                    // we cannot allow the jar indexes to be copied over or the
                    // jar is useless. Ideally, we could create a new one
                    // later
                    continue;
                }

                if ( !entry.isDirectory() && !isFiltered( jarFilters, name ) )
                {
                    InputStream is = jarFile.getInputStream( entry );

                    String mappedName = remapper.map( name );

                    int idx = mappedName.lastIndexOf( '/' );
                    if ( idx != -1 )
                    {
                        // make sure dirs are created
                        String dir = mappedName.substring( 0, idx );
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
                        if ( !resourceTransformed( transformers, mappedName, is, relocators ) )
                        {
                            // Avoid duplicates that aren't accounted for by the resource transformers
                            if ( resources.contains( mappedName ) )
                            {
                                continue;
                            }

                            addResource( resources, jos, mappedName, is );
                        }
                    }

                    IOUtil.close( is );
                }
            }

            jarFile.close();
        }

        for ( Iterator i = transformers.iterator(); i.hasNext(); )
        {
            ResourceTransformer transformer = (ResourceTransformer) i.next();

            if ( transformer.hasTransformedResource() )
            {
                transformer.modifyOutputStream( jos );
            }
        }

        IOUtil.close( jos );

        for ( Iterator it = filters.iterator(); it.hasNext(); )
        {
            Filter filter = (Filter) it.next();
            filter.finished();
        }
    }

    private JarFile newJarFile( File jar )
        throws IOException
    {
        try
        {
            return new JarFile( jar );
        }
        catch ( ZipException zex )
        {
            // JarFile is not very verbose and doesn't tell the user which file it was
            // so we will create a new Exception instead
            throw new ZipException( "error in opening zip file " + jar );
        }
    }

    private List getFilters( File jar, List filters )
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

        // directory entries must end in "/"
        JarEntry entry = new JarEntry( name + "/" );
        jos.putNextEntry( entry );

        resources.add( name );
    }

    private void addRemappedClass( RelocatorRemapper remapper, JarOutputStream jos, File jar, String name,
                                   InputStream is )
        throws IOException, MojoExecutionException
    {
        if ( !remapper.hasRelocators() )
        {
            try
            {
                jos.putNextEntry( new JarEntry( name ) );
                IOUtil.copy( is, jos );
            }
            catch ( ZipException e )
            {
                getLogger().warn( "We have a duplicate " + name + " in " + jar );
            }

            return;
        }

        ClassReader cr = new ClassReader( is );

        ClassWriter cw = new ClassWriter( cr, 0 );

        ClassVisitor cv = new TempRemappingClassAdapter( cw, remapper );

        try {
        	cr.accept( cv, ClassReader.EXPAND_FRAMES );
        } catch ( Throwable ise ) {
        	throw new MojoExecutionException ("Error in ASM processing class "
        			+ name, ise );
        }

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

    private boolean resourceTransformed( List resourceTransformers, String name, InputStream is, List relocators )
        throws IOException
    {
        boolean resourceTransformed = false;

        for ( Iterator k = resourceTransformers.iterator(); k.hasNext(); )
        {
            ResourceTransformer transformer = (ResourceTransformer) k.next();

            if ( transformer.canTransformResource( name ) )
            {
                getLogger().debug( "Transforming " + name + " using " + transformer.getClass().getName() );

                transformer.processResource( name, is, relocators );

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

        private final Pattern classPattern = Pattern.compile( "(\\[*)?L(.+);" );

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
            if ( object instanceof String )
            {
                String name = (String) object;
                String value = name;

                String prefix = "";
                String suffix = "";

                Matcher m = classPattern.matcher( name );
                if ( m.matches() )
                {
                    prefix = m.group( 1 ) + "L";
                    suffix = ";";
                    name = m.group( 2 );
                }

                for ( Iterator i = relocators.iterator(); i.hasNext(); )
                {
                    Relocator r = (Relocator) i.next();

                    if ( r.canRelocateClass( name ) )
                    {
                        value = prefix + r.relocateClass( name ) + suffix;
                        break;
                    }
                    else if ( r.canRelocatePath( name ) )
                    {
                        value = prefix + r.relocatePath( name ) + suffix;
                        break;
                    }
                }

                return value;
            }

            return super.mapValue( object );
        }

        public String map( String name )
        {
            String value = name;

            String prefix = "";
            String suffix = "";

            Matcher m = classPattern.matcher( name );
            if ( m.matches() )
            {
                prefix = m.group( 1 ) + "L";
                suffix = ";";
                name = m.group( 2 );
            }

            for ( Iterator i = relocators.iterator(); i.hasNext(); )
            {
                Relocator r = (Relocator) i.next();

                if ( r.canRelocatePath( name ) )
                {
                    value = prefix + r.relocatePath( name ) + suffix;
                    break;
                }
            }

            return value;
        }

    }

}
