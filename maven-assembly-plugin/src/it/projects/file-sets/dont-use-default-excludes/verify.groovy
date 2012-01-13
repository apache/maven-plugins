File assemblyDir = new File( basedir, "target/dont-use-default-excludes-1.0-SNAPSHOT-src/src/main/assembly" );

assert new File( assemblyDir, "src.xml" ).exists();
assert new File( assemblyDir, ".svn" ).exists();

return true;
