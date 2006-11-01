package org.apache.maven.plugin.pmd;

import java.util.Iterator;
import java.util.ResourceBundle;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.cpd.Match;

import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

/**
 * Class that generated the CPD report.
 *
 * @author mperham
 * @version $Id: $
 */
public class CpdReportGenerator
{
    private Sink sink;

    private String sourceDirectory;

    private ResourceBundle bundle;

    private String xrefLocation;

    public CpdReportGenerator( Sink sink, String sourceDirectory, ResourceBundle bundle, String xrefLocation )
    {
        this.sink = sink;
        this.sourceDirectory = sourceDirectory;
        this.bundle = bundle;
        this.xrefLocation = xrefLocation;
    }

    /**
     * Method that returns the title of the CPD Report
     *
     * @return a String that contains the title
     */
    private String getTitle()
    {
        return bundle.getString( "report.cpd.title" );
    }

    /**
     * Method that generates the start of the CPD report.
     */
    public void beginDocument()
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
        sink.text( bundle.getString( "report.cpd.cpdlink" ) + " " );
        sink.link( "http://pmd.sourceforge.net/cpd.html" );
        sink.text( "CPD" );
        sink.link_();
        sink.text( " " + PMD.VERSION + "." );
        sink.paragraph_();

        // TODO overall summary

        sink.section1_();
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.cpd.dupes" ) );
        sink.sectionTitle1_();

        // TODO files summary
    }

    /**
     * Method that generates the contents of the CPD report
     *
     * @param matches
     */
    public void generate( Iterator matches )
    {
        beginDocument();

        if ( !matches.hasNext() )
        {
            sink.text( "CPD found no problems in your source code." );            
        }

        while ( matches.hasNext() )
        {
            Match match = (Match) matches.next();
            String filename1 = match.getFirstMark().getTokenSrcID();
            filename1 = StringUtils.substring( filename1, sourceDirectory.length() + 1 );

            String filename2 = match.getSecondMark().getTokenSrcID();
            filename2 = StringUtils.substring( filename2, sourceDirectory.length() + 1 );

            String code = match.getSourceCodeSlice();
            int line1 = match.getFirstMark().getBeginLine();
            int line2 = match.getSecondMark().getBeginLine();

            sink.paragraph();
            sink.table();
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.cpd.column.file" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.cpd.column.line" ) );
            sink.tableHeaderCell_();
            sink.tableRow_();

            // File 1
            sink.tableRow();
            sink.tableCell();
            sink.text( filename1 );
            sink.tableCell_();
            sink.tableCell();

            if ( xrefLocation != null )
            {
                sink.link( xrefLocation + "/" + filename1.replaceAll( "\\.java$", ".html" ).replace( '\\', '/' ) + "#" +
                    line1 );
            }
            sink.text( String.valueOf( line1 ) );
            if ( xrefLocation != null )
            {
                sink.link_();
            }

            sink.tableCell_();
            sink.tableRow_();

            // File 2
            sink.tableRow();
            sink.tableCell();
            sink.text( filename2 );
            sink.tableCell_();
            sink.tableCell();
            if ( xrefLocation != null )
            {
                sink.link( xrefLocation + "/" + filename2.replaceAll( "\\.java$", ".html" ).replace( '\\', '/' ) + "#" +
                    line2 );
            }
            sink.text( String.valueOf( line2 ) );
            if ( xrefLocation != null )
            {
                sink.link_();
            }
            sink.tableCell_();
            sink.tableRow_();

            // Source snippet
            sink.tableRow();

            // TODO Cleaner way to do this?
            sink.rawText( "<td colspan='2'>" );
            sink.verbatim( false );
            sink.text( code );
            sink.verbatim_();
            sink.rawText( "</td>" );
            sink.tableRow_();
            sink.table();
            sink.paragraph_();
        }

        sink.section1_();
        sink.body_();
        sink.flush();
        sink.close();
    }
}
