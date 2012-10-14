// clears security history nodes that fall on weekends
importPackage(Packages.jgnash.engine);
importPackage(Packages.java.util);
importPackage(Packages.jgnash.ui);

function debug(message) {   // helper function to print messages to the console
    java.lang.System.out.println(message);
}

//show the console dialog to see the debug information
ConsoleDialog.show();   

// this is how to get the default Engine instance
var engine = EngineFactory.getEngine(EngineFactory.DEFAULT);   

// List<SecurityNode>
var securities = engine.getSecurities();

// calendar
var cal = new GregorianCalendar();


for (var i = 0; i < securities.size(); i++) {
    var security = securities.get(i);
    debug(security.getSymbol());

    var securityHistory = engine.getSecurityHistory(securities.get(i));

    for (var j = 0; j < securityHistory.size(); j++) {
    
        var historyNode = securityHistory.get(j);
        cal.setTime(historyNode.getDate());
        
        var day = cal.get(Calendar.DAY_OF_WEEK);
        
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            debug("removing one");
            engine.removeSecurityHistory(security, historyNode);
        }
    }
}

debug("finished");
