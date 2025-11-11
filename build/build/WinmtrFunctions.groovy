package build

import okhttp3.Response

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream;

import static Functions.*

public class WinmtrFunctions {
    public static void download(String saveTo) {
        File mtrTmpFile = new File(getTmpDir() + "/mtr.exe")
        File mtrSaveFile = new File(saveTo)
        if (mtrTmpFile.exists()) {
            copyFile(mtrTmpFile, mtrSaveFile)
            return
        }

        String url = "https://sourceforge.net/projects/winmtrcmd/files/WinMTRCmd-0.1.zip/download";
        out("Download mtr from $url\n")
        Response resp = httpGet(url)
        byte[] zipBinary = resp.body().bytes()
        ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zipBinary))

        while (true) {
            ZipEntry entry = stream.getNextEntry()
            if (entry == null) {
                break
            }
            if (entry.name.equals("WinMTRCmd-0.1/Release_x64/WinMTRCmd.exe")) {
                FileOutputStream mtrTmpStream = new FileOutputStream(mtrTmpFile)
                stream.transferTo(mtrTmpStream)
                copyFile(mtrTmpFile, mtrSaveFile)
                return
            }
        }

        throw new RuntimeException("Zip archive $url does not contain file WinMTRCmd-0.1/Release_x64/WinMTRCmd.exe")
    }
}
