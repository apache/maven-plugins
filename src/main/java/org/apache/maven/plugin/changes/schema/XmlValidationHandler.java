package org.apache.maven.plugin.changes.schema;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 28 juil. 2008
 * @version $Id$
 */
public class XmlValidationHandler
    extends DefaultHandler
{
   
    private boolean parsingError = false;

    private List<SAXParseException> errors = new ArrayList<SAXParseException>();

    private List<SAXParseException> fatalErrors = new ArrayList<SAXParseException>();

    private List<SAXParseException> warnings = new ArrayList<SAXParseException>();

    private boolean failOnValidationError;

    /**
     * see name
     */
    public XmlValidationHandler( boolean failOnValidationError )
    {
        this.failOnValidationError = failOnValidationError;
    }

    /**
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error( SAXParseException excp )
        throws SAXException
    {
        this.setErrorParsing( true );
        this.errors.add( excp );
        if ( this.failOnValidationError )
        {
            throw new SAXException( excp.getMessage(), excp );
        }
    }

    /**
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError( SAXParseException excp )
        throws SAXException
    {
        this.fatalErrors.add( excp );
        if ( this.failOnValidationError )
        {
            throw new SAXException( excp.getMessage(), excp );
        }
    }

    /**
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning( SAXParseException excp )
        throws SAXException
    {
        this.warnings.add( excp );
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        // nothing
    }

    /**
     * @return Returns the errorParsing.
     */
    public boolean isErrorParsing()
    {
        return this.parsingError;
    }

    /**
     * @param error The errorParsing to set.
     */
    public void setErrorParsing( boolean error )
    {
        this.parsingError = error;
    }

    public List<SAXParseException> getErrors()
    {
        return errors;
    }

    public void setErrors( List<SAXParseException> errors )
    {
        this.errors = errors;
    }

    public List<SAXParseException> getFatalErrors()
    {
        return fatalErrors;
    }

    public void setFatalErrors( List<SAXParseException> fatalErrors )
    {
        this.fatalErrors = fatalErrors;
    }

    public List<SAXParseException> getWarnings()
    {
        return warnings;
    }

    public void setWarnings( List<SAXParseException> warnings )
    {
        this.warnings = warnings;
    }
}
