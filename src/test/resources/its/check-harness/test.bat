@echo off
rem cmd.exe /X /C ""C:\Program Files\maven2\bin\mvn.bat" -B -X -Dmaven.repo.local=E:\svn\Maven\maven-plugins\maven-dependency-plugin\target\test-classes\m2repo -Dtest=true install && exit /B %ERRORLEVEL%"
cmd.exe /X /C "ant --version && exit /B %ERRORLEVEL%"
rem cmd /C ant
goto answer%errorlevel%
:answer0
echo Program had return code 0
goto end
:answer1
echo Program had return code 1
goto end
:end
echo done! 