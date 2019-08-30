module jgnashfx {
    requires java.logging;
    requires java.prefs;
    requires java.desktop;
    requires java.scripting;

    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.graphics;
    requires javafx.media;

    requires info.picocli;
    
    requires jgnash.resources;
    
    exports jgnash.app;
    exports jgnash.uifx;

    opens jgnash.uifx to javafx.fxml, java.base;
    opens jgnash.uifx.views.main to javafx.fxml;
    opens jgnash.uifx.resource.font to javafx.fxml;
    opens jgnash.uifx.control to javafx.fxml;
    opens jgnash.app to javafx.fxml, java.base, info.picocli;
}