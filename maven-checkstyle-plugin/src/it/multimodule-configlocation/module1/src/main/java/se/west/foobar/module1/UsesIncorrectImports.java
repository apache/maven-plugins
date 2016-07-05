package se.west.foobar.module1;

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

import java.util.Date;

/**
 * Class which uses an incorrect import, namely <code>java.util.Date</code>.
 */
public class UsesIncorrectImports
{
    private Date startOfDates;

    public UsesIncorrectImports()
    {
        this.startOfDates = new Date(0L);
    }

    /**
     * @return Retrieves a {@link Date} referring to the 1 of January 1970 (i.e. the "start of Dates" timestamp).
     */
    public Date getStartOfDates()
    {
        return startOfDates;
    }
}
