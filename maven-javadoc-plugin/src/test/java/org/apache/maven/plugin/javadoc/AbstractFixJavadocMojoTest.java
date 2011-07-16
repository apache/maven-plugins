package org.apache.maven.plugin.javadoc;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;
import junitx.util.PrivateAccessor;

import com.thoughtworks.qdox.model.AbstractInheritableJavaEntity;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.IndentBuffer;
import com.thoughtworks.qdox.model.JavaClass;

public class AbstractFixJavadocMojoTest
    extends TestCase
{

    public void testReplaceLinkTags_noLinkTag()
        throws Throwable
    {
        String comment = "/** @see ConnectException */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );
        assertEquals( "/** @see ConnectException */", newComment );
    }

    public void testReplaceLinkTags_oneLinkTag()
        throws Throwable
    {
        String comment = "/** {@link ConnectException} */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );
        assertEquals( "/** {@link java.net.ConnectException} */", newComment );
    }

    public void testReplaceLinkTags_missingEndBrace()
        throws Throwable
    {
        String comment = "/** {@link ConnectException */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** {@link ConnectException */", newComment );
    }

    public void testReplaceLinkTags_spacesAfterLinkTag()
        throws Throwable
    {
        String comment = "/** {@link     ConnectException} */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** {@link java.net.ConnectException} */", newComment );
    }

    public void testReplaceLinkTags_spacesAfterClassName()
        throws Throwable
    {
        String comment = "/** {@link ConnectException       } */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** {@link java.net.ConnectException} */", newComment );
    }

    public void testReplaceLinkTags_spacesAfterMethod()
        throws Throwable
    {
        String comment = "/** {@link ConnectException#getMessage()       } */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** {@link java.net.ConnectException#getMessage()} */", newComment );
    }

    public void testReplaceLinkTags_containingHash()
        throws Throwable
    {
        String comment = "/** {@link ConnectException#getMessage()} */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** {@link java.net.ConnectException#getMessage()} */", newComment );
    }

    public void testReplaceLinkTags_followedByHash()
        throws Throwable
    {
        String comment = "/** {@link ConnectException} ##important## */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** {@link java.net.ConnectException} ##important## */", newComment );
    }

    public void testReplaceLinkTags_twoLinks()
        throws Throwable
    {
        String comment = "/** Use {@link ConnectException} instead of {@link Exception} */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        when( clazz.resolveType( "Exception" ) ).thenReturn( "java.lang.Exception" );
        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** Use {@link java.net.ConnectException} instead of {@link java.lang.Exception} */", newComment );
    }

    public void testReplaceLinkTags_OnlyAnchor()
        throws Throwable
    {
        String comment = "/** There's a {@link #getClass()} but no setClass() */";
        AbstractInheritableJavaEntity entity = spy( new PrivateAbstractInheritableJavaEntity() );
        JavaClass clazz = mock( JavaClass.class );
        when( entity.getParentClass() ).thenReturn( clazz );
        when( clazz.resolveType( "ConnectException" ) ).thenReturn( "java.net.ConnectException" );
        when( clazz.resolveType( "Exception" ) ).thenReturn( "java.lang.Exception" );

        String newComment =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "replaceLinkTags", new Class[] {
                String.class, AbstractInheritableJavaEntity.class }, new Object[] { comment, entity } );

        assertEquals( "/** There's a {@link #getClass()} but no setClass() */", newComment );
    }

    protected class PrivateAbstractInheritableJavaEntity
        extends AbstractInheritableJavaEntity
    {
        public int compareTo( Object o )
        {
            return 0;
        }

        @Override
        public DocletTag[] getTagsByName( String arg0, boolean arg1 )
        {
            return null;
        }

        @Override
        protected void writeBody( IndentBuffer arg0 )
        {
        }
    }
}
