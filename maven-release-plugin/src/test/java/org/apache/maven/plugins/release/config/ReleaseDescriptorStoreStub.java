package org.apache.maven.plugins.release.config;

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
 * Test stub for the release configuration store that holds a single configuration in memory.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseDescriptorStoreStub
    implements ReleaseDescriptorStore
{
    /**
     * The release configuration to use.
     */
    private ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

    public ReleaseDescriptor read( ReleaseDescriptor mergeDescriptor )
    {
        ReleaseUtils.merge( releaseDescriptor, mergeDescriptor );
        return releaseDescriptor;
    }

    public void write( ReleaseDescriptor config )
    {
        this.releaseDescriptor = config;
    }

    public void delete( ReleaseDescriptor config )
    {
    }

    public ReleaseDescriptor getReleaseConfiguration()
    {
        return releaseDescriptor;
    }
}
