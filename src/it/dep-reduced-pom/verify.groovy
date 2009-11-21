File pomFile = new File( basedir, "target/dependency-reduced-pom.xml" );
assert pomFile.isFile()

def ns = new groovy.xml.Namespace("http://maven.apache.org/POM/4.0.0") 
def pom = new XmlParser().parse( pomFile )

assert pom[ns.modelVersion].size() == 1
assert pom[ns.dependencies][ns.dependency].size() == 0
