new File(basedir, "target/resolved.txt").eachLine { line -> 
  if ( line =~ /maven-surefire-plugin/ ){
    throw new RuntimeException( "Surefire plugin should be excluded!" )
  }
  else if ( line =~ /maven-dependency-plugin/ ){
    throw new RuntimeException( "Dependency plugin should be excluded!" )
  }
}

return true;
