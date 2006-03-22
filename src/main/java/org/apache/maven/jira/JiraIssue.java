package org.apache.maven.jira;

public class JiraIssue
{
    private String key;
    private String link;
    private String summary;
    private String status;
    private String resolution;
    private String assignee;

    public JiraIssue(  )
    {
    }

    public String getKey(  )
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getLink(  )
    {
        return link;
    }

    public void setLink( String link )
    {
        this.link = link;
    }

    public String getSummary(  )
    {
        return summary;
    }

    public void setSummary( String summary )
    {
        this.summary = summary;
    }

    public String getStatus(  )
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public String getResolution(  )
    {
        return resolution;
    }

    public void setResolution( String resolution )
    {
        this.resolution = resolution;
    }

    public String getAssignee(  )
    {
        return assignee;
    }

    public void setAssignee( String assignee )
    {
        this.assignee = assignee;
    }
}
