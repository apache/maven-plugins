package org.apache.maven.plugin.ear.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.ear.EarModule;

/**
 * This class will check the list of modules if there exist a duplicate artifactId. If we have such case it's necessary
 * to create a warning to the user otherwise it can happen to overwrite existing artifacts during the EAR creation
 * process. This is a temporary solution to keep backward compatibility with previous versions. For the next major
 * release 3.X the creation of the EAR archive should be done based on unique identifiers like
 * {@code groupId:artifactId:version}.
 * 
 * @author Karl Heinz Marbaise <khmarbaise@apache.org>
 */
public class ModuleIdentifierValidator
{

    private List<EarModule> earModules;

    private Map<String, List<EarModule>> result;

    public ModuleIdentifierValidator( List<EarModule> earModules )
    {
        if ( earModules == null )
        {
            throw new IllegalArgumentException( "Not allowed to give null for earModules." );
        }
        this.earModules = earModules;
        this.result = new HashMap<String, List<EarModule>>();
    }

    /**
     * You have to call {@link #checkForDuplicateArtifacts()} before
     * otherwise you will get always {@code false}.
     * @return true in case of existing duplicates false otherwise.
     */
    public boolean existDuplicateArtifacts()
    {
        return !result.isEmpty();
    }

    /**
     * Trigger the module list check.
     * 
     * @return this for fluent usage.
     */
    public ModuleIdentifierValidator checkForDuplicateArtifacts()
    {
        analyze();
        return this;
    }

    private void analyze()
    {
        final Map<String, List<EarModule>> newList = new HashMap<String, List<EarModule>>();

        for ( EarModule earModule : earModules )
        {
            String earId = earModule.getArtifact().getArtifactId() + ":" + earModule.getArtifact().getVersion();

            if ( newList.containsKey( earId ) )
            {
                newList.get( earId ).add( earModule );
            }
            else
            {
                List<EarModule> list = new ArrayList<EarModule>();
                list.add( earModule );
                newList.put( earId, list );
            }
        }

        result.clear();
        for ( Map.Entry<String, List<EarModule>> item : newList.entrySet() )
        {
            if ( item.getValue().size() > 1 )
            {
                result.put( item.getKey(), item.getValue() );
            }
        }

    }

    public Map<String, List<EarModule>> getDuplicateArtifacts()
    {
        return result;
    }

    public List<EarModule> getEarModules()
    {
        return earModules;
    }

    public ModuleIdentifierValidator setEarModules( List<EarModule> earModules )
    {
        if ( earModules == null )
        {
            throw new IllegalArgumentException( "Not allowed to give null for earModules." );
        }
        this.earModules = earModules;
        return this;
    }
}