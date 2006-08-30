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
 * Class providing utility methods used during the release process
 *
 * @author <a href="mailto:jwhitlock@apache.org">Jeremy Whitlock</a>
 */
public class ReleaseUtils
{
    private ReleaseUtils()
    {
        // nothing to see here
    }

    /**
     * Merge two descriptors together. All SCM settings are overridden by the merge descriptor, as is the
     * <code>workingDirectory</code> field. The <code>completedPhase</code> field is used as
     * a default from the merge descriptor, but not overridden if it exists.
     *
     * @param mergeInto  the descriptor to be merged into
     * @param toBeMerged the descriptor to merge into mergeInto
     * @return ReleaseDescriptor the merged descriptor
     */
    public static ReleaseDescriptor merge( ReleaseDescriptor mergeInto, ReleaseDescriptor toBeMerged )
    {
        // Overridden if configured from the caller
        mergeInto.setScmSourceUrl( mergeOverride( mergeInto.getScmSourceUrl(), toBeMerged.getScmSourceUrl() ) );
        mergeInto.setScmReleaseLabel(
            mergeOverride( mergeInto.getScmReleaseLabel(), toBeMerged.getScmReleaseLabel() ) );
        mergeInto.setScmTagBase( mergeOverride( mergeInto.getScmTagBase(), toBeMerged.getScmTagBase() ) );
        mergeInto.setScmUsername( mergeOverride( mergeInto.getScmUsername(), toBeMerged.getScmUsername() ) );
        mergeInto.setScmPassword( mergeOverride( mergeInto.getScmPassword(), toBeMerged.getScmPassword() ) );
        mergeInto.setScmPrivateKey( mergeOverride( mergeInto.getScmPrivateKey(), toBeMerged.getScmPrivateKey() ) );
        mergeInto.setScmPrivateKeyPassPhrase(
            mergeOverride( mergeInto.getScmPrivateKeyPassPhrase(), toBeMerged.getScmPrivateKeyPassPhrase() ) );
        mergeInto.setAdditionalArguments(
            mergeOverride( mergeInto.getAdditionalArguments(), toBeMerged.getAdditionalArguments() ) );
        mergeInto.setPreparationGoals(
            mergeOverride( mergeInto.getPreparationGoals(), toBeMerged.getPreparationGoals() ) );
        mergeInto.setPomFileName( mergeOverride( mergeInto.getPomFileName(), toBeMerged.getPomFileName() ) );
        mergeInto.setScmUseEditMode( toBeMerged.isScmUseEditMode() );
        mergeInto.setAddSchema( toBeMerged.isAddSchema() );
        mergeInto.setGenerateReleasePoms( toBeMerged.isGenerateReleasePoms() );
        mergeInto.setInteractive( toBeMerged.isInteractive() );

        // These must be overridden, as they are not stored
        mergeInto.setWorkingDirectory(
            mergeOverride( mergeInto.getWorkingDirectory(), toBeMerged.getWorkingDirectory() ) );

        // Not overridden - not configured from caller
        mergeInto.setCompletedPhase( mergeDefault( mergeInto.getCompletedPhase(), toBeMerged.getCompletedPhase() ) );

        return mergeInto;
    }

    private static String mergeOverride( String thisValue, String mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static String mergeDefault( String thisValue, String mergeValue )
    {
        return thisValue != null ? thisValue : mergeValue;
    }
}
