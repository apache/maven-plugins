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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.codehaus.plexus.util.FastMap;
import org.xml.sax.SAXException;

/**
 * 
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 28 juil. 2008
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.plugin.changes.schema.ChangesSchemaValidator" role-hint="default"
 */
public class DefaultChangesSchemaValidator
    implements ChangesSchemaValidator
{

    /** property schema */
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";    
    
    public static final String CHANGES_SCHEMA_PATH = "META-INF/changes/xsd/";
    
    private Map compiledSchemas = new FastMap();

    public XmlValidationHandler validateXmlWithSchema( File file, String schemaVersion, boolean failOnValidationError )
        throws SchemaValidatorException
    {
        try
        {
            String schemaPath = CHANGES_SCHEMA_PATH + "changes-" + schemaVersion + ".xsd";
            
            Schema schema = getSchema( schemaPath );
            
            Validator validator = schema.newValidator();
            
            XmlValidationHandler baseHandler = new XmlValidationHandler( failOnValidationError );
            
            validator.setErrorHandler( baseHandler );
            
            validator.validate( new StreamSource( new FileReader( file ) ) );
            
            return baseHandler;
        }
        catch ( IOException e )
        {
            throw new SchemaValidatorException( "IOException : " + e.getMessage(), e );
        }
        catch ( SAXException e )
        {
            throw new SchemaValidatorException( "SAXException : " + e.getMessage(), e );
        }
        catch ( Exception e )
        {
            throw new SchemaValidatorException( "Exception : " + e.getMessage(), e );
        }
    }

    public Schema getSchema( String schemaPath )
        throws SAXException
    {
        if ( this.compiledSchemas.containsKey( schemaPath ) )
        {
            return (Schema) this.compiledSchemas.get( schemaPath );
        }
        Schema schema = this.compileJAXPSchema( schemaPath );
        
        this.compiledSchemas.put( schemaPath, schema );
        
        return schema;
    }

    /**
     * @param uriSchema
     * @return Schema
     * @throws Exception
     */
    private Schema compileJAXPSchema( String uriSchema )
        throws SAXException, NullPointerException
    {

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( uriSchema );

        if ( is == null )
        {
            throw new NullPointerException(" impossible to load schema with path " + uriSchema );
        }

        //newInstance de SchemaFactory not ThreadSafe
        return SchemaFactory.newInstance( W3C_XML_SCHEMA ).newSchema( new StreamSource( is ) );

    }

    /**
     * @see com.accor.commons.xmlschemas.SchemaValidator#loadSchema(java.lang.String)
     */
    public void loadSchema( String uriSchema )
        throws SchemaValidatorException
    {
        try
        {
            this.getSchema( uriSchema );
        }
        catch ( SAXException e )
        {
            throw new SchemaValidatorException( "SAXException : " + e.getMessage(), e );
        }

    }    


}
