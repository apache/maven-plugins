package org.apache.maven.plugin.ear;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Properties;

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

    /**
     * Builds an EAR and make sure that EAR resources are bundled within the EAR.
     */
    public void testProject012()
        throws Exception
    {
        doTestProject( "project-012", new String[]{"README.txt", "LICENSE.txt", "ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR and make sure that EAR resources in a customized resources directory are bundled within the EAR.
     */
    public void testProject013()
        throws Exception
    {
        doTestProject( "project-013", new String[]{"README.txt", "LICENSE.txt", "ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR and make sure that EAR resources are bundled within the EAR using includes and excludes.
     */
    public void testProject014()
        throws Exception
    {
        doTestProject( "project-014", new String[]{"LICENSE.txt", "ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR and make sure that default manifest is taken into account.
     */
    public void testProject015()
        throws Exception
    {
        final File baseDir = doTestProject( "project-015", new String[]{"ejb-sample-one-1.0.jar"} );
        final File expectedManifest = new File( baseDir, "src/main/application/META-INF/MANIFEST.MF" );
        final File actualManifest = new File( getEarDirectory( baseDir, "project-015" ), "META-INF/MANIFEST.MF" );
        assertTrue( "Manifest was not copied", actualManifest.exists() );
        assertTrue( FileUtils.contentEquals( expectedManifest, actualManifest ) );
    }

    /**
     * Builds an EAR and make sure that custom manifest is taken into account.
     */
    public void testProject016()
        throws Exception
    {
        System.out.println( "Skipped project-016: need a way to extract the EAR archive" );
        /*
        final File baseDir = doTestProject( "project-016", new String[]{"ejb-sample-one-1.0.jar"} );
        final File expectedManifest = new File(baseDir, "src/main/ear/META-INF/MANIFEST.MF");
        // TODO: needs a way to extract the EAR archive
        */
    }

    /**
     * Builds an EAR and make sure that custom application.xml is taken into account.
     */
    public void testProject017()
        throws Exception
    {
        doTestProject( "project-017", new String[]{"ejb-sample-one-1.0.jar"} );
    }

    /**
     * Builds an EAR with a custom final name.
     */
    public void testProject018()
        throws Exception
    {
        final File baseDir = executeMojo( "project-018", new Properties() );
        final File expectedFile = new File( baseDir, "target/my-custom-file.ear" );
        assertTrue( "EAR archive not found", expectedFile.exists() );
    }

    /**
     * Builds an EAR with unpacked archives using the unpackTypes.
     */
    public void testProject019()
        throws Exception
    {
        doTestProject( "project-019", new String[]{"ejb-sample-one-1.0.jar", "sar-sample-one-1.0.sar", "jar-sample-one-1.0.jar"},
                       new boolean[]{false, true, true} );
    }

    /**
     * Builds an EAR with unpacked archives using the unpack module attribute.
     */
    public void testProject020()
        throws Exception
    {
        doTestProject( "project-020", new String[]{"ejb-sample-one-1.0.jar", "sar-sample-one-1.0.sar", "jar-sample-one-1.0.jar"},
                       new boolean[]{true, false, false} );
    }

    /**
     * Builds an EAR with unpacked archives using both unpackTypes and the unpack module attribute.
     */
    public void testProject021()
        throws Exception
    {
        doTestProject( "project-021", new String[]{"ejb-sample-one-1.0.jar", "ejb-sample-two-1.0.jar",
            "sar-sample-one-1.0.sar", "jar-sample-one-1.0.jar", "jar-sample-two-1.0.jar"},
                       new boolean[]{false, true, false, false, true} );
    }

}
