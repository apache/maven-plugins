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

package org.apache.maven.plugin.eclipse;

import static org.junit.Assert.assertEquals;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

public class LinkedResourceTest
{
    @Test
    public void nodeWithOnlyLocationIsAccepted()
    {
        Xpp3Dom node = new Xpp3Dom("test");
        node.addChild(new Xpp3Dom("name"));
        node.addChild(new Xpp3Dom("type"));

        Xpp3Dom location = new Xpp3Dom("location");
        location.setValue("the-location");

        node.addChild(location);

        LinkedResource lr = new LinkedResource(node);
        assertEquals("the-location", lr.getLocation());
    }

    @Test
    public void nodeWithOnlyLocationUriIsAccepted()
    {
        Xpp3Dom node = new Xpp3Dom("test");
        node.addChild(new Xpp3Dom("name"));
        node.addChild(new Xpp3Dom("type"));

        Xpp3Dom location = new Xpp3Dom("locationURI");
        location.setValue("the-location-uri");

        node.addChild(location);

        LinkedResource lr = new LinkedResource(node);
        assertEquals("the-location-uri", lr.getLocationURI());
    }
}
