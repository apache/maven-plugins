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

public class ClassWithNoJavadoc
    implements InterfaceWithNoJavadoc
{
    public static final String MY_STRING_CONSTANT = "value";

    public static final String MY_STRING_CONSTANT2 = "default" + " value";

    public static final int MY_INT_CONSTANT = 1;

    public static final String EOL = System.getProperty( "line.separator" );

    // take care of identifier
    private static final String MY_PRIVATE_STRING_CONSTANT = "";

    // QDOX-155
    public static final char SEPARATOR = ',';

    // QDOX-156
    public static final String TEST1 = "test1";

    public ClassWithNoJavadoc()
    {
    }

    public ClassWithNoJavadoc( String aString )
    {
    }

    // take care of primitive
    public void missingJavadocTagsForPrimitives( int i, byte b, float f, char c, short s, long l, double d, boolean bb )
    {
    }

    // take care of object
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

    public String newClassMethod( String aString )
    {
        return null;
    }

    // ----------------------------------------------------------------------
    // Inheritance
    // ----------------------------------------------------------------------

    public void missingJavadoc( String aString )
    {
    }

    // take care of identifier
    public void missingJavadoc2( String aString )
    {
    }

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
