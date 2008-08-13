import java.io.*
import java.lang.reflect.*
import java.util.*
import java.util.regex.*

import org.codehaus.plexus.util.*

try
{
    println "Invoking class from project's test class path"
    println org.MyUtils.getNothing()

    println "Trying to access method which is unavaible in old plexus-utils"
    try
    {
        Class[] types = [ File.class, File.class ];
        println FileUtils.class.getMethod( "copyFileIfModified", types )
        println "FAILED!"
        return false
    }
    catch( NoSuchMethodException e )
    {
        // expected
        e.printStackTrace()
    }
      
}
catch( Throwable t )
{
    t.printStackTrace()
    return false
}

return true
