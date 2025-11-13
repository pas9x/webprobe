package build

@Grab(group='com.squareup.okhttp3',  module='okhttp-jvm', version='5.3.0')
@Grab(group='tools.jackson.core',  module='jackson-databind', version='3.0.2')

public class Bootstrap {
    public static String buildDir
    public static String projectDir
    public static String[] args
    public static boolean isRelease
}

Class currentScriptClass = getClass()
if (!(currentScriptClass.classLoader instanceof GroovyClassLoader)) {
    System.err.println("BUG: script class loader is not instance of GroovyClassLoader.\n")
    System.exit(1)
    throw new RuntimeException("BUG: script class loader is not instance of GroovyClassLoader.")
}

GroovyClassLoader loader = (GroovyClassLoader)currentScriptClass.classLoader
File currentScriptFile = new File(currentScriptClass.protectionDomain.codeSource.location.path)
Bootstrap.buildDir = currentScriptFile.parentFile.parent
Bootstrap.projectDir = currentScriptFile.parentFile.parentFile.parent
loader.addURL(new File(Bootstrap.buildDir).toURI().toURL())
Bootstrap.args = args

for (def arg : args) {
    if (arg.equals("release")) {
        Bootstrap.isRelease = true
    }
}

GroovyShell shell = new GroovyShell(loader)
shell.evaluate(new File(Bootstrap.buildDir + "/" + runScript))
