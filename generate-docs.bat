@echo off
echo Generating Dokka documentation for MAD Gateway...
call gradlew generateDocs
echo.
echo Documentation generated in build\dokka
echo.
echo Opening documentation in browser...
start build\dokka\index.html
echo.
echo Done!