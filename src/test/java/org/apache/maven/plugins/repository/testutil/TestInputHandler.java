package org.apache.maven.plugins.repository.testutil;

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

import org.codehaus.plexus.components.interactivity.InputHandler;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

public class TestInputHandler
    implements InputHandler
{
    
    private Stack<String> lineResponses;
    
    private Stack<List<String>> lineListResponses;
    
    private Stack<String> passwordResponses;

    public String readLine()
        throws IOException
    {
        return lineResponses == null || lineResponses.isEmpty() ? null : lineResponses.pop();
    }

    public List<String> readMultipleLines()
        throws IOException
    {
        return lineListResponses == null || lineListResponses.isEmpty() ? null : lineListResponses.pop();
    }

    public String readPassword()
        throws IOException
    {
        return passwordResponses == null || passwordResponses.isEmpty() ? null : passwordResponses.pop();
    }

    public void setLineResponses( Stack<String> responses )
    {
        this.lineResponses = responses;
    }

    public void setLineListResponses( Stack<List<String>> lineLists )
    {
        this.lineListResponses = lineLists;
    }

    public void setPasswordResponses( Stack<String> responses )
    {
        this.passwordResponses = responses;
    }

}
