package org.apache.maven.doxia.module.fo;

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

import org.apache.maven.doxia.sink.Sink;

/**
 * Used to count the position in a numbered list.
 *
 * @author ltheussl
 * @version $Id: NumberedListItem.java 946933 2010-05-21 08:39:07Z ltheussl $
 * @since 1.1
 */
public class NumberedListItem
{

    /** Arabic decimals from 1 - 26. */
    private static final String[] DECIMALS =
    {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
        "21", "22", "23", "24", "25", "26"
    };

    /** Lower-case alphanumerics from a - z. */
    private static final String[] LOWER_ALPHAS =
    {
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
        "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
        "u", "v", "w", "x", "y", "z"
    };

    /** Upper-case alphanumerics from A - Z. */
    private static final String[] UPPER_ALPHAS =
    {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y", "Z"
    };

    /** Lower-case roman numbers from i - xxvi. */
    private static final String[] LOWER_ROMANS =
    {
        "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x",
        "xi", "xii", "xiii", "xiv", "xv", "xvi", "xvii", "xviii", "xix", "xx",
        "xxi", "xxii", "xxiii", "xxiv", "xxv", "xxvi"
    };

    /** Upper-case roman numbers from I - XXVI. */
    private static final String[] UPPER_ROMANS =
    {
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
        "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
        "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI"
    };

    /** The position in the list. */
    private int count;

    /** The numbering format. */
    private final int format;

    /**
     * Constructor. Initializes count and format.
     *
     * @param itemFormat The numbering format of this List.
     * Should be one of the formats defined in {@link org.apache.maven.doxia.sink.Sink}.
     */
    public NumberedListItem( int itemFormat )
    {
        if ( !isValidItemFormat( itemFormat ) )
        {
            throw new IllegalArgumentException( "Unknown item format!" );
        }

        this.format = itemFormat;
        this.count = 0;
    }

    /**
     * Returns the current count, ie the position in the list.
     *
     * @return The current count.
     */
    public int count()
    {
        return count;
    }

    /**
     * Returns the numbering format.
     *
     * @return The numbering format.
     */
    public int format()
    {
        return format;
    }

    /**
     * Increase the current count by 1.
     */
    public void next()
    {
        count++;
    }

    /**
     * Returns the symbol for the current list item.
     *
     * @return The symbol for the current list item.
     */
    public String getListItemSymbol()
    {
        int j = count() - 1;

        if ( j < 0 )
        {
            j = 0;
        }
        else if ( j > DECIMALS.length - 1 )
        {
            j = DECIMALS.length - 1;
        }

        String symbol;

        switch ( format() )
        {
            case Sink.NUMBERING_UPPER_ALPHA:
                symbol = UPPER_ALPHAS[j];
                break;
            case Sink.NUMBERING_LOWER_ALPHA:
                symbol = LOWER_ALPHAS[j];
                break;
            case Sink.NUMBERING_UPPER_ROMAN:
                symbol = UPPER_ROMANS[j];
                break;
            case Sink.NUMBERING_LOWER_ROMAN:
                symbol = LOWER_ROMANS[j];
                break;
            case Sink.NUMBERING_DECIMAL:
            default:
                symbol = DECIMALS[j];
        }

        return symbol + ".";
    }

    /**
     * Determines if the given format is one of the formats defined in
     * {@link org.apache.maven.doxia.sink.Sink}.
     *
     * @param itemFormat the format to check.
     * @return True if the format is a valid item format according to the Sink API.
     */
    private boolean isValidItemFormat( int itemFormat )
    {
        return ( ( itemFormat == Sink.NUMBERING_UPPER_ALPHA )
            || ( itemFormat == Sink.NUMBERING_LOWER_ALPHA )
            || ( itemFormat == Sink.NUMBERING_UPPER_ROMAN )
            || ( itemFormat == Sink.NUMBERING_LOWER_ROMAN )
            || ( itemFormat == Sink.NUMBERING_DECIMAL ) );
    }

}
