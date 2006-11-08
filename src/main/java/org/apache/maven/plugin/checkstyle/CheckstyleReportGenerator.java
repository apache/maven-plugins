package org.apache.maven.plugin.checkstyle;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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

import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Generate a report based on CheckstyleResults.
 */
public class CheckstyleReportGenerator
{
    private Log log;

    private ResourceBundle bundle;

    private Sink sink;

    private SeverityLevel severityLevel;

    private Configuration checkstyleConfig;

    private ModuleFactory checkstyleModuleFactory;

    private boolean enableRulesSummary;

    private boolean enableSeveritySummary;

    private boolean enableFilesSummary;

    private boolean enableRSS;

    private String xrefLocation;

    public CheckstyleReportGenerator( Sink sink, ResourceBundle bundle )
    {
        this.bundle = bundle;

        this.sink = sink;

        this.enableRulesSummary = true;
        this.enableSeveritySummary = true;
        this.enableFilesSummary = true;
        this.enableRSS = true;
    }

    public Log getLog()
    {
        if ( this.log == null )
        {
            this.log = new SystemStreamLog();
        }
        return this.log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    private String getTitle()
    {
        String title;

        if ( getSeverityLevel() == null )
        {
            title = bundle.getString( "report.checkstyle.title" );
        }
        else
        {
            title = bundle.getString( "report.checkstyle.severity_title" ) + severityLevel.getName();
        }

        return title;
    }

    public void generateReport( CheckstyleResults results )
    {
        doHeading();

        if ( getSeverityLevel() == null )
        {
            if ( enableSeveritySummary )
            {
                doSeveritySummary( results );
            }

            if ( enableFilesSummary )
            {
                doFilesSummary( results );
            }

            if ( enableRulesSummary )
            {
                doRulesSummary( results );
            }
        }

        doDetails( results );
        sink.body_();
        sink.flush();
    }

    private void doHeading()
    {
        sink.head();
        sink.title();
        sink.text( getTitle() );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( getTitle() );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( bundle.getString( "report.checkstyle.checkstylelink" ) + " " );
        sink.link( "http://checkstyle.sourceforge.net/" );
        sink.text( "Checkstyle" );
        sink.link_();
        sink.text( "." );

        if ( enableRSS )
        {
            sink.nonBreakingSpace();
            sink.link( "checkstyle.rss" );
            sink.figure();
            sink.figureCaption();
            sink.text( "rss feed" );
            sink.figureCaption_();
            sink.figureGraphics( "images/rss.png" );
            sink.figure_();
            sink.link_();
        }

        sink.paragraph_();
    }

    private void iconSeverity( String level )
    {
        if ( SeverityLevel.INFO.getName().equalsIgnoreCase( level ) )
        {
            iconInfo();
        }
        else if ( SeverityLevel.WARNING.getName().equalsIgnoreCase( level ) )
        {
            iconWarning();
        }
        else if ( SeverityLevel.ERROR.getName().equalsIgnoreCase( level ) )
        {
            iconError();
        }
    }

    private void iconInfo()
    {
        sink.figure();
        sink.figureCaption();
        sink.text( bundle.getString( "report.checkstyle.infos" ) );
        sink.figureCaption_();
        sink.figureGraphics( "images/icon_info_sml.gif" );
        sink.figure_();
    }

    private void iconWarning()
    {
        sink.figure();
        sink.figureCaption();
        sink.text( bundle.getString( "report.checkstyle.warnings" ) );
        sink.figureCaption_();
        sink.figureGraphics( "images/icon_warning_sml.gif" );
        sink.figure_();
    }

    private void iconError()
    {
        sink.figure();
        sink.figureCaption();
        sink.text( bundle.getString( "report.checkstyle.errors" ) );
        sink.figureCaption_();
        sink.figureGraphics( "images/icon_error_sml.gif" );
        sink.figure_();
    }

    private String getConfigAttribute( Configuration config, String attname, String defvalue )
    {
        String ret;
        try
        {
            ret = config.getAttribute( attname );
        }
        catch ( CheckstyleException e )
        {
            ret = defvalue;
        }
        return ret;
    }

    /**
     * Create the rules summary section of the report.
     *
     * @param results The results to summarize
     */
    private void doRulesSummary( CheckstyleResults results )
    {
        if ( checkstyleConfig == null )
        {
            return;
        }

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.rules" ) );
        sink.sectionTitle1_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.rules" ) );
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.violations" ) );
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.column.severity" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        // Top level should be the checker.
        if ( "checker".equalsIgnoreCase( checkstyleConfig.getName() ) )
        {
            doRuleChildren( checkstyleConfig.getChildren(), results );
        }
        else
        {
            sink.tableRow();
            sink.tableCell();
            sink.text( bundle.getString( "report.checkstyle.norule" ) );
            sink.tableCell_();
            sink.tableRow_();
        }

        sink.table_();

        sink.section1_();
    }

    /**
     * Create a summary for each Checkstyle rule.
     *
     * @param configChildren Configurations for each Checkstyle rule
     * @param results The results to summarize
     */
    private void doRuleChildren( Configuration configChildren[], CheckstyleResults results )
    {
        for ( int cci = 0; cci < configChildren.length; cci++ )
        {
            String ruleName = configChildren[cci].getName();

            if ( "TreeWalker".equals( ruleName ) )
            {
                // special sub-case
                doRuleChildren( configChildren[cci].getChildren(), results );
            }
            else
            {
                doRuleRow( configChildren[cci], ruleName, results );
            }
        }
    }

    /**
     * Create a summary for one Checkstyle rule.
     *
     * @param checkerConfig Configuration for the Checkstyle rule
     * @param ruleName The name of the rule, for example "JavadocMethod"
     * @param results The results to summarize
     */
    private void doRuleRow( Configuration checkerConfig, String ruleName, CheckstyleResults results )
    {
        sink.tableRow();
        sink.tableCell();
        sink.text( ruleName );

        List attribnames = new ArrayList( Arrays.asList( checkerConfig.getAttributeNames() ) );
        attribnames.remove( "severity" ); // special value (deserves unique column)
        if ( !attribnames.isEmpty() )
        {
            sink.list();
            Iterator it = attribnames.iterator();
            while ( it.hasNext() )
            {
                sink.listItem();
                String name = (String) it.next();
                sink.bold();
                sink.text( name );
                sink.bold_();

                String value = getConfigAttribute( checkerConfig, name, "" );
                // special case, Header.header and RegexpHeader.header
                if ( "header".equals( name ) && ( "Header".equals( ruleName ) || "RegexpHeader".equals( ruleName ) ) )
                {
                    List lines = stringSplit( value, "\\n" );
                    int linenum = 1;
                    Iterator itl = lines.iterator();
                    while ( itl.hasNext() )
                    {
                        String line = (String) itl.next();
                        sink.lineBreak();
                        sink.rawText( "<span style=\"color: gray\">" );
                        sink.text( linenum + ":" );
                        sink.rawText( "</span>" );
                        sink.nonBreakingSpace();
                        sink.monospaced();
                        sink.text( line );
                        sink.monospaced_();
                        linenum++;
                    }
                }
                else
                {
                    sink.text( ": " );
                    sink.monospaced();
                    sink.text( "\"" );
                    sink.text( value );
                    sink.text( "\"" );
                    sink.monospaced_();
                }
                sink.listItem_();
            }
            sink.list_();
        }

        sink.tableCell_();

        sink.tableCell();
        String fixedmessage = getConfigAttribute( checkerConfig, "message", null );
        // Grab the severity from the rule configuration, use null as default value
        String configSeverity = getConfigAttribute( checkerConfig, "severity", null );
        sink.text( countRuleViolation( results.getFiles().values().iterator(), ruleName, fixedmessage,
                                       configSeverity ) );
        sink.tableCell_();

        sink.tableCell();
        // Grab the severity again from the rule configuration, this time use error as default value
        configSeverity = getConfigAttribute( checkerConfig, "severity", "error" );
        iconSeverity( configSeverity );
        sink.nonBreakingSpace();
        sink.text( StringUtils.capitalise( configSeverity ) );
        sink.tableCell_();

        sink.tableRow_();
    }

    /**
     * Splits a string against a delim consisting of a string (not a single character).
     *
     * @param input
     * @param delim
     * @return
     */
    private List stringSplit( String input, String delim )
    {
        List ret = new ArrayList();

        int delimLen = delim.length();
        int offset = 0;
        int lastOffset = 0;
        String line;

        while ( ( offset = input.indexOf( delim, offset ) ) >= 0 )
        {
            line = input.substring( lastOffset, offset );
            ret.add( line );
            offset += delimLen;
            lastOffset = offset;
        }

        line = input.substring( lastOffset );
        ret.add( line );

        return ret;
    }

    /**
     * Count the number of violations for the given rule.
     *
     * @param files An iterator over the set of files that has violations
     * @param ruleName The name of the rule
     * @param message A message that, if it's not null, will be matched to the message from the violation
     * @param severity A severity that, if it's not null, will be matched to the severity from the violation
     * @return The number of rule violations
     */
    private String countRuleViolation( Iterator files, String ruleName, String message, String severity )
    {
        long count = 0;
        String sourceName;

        try
        {
            sourceName = checkstyleModuleFactory.createModule( ruleName ).getClass().getName();
        }
        catch ( CheckstyleException e )
        {
            getLog().error( "Unable to obtain Source Name for Rule '" + ruleName + "'.", e );
            return "(report failure)";
        }

        while ( files.hasNext() )
        {
            List errors = (List) files.next();

            for ( Iterator error = errors.iterator(); error.hasNext(); )
            {
                AuditEvent event = (AuditEvent) error.next();

                if ( event.getSourceName().equals( sourceName ) )
                {
                    // check message too, for those that have a specific one.
                    // like GenericIllegalRegexp and Regexp
                    if ( message != null )
                    {
                        // event.getMessage() uses java.text.MessageFormat in its implementation.
                        // Read MessageFormat Javadoc about single quote:
                        // http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html
                        String msgWithoutSingleQuote = StringUtils.replace( message, "'", "" );
                        if ( message.equals( event.getMessage() )
                            || msgWithoutSingleQuote.equals( event.getMessage() ) )
                        {
                            count++;
                        }
                    }
                    // Check the severity. This helps to distinguish between
                    // different configurations for the same rule, where each
                    // configuration has a different severity, like JavadocMetod.
                    // See also http://jira.codehaus.org/browse/MCHECKSTYLE-41
                    else if ( severity != null )
                    {
                        if ( severity.equals( event.getSeverityLevel().getName() ) )
                        {
                            count++;
                        }
                    }
                    else
                    {
                        count++;
                    }
                }
            }
        }

        return String.valueOf( count );
    }

    private void doSeveritySummary( CheckstyleResults results )
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.summary" ) );
        sink.sectionTitle1_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.infos" ) );
        sink.nonBreakingSpace();
        iconInfo();
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.warnings" ) );
        sink.nonBreakingSpace();
        iconWarning();
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.errors" ) );
        sink.nonBreakingSpace();
        iconError();
        sink.tableHeaderCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.text( String.valueOf( results.getFileCount() ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( String.valueOf( results.getSeverityCount( SeverityLevel.INFO ) ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( String.valueOf( results.getSeverityCount( SeverityLevel.WARNING ) ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( String.valueOf( results.getSeverityCount( SeverityLevel.ERROR ) ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.table_();

        sink.section1_();
    }

    private void doFilesSummary( CheckstyleResults results )
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.sectionTitle1_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.infos.abbrev" ) );
        sink.nonBreakingSpace();
        iconInfo();
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.warnings.abbrev" ) );
        sink.nonBreakingSpace();
        iconWarning();
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.errors.abbrev" ) );
        sink.nonBreakingSpace();
        iconError();
        sink.tableHeaderCell_();
        sink.tableRow_();

        // Sort the files before writing them to the report
        ArrayList fileList = new ArrayList( results.getFiles().keySet() );
        Collections.sort( fileList );

        for ( Iterator files = fileList.iterator(); files.hasNext(); )
        {
            String filename = (String) files.next();
            List violations = results.getFileViolations( filename );
            if ( violations.isEmpty() )
            {
                // skip files without violations
                continue;
            }

            sink.tableRow();

            sink.tableCell();
            sink.link( "#" + filename.replace( '/', '.' ) );
            sink.text( filename );
            sink.link_();
            sink.tableCell_();

            sink.tableCell();
            sink.text( String.valueOf( results.getSeverityCount( violations, SeverityLevel.INFO ) ) );
            sink.tableCell_();

            sink.tableCell();
            sink.text( String.valueOf( results.getSeverityCount( violations, SeverityLevel.WARNING ) ) );
            sink.tableCell_();

            sink.tableCell();
            sink.text( String.valueOf( results.getSeverityCount( violations, SeverityLevel.ERROR ) ) );
            sink.tableCell_();

            sink.tableRow_();
        }

        sink.table_();
        sink.section1_();
    }

    private void doDetails( CheckstyleResults results )
    {

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.details" ) );
        sink.sectionTitle1_();

        // Sort the files before writing their details to the report
        ArrayList fileList = new ArrayList( results.getFiles().keySet() );
        Collections.sort( fileList );
        Iterator files = fileList.iterator();

        while ( files.hasNext() )
        {
            String file = (String) files.next();
            List violations = results.getFileViolations( file );

            if ( violations.isEmpty() )
            {
                // skip files without violations
                continue;
            }

            sink.section2();
            sink.sectionTitle2();
            sink.anchor( file.replace( '/', '.' ) );
            sink.anchor_();
            sink.text( file );
            sink.sectionTitle2_();

            sink.table();
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.column.violation" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.column.message" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.column.line" ) );
            sink.tableHeaderCell_();
            sink.tableRow_();

            doFileEvents( violations, file );

            sink.table_();
            sink.section2_();
        }

        sink.section1_();
    }

    private void doFileEvents( List eventList, String filename )
    {
        Iterator events = eventList.iterator();
        while ( events.hasNext() )
        {
            AuditEvent event = (AuditEvent) events.next();
            SeverityLevel level = event.getSeverityLevel();

            if ( ( getSeverityLevel() != null ) && !getSeverityLevel().equals( level ) )
            {
                continue;
            }

            sink.tableRow();

            sink.tableCell();

            if ( SeverityLevel.INFO.equals( level ) )
            {
                iconInfo();
            }
            else if ( SeverityLevel.WARNING.equals( level ) )
            {
                iconWarning();
            }
            else if ( SeverityLevel.ERROR.equals( level ) )
            {
                iconError();
            }

            sink.tableCell_();

            sink.tableCell();
            sink.text( event.getMessage() );
            sink.tableCell_();

            sink.tableCell();
            if ( getXrefLocation() != null )
            {
                sink
                    .link(
                        getXrefLocation() + "/" + filename.replaceAll( "\\.java$", ".html" ) + "#" + event.getLine() );
            }
            sink.text( String.valueOf( event.getLine() ) );
            if ( getXrefLocation() != null )
            {
                sink.link_();
            }
            sink.tableCell_();

            sink.tableRow_();
        }
    }

    public SeverityLevel getSeverityLevel()
    {
        return severityLevel;
    }

    public void setSeverityLevel( SeverityLevel severityLevel )
    {
        this.severityLevel = severityLevel;
    }

    public boolean isEnableRulesSummary()
    {
        return enableRulesSummary;
    }

    public void setEnableRulesSummary( boolean enableRulesSummary )
    {
        this.enableRulesSummary = enableRulesSummary;
    }

    public boolean isEnableSeveritySummary()
    {
        return enableSeveritySummary;
    }

    public void setEnableSeveritySummary( boolean enableSeveritySummary )
    {
        this.enableSeveritySummary = enableSeveritySummary;
    }

    public boolean isEnableFilesSummary()
    {
        return enableFilesSummary;
    }

    public void setEnableFilesSummary( boolean enableFilesSummary )
    {
        this.enableFilesSummary = enableFilesSummary;
    }

    public boolean isEnableRSS()
    {
        return enableRSS;
    }

    public void setEnableRSS( boolean enableRSS )
    {
        this.enableRSS = enableRSS;
    }

    public String getXrefLocation()
    {
        return xrefLocation;
    }

    public void setXrefLocation( String xrefLocation )
    {
        this.xrefLocation = xrefLocation;
    }

    public Configuration getCheckstyleConfig()
    {
        return checkstyleConfig;
    }

    public void setCheckstyleConfig( Configuration config )
    {
        this.checkstyleConfig = config;
    }

    public ModuleFactory getCheckstyleModuleFactory()
    {
        return checkstyleModuleFactory;
    }

    public void setCheckstyleModuleFactory( ModuleFactory checkstyleModuleFactory )
    {
        this.checkstyleModuleFactory = checkstyleModuleFactory;
    }
}
