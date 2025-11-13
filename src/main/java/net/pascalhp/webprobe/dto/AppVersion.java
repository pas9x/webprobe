package net.pascalhp.webprobe.dto;

public class AppVersion {
    public int major;
    public int minor;
    public int build;
    public String commit;

    public String getReadable() {
        return this.major + "." + minor + "." + this.build;
    }

    public String getFull() {
        String full = this.getReadable();
        if (this.commit == null || this.commit.isEmpty()) {
            full += "-dev";
        } else {
            full += "-" + this.commit;
        }
        return full;
    }

    public String toString() {
        return this.getFull();
    }
}
