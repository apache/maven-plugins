File file = new File( basedir, "build.log" );
assert file.exists();

String buildLog = file.getText( "UTF-8" );
assert buildLog.contains( 'No Ancestor POMs!' );

return true;
