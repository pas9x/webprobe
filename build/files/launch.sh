#!/bin/sh
APPDIR=$(dirname "$0")/../
LIBDIR=$APPDIR/Frameworks/lib

$APPDIR/Frameworks/jre/bin/java --module-path $LIBDIR --add-modules javafx.controls,javafx.fxml -classpath $APPDIR/Resources:target/classes:$LIBDIR/dnsjava-3.6.3.jar:$LIBDIR/jackson-annotations-2.20.jar:$LIBDIR/jackson-core-3.0.2.jar:$LIBDIR/jackson-databind-3.0.2.jar:$LIBDIR/slf4j-api-1.7.36.jar net.pascalhp.webprobe.Application
