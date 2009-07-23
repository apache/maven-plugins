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
 * To add default interface tags.
 *
 * @author <a href="mailto:vsiveton@apache.org">vsiveton@apache.org</a>
 * @version $Id: $
 */
public interface InterfaceWithJavadoc
{
    /** default comment */
    String MY_STRING_CONSTANT = "value";

    /**
     * To add default method tags.
     *
     * @param aString a {@link java.lang.String} object.
     */
    public void method1( String aString );

    /**
     * To take care of identifier.
     *
     * @param aString a {@link java.lang.String} object.
     */
    void method2( String aString );

    // one single comment
    /**
     * To take care of single comments.
     *
     * @param aString a string
     * @return null
     */
    // other single comment
    public String method3( String aString );

    /**
     * Nothing.
     *
     * @param aString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String method4( String aString );

    /**
     * Nothing.
     *
     * @param aString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String method5( String aString );

    // ----------------------------------------------------------------------
    // New methods to be found by Clirr.
    // ----------------------------------------------------------------------

    /**
     * New interface method to be found by Clirr.
     *
     * @param aString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @since 1.1
     */
    public String newInterfaceMethod( String aString );
}
