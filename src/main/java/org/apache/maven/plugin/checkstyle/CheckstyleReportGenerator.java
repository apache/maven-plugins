package org.apache.maven.plugin.checkstyle;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.StringUtils;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/**
 * Generate a report based on CheckstyleResults.
 *
 * @version $Id$
 */
public class CheckstyleReportGenerator
{
    private Log log;

    private File basedir;

    private ResourceBundle bundle;

    private Sink sink;

    private SeverityLevel severityLevel;

    private Configuration checkstyleConfig;

    private boolean enableRulesSummary;

    private boolean enableSeveritySummary;

    private boolean enableFilesSummary;

    private boolean enableRSS;

    private SiteTool siteTool;

    private String xrefLocation;

    public CheckstyleReportGenerator( Sink sink, ResourceBundle bundle, File basedir, SiteTool siteTool )
    {
        this.bundle = bundle;

        this.sink = sink;

        this.basedir = basedir;

        this.siteTool = siteTool;

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
        sink.close();
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
        sink.section1_();
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

    /**
     * Get the value of the specified attribute from the Checkstyle configuration.
     * If parentConfigurations is non-null and non-empty, the parent
     * configurations are searched if the attribute cannot be found in the
     * current configuration. If the attribute is still not found, the
     * specified default value will be returned.
     *
     * @param config The current Checkstyle configuration
     * @param parentConfigurations The configurations for the parents of the current configuration
     * @param attributeName The name of the attribute
     * @param defaultValue The default value to use if the attribute cannot be found in any configuration
     * @return The value of the specified attribute
     */
    private String getConfigAttribute( Configuration config, List<Configuration> parentConfigurations,
                                       String attributeName, String defaultValue )
    {
        String ret;
        try
        {
            ret = config.getAttribute( attributeName );
        }
        catch ( CheckstyleException e )
        {
            // Try to find the attribute in a parent, if there are any
            if ( parentConfigurations != null && !parentConfigurations.isEmpty() )
            {
                Configuration parentConfiguration = parentConfigurations.get( parentConfigurations.size() - 1 );
                List<Configuration> newParentConfigurations = new ArrayList<Configuration>( parentConfigurations );
                // Remove the last parent
                newParentConfigurations.remove( parentConfiguration );
                ret = getConfigAttribute( parentConfiguration, newParentConfigurations, attributeName, defaultValue );
            }
            else
            {
                ret = defaultValue;
            }
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
            doRuleChildren( checkstyleConfig, null, results );
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
     * @param configuration The Checkstyle configuration
     * @param parentConfigurations A List of configurations for the chain of parents to the current configuration
     * @param results The results to summarize
     */
    private void doRuleChildren( Configuration configuration, List<Configuration> parentConfigurations,
                                 CheckstyleResults results )
    {
        // Remember the chain of parent configurations
        if ( parentConfigurations == null )
        {
            parentConfigurations = new ArrayList<Configuration>();
        }
        // The "oldest" parent will be first in the list
        parentConfigurations.add( configuration );

        if ( getLog().isDebugEnabled() )
        {
            // Log the parent configuration path
            StringBuffer parentPath = new StringBuffer();
            for ( Iterator<Configuration> iterator = parentConfigurations.iterator(); iterator.hasNext(); )
            {
                Configuration parentConfiguration = iterator.next();
                parentPath.append( parentConfiguration.getName() );
                if ( iterator.hasNext() )
                {
                    parentPath.append( " --> " );
                }
            }
            if ( parentPath.length() > 0 )
            {
                getLog().debug( "Parent Configuration Path: " + parentPath.toString() );
            }
        }

        Configuration configChildren[] = configuration.getChildren();
        for ( int cci = 0; cci < configChildren.length; cci++ )
        {
            String ruleName = configChildren[cci].getName();

            if ( "TreeWalker".equals( ruleName ) )
            {
                // special sub-case
                doRuleChildren( configChildren[cci], parentConfigurations, results );
            }
            else
            {
                doRuleRow( configChildren[cci], parentConfigurations, ruleName, results );
            }
        }
    }

    /**
     * Create a summary for one Checkstyle rule.
     *
     * @param checkerConfig Configuration for the Checkstyle rule
     * @param parentConfigurations Configurations for the parents of this rule
     * @param ruleName The name of the rule, for example "JavadocMethod"
     * @param results The results to summarize
     */
    private void doRuleRow( Configuration checkerConfig, List<Configuration> parentConfigurations, String ruleName,
                            CheckstyleResults results )
    {
        sink.tableRow();
        sink.tableCell();
        sink.text( ruleName );

        List<String> attribnames = new ArrayList<String>( Arrays.asList( checkerConfig.getAttributeNames() ) );
        attribnames.remove( "severity" ); // special value (deserves unique column)
        if ( !attribnames.isEmpty() )
        {
            sink.list();
            for ( String name : attribnames )
            {
                sink.listItem();

                sink.bold();
                sink.text( name );
                sink.bold_();

                String value = getConfigAttribute( checkerConfig, null, name, "" );
                // special case, Header.header and RegexpHeader.header
                if ( "header".equals( name ) && ( "Header".equals( ruleName ) || "RegexpHeader".equals( ruleName ) ) )
                {
                    List<String> lines = stringSplit( value, "\\n" );
                    int linenum = 1;
                    for ( String line : lines )
                    {
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
                else if ( "headerFile".equals( name ) && "RegexpHeader".equals( ruleName ) )
                {
                    sink.text( ": " );
                    sink.monospaced();
                    sink.text( "\"" );
                    if ( basedir != null )
                    {
                        // Make the headerFile value relative to ${basedir}
                        String path = siteTool.getRelativePath( value, basedir.getAbsolutePath() );
                        sink.text( path.replace( '\\', '/' ) );
                    }
                    else
                    {
                        sink.text( value );
                    }
                    sink.text( "\"" );
                    sink.monospaced_();
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
        String fixedmessage = getConfigAttribute( checkerConfig, null, "message", null );
        // Grab the severity from the rule configuration, use null as default value
        String configSeverity = getConfigAttribute( checkerConfig, null, "severity", null );
        sink.text( countRuleViolation( results.getFiles().values(), ruleName, fixedmessage,
                                       configSeverity ) );
        sink.tableCell_();

        sink.tableCell();
        // Grab the severity from the rule configuration, this time use error as default value
        // Also pass along all parent configurations, so that we can try to find the severity there
        configSeverity = getConfigAttribute( checkerConfig, parentConfigurations, "severity", "error" );
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
    private List<String> stringSplit( String input, String delim )
    {
        List<String> ret = new ArrayList<String>();

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
     * @param files A collection over the set of files that has violations
     * @param ruleName The name of the rule
     * @param message A message that, if it's not null, will be matched to the message from the violation
     * @param severity A severity that, if it's not null, will be matched to the severity from the violation
     * @return The number of rule violations
     */
    private String countRuleViolation( Collection<List<AuditEvent>> files, String ruleName, String message,
                                       String severity )
    {
        long count = 0;

        for ( List<AuditEvent> errors : files )
        {
            for ( AuditEvent event : errors )
            {
                String eventSrcName = event.getSourceName();
                if ( eventSrcName != null
                        && ( eventSrcName.endsWith( ruleName )
                        || eventSrcName.endsWith( ruleName + "Check" ) ) )
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
        List<String> fileList = new ArrayList<String>( results.getFiles().keySet() );
        Collections.sort( fileList );

        for ( String filename : fileList )
        {
            List<AuditEvent> violations = results.getFileViolations( filename );
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
        List<String> fileList = new ArrayList<String>( results.getFiles().keySet() );
        Collections.sort( fileList );

        for ( String file : fileList )
        {
            List<AuditEvent> violations = results.getFileViolations( file );

            if ( violations.isEmpty() )
            {
                // skip files without violations
                continue;
            }

            sink.section2();
            sink.sectionTitle2();
            sink.text( file );
            sink.sectionTitle2_();

            sink.anchor( file.replace( '/', '.' ) );
            sink.anchor_();

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

    private void doFileEvents( List<AuditEvent> eventList, String filename )
    {
        for ( AuditEvent event : eventList )
        {
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

}
