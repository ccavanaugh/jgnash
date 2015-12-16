load("nashorn:mozilla_compat.js");  // Load compatibility script

importPackage(javax.swing);
importPackage(Packages.jgnash.ui);
importPackage(Packages.jgnash.engine);

function debug(message) {   // helper function to print messages to the console
    java.lang.System.out.println(message);
}

ConsoleDialog.show();   // show the console dialog to see the debug information

var engine = EngineFactory.getEngine(EngineFactory.DEFAULT);    // this is how to get the default Engine instance

var transactionList = engine.getTransactions();  // get a list of transactions

var searchList = SearchEngine.matchMemo("*cash*", transactionList, true);

for (var i = 0; i < searchList.size(); i++)   // loop and print the account names to the console
{
    var tran = searchList.get(i);
    debug(tran.getMemo());
}








