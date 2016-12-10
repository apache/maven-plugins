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
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.checkstyle.exec.CheckstyleResults;
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

    private final File basedir;

    private final ResourceBundle bundle;

    private final Sink sink;

    private SeverityLevel severityLevel;

    private Configuration checkstyleConfig;

    private boolean enableRulesSummary;

    private boolean enableSeveritySummary;

    private boolean enableFilesSummary;

    private boolean enableRSS;

    private final SiteTool siteTool;

    private String xrefLocation;

    private List<String> treeWalkerNames = Collections.singletonList( "TreeWalker" );

    private final IconTool iconTool;

    private final String ruleset;

    public CheckstyleReportGenerator( Sink sink, ResourceBundle bundle, File basedir, SiteTool siteTool,
                                      String ruleset )
    {
        this.bundle = bundle;

        this.sink = sink;

        this.basedir = basedir;

        this.siteTool = siteTool;

        this.ruleset = ruleset;

        this.enableRulesSummary = true;
        this.enableSeveritySummary = true;
        this.enableFilesSummary = true;
        this.enableRSS = true;
        this.iconTool = new IconTool( sink, bundle );
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
        String version = getCheckstyleVersion();
        if ( version != null )
        {
            sink.text( " " );
            sink.text( version );
        }
        sink.text( " " );
        sink.text( String.format( bundle.getString( "report.checkstyle.ruleset" ), ruleset ) );
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

    /**
     * Get the value of the specified attribute from the Checkstyle configuration.
     * If parentConfigurations is non-null and non-empty, the parent
     * configurations are searched if the attribute cannot be found in the
     * current configuration. If the attribute is still not found, the
     * specified default value will be returned.
     *
     * @param config The current Checkstyle configuration
     * @param parentConfiguration The configuration of the parent of the current configuration
     * @param attributeName The name of the attribute
     * @param defaultValue The default value to use if the attribute cannot be found in any configuration
     * @return The value of the specified attribute
     */
    private String getConfigAttribute( Configuration config, ChainedItem<Configuration> parentConfiguration,
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
            if ( parentConfiguration != null )
            {
                ret =
                    getConfigAttribute( parentConfiguration.value, parentConfiguration.parent, attributeName,
                                        defaultValue );
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
        sink.text( bundle.getString( "report.checkstyle.rule.category" ) );
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.rule" ) );
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
            String category = null;
            for ( ConfReference ref: sortConfiguration( results ) )
            {
                doRuleRow( ref, results, category );

                category = ref.category;
            }
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
     * Create a summary for one Checkstyle rule.
     *
     * @param ref The configuration reference for the row
     * @param results The results to summarize
     * @param previousCategory The previous row's category
     */
    private void doRuleRow( ConfReference ref, CheckstyleResults results, String previousCategory )
    {
        Configuration checkerConfig = ref.configuration;
        ChainedItem<Configuration> parentConfiguration = ref.parentConfiguration;
        String ruleName = checkerConfig.getName();

        sink.tableRow();

        // column 1: rule category
        sink.tableCell();
        String category = ref.category;
        if ( !category.equals( previousCategory ) )
        {
            sink.text( category );
        }
        sink.tableCell_();

        // column 2: Rule name + configured attributes
        sink.tableCell();
        if ( !"extension".equals( category ) )
        {
            sink.link( "http://checkstyle.sourceforge.net/config_" + category + ".html#" + ruleName );
            sink.text( ruleName );
            sink.link_();
        }
        else
        {
            sink.text( ruleName );
        }

        List<String> attribnames = new ArrayList<>( Arrays.asList( checkerConfig.getAttributeNames() ) );
        attribnames.remove( "severity" ); // special value (deserves unique column)
        if ( !attribnames.isEmpty() )
        {
            sink.list();
            for ( String name : attribnames )
            {
                sink.listItem();

                sink.text( name );

                String value = getConfigAttribute( checkerConfig, null, name, "" );
                // special case, Header.header and RegexpHeader.header
                if ( "header".equals( name ) && ( "Header".equals( ruleName ) || "RegexpHeader".equals( ruleName ) ) )
                {
                    String[] lines = StringUtils.split( value, "\\n" );
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

        // column 3: rule violation count
        sink.tableCell();
        sink.text( String.valueOf( ref.violations ) );
        sink.tableCell_();

        // column 4: severity
        sink.tableCell();
        // Grab the severity from the rule configuration, this time use error as default value
        // Also pass along all parent configurations, so that we can try to find the severity there
        String severity = getConfigAttribute( checkerConfig, parentConfiguration, "severity", "error" );
        iconTool.iconSeverity( severity, IconTool.TEXT_SIMPLE );
        sink.tableCell_();

        sink.tableRow_();
    }

    /**
     * Check if a violation matches a rule.
     *
     * @param event the violation to check
     * @param ruleName The name of the rule
     * @param expectedMessage A message that, if it's not null, will be matched to the message from the violation
     * @param expectedSeverity A severity that, if it's not null, will be matched to the severity from the violation
     * @return The number of rule violations
     */
    public boolean matchRule( AuditEvent event, String ruleName, String expectedMessage, String expectedSeverity )
    {
        if ( !ruleName.equals( RuleUtil.getName( event ) ) )
        {
            return false;
        }

        // check message too, for those that have a specific one.
        // like GenericIllegalRegexp and Regexp
        if ( expectedMessage != null )
        {
            // event.getMessage() uses java.text.MessageFormat in its implementation.
            // Read MessageFormat Javadoc about single quote:
            // http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html
            String msgWithoutSingleQuote = StringUtils.replace( expectedMessage, "'", "" );

            return expectedMessage.equals( event.getMessage() ) || msgWithoutSingleQuote.equals( event.getMessage() );
        }
        // Check the severity. This helps to distinguish between
        // different configurations for the same rule, where each
        // configuration has a different severity, like JavadocMetod.
        // See also https://issues.apache.org/jira/browse/MCHECKSTYLE-41
        if ( expectedSeverity != null )
        {
            return expectedSeverity.equals( event.getSeverityLevel().getName() );
        }

        return true;
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
        iconTool.iconInfo( IconTool.TEXT_TITLE );
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        iconTool.iconWarning( IconTool.TEXT_TITLE );
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        iconTool.iconError( IconTool.TEXT_TITLE );
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
        sink.text( bundle.getString( "report.checkstyle.file" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconTool.iconInfo( IconTool.TEXT_ABBREV );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconTool.iconWarning( IconTool.TEXT_ABBREV );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconTool.iconError( IconTool.TEXT_ABBREV );
        sink.tableHeaderCell_();
        sink.tableRow_();

        // Sort the files before writing them to the report
        List<String> fileList = new ArrayList<>( results.getFiles().keySet() );
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
        List<String> fileList = new ArrayList<>( results.getFiles().keySet() );
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
            SinkEventAttributes attrs = new SinkEventAttributeSet();
            attrs.addAttribute( SinkEventAttributes.ID, file.replace( '/', '.' ) );
            sink.sectionTitle( Sink.SECTION_LEVEL_2, attrs );
            sink.text( file );
            sink.sectionTitle_( Sink.SECTION_LEVEL_2 );

            sink.table();
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.column.severity" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.rule.category" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.rule" ) );
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

            if ( ( getSeverityLevel() != null ) && !( getSeverityLevel() != level ) )
            {
                continue;
            }

            sink.tableRow();

            sink.tableCell();
            iconTool.iconSeverity( level.getName(), IconTool.TEXT_SIMPLE );
            sink.tableCell_();

            sink.tableCell();
            String category = RuleUtil.getCategory( event );
            if ( category != null )
            {
                sink.text( category );
            }
            sink.tableCell_();

            sink.tableCell();
            String ruleName = RuleUtil.getName( event );
            if ( ruleName != null )
            {
                sink.text( ruleName );
            }
            sink.tableCell_();

            sink.tableCell();
            sink.text( event.getMessage() );
            sink.tableCell_();

            sink.tableCell();

            int line = event.getLine();
            if ( getXrefLocation() != null && line != 0 )
            {
                sink.link( getXrefLocation() + "/" + filename.replaceAll( "\\.java$", ".html" ) + "#L"
                    + line );
                sink.text( String.valueOf( line ) );
                sink.link_();
            }
            else if ( line != 0 )
            {
                sink.text( String.valueOf( line ) );
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

    public void setTreeWalkerNames( List<String> treeWalkerNames )
    {
        this.treeWalkerNames = treeWalkerNames;
    }

    public List<String> getTreeWalkerNames()
    {
        return treeWalkerNames;
    }

    /**
     * Get the effective Checkstyle version at runtime.
     * @return the MANIFEST implementation version of Checkstyle API package (can be <code>null</code>)
     */
    private String getCheckstyleVersion()
    {
        Package checkstyleApiPackage = Configuration.class.getPackage();

        return ( checkstyleApiPackage == null ) ? null : checkstyleApiPackage.getImplementationVersion();
    }

    public List<ConfReference> sortConfiguration( CheckstyleResults results )
    {
        List<ConfReference> result = new ArrayList<>();

        sortConfiguration( result, checkstyleConfig, null, results );

        Collections.sort( result );

        return result;
    }

    private void sortConfiguration( List<ConfReference> result, Configuration config,
                                    ChainedItem<Configuration> parent, CheckstyleResults results )
    {
        for ( Configuration childConfig : config.getChildren() )
        {
            String ruleName = childConfig.getName();

            if ( treeWalkerNames.contains( ruleName ) )
            {
                // special sub-case: TreeWalker is the parent of multiple rules, not an effective rule
                sortConfiguration( result, childConfig, new ChainedItem<>( config, parent ), results );
            }
            else
            {
                String fixedmessage = getConfigAttribute( childConfig, null, "message", null );
                // Grab the severity from the rule configuration, use null as default value
                String configSeverity = getConfigAttribute( childConfig, null, "severity", null );

                // count rule violations
                long violations = 0;
                AuditEvent lastMatchedEvent = null;
                for ( List<AuditEvent> errors : results.getFiles().values() )
                {
                    for ( AuditEvent event : errors )
                    {
                        if ( matchRule( event, ruleName, fixedmessage, configSeverity ) )
                        {
                            lastMatchedEvent = event;
                            violations++;
                        }
                    }
                }

                if ( violations > 0 ) // forget rules without violations
                {
                    String category = RuleUtil.getCategory( lastMatchedEvent );

                    result.add( new ConfReference( category, childConfig, parent, violations, result.size() ) );
                }
            }
        }
    }

    private static class ConfReference
        implements Comparable<ConfReference>
    {
        private final String category;
        private final Configuration configuration;
        private final ChainedItem<Configuration> parentConfiguration;
        private final long violations;
        private final int count;

        public ConfReference( String category, Configuration configuration,
                              ChainedItem<Configuration> parentConfiguration, long violations, int count )
        {
            this.category = category;
            this.configuration = configuration;
            this.parentConfiguration = parentConfiguration;
            this.violations = violations;
            this.count = count;
        }

        public int compareTo( ConfReference o )
        {
            int compare = category.compareTo( o.category );
            if ( compare == 0 )
            {
                compare = configuration.getName().compareTo( o.configuration.getName() );
            }
            return ( compare == 0 ) ? ( o.count - count ) : compare;
        }
    }

    private static class ChainedItem<T>
    {
        private final ChainedItem<T> parent;

        private final T value;

        public ChainedItem( T value, ChainedItem<T> parent )
        {
            this.parent = parent;
            this.value = value;
        }
    }

}
