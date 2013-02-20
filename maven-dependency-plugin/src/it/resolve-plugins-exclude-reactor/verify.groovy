new File(basedir, "target/resolved.txt").eachLine { line -> 
  if ( line =~ /child-a/ ){
    throw new RuntimeException( "Reactor plugin 'child-a' should be excluded!" )
  }
}

return true;
