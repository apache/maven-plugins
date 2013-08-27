package org.apache.maven.plugin.verifier;

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

import org.apache.maven.plugin.logging.Log;

public class ConsoleVerificationResultPrinter
    implements VerificationResultPrinter
{
    private Log log;

    public ConsoleVerificationResultPrinter( Log log )
    {
        this.log = log;
    }

    public void print( VerificationResult results )
    {
        printExistenceFailures( results );
        printNonExistenceFailures( results );
        printContentFailures( results );
    }

    private void printExistenceFailures( VerificationResult results )
    {
        for (Object o : results.getExistenceFailures()) {
            org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) o;

            printMessage("File not found [" + file.getLocation() + "]");
        }
    }

    private void printNonExistenceFailures( VerificationResult results )
    {
        for (Object o : results.getNonExistenceFailures()) {
            org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) o;

            printMessage("File should not exist [" + file.getLocation() + "]");
        }
    }

    private void printContentFailures( VerificationResult results )
    {
        for (Object o : results.getContentFailures()) {
            org.apache.maven.plugin.verifier.model.File file = (org.apache.maven.plugin.verifier.model.File) o;

            printMessage("File [" + file.getLocation() + "] does not match regexp [" + file.getContains() + "]");
        }
    }

    private void printMessage( String message )
    {
        this.log.error( "[Verifier] " + message );
    }
}
