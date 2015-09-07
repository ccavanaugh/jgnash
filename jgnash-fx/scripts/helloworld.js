// Load compatibility script
load("nashorn:mozilla_compat.js");

importPackage(Packages.jgnash.uifx.views.main);

function debug(message) {   // helper function to print messages to the console
    java.lang.System.out.println(message);
}

//show the console dialog to see the debug information
ConsoleDialogController.show();

debug("Hello World!");
