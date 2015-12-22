
function debug(message) {   // helper function to print messages to the console
    java.lang.System.out.println(message);
}

var Console = Java.type("jgnash.uifx.views.main.ConsoleDialogController");

//show the console dialog to see the debug information
Console.show();

debug("Hello World!");
