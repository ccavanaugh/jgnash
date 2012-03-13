// http://www.mozilla.org/rhino/ScriptingJava.html

importPackage(javax.swing);
importPackage(Packages.jgnash.ui);
importPackage(Packages.jgnash.engine);

function debug(message) {   // helper function to print messages to the console
    java.lang.System.out.println(message);
}

ConsoleDialog.show();   // show the console dialog to see the debug information

var engine = EngineFactory.getEngine(EngineFactory.DEFAULT);    // this is how to get the default Engine instance

var accountList = engine.getAccountList();  // get a list of accounts

for (i = 0; i < accountList.size(); i++)   // loop and print the account names to the console
{
    var account = accountList.get(i);
    debug(account.getName());
}

// just to show how to use swing
var optionPane = JOptionPane.showMessageDialog(null, 'Hello, world!');







