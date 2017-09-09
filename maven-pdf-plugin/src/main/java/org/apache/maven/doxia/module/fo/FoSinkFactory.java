package org.apache.maven.doxia.module.fo;

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

import java.io.Writer;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.sink.impl.AbstractXmlSinkFactory;
import org.codehaus.plexus.component.annotations.Component;

/**
 * FO implementation of the Sink factory.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: FoSinkFactory.java 1726411 2016-01-23 16:34:09Z hboutemy $
 * @since 1.1
 */
@Component( role = SinkFactory.class, hint = "fo" )
public class FoSinkFactory
    extends AbstractXmlSinkFactory
{
    /** {@inheritDoc} */
    protected Sink createSink( Writer writer, String encoding )
    {
        return new FoSink( writer, encoding );
    }

    /** {@inheritDoc} */
    protected Sink createSink( Writer writer, String encoding, String languageId )
    {
        return new FoSink( writer, encoding, languageId );
    }
}
