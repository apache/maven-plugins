import java.io.*
import java.util.*

try
{
    println hello
    if ( !hello.equals( "world" ) )
    {
        throw new Exception( "Additional script variable declared in mojo configuration not defined: hello" )
    }

    println version
    if ( !version.equals( "1.0-SNAPSHOT" ) )
    {
        throw new Exception( "Additional script variable declared in mojo configuration not defined: version" )
    }
}
catch( Throwable t )
{
    t.printStackTrace()
    throw t
}
