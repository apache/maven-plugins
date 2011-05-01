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

/**
 * <p>InterfaceWithNoJavadoc interface.</p>
 *
 * @author <a href="mailto:vsiveton@apache.org">vsiveton@apache.org</a>
 * @version $Id: $
 */
public interface InterfaceWithNoJavadoc
{
    /** Constant <code>MY_STRING_CONSTANT="value"</code> */
    String MY_STRING_CONSTANT = "value";

    /**
     * <p>missingJavadoc.</p>
     *
     * @param aString a {@link java.lang.String} object.
     */
    public void missingJavadoc( String aString );

    // take care of identifier
    /**
     * <p>missingJavadoc2.</p>
     *
     * @param aString a {@link java.lang.String} object.
     */
    void missingJavadoc2( String aString );

    // ----------------------------------------------------------------------
    // New methods to be found by Clirr.
    // ----------------------------------------------------------------------

    /**
     * <p>newInterfaceMethod.</p>
     *
     * @param aString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @since 1.1
     */
    public String newInterfaceMethod( String aString );
}
