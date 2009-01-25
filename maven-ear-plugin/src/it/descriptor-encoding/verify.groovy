def latin1File = new File( basedir, "latin-1/target/application.xml" )
assert latin1File.exists()
def latin1Chars = new XmlParser().parse( latin1File ).description.text()
println "Latin-1: " + latin1Chars
assert "TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF".equals( latin1Chars )

def utf8File = new File( basedir, "utf-8/target/application.xml" )
assert utf8File.exists()
def utf8Chars = new XmlParser().parse( utf8File ).description.text()
println "UTF-8: " + utf8Chars
assert "TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF".equals( utf8Chars )

return true;
