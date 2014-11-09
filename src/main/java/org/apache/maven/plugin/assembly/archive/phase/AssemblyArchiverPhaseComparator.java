package org.apache.maven.plugin.assembly.archive.phase;
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

import java.util.Comparator;

public class AssemblyArchiverPhaseComparator implements Comparator<AssemblyArchiverPhase>
{
    public int compare( AssemblyArchiverPhase o1, AssemblyArchiverPhase o2 )
    {
        boolean o1hasOrder = o1 instanceof PhaseOrder;
        boolean o2hasOrder = o2 instanceof PhaseOrder;
        if (!o1hasOrder && ! o2hasOrder) return o1.getClass().getName().compareTo( o2.getClass().getName() );
        if (!o1hasOrder) return -1;
        if (!o2hasOrder) return +1;
        return Integer.compare( ((PhaseOrder)o1).order(), ((PhaseOrder)o2).order() );
    }
}
