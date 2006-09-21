package org.apache.maven.plugin.ant;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Utility class for the <code>AntBuildWriter</code> class.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntBuildWriterUtil
{
    /**
     * Convenience method to write one CRLF
     *
     * @param writer
     */
    public static void writeLineBreak( XMLWriter writer )
    {
        writeLineBreak( writer, 1 );
    }

    /**
     * Convenience method to repeat <code>CRLF</code>
     *
     * @param writer
     * @param repeat
     */
    public static void writeLineBreak( XMLWriter writer, int repeat )
    {
        for ( int i = 0; i < repeat; i++ )
        {
            writer.writeMarkup( "\n" );
        }
    }

    /**
     * Convenience method to repeat <code>CRLF</code> and to indent the writer
     *
     * @param writer
     * @param repeat
     * @param indent
     */
    public static void writeLineBreak( XMLWriter writer, int repeat, int indent )
    {
        writeLineBreak( writer, repeat );

        if ( indent < 0 )
        {
            indent = 0;
        }

        writer.writeText( StringUtils.repeat( " ", indent * AntBuildWriter.DEFAULT_INDENTATION_SIZE ) );
    }

    /**
     * Convenience method to write XML comment line break. Its size is 80.
     *
     * @param writer
     */
    public static void writeCommentLineBreak( XMLWriter writer )
    {
        writer.writeMarkup( "<!-- " + StringUtils.repeat( "=", 70 ) + " -->\n" );
    }

    /**
     * Convenience method to write XML comment line. The <code>comment</code> is splitted to have a size of 80.
     *
     * @param writer
     * @param comment
     */
    public static void writeComment( XMLWriter writer, String comment )
    {
        if ( comment == null )
        {
            comment = "null";
        }

        String[] words = StringUtils.split( comment, " " );

        StringBuffer line = new StringBuffer( "<!-- " );
        for ( int i = 0; i < words.length; i++ )
        {
            String[] sentences = StringUtils.split( words[i], "\n" );
            if ( sentences.length > 1 )
            {
                for ( int j = 0; j < sentences.length - 1; j++ )
                {
                    line.append( sentences[j] ).append( ' ' );
                    line.append( StringUtils.repeat( " ", 76 - line.length() ) ).append( "-->" ).append( '\n' );
                    writer.writeMarkup( line.toString() );
                    line = new StringBuffer( "<!-- " );
                }
                line.append( sentences[sentences.length - 1] ).append( ' ' );
            }
            else
            {
                StringBuffer sentenceTmp = new StringBuffer( line.toString() );
                sentenceTmp.append( words[i] ).append( ' ' );
                if ( sentenceTmp.length() > 76 )
                {
                    line.append( StringUtils.repeat( " ", 76 - line.length() ) ).append( "-->" ).append( '\n' );
                    writer.writeMarkup( line.toString() );
                    line = new StringBuffer( "<!-- " );
                    line.append( words[i] ).append( ' ' );
                }
                else
                {
                    line.append( words[i] ).append( ' ' );
                }
            }
        }
        if ( line.length() <= 76 )
        {
            line.append( StringUtils.repeat( " ", 76 - line.length() ) ).append( "-->" ).append( '\n' );
        }
        writer.writeMarkup( line.toString() );
    }

    /**
     * Convenience method to write XML comment between two comment line break. The XML comment block is also indented.
     *
     * @param writer
     * @param comment
     * @param indent
     */
    public static void writeCommentText( XMLWriter writer, String comment, int indent )
    {
        if ( indent < 0 )
        {
            indent = 0;
        }

        writeLineBreak( writer, 1 );

        writer.writeMarkup( StringUtils.repeat( " ", indent * AntBuildWriter.DEFAULT_INDENTATION_SIZE ) );
        writeCommentLineBreak( writer );

        writer.writeMarkup( StringUtils.repeat( " ", indent * AntBuildWriter.DEFAULT_INDENTATION_SIZE ) );
        writeComment( writer, "         " + comment );

        writer.writeMarkup( StringUtils.repeat( " ", indent * AntBuildWriter.DEFAULT_INDENTATION_SIZE ) );
        writeCommentLineBreak( writer );

        writeLineBreak( writer, 1, indent );
    }

    /**
     * Convenience method to wrap long element tags for a given attribute.
     *
     * @param writer
     * @param tag
     * @param name
     * @param value
     * @param indent
     */
    public static void addWrapAttribute( XMLWriter writer, String tag, String name, String value, int indent )
    {
        if ( indent < 0 )
        {
            writer.addAttribute( name, value );
        }
        else
        {
            writer.addAttribute( "\n"
                + StringUtils.repeat( " ", ( StringUtils.isEmpty( tag ) ? 0 : tag.length() ) + indent
                    * AntBuildWriter.DEFAULT_INDENTATION_SIZE ) + name, value );
        }
    }
}
