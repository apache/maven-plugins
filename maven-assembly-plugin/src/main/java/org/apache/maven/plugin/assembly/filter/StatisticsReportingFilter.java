package org.apache.maven.plugin.assembly.filter;

import org.codehaus.plexus.logging.Logger;

public interface StatisticsReportingFilter
{
    
    void reportMissedCriteria( Logger logger );
    
    void reportFilteredArtifacts( Logger logger );
    
    boolean hasMissedCriteria();

}
