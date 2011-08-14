#! /bin/bash
mvn install:install-file -Dfile=smack.jar -DgroupId=self -DartifactId=smack -Dversion=3.2.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=smackx.jar -DgroupId=self -DartifactId=smackx -Dversion=3.2.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=smackx-debug.jar -DgroupId=self -DartifactId=smackx-debug -Dversion=3.2.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=smackx-jingle.jar -DgroupId=self -DartifactId=smackx-jingle -Dversion=3.2.1 -Dpackaging=jar -DgeneratePom=true

