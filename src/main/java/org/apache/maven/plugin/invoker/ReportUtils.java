package org.apache.maven.plugin.invoker;

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

import java.io.File;
import java.io.FilenameFilter;

/**
 * Provides utility methods for invoker report processing.
 *
 * @author Benjamin Bentmann
 */
class ReportUtils
{

    private static class FileFilterOnlyXmlFile
        implements FilenameFilter
    {

        public boolean accept( File dir, String name )
        {
            return name.startsWith( "BUILD-" ) && name.endsWith( ".xml" );
        }

    }

    /**
     * Gets the paths to the invoker reports available in the specified directory.
     *
     * @param reportsDirectory The base directory where the invoker reports are located in, may be <code>null</code>.
     * @return The paths to the invoker reports, can be empty but never <code>null</code>.
     */
    public static File[] getReportFiles( File reportsDirectory )
    {
        File[] reportFiles =
            ( reportsDirectory != null ) ? reportsDirectory.listFiles( new FileFilterOnlyXmlFile() ) : null;

        if ( reportFiles == null )
        {
            reportFiles = new File[0];
        }

        return reportFiles;
    }

}
