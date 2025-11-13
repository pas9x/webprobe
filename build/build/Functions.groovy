package build

import okhttp3.Request
import okhttp3.Response
import okhttp3.OkHttpClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.ObjectWriter
import com.fasterxml.jackson.annotation.JsonIgnore

import java.nio.charset.StandardCharsets;

class Functions {
    public static Map<String, String> env

    private static final Object sync = new Object()
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().matches("^win.*")
    private static String targetDir

    public static void test() {
        System.out.println(Functions.getClass().getProtectionDomain())
    }

    public static void red() {
        if (isWindows) return;
        out("\u001b[31m")
    }

    public static void green() {
        if (isWindows) return;
        out("\u001b[32m")
    }

    public static void reset() {
        if (isWindows) return;
        out("\u001b[0m")
    }

    public static void out(msg) {
        synchronized (sync) {
            System.out.print(msg)
        }
    }

    public static void err(msg) {
        synchronized (sync) {
            System.err.print(msg)
        }
    }

    public static void panic(error) {
        err(error)
        System.exit(1)
    }

    public static CmdResult exec(String command, String stdin = null, boolean tolerant = false, String cwd = null) {
        ProcessBuilder builder
        if (isWindows) {
            builder = new ProcessBuilder("cmd.exe", "/c", command)
        } else {
            builder = new ProcessBuilder("sh", "-c", command)
        }
        if (env != null) {
            builder.environment().putAll(env)
        }
        if (cwd != null) {
            builder.directory(new File(cwd))
        }
        Process proc = builder.start()
        OutputStream stdinStream = proc.getOutputStream()
        if (stdin != null) {
            stdinStream.write(stdin.getBytes())
        }
        stdinStream.close()

        ByteArrayOutputStream bufStdout = new ByteArrayOutputStream();
        ByteArrayOutputStream bufStderr = new ByteArrayOutputStream();

        InputStream stdoutStream = proc.getInputStream()
        InputStream stderrStream = proc.getErrorStream()

        Runnable stdoutReader = {
            byte[] buf = new byte[1024]
            byte[] readBytes
            int nBytesRead
            while (true) {
                nBytesRead = stdoutStream.read(buf)
                if (nBytesRead < 1) {
                    break
                }
                readBytes = Arrays.copyOfRange(buf, 0, nBytesRead)
                bufStdout.write(readBytes)
                green()
                System.out.write(readBytes)
                reset()
            }
        }

        Runnable stderrReader = {
            byte[] buf = new byte[1024]
            byte[] readBytes
            int nBytesRead
            while (true) {
                nBytesRead = stderrStream.read(buf)
                if (nBytesRead < 1) {
                    break
                }
                readBytes = Arrays.copyOfRange(buf, 0, nBytesRead)
                bufStderr.write(readBytes)
                red()
                System.err.write(readBytes)
                reset()
            }
        }

        Thread stdoutReaderThread = new Thread(stdoutReader)
        Thread stderrReaderThread =  new Thread(stderrReader)

        stdoutReaderThread.start()
        stderrReaderThread.start()

        CmdResult result = new CmdResult()
        result.exitCode = proc.waitFor()
        stdoutReaderThread.join()
        stderrReaderThread.join()
        result.stdout = bufStdout.toByteArray()
        result.stderr = bufStderr.toByteArray()

        if (result.exitCode != 0 && !tolerant) {
            throw new CmdErrorException(result)
        }

        return result;
    }

    public static Response httpGet(String url) {
        OkHttpClient client = new OkHttpClient()
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Webprobe build script")
            .build()
        return client.newCall(request).execute()
    }

    public static void download(String url, OutputStream stream) {
        Response resp = httpGet(url)
        resp.body().byteStream().transferTo(stream);
    }

    public static void download(String url, String fileName) {
        FileOutputStream stream = new FileOutputStream(fileName)
        download(url, stream)
    }

    private static void mkdir(String dir) {
        File dirFile = new File(dir)
        if (!dirFile.isDirectory()) {
            if (!dirFile.mkdirs()) {
                throw new RuntimeException("Failed to create directory $dir")
            }
        }
    }

