package net.pascalhp.webprobe.checker;

import net.pascalhp.webprobe.tasks.Task;

class CheckerTask implements Task {
    public final Checker checker;

    public CheckerTask(Checker checker) {
        this.checker = checker;
    }

    public Object run() {
        return this.checker.check();
    }

    public void stop(String reason) {
        this.checker.stop(reason);
    }
}
