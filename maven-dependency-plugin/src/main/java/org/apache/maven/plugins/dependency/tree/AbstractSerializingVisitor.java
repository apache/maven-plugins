package org.apache.maven.plugins.dependency.tree;

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

import java.io.PrintWriter;
import java.io.Writer;

/**
 * Abstract class for visitors performing serialization.
 *
 * @author <a href="mailto:jerome.creignou@gmail.com">Jerome Creignou</a>
 */
public abstract class AbstractSerializingVisitor
{

    /**
     * The writer to serialize to.
     */
    protected final PrintWriter writer;

    /**
     * Constructor.
     * <p>
     * Build a new AbstractSerializingDependencyNodeVisitor with the writer to serialize to.
     * </p>
     *
     * @param writer the writer to serialize to.
     */
    public AbstractSerializingVisitor( Writer writer )
    {
        if ( writer instanceof PrintWriter )
        {
            this.writer = (PrintWriter) writer;
        }
        else
        {
            this.writer = new PrintWriter( writer, true );
        }
    }
}
