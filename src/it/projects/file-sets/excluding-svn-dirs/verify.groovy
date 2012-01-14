assert new File( basedir, "src/main/assembly/.svn" ).exists();

File assemblyDir = new File( basedir, "target/excluding-svn-dirs-1.0-SNAPSHOT-src/src/main/assembly" );

assert new File( assemblyDir, "src.xml" ).exists();
assert !new File( assemblyDir, ".svn" ).exists();

return true;
