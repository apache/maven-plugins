import java.io.*
import java.util.*
import java.util.regex.*

try
{
    println basedir
    if ( !( basedir instanceof File ) )
    {
        println "Global script variable not defined: basedir"
        return false
    }

    println localRepositoryPath
    if ( !( localRepositoryPath instanceof File ) )
    {
        println "Global script variable not defined: localRepositoryPath"
        return false
    }

    println mavenVersion
    if ( !mavenVersion )
    {
        println "Global script variable not defined: mavenVersion"
        return false
    }
    if ( !mavenVersion?.trim() )
    {
        println "Global script variable empty: mavenVersion"
        return false
    }
}
catch( Throwable t )
{
    t.printStackTrace()
    return false
}
