rm symlinks.zip
rm symlinks.tar
cd src
zip --symlinks ../symlinks.zip file* targetDir sym*
tar -cvf ../symlinks.tar file* targetDir sym*

