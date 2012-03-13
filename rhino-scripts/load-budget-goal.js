// loads the budget goals for the selected account and budget
// $Id: load-budget-goal.js 2995 2011-12-19 22:35:47Z ccavanaugh $

importPackage(javax.swing);
importPackage(Packages.jgnash.ui);
importPackage(Packages.jgnash.ui.account);
importPackage(Packages.jgnash.ui.components);
importPackage(Packages.jgnash.ui.budget);
importPackage(Packages.jgnash.engine);
importPackage(Packages.jgnash.engine.budget);

function debug(message) { // helper function to print messages to the console
	java.lang.System.out.println(message);
}

// show the console dialog to see the debug information
// ConsoleDialog.show();

// this is how to get the default Engine instance
var engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

var account;

var dlg = new AccountListDialog();
dlg.setTitle("Select account to set goals");
dlg.setVisible(true);

if (dlg.getReturnStatus()) {
	account = dlg.getAccount();

	debug(account.getName());

	var budgetCombo = new BudgetComboBox();

	dlg = new GenericCloseDialog(budgetCombo, "Select Budget");
	dlg.setVisible(true);

	var budget = budgetCombo.getSelectedBudget();

	debug(budget.getName());

	// have budget, have account, now get the existing budget goal
	var budgetGoal = new BudgetGoal();
	budgetGoal.setBudgetPeriod(BudgetPeriod.DAILY);

	// create array of amount and clear to zero
	var amounts = java.lang.reflect.Array
			.newInstance(java.math.BigDecimal, BudgetGoal.PERIODS);

	// zero all amounts first
	for ( var i = 0; i < amounts.length; i++)
		amounts[i] = java.math.BigDecimal.ZERO;

	for ( var i = 6; i < amounts.length;) {
		amounts[i] = new java.math.BigDecimal("1230.0");
		i = i + 14;
	}

	// set the new amounts and update the budget
	budgetGoal.setGoals(amounts);
	engine.updateBudgetGoals(budget, account, budgetGoal);
}

debug("finished");
