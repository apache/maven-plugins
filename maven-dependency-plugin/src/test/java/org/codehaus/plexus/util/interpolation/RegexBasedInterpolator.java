package org.codehaus.plexus.util.interpolation;

import java.util.List;

/**
 * COPIED FROM plexus-utils-1.5.15 TO SATISFY TESTS
 *
 * @version $Id: RegexBasedInterpolator.java 12174 2010-05-16 21:04:35Z rfscholte $
 * @deprecated Use plexus-interpolation APIs instead.
 */
public class RegexBasedInterpolator
    extends org.codehaus.plexus.interpolation.RegexBasedInterpolator
    implements Interpolator
{
    public RegexBasedInterpolator()
    {
        super();
    }

    public RegexBasedInterpolator( List valueSources )
    {
        super( valueSources );
    }

    public RegexBasedInterpolator( String startRegex,
                                   String endRegex,
                                   List valueSources )
    {
        super( startRegex, endRegex, valueSources );
    }

    public RegexBasedInterpolator( String startRegex,
                                   String endRegex )
    {
        super( startRegex, endRegex );
    }

    public void addValueSource( ValueSource valueSource )
    {
        super.addValueSource( valueSource );
    }

    public void removeValuesSource( ValueSource valueSource )
    {
        super.removeValuesSource( valueSource );
    }
}