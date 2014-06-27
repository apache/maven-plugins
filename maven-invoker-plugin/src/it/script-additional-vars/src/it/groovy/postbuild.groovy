import java.io.*
import java.util.*

try
{
    println hello
    if ( !hello.equals( "world" ) )
    {
        println "Additional script variable declared in mojo configuration not defined: hello"
        return false
    }

    println version
    if ( !version.equals( "1.0-SNAPSHOT" ) )
    {
        println "Additional script variable declared in mojo configuration not defined: version"
        return false
    }
}
catch( Throwable t )
{
    t.printStackTrace()
    return false
}
