package custom.configuration;

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
 * @author Maria Odea Ching
 */
public class AnotherSample
{

    /**
     * The main method
     *
     * @param args  an array of strings that contains the arguments
     */
    public static void main( String[] args )
    {
        System.out.println( "Another Sample Application" );
    }

    /**
     * Unused method
     *
     * @return a blank String
     */
    private String unusedMethod( )
    {
        System.out.println( "This is just a test." );

        return "";
    }

    /**
     *
     * @param tst
     */
    public void sample( String tst )
    {
        if ( tst.equals("") )
            System.out.println( "Empty string." );
        else
            System.out.println( "String is not empty" );
    }

    /**
     * Sample duplicate method
     *
     * @param i
     */
    public void duplicateMethod( int i )
    {
        for( i = 0; i <= 5; i++ )
        {
            System.out.println( "The value of i is " + i );
        }

        i = i + 20;

        System.out.println( "The new value of i is " + i );
    }
    
}