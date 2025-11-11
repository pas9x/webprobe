package build;

public class CmdErrorException extends Exception {
    public final CmdResult result

    public CmdErrorException(CmdResult result) {
        super("Process exit code = " + result.exitCode)
        this.result = result
    }
}
