package org.apache.maven.plugin.trac;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;

/**
 * Generates a Trac report.
 *
 * @author Noriko Kinugasa
 * @version $Id$
 */
public class TracReportGenerator
{

    public TracReportGenerator()
    {
        // nothing here
    }

    public void doGenerateEmptyReport( ResourceBundle bundle, Sink sink )
    {
        sinkBeginReport( sink, bundle );

        sink.paragraph();

        sink.text( bundle.getString( "report.trac.error" ) );

        sink.paragraph_();

        sinkEndReport( sink );
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink, ArrayList ticketList )
    {

        sinkBeginReport( sink, bundle );

        constructHeaderRow( sink, ticketList, bundle );

        constructDetailRows( sink, ticketList, bundle );

        sinkEndReport( sink );
    }

    private void constructHeaderRow( Sink sink, List ticketList, ResourceBundle bundle )
    {
        if ( ticketList == null )
        {
            return;
        }

        sink.table();

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.trac.label.id" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.type" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.summary" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.owner" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.reporter" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.priority" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.status" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.resolution" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.created" ) );

        sinkHeader( sink, bundle.getString( "report.trac.label.changed" ) );

        sink.tableRow_();
    }

    private void constructDetailRows( Sink sink, List ticketList, ResourceBundle bundle )
    {
        if ( ticketList == null )
        {
            return;
        }

        for ( int idx = 0; idx < ticketList.size(); idx++ )
        {
            SimpleDateFormat sdf = new SimpleDateFormat( bundle.getString( "report.trac.dateformat" ) );

            TracTicket ticket = (TracTicket) ticketList.get( idx );

            sink.tableRow();

            sink.tableCell();

            sink.link( ticket.getLink() );

            sink.text( ticket.getId() );

            sink.link_();

            sink.tableCell_();

            sinkCell( sink, ticket.getType() );

            sinkCell( sink, ticket.getSummary() );

            sinkCell( sink, ticket.getOwner() );

            sinkCell( sink, ticket.getReporter() );

            sinkCell( sink, ticket.getPriority() );

            sinkCell( sink, ticket.getStatus() );

            sinkCell( sink, ticket.getResolution() );

            sinkCell( sink, sdf.format( ticket.getTimeCreated() ) );

            sinkCell( sink, sdf.format( ticket.getTimeChanged() ) );

            sink.tableRow_();
        }

        sink.table_();
    }

    private void sinkBeginReport( Sink sink, ResourceBundle bundle )
    {
        sink.head();

        sink.text( bundle.getString( "report.trac.header" ) );

        sink.head_();

        sink.body();

        sink.section1();

        sinkSectionTitle1( sink, bundle.getString( "report.trac.header" ) );

    }

    private void sinkEndReport( Sink sink )
    {
        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();
    }

    private void sinkFigure( Sink sink, String image )
    {
        sink.figure();

        sink.figureGraphics( image );

        sink.figure_();
    }

    private void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();

        sink.text( header );

        sink.tableHeaderCell_();
    }

    private void sinkCell( Sink sink, String text )
    {
        sink.tableCell();

        if ( text != null )
        {
            sink.rawText( text );
        }
        else
        {
            sink.rawText( "&nbsp;" );
        }

        sink.tableCell_();
    }

    private void sinkSectionTitle1( Sink sink, String text )
    {
        sink.sectionTitle1();

        sink.text( text );

        sink.sectionTitle1_();
    }
}
