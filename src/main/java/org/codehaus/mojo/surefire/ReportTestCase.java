package org.codehaus.mojo.surefire;


/*
 * Copyright 2001-2005 The Codehaus.
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
import java.util.HashMap;

public class ReportTestCase
{
    private String fullName;
    private String name;
    private float time;
    private HashMap failure;

    public ReportTestCase(  )
    {
    }

    public String getName(  )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public float getTime(  )
    {
        return time;
    }

    public void setTime( float time )
    {
        this.time = time;
    }

    public HashMap getFailure(  )
    {
        return failure;
    }

    public void setFailure( HashMap failure )
    {
        this.failure = failure;
    }

    public String getFullName(  )
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }
}
