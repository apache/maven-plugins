File file = new File( basedir, "build.log" );
assert file.exists();

String buildLog = file.getText( "UTF-8" );
assert buildLog.contains( 'Ancestor POMs: org.apache:apache:5' );

return true;
