package org.apache.maven.plugin.ear;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class EarMojoTest
    extends AbstractEarPluginTestCase
{

    /**
     * Builds an EAR with a single EJB and no configuration.
     */
    public void testProject001()
        throws Exception
    {
        doTestProject( "project-001", new String[]{"ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR with a customized artifact location and a customized artifact name.
     */
    public void testProject002()
        throws Exception
    {
        doTestProject( "project-002", new String[]{"APP-INF/lib/ejb-sample-one-1.0.jar", "ejb-sample-two.jar"} );
    }

    /**
     * Builds an EAR with a defalt bundle directory for <tt>java</tt> modules.
     */
    public void testProject003()
        throws Exception
    {
        doTestProject( "project-003", new String[]{"ejb-sample-one-1.0.jar", "APP-INF/lib/jar-sample-one-1.0.jar",
            "APP-INF/lib/jar-sample-two-1.0.jar"} );
    }

    /**
     * Builds an EAR with a defalt bundle directory for _java_ modules and a custom
     * location overriding the default.
     */
    public void testProject004()
        throws Exception
    {
        doTestProject( "project-004", new String[]{"ejb-sample-one-1.0.jar", "jar-sample-one-1.0.jar",
            "APP-INF/lib/jar-sample-two-1.0.jar"} );
    }

    /**
     * Builds an EAR with a custom URI.
     */
    public void testProject005()
        throws Exception
    {
        doTestProject( "project-005", new String[]{"ejb-sample-one-1.0.jar", "libs/another-name.jar"} );
    }

    /**
     * Builds an EAR with an excluded module.
     */
    public void testProject006()
        throws Exception
    {
        doTestProject( "project-006", new String[]{"ejb-sample-one-1.0.jar", "jar-sample-two-1.0.jar"} );
    }

    /**
     * Builds an EAR with a classified artifact and no extra configuration.
     */
    public void testProject007()
        throws Exception
    {
        doTestProject( "project-007", new String[]{"ejb-sample-one-1.0-classified.jar"} );
    }

    /**
     * Builds an EAR with deployment descriptor configuration for J2EE 1.3.
     */
    public void testProject008()
        throws Exception
    {
        doTestProject( "project-008", new String[]{"ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR with deployment descriptor configuration for J2EE 1.4.
     */
    public void testProject009()
        throws Exception
    {
        doTestProject( "project-009", new String[]{"ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR with deployment descriptor configuration for Java EE 5.
     */
    public void testProject010()
        throws Exception
    {
        doTestProject( "project-010", new String[]{"ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR and make sure that deployment descriptor default settings are applied.
     */
    public void testProject011()
        throws Exception
    {
        doTestProject( "project-011", new String[]{"ejb-sample-one-1.0.jar"} );
    }   


    /* Need the embedder for this one since it's new code
   public void testProject04()
       throws Exception
   {
       doTestProject( "project-04", new String[]{"ejb-sample-one-1.0.jar", "sar-sample-one-1.0.sar"},
                      new boolean[]{false, true} );
   } */


}
