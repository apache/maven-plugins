package org.apache.maven.plugins.ejb;

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

import java.io.File;

/**
 * This class contains some helper methods which do not belong to {@link EjbMojo}.
 * 
 * @author Karl Heinz Marbaise <khmarbaise@apache.org>
 */
public class EjbHelper
{
    /**
     * Check if a <code>classifier</code> is valid or not.
     * 
     * @param classifier The classifier which should be checked.
     * @return true in case of a valid classifier false otherwise.
     */
    public static boolean isClassifierValid( String classifier )
    {
        // @FIXME: Check classifier and clientClassifier for leading "-" ??
        // What are the rules for a valid classifier? Somewhere documented? which can be used as a reference?
        boolean result = false;

        // The following check is only based on an educated guess ;-)
        if ( classifier.matches( "^[a-zA-Z]+[0-9a-zA-Z\\-]*" ) )
        {
            result = true;
        }

        return result;
    }

    /**
     * Check if the given classifier exists in the meaning of not being {@code null} and contain something else than
     * only white spaces.
     * 
     * @param classifier The classifier to be used.
     * @return true in case when the given classifier is not {@code null} and contains something else than white spaces.
     */
    public static boolean hasClassifier( String classifier )
    {
        boolean result = false;
        if ( classifier != null && classifier.trim().length() > 0 )
        {
            result = true;
        }
        return result;
    }

    /**
     * Returns the Jar file to generate, based on an optional classifier.
     *
     * @param basedir the output directory
     * @param finalName the name of the ear file
     * @param classifier an optional classifier
     * @return the file to generate
     */
    public static File getJarFileName( File basedir, String finalName, String classifier )
    {
        StringBuilder fileName = new StringBuilder( finalName );

        if ( hasClassifier( classifier ) )
        {
            fileName.append( "-" ).append( classifier );
        }

        fileName.append( ".jar" );

        return new File( basedir, fileName.toString() );
    }

}
