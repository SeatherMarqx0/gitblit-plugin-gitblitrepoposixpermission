# gitblit-plugin-gitblitrepoposixpermission

_wip_

At the moment, this can be built with:
```
./gradlew distZip
```
or
```
gradlew.bat distZip
```

Plugin zip is built to build/distributions

A plugin for gitblit that assigns the posix user and group of the *.git folder to the contained repository.

This is very useful when importing a large number of repositories that are generated via ssh.

The plugin will eventually be controlled via properties file in the same directory as gitblit.
