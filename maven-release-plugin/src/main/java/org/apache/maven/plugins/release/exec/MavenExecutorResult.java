package org.apache.maven.plugins.release.exec;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
 * @author Edwin Punzalan
 */
public class MavenExecutorResult
{
    private int exitCode;

    private String sOut;

    private String sErr;

    public int getExitCode()
    {
        return exitCode;
    }

    public void setExitCode( int exitCode )
    {
        this.exitCode = exitCode;
    }

    public String getsOut()
    {
        return sOut;
    }

    public void setsOut( String sOut )
    {
        this.sOut = sOut;
    }

    public String getsErr()
    {
        return sErr;
    }

    public void setsErr( String sErr )
    {
        this.sErr = sErr;
    }
}
