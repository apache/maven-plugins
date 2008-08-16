import java.io.*
import java.lang.reflect.*
import java.util.*
import java.util.regex.*

try
{
    // create touch file so that the parent build can verify whether this script was called
    File touchFile = new File( basedir, "target/postbuild.groovy" )
    println "Creating touch file: " + touchFile
    touchFile.getParentFile().mkdirs()
    touchFile.createNewFile()
}
catch( Throwable t )
{
    t.printStackTrace()
    return false
}

return true
