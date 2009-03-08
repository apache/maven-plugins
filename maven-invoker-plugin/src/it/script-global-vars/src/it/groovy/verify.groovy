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
}
catch( Throwable t )
{
    t.printStackTrace()
    return false
}
