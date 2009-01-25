import java.io.*;

// marker for parent build that this sub build was indeed run
File touchFile = new File( basedir, "touch.txt" )
touchFile.createNewFile()

if ( true ) throw new AssertionError( "This should not cause the main build to fail" )

