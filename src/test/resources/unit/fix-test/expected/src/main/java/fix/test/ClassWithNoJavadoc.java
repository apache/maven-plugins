package fix.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Map;

/**
 * <p>ClassWithNoJavadoc class.</p>
 *
 * @author <a href="mailto:vsiveton@apache.org">vsiveton@apache.org</a>
 * @version $Id: $
 */
public class ClassWithNoJavadoc
    implements InterfaceWithNoJavadoc
{
    /** Constant <code>MY_STRING_CONSTANT="value"</code> */
    public static final String MY_STRING_CONSTANT = "value";

    /** Constant <code>MY_STRING_CONSTANT2="default value"</code> */
    public static final String MY_STRING_CONSTANT2 = "default" + " value";

    /** Constant <code>MY_INT_CONSTANT=1</code> */
    public static final int MY_INT_CONSTANT = 1;

    /** Constant <code>EOL="System.getProperty( line.separator )"</code> */
    public static final String EOL = System.getProperty( "line.separator" );

    // take care of identifier
    private static final String MY_PRIVATE_STRING_CONSTANT = "";

    // QDOX-155
    /** Constant <code>SEPARATOR=','</code> */
    public static final char SEPARATOR = ',';

    // QDOX-156
    /** Constant <code>TEST1="test1"</code> */
    public static final String TEST1 = "test1";

    /**
     * <p>Constructor for ClassWithNoJavadoc.</p>
     */
    public ClassWithNoJavadoc()
    {
    }

    /**
     * <p>Constructor for ClassWithNoJavadoc.</p>
     *
     * @param aString a {@link java.lang.String} object.
     */
    public ClassWithNoJavadoc( String aString )
    {
    }

    // take care of primitive
    /**
     * <p>missingJavadocTagsForPrimitives.</p>
     *
     * @param i a int.
     * @param b a byte.
     * @param f a float.
     * @param c a char.
     * @param s a short.
     * @param l a long.
     * @param d a double.
     * @param bb a boolean.
     */
    public void missingJavadocTagsForPrimitives( int i, byte b, float f, char c, short s, long l, double d, boolean bb )
    {
    }

    // take care of object
    /**
     * <p>missingJavadocTagsForObjects.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param m a {@link java.util.Map} object.
     */
    public void missingJavadocTagsForObjects( String s, Map m )
    {
    }

    // no Javadoc needed
    private void privateMethod( String str )
    {
    }

    // ----------------------------------------------------------------------
    // New methods to be found by Clirr.
    // ----------------------------------------------------------------------

    /**
     * <p>newClassMethod.</p>
     *
     * @param aString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @since 1.1
     */
    public String newClassMethod( String aString )
    {
        return null;
    }

    // ----------------------------------------------------------------------
    // Inheritance
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void missingJavadoc( String aString )
    {
    }

    // take care of identifier
    /** {@inheritDoc} */
    public void missingJavadoc2( String aString )
    {
    }

    /** {@inheritDoc} */
    public String newInterfaceMethod( String aString )
    {
        return null;
    }

    // ----------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------

    //No javadoc for inner class.
    public class InnerClass
    {
        public InnerClass()
        {
        }

        public void nothing()
        {
        }
    }
}
