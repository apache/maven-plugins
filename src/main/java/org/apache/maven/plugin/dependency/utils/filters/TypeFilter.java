/*
 * Copyright Apache Software Foundation
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

package org.apache.maven.plugin.dependency.utils.filters;

import org.apache.maven.artifact.Artifact;

public class TypeFilter extends AbstractArtifactFeatureFilter
{
    public TypeFilter( String include, String exclude )
    {
        super(include, exclude, "Types");
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.dependency.utils.filters.AbstractArtifactFeatureFilter#getArtifactFeature(org.apache.maven.artifact.Artifact)
     */
    protected String getArtifactFeature(Artifact artifact) {
        return artifact.getType();
    }
}
