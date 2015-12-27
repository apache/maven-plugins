package org.apache.maven.plugins.ejb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;

public class IncludesExcludesTest
{

    @Test
    public void emptyListsShouldResultInZeroSizeResults()
    {
        IncludesExcludes ie = new IncludesExcludes( Collections.<String>emptyList(), Collections.<String>emptyList(),
                                                    Collections.<String>emptyList(), Collections.<String>emptyList() );

        assertThat( ie.resultingIncludes(), is( new String[0] ) );
        assertThat( ie.resultingExcludes(), is( new String[0] ) );
    }

    @Test
    public void nullForInclucesShouldResultInZeroSizeResults()
    {
        IncludesExcludes ie = new IncludesExcludes( null, Collections.<String>emptyList(),
                                                    Collections.<String>emptyList(), Collections.<String>emptyList() );

        assertThat( ie.resultingIncludes(), is( new String[0] ) );
        assertThat( ie.resultingExcludes(), is( new String[0] ) );
    }

    @Test
    public void nullForExclucesShouldResultInZeroSizeResults()
    {
        IncludesExcludes ie = new IncludesExcludes( Collections.<String>emptyList(), null,
                                                    Collections.<String>emptyList(), Collections.<String>emptyList() );

        assertThat( ie.resultingIncludes(), is( new String[0] ) );
        assertThat( ie.resultingExcludes(), is( new String[0] ) );
    }
}
