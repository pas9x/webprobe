package net.pascalhp.webprobe.exceptions;

import javafx.scene.control.Alert;
import net.pascalhp.webprobe.Application;
import static net.pascalhp.webprobe.Localization.lang;

public class ErrorMessage extends RuntimeException {
    public ErrorMessage(String msg) {
        super(msg);
    }

    public void alert() {
        Application.displayError(this.getMessage());
    }
}
