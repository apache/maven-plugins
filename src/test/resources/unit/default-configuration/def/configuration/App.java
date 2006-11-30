package def.configuration;

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
 * This is a sample class used for testing
 *
 * @author Maria Odea Ching
 */
public class App
{
    protected String unusedVar1;

    private int unusedVar2;

    String unusedvar3;

    /**
     * The main method
     *
     * @param args  an array of strings that contains the arguments
     */
    public static void main( String[] args )
    {
        System.out.println( "Sample Application." );
    }

    /**
     * Sample method
     *
     * @param str   the value to be displayed
     */
    protected void sampleMethod( String str )
    {
        try
        {
            System.out.println( str );
        }
        catch ( Exception e )
        {

        }
    }

    /**
     * Test method
     *
     * @param unusedParam1
     * @param unusedParam2
     */
    public void testMethod( String unusedParam1, String unusedParam2)
    {
        System.out.println( "Test method" );
    }


    public String dup( String str )
    {
        String tmp = "";

        for( int i = 0; i < str.length(); i++ )
        {
            if ( i != ( str.length() -1 ) )
            {
                tmp = tmp + str.substring( i, i + 1);
            }
            else
            {
                tmp = tmp + str.substring( i );
            }
        }

        if ( "".equals( tmp ) || tmp == null )
        {
                tmp = "EMPTY";
        }

        System.out.println( "The value of tmp is " + tmp );
        return tmp;
    }

}