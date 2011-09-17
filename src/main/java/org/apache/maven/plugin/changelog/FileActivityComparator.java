package org.apache.maven.plugin.changelog;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.ChangeFile;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * Object used to sort the file-activity report into descending order.
 *
 * @version $Id$
 */
public class FileActivityComparator
    implements Comparator
{
    /**
     * {@inheritDoc}
     */
    public int compare( Object o1, Object o2 )
        throws ClassCastException
    {
        int returnValue;

        List list1 = (List) o1;

        List list2 = (List) o2;

        returnValue = sortByCommits( list1, list2 );

        if ( returnValue != 0 )
        {
            return returnValue;
        }

        returnValue = sortByRevision( list1, list2 );

        if ( returnValue != 0 )
        {
            return returnValue;
        }

        returnValue = sortByName( list1, list2 );

        return returnValue;
    }

    /**
     * compares list1 and list2 by the number of commits
     *
     * @param list1 the first object in a compare statement
     * @param list2 the object to compare list1 against
     * @return an integer describing the order comparison of list1 and list2
     */
    private int sortByCommits( List list1, List list2 )
    {
        if ( list1.size() > list2.size() )
        {
            return -1;
        }

        if ( list1.size() < list2.size() )
        {
            return 1;
        }

        return 0;
    }

    /**
     * compares list1 and list2 by comparing their revision code
     *
     * @param list1 the first object in a compare statement
     * @param list2 the object to compare list1 against
     * @return an integer describing the order comparison of list1 and list2
     */
    private int sortByRevision( List list1, List list2 )
    {
        String revision1 = getLatestRevision( list1 );

        String revision2 = getLatestRevision( list2 );

        if ( revision1 == null )
        {
            return -1;
        }

        if ( revision2 == null )
        {
            return 1;
        }

        return revision1.compareTo( revision2 );
    }

    /**
     * retrieves the latest revision from the commits made from the SCM
     *
     * @param list The list of revisions from the file
     * @return the latest revision code
     */
    private String getLatestRevision( List list )
    {
        String latest = "";

        for ( Iterator i = list.iterator(); i.hasNext(); )
        {
            ChangeFile file = (ChangeFile) i.next();

            if ( StringUtils.isNotBlank( latest) )
            {
                latest = file.getRevision();
            }
            else if ( latest.compareTo( file.getRevision() ) < 0 )
            {
                latest = file.getRevision();
            }
        }

        return latest;
    }

    /**
     * compares list1 and list2 by comparing their filenames. Least priority sorting when both number of commits and
     * and revision are the same
     *
     * @param list1 the first object in a compare statement
     * @param list2 the object to compare list1 against
     * @return an integer describing the order comparison of list1 and list2
     */
    private int sortByName( List list1, List list2 )
    {
        ChangeFile file1 = (ChangeFile) list1.get( 0 );

        ChangeFile file2 = (ChangeFile) list2.get( 0 );

        return file1.getName().compareTo( file2.getName() );
    }
}