content = new File(basedir, 'build.log').text
assert content.indexOf( "NullPointerException" ) < 0