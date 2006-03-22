package org.apache.maven.announcement;

import org.apache.maven.jira.JiraIssue;

import java.util.ArrayList;
import java.util.List;

public class JiraAnnouncement 
        extends JiraIssue
{
    private String type;
    
    private String comment;
    
    private String title;
    
    private String fixVersion;
    
    private String reporter;
    
    List comments;
    
    public void addComment( String comment )
    {
        if( comments == null )
        {
            comments = new ArrayList();
        }
        comments.add( comment );
    }

    public String getFixVersion()
    {
        return fixVersion;
    }

    public void setFixVersion( String fixVersion ) 
    {
        this.fixVersion = fixVersion;
    }
    
    public String getComment() 
    {
        return comment;
    }

    public void setComment( String comment ) 
    {
        this.comment = comment;
    }
    
    public String getTitle() 
    {
        return title;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }    
    
    public void setType( String type )
    {
        this.type = type;
    }
    
    public String getType()
    {
        return this.type;
    }
    
    public void setReporter( String reporter )
    {
        this.reporter = reporter;
    }
    
    public String getReporter()
    {
        return this.reporter;
    }
    
    public List getComments()
    {
        return comments;
    }    
}