    public static String getTargetDir() {
        if (targetDir == null) {
            String dir = Bootstrap.projectDir + "/target"
            mkdir("$dir/classes")
            targetDir = dir
        }
        return targetDir
    }

    public static String getTmpDir() {
        String tmpDir = getTargetDir() + "/tmp"
        mkdir(tmpDir)
        return tmpDir
    }

    public static String getReleaseDir() {
        String releaseDir = Bootstrap.projectDir + "/release"
        mkdir(releaseDir)
        return releaseDir
    }

    public static void copyFile(File from, File to) {
        InputStream fromStream = new FileInputStream(from)
        OutputStream toStream = new FileOutputStream(to)
        fromStream.transferTo(toStream)
        fromStream.close()
        toStream.close()
    }

    protected static String getCommit() {
        def execResult = exec("git rev-parse HEAD", null, false, Bootstrap.projectDir)
        if (execResult.stderr.size() > 0) {
            throw new RuntimeException("Git returned stderr of non-zero length. Can't get commit hash.")
        }
        String commit = new String(execResult.stdout).trim()
        if (!commit.matches("^[a-f0-9]{8,100}\$")) {
            throw new RuntimeException("Invalid git commit hash: " + commit)
        }
        return commit
    }

    public static Version getSourceVersion() {
        return Version.load(Bootstrap.projectDir + "/src/main/resources/version.json")
    }

    public static Version getBuildVersion() {
        Version ver = getSourceVersion()
        ver.commit = getCommit()
        VersionMap map = VersionMap.load()
        BaseVersion baseVersion = map.get(ver.getBase())
        boolean updateMap = false

        if (baseVersion == null) {
            baseVersion = new BaseVersion()
            map.put(ver.getBase(), baseVersion)
            updateMap = true
        }

        Integer build = baseVersion.commit2build.get(ver.commit)
        if (build == null) {
            build = ++baseVersion.lastBuild
            baseVersion.commit2build.put(ver.commit, build)
            updateMap = true
        }

        if (updateMap) {
            map.save()
        }

        ver.build = build

        return ver
    }

    public static int getBuildCpuCount() {
        int count = Runtime.getRuntime().availableProcessors() - 1
        if (count < 1) {
            count = 1
        }
        return count
    }

    protected static class VersionMap extends HashMap<String, BaseVersion> {
        public static VersionMap load() {
            File mapFile = new File(getReleaseDir() + "/map.json")
            if (!mapFile.isFile()) {
                return new VersionMap()
            }
            ObjectMapper mapper = new ObjectMapper();
            InputStream stream = new FileInputStream(mapFile)
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            return mapper.readValue(reader, VersionMap.class)
        }

        public void save() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(new File(getReleaseDir() + "/map.json"), this)
        }
    }

    private static class BaseVersion {
        public int lastBuild = 0
        public HashMap<String, Integer> commit2build = new HashMap<>()
    }

    public static class Version {
        public int major
        public int minor
        public int build
        public String commit
        private String file;

        public static Version load(String file) {
            ObjectMapper mapper = new ObjectMapper();
            InputStream stream = new FileInputStream(file)
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            Version ver = mapper.readValue(reader, Version.class)
            return ver
        }

        @JsonIgnore
        public String getBase() {
            return "${this.major}.${this.minor}"
        }

        @JsonIgnore
        public String getShort() {
            if (this.build < 1) {
                throw new RuntimeException("Build number for this version was not determined")
            }
            return "${this.major}.${this.minor}.${this.build}"
        }

        public void saveTo(String file) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(new File(file), this)
        }

        public String toJson() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            StringWriter buf = new StringWriter()
            writer.writeValue(buf, this)
            return buf.toString()
        }

        public String toString() {
            return toJson()
        }

        @JsonIgnore
        public String getBuildDir() {
            if (this.build < 1) {
                throw new RuntimeException("Build number for this version was not determined")
            }
            String dir = getReleaseDir() + "/${this.major}.${this.minor}.${this.build}"
            mkdir(dir)
            File commitFile = new File(dir + "/commit.txt")
            if (!commitFile.isFile()) {
                commitFile.write(this.commit)
            }
            return dir
        }
    }
}