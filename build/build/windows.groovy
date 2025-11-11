package build

import static Functions.*
import static CacertFunctions.getFreshCacertStore

String javaHome = System.getenv("JAVA_HOME");
if (javaHome == null || javaHome.trim().isEmpty()) {
    panic("No \$JAVA_HOME environment variable. Can't build.\n")
}

String projectDir = Bootstrap.projectDir;
String targetDir = projectDir + "\\target"
String winDir = projectDir + "\\target\\windows"
String buildDir = Bootstrap.buildDir

def ver = getVersion()

out("--- Create directory structure\n")
exec("rmdir /s /q $winDir", null, true)
exec("rmdir /s /q $targetDir\\classes", null, true)

out("--- Download dependencies\n")
exec("mvn dependency:copy-dependencies -DoutputDirectory=$winDir\\app -f $projectDir\\pom.xml")
exec("del /f /s /q $winDir\\app\\javafx-base-17.jar")
exec("del /f /s /q $winDir\\app\\javafx-controls-17.jar")
exec("del /f /s /q $winDir\\app\\javafx-fxml-17.jar")
exec("del /f /s /q $winDir\\app\\javafx-graphics-17.jar")
WinmtrFunctions.download("$winDir\\app\\mtr.exe")

out("--- Build jre\n")
exec("$javaHome\\bin\\jlink --add-modules java.base,java.logging,java.scripting,java.xml,jdk.unsupported,java.desktop,jdk.naming.dns,jdk.crypto.ec,javafx.controls,javafx.fxml --module-path $winDir\\app --strip-debug --no-header-files --no-man-pages --compress=0 --output $winDir\\runtime")
exec("del /f /s /q $winDir\\app\\javafx-base-17-win.jar")
exec("del /f /s /q $winDir\\app\\javafx-controls-17-win.jar")
exec("del /f /s /q $winDir\\app\\javafx-fxml-17-win.jar")
exec("del /f /s /q $winDir\\app\\javafx-graphics-17-win.jar")

out("--- Compile code\n")
exec("mvn compile -f $projectDir\\pom.xml")
exec("del /f /s /q $targetDir\\classes\\mtr.exe")
exec("del /f /s /q $targetDir\\classes\\\\icons\\app-icon.iconset")
ver.saveTo("$targetDir\\classes\\version.json")

out("-- Update CA certificates\n")
getFreshCacertStore()

out("--- Build webprobe.jar\n")
exec("$javaHome\\bin\\jar --no-compress --create --main-class=net.pascalhp.webprobe.Application --file $winDir\\app\\webprobe.jar -C $targetDir\\classes .")

out("--- Build application distribution\n")
exec("$javaHome\\bin\\jpackage --type app-image --name Webprobe --icon $projectDir\\src\\main\\resources\\icons\\app-icon.ico --dest $winDir --input $winDir\\app --main-jar webprobe.jar --runtime-image $winDir\\runtime")
exec("$javaHome\\bin\\jpackage --type app-image --name Webprobec --win-console --icon $projectDir\\src\\main\\resources\\icons\\app-icon.ico --dest $winDir --input $winDir\\app --main-jar webprobe.jar --runtime-image $winDir\\runtime")
exec("copy $winDir\\Webprobec\\Webprobec.exe $winDir\\Webprobe\\")
exec("copy $winDir\\Webprobec\\app\\Webprobec.cfg $winDir\\Webprobe\\app\\")
exec("del /f /s /q $winDir\\Webprobe\\Webprobe.ico")
exec("rmdir /s /q $winDir\\Webprobec")

out("OK\n")
