package org.apache.maven.plugin.ear;

import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

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
 * A base class for deployment descriptor file generators.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
abstract class AbstractXmlWriter
{

    private final String encoding;

    AbstractXmlWriter( String encoding )
    {
        this.encoding = encoding;
    }

    protected Writer initializeWriter( final File destinationFile )
        throws EarPluginException
    {
        try
        {
            return new FileWriter( destinationFile );
        }
        catch ( IOException ex )
        {
            throw new EarPluginException( "Exception while opening file[" + destinationFile.getAbsolutePath() + "]",
                                          ex );
        }
    }

    protected XMLWriter initializeXmlWriter( final Writer writer, final String docType )
    {
        return new PrettyPrintXMLWriter( writer, encoding, docType );
    }

    protected void close( Writer closeable )
    {
        if ( closeable == null )
        {
            return;
        }

        try
        {
            closeable.close();
        }
        catch ( Exception e )
        {
            // TODO: warn
        }
    }

    public String getEncoding()
    {
        return encoding;
    }
}
