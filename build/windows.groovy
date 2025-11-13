String bootstrapFile = new File(getClass().protectionDomain.codeSource.location.file).parent + "/build/bootstrap.groovy"
GroovyShell shell = new GroovyShell()
shell.setVariable("runScript", "build/windows.groovy")
shell.setVariable("args", args)
shell.evaluate(new File(bootstrapFile))
