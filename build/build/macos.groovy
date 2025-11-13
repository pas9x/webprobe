package build

import static Functions.*
import static CacertFunctions.getFreshCacertStore

String javaHome = System.getenv("JAVA_HOME");
if (javaHome == null || javaHome.trim().isEmpty()) {
    panic("No \$JAVA_HOME environment variable. Can't build.\n")
}

File mtrFile = new File("/usr/local/Cellar/mtr/0.96/sbin/mtr")
if (!mtrFile.isFile()) {
    System.err.println("File /usr/local/Cellar/mtr/0.96/sbin/mtr not found.\n")
    System.err.println("It is probable because you have no mtr installed to your system. You can install it with `brew install mtr`.\n");
}

String projectDir = Bootstrap.projectDir;
String targetDir = projectDir + "/target"
String macosDir = targetDir + "/macos"
String buildDir = Bootstrap.buildDir

Version ver
String suffix, distributionFile
if (Bootstrap.isRelease) {
    ver = getBuildVersion()
    suffix = ver.getShort()
    distributionFile = ver.getBuildDir() + "/Webprobe-${suffix}.dmg"
} else {
    suffix = "dev"
    ver = getSourceVersion()
    distributionFile = targetDir + "/Webprobe-${suffix}.dmg"
}

out("--- Create directory structure\n")
exec("rm -rf $macosDir")
exec("rm -rf $targetDir/classes")
exec("mkdir -p $macosDir/Webprobe.app/Contents")
exec("mkdir $macosDir/Webprobe.app/Contents/MacOS")
exec("mkdir $macosDir/Webprobe.app/Contents/Resources")
exec("mkdir $macosDir/Webprobe.app/Contents/Frameworks")
exec("cp $buildDir/files/Info.plist $macosDir/Webprobe.app/Contents/")
exec("cp $buildDir/files/launch.sh $macosDir/Webprobe.app/Contents/MacOS/")
exec("chmod 755 $macosDir/Webprobe.app/Contents/MacOS/launch.sh")

out("--- Download dependencies\n")
exec("mvn dependency:copy-dependencies -DoutputDirectory=$macosDir/Webprobe.app/Contents/Frameworks/lib -f $projectDir/pom.xml")
exec("rm -f $macosDir/Webprobe.app/Contents/Frameworks/lib/javafx-base-17.jar")
exec("rm -f $macosDir/Webprobe.app/Contents/Frameworks/lib/javafx-controls-17.jar")
exec("rm -f $macosDir/Webprobe.app/Contents/Frameworks/lib/javafx-fxml-17.jar")
exec("rm -f $macosDir/Webprobe.app/Contents/Frameworks/lib/javafx-graphics-17.jar")

out("-- Compile code\n")
exec("mvn compile -f $projectDir/pom.xml")
ver.saveTo("$targetDir/classes/version.json")

out("--- Update CA certificates\n")
getFreshCacertStore()

out("--- Copy dependencies, build JRE\n")
exec("rm -rf $targetDir/classes/mtr $targetDir/classes/mtr-packet $targetDir/classes/mtr.exe")
exec("cp -rp $targetDir/classes/* $macosDir/Webprobe.app/Contents/Resources/")
exec("$javaHome/bin/jlink --module-path $javaHome/jmods --add-modules java.base,java.logging,java.scripting,java.xml,jdk.unsupported,java.desktop,jdk.naming.dns,jdk.crypto.ec --strip-debug --no-header-files --no-man-pages --compress=0 --output $macosDir/Webprobe.app/Contents/Frameworks/jre")

out("--- Embed mtr to application distribution\n")
exec("cp /usr/local/Cellar/mtr/0.96/sbin/mtr $macosDir/Webprobe.app/Contents/Frameworks/")
exec("chmod +x $macosDir/Webprobe.app/Contents/Frameworks/mtr")
exec("cp /usr/local/Cellar/mtr/0.96/sbin/mtr-packet $macosDir/Webprobe.app/Contents/Frameworks/")
exec("chmod +x $macosDir/Webprobe.app/Contents/Frameworks/mtr-packet")
exec("cp /usr/local/opt/jansson/lib/libjansson.4.dylib $macosDir/Webprobe.app/Contents/Frameworks/")
// Patch mtr with correct libjansson path
exec("chmod 755 $macosDir/Webprobe.app/Contents/Frameworks/mtr")
exec("chmod 755 $macosDir/Webprobe.app/Contents/Frameworks/mtr-packet")
exec("install_name_tool -change /usr/local/opt/jansson/lib/libjansson.4.dylib @executable_path/libjansson.4.dylib $macosDir/Webprobe.app/Contents/Frameworks/mtr")

out("--- Build Webprobe.dmg\n")
exec("ln -s /Applications $macosDir/Applications")
exec("hdiutil create -volname Webprobe -srcfolder $macosDir -ov -format UDZO -imagekey zlib-level=9 $distributionFile")

out("---\n")
out("The application distribution has been successfully built and saved to file $distributionFile\n")
