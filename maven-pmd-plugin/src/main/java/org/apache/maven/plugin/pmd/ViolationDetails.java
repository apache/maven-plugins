package org.apache.maven.plugin.pmd;

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

/**
 * Collector of warnings and failures.
 * 
 * @author Robert Scholte
 * @param <D> 
 * @since 2.7
 */
public class ViolationDetails<D>
{
    private List<D> warningDetails = new ArrayList<D>();
    
    private List<D> failureDetails = new ArrayList<D>();

    /**
     * @return the warningDetails, never {@code null}
     */
    public List<D> getWarningDetails()
    {
        return warningDetails;
    }

    /**
     * @param warningDetails the warningDetails to set
     */
    public void setWarningDetails( List<D> warningDetails )
    {
        this.warningDetails = warningDetails;
    }

    /**
     * @return the failureDetails, never {@code null}
     */
    public List<D> getFailureDetails()
    {
        return failureDetails;
    }

    /**
     * @param failureDetails the failureDetails to set
     */
    public void setFailureDetails( List<D> failureDetails )
    {
        this.failureDetails = failureDetails;
    }
}