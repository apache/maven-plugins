
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

try
{

  // Make sure the -classes jar from the first webapp is installed into the local repo
  File localRepoClassesJar = new File( basedir,
      "../../../target/local-repo/com/example/mwar131-webapp/1.0-SNAPSHOT/mwar131-webapp-1.0-SNAPSHOT-classes.jar");

  if ( !localRepoClassesJar.exists() || localRepoClassesJar.isDirectory() )
  {
      System.err.println( "The -classes jar file is missing or is a directory." );
      return false;
  }

  // Make sure the -classes jar is included in WEB-INF/lib of the second webapp
  File classesJar = new File( basedir,
      "mwar131-webapp2/target/mwar131-webapp2/WEB-INF/lib/mwar131-webapp-1.0-SNAPSHOT-classes.jar");

  if ( !classesJar.exists() || classesJar.isDirectory() )
  {
      System.err.println( "The -classes jar file is missing or is a directory." );
      return false;
  }

  // Make sure dependencies of the -classes jar are included in WEB-INF/lib of the second webapp
  File strutsJar = new File( basedir,
      "mwar131-webapp2/target/mwar131-webapp2/WEB-INF/lib/struts-core-1.3.9.jar");

  if ( !strutsJar.exists() || strutsJar.isDirectory() )
  {
      System.err.println( "The Struts 1.3.9 jar file is missing or is a directory." );
      return false;
  }

  // Make sure transitive dependencies of the -classes jar are included in WEB-INF/lib of the second webapp
  File digesterJar = new File( basedir,
      "mwar131-webapp2/target/mwar131-webapp2/WEB-INF/lib/commons-digester-1.8.jar");

  if ( !digesterJar.exists() || digesterJar.isDirectory() )
  {
      System.err.println( "The Commons Digester 1.8 jar file is missing or is a directory." );
      return false;
  }

}
catch( Throwable e )
{
    e.printStackTrace();
    result = false;
}

return result;

