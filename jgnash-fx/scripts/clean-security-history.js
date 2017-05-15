// Load compatibility script
load("nashorn:mozilla_compat.js");

// clears security history nodes that fall on weekends
importPackage(Packages.jgnash.engine);
importPackage(Packages.java.util);
importPackage(Packages.java.time);

function debug(message) {   // helper function to print messages to the console
    java.lang.System.out.println(message);
}

var Console = Java.type("jgnash.uifx.views.main.ConsoleDialogController");

//show the console dialog to see the debug information
Console.show();

// this is how to get the default Engine instance
var engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

if (engine !== null) {

    // List<SecurityNode>
    var securities = engine.getSecurities();

    for (var i = 0; i < securities.size(); i++) {
        var security = securities.get(i);
        debug(security.getSymbol());

        var securityHistory = securities.get(i).getHistoryNodes();

        for (var j = 0; j < securityHistory.size(); j++) {
            var historyNode = securityHistory.get(j);
            var dayOfWeek = historyNode.getLocalDate().getDayOfWeek();

            if (dayOfWeek === DayOfWeek.SATURDAY || dayOfWeek === DayOfWeek.SUNDAY) {
                debug("removing one");
                engine.removeSecurityHistory(security, historyNode.getLocalDate());
            }
        }
    }
}

debug("finished");
