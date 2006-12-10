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

// java imports

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Change log task. It uses a ChangeLogGenerator and ChangeLogParser to create
 * a Collection of ChangeLogEntry objects, which are used to produce an XML
 * output that represents the list of changes.
 *
 * @author <a href="mailto:glenn@somanetworks.com">Glenn McAllister</a>
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @author <a href="mailto:dion@multitask.com.au">dIon Gillard</a>
 * @author <a href="mailto:bodewig@apache.org">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Id:ChangeLog.java 437762 2006-08-28 19:29:49 +0200 (må, 28 aug 2006) dennisl $
 */
public class ChangeLog
{
    /**
     * parses a previously generated changelog xml document and return its changed sets
     *
     * @param stream the changelog xml document
     * @return changelog sets parsed from the xml document
     * @throws ParserConfigurationException when instantiation of the SAX parser failed
     * @throws SAXException                 when an error occurred while parsing the xml document
     * @throws IOException                  when an error occurred while accessing the xml document
     */
    public static List loadChangedSets( InputStream stream )
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

        List changeLogSets = new ArrayList();

        parser.parse( stream, new ChangeLogHandler( changeLogSets ) );

        return changeLogSets;
    }
} // end of ChangeLog
