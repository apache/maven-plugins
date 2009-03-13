expected = ['/META-INF/MANIFEST.MF']

for (item in expected)
{
    def file = new File(basedir, 'target/dependency' + item)
    if (!file.exists())
    {
       throw new RuntimeException("Missing "+file.name);
    }
}

notExpected = ['/stylesheet.css']

for (item in notExpected)
{
    def file = new File(basedir, 'target/dependency' + item)    
    if (file.exists())
    {
       throw new RuntimeException("This file shouldn't be here: "+file.name);
    }
}

return true;
