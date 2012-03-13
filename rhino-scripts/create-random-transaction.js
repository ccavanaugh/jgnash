// creates slightly random transaction data
// $Id: clear-budget-goal.js 2990 2011-12-17 12:54:21Z ccavanaugh $

importPackage(javax.swing);
importPackage(Packages.jgnash.ui);
importPackage(Packages.jgnash.ui.account);
importPackage(Packages.jgnash.ui.components);
importPackage(Packages.jgnash.ui.budget);
importPackage(Packages.jgnash.engine);
importPackage(Packages.jgnash.engine.budget);

var rand = new java.util.Random();

function debug(message) { // helper function to print messages to the console
	java.lang.System.out.println(message);
}

function random(min, max) {
	return min + rand.nextFloat() * (max - min);
}

// show the console dialog to see the debug information
// ConsoleDialog.show();

// this is how to get the default Engine instance
var engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

var debitAccount;
var creditAccount;

var dlg = new AccountListDialog();
dlg.setTitle("Select debit account for transaction");
dlg.setVisible(true);

if (dlg.getReturnStatus()) {
	debitAccount = dlg.getAccount();

	debug(debitAccount.getName());

	var budgetCombo = new BudgetComboBox();

	dlg.setTitle("Select credit account for transaction");
	dlg.setVisible(true);

	if (dlg.getReturnStatus()) {
		creditAccount = dlg.getAccount();

		var amount = new java.math.BigDecimal(random(10.12, 32.23));

		var tran = TransactionFactory.generateDoubleEntryTransaction(
				creditAccount, debitAccount, amount, new Date(), "memo",
				"payee", "");

		engine.addTransaction(tran);
	}
}

debug("finished");