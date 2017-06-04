package org.apache.maven.plugins.checkstyle;

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

import java.util.ArrayList;
import java.util.List;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

/**
 * AuditListener that forwards events to a list of other AuditListeners
 */
public class CompositeAuditListener
    implements AuditListener
{

    private final List<AuditListener> delegates = new ArrayList<>();

    public void addListener( AuditListener listener )
    {
        delegates.add( listener );
    }

    @Override
    public void auditStarted( AuditEvent event )
    {
        for ( AuditListener listener : delegates )
        {
            listener.auditStarted( event );
        }
    }

    @Override
    public void auditFinished( AuditEvent event )
    {
        for ( AuditListener listener : delegates )
        {
            listener.auditFinished( event );
        }
    }

    @Override
    public void fileStarted( AuditEvent event )
    {
        for ( AuditListener listener : delegates )
        {
            listener.fileStarted( event );
        }
    }

    @Override
    public void fileFinished( AuditEvent event )
    {
        for ( AuditListener listener : delegates )
        {
            listener.fileFinished( event );
        }
    }

    @Override
    public void addError( AuditEvent event )
    {
        for ( AuditListener listener : delegates )
        {
            listener.addError( event );
        }
    }

    @Override
    public void addException( AuditEvent event, Throwable throwable )
    {
        for ( AuditListener listener : delegates )
        {
            listener.addException( event, throwable );
        }
    }

}
