package org.apache.maven.plugins.checkstyle;

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

import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;

/**
 * A little tool to deal with info/warning/error icons in Checkstyle reports, with eventual text.
 *
 * @since 2.13
 */
public class IconTool
{
    public static final String INFO = "info";

    public static final String WARNING = "warning";

    public static final String ERROR = "error";

    public static final int NO_TEXT = 0;
    public static final int TEXT_SIMPLE = 1;
    public static final int TEXT_TITLE = 2;
    public static final int TEXT_ABBREV = 3;

    private final Sink sink;

    private final ResourceBundle bundle;

    public IconTool( Sink sink, ResourceBundle bundle )
    {
        this.sink = sink;
        this.bundle = bundle;
    }

    /**
     * Render a simple icon of given level.
     * @param level one of <code>INFO</code>, <code>WARNING</code> or <code>ERROR</code> constants
     */
    public void iconSeverity( String level )
    {
        sink.figure();
        sink.figureGraphics( "images/icon_" + level + "_sml.gif" );
        sink.figure_();
    }

    /**
     * Render an icon of given level with associated text.
     * @param level one of <code>INFO</code>, <code>WARNING</code> or <code>ERROR</code> constants
     * @param textType one of <code>NO_TEXT</code>, <code>TEXT_SIMPLE</code>, <code>TEXT_TITLE</code> or
     * <code>TEXT_ABBREV</code> constants
     */
    public void iconSeverity( String level, int textType )
    {
        sink.figure();
        sink.figureGraphics( "images/icon_" + level + "_sml.gif" );
        sink.figure_();

        if ( textType > 0 )
        {
            sink.nonBreakingSpace();

            sink.text( bundle.getString( "report.checkstyle." + level + suffix( textType ) ) );
        }
    }

    /**
     * Render an info icon.
     */
    public void iconInfo()
    {
        iconSeverity( INFO );
    }

    /**
     * Render an info icon with associated text.
     * @param textType one of <code>NO_TEXT</code>, <code>TEXT_SIMPLE</code>, <code>TEXT_TITLE</code> or
     * <code>TEXT_ABBREV</code> constants
     */
    public void iconInfo( int textType )
    {
        iconSeverity( INFO, textType );
    }

    /**
     * Render a warning icon.
     */
    public void iconWarning()
    {
        iconSeverity( WARNING );
    }

    /**
     * Render a warning icon with associated text.
     * @param textType one of <code>NO_TEXT</code>, <code>TEXT_SIMPLE</code>, <code>TEXT_TITLE</code> or
     * <code>TEXT_ABBREV</code> constants
     */
    public void iconWarning( int textType )
    {
        iconSeverity( WARNING, textType );
    }

    /**
     * Render an error icon.
     */
    public void iconError()
    {
        iconSeverity( ERROR );
    }

    /**
     * Render an error icon with associated text.
     * @param textType one of <code>NO_TEXT</code>, <code>TEXT_SIMPLE</code>, <code>TEXT_TITLE</code> or
     * <code>TEXT_ABBREV</code> constants
     */
    public void iconError( int textType )
    {
        iconSeverity( ERROR, textType );
    }

    private String suffix( int textType )
    {
        switch ( textType )
        {
            case TEXT_TITLE:
                return "s";
            case TEXT_ABBREV:
                return "s.abbrev";
            default:
                return "";
        }
    }
}
