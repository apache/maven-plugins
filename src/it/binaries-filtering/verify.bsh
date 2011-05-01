
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

import java.io.*;
import org.codehaus.plexus.util.*;

boolean result = true;

filesAreIdentical( File expected, File current )
    throws IOException
{
    if ( expected.length() != current.length() )
    {
        return false;
    }
    FileInputStream expectedIn = new FileInputStream( expected );
    FileInputStream currentIn = new FileInputStream( current );
    try
    {
        byte[] expectedBuffer = IOUtil.toByteArray( expectedIn );

        byte[] currentBuffer = IOUtil.toByteArray( currentIn );
        if (expectedBuffer.length != currentBuffer.length)
        {
            return false;
        }
        for (int i = 0,size = expectedBuffer.length;i<size;i++)
        {
            if(expectedBuffer[i]!= currentBuffer[i])
            {
                return false;
            }
        }
    }
    finally
    {
        expectedIn.close();
        currentIn.close();
    }
    return true;
}

try
{

	File originalImg = new File( basedir, "src/main/resources/happy_duke.gif" );
	
	File targetImg = new File( basedir, "target/classes/happy_duke.gif" );

  boolean identical = filesAreIdentical( originalImg, targetImg );

  if (!identical)
  {
      System.err.println( "filtered images happy_duke.gif are not identical." );
      return false;
  }

  originalImg = new File( basedir, "src/main/resources/duke-beerjpg.img" );

  targetImg = new File( basedir, "target/classes/duke-beerjpg.img" );

  identical = filesAreIdentical( originalImg, targetImg );

  if (!identical)
  {
      System.err.println( "filtered .img duke-beer.jpg.img are not identical." );
      return false;
  }

}
catch( Throwable e )
{
    e.printStackTrace();
    result = false;
}

return result;

