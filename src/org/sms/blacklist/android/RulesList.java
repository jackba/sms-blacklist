package org.sms.blacklist.android;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class RulesList extends ListActivity {

	private LayoutInflater rInflater;
	private ListView rListView;
	private Cursor rCursor;
	private RulesAdapter rAdapter;
	private RulesDatabaseAdapter rDatabaseAdapter;

	private static final int createRule = Menu.FIRST + 1;
	private static final int enableRules = Menu.FIRST + 2;
	private static final int disableRules = Menu.FIRST + 3;
	private static final int contextEdit = 1;
	private static final int contextEnable = 2;
	private static final int contextDisable = 3;
	private static final int contextDelete = 4;
	private static final int contextCancel = 5;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rules_list);

		rListView = (ListView) findViewById(android.R.id.list);
		rInflater = getLayoutInflater();

		// Listview clicked
		rListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Execute the selected script
				rCursor.moveToPosition(position);
				int ruleId = rCursor.getInt(0);
				editRule(ruleId);
			}
		});

		rListView
				.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						menu.add(0, contextEdit, 0,
								getString(R.string.edit_rule));
						menu.add(0, contextEnable, 0,
								getString(R.string.enable));
						menu.add(0, contextDisable, 0,
								getString(R.string.disable));
						menu.add(0, contextDelete, 0,
								getString(R.string.delete));
						menu.add(0, contextCancel, 0,
								getString(R.string.cancel));
					}
				});
	}

	@Override
	public void onStart() {
		refreshCursor();
		super.onStart();
	}

	@Override
	public void onStop() {
		rCursor.close();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		rCursor.close();
		super.onDestroy();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		int itemId = Integer.valueOf(String.valueOf(info.position));
		rCursor.moveToPosition(itemId);
		int ruleId = rCursor.getInt(0);

		switch (item.getItemId()) {
		case contextEdit:
			editRule(ruleId);
			break;
		case contextEnable:
			enableRule(ruleId);
			break;
		case contextDisable:
			disableRule(ruleId);
			break;
		case contextDelete:
			deleteRule(ruleId);
			break;
		case contextCancel:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.setQwertyMode(true);
		MenuItem itemAdd = menu.add(0, createRule, 0,
				getString(R.string.create_rule));
		{
			itemAdd.setAlphabeticShortcut('n');
			itemAdd.setIcon(android.R.drawable.ic_menu_add);
		}

		MenuItem itemEnableAll = menu.add(0, enableRules, 0,
				getString(R.string.enable_rules));
		{
			itemEnableAll.setAlphabeticShortcut('n');
			itemEnableAll.setIcon(R.drawable.ic_menu_enable_rules);
		}
		MenuItem itemDisableAll = menu.add(0, disableRules, 0,
				getString(R.string.disable_rules));
		{
			itemDisableAll.setAlphabeticShortcut('n');
			itemDisableAll.setIcon(R.drawable.ic_menu_disable_rules);
		}
		return super.onCreateOptionsMenu(menu);
	}

	/** when menu button option selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case createRule:
			createRule();
			break;
		case enableRules:
			enableAllRules();
			break;
		case disableRules:
			disableAllRules();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != RESULT_OK) {
			return;
		}
		if (requestCode == Constants.RESULT_EDIT) {
			if (resultCode == RESULT_OK) {
				refreshCursor();
			}
		}
	}

	/** Cursor adapter */
	public class RulesAdapter extends CursorAdapter {

		public RulesAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			TextView rRule = (TextView) view.findViewById(R.id.rule_text);
			TextView rType = (TextView) view.findViewById(R.id.rule_type);

			String ruleName = cursor.getString(1);
			int ruleType = cursor.getInt(2);
			String ruleEnabled = cursor.getString(3);

			switch (ruleType) {
			case Constants.TYPE_BLOCKED_NUMBER:
				rType.setText(R.string.type_blocked_number);
				break;
			case Constants.TYPE_TRUSTED_NUMBER:
				rType.setText(R.string.type_trusted_number);
				break;
			case Constants.TYPE_BLOCKED_KEYWORD:
				rType.setText(R.string.type_blocked_keyword);
				break;
			case Constants.TYPE_ONLY_TRUSTED_NUMBER:
				rType.setText(R.string.type_only_trusted_number);
				break;
			case Constants.TYPE_BLOCKED_NUMBER_REGEXP:
				rType.setText(R.string.type_blocked_number_regexp);
				break;
			case Constants.TYPE_BLOCKED_KEYWORD_REGEXP:
				rType.setText(R.string.type_blocked_keyword_regexp);
				break;
			}

			if (ruleEnabled.equals("true")) {
				rRule.setTypeface(null, Typeface.BOLD);
				rRule.setTextColor(Color.WHITE);
			} else if (ruleEnabled.equals("false")) {
				rRule.setTypeface(null, Typeface.NORMAL);
				rRule.setTextColor(Color.GRAY);
			}
			rRule.setText(ruleName);
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = rInflater.inflate(R.layout.rule_list_item, null);
			bindView(view, context, cursor);
			return view;
		}
	}

	private void refreshCursor() {
		rDatabaseAdapter = new RulesDatabaseAdapter(this);
		rDatabaseAdapter.open();
		rCursor = rDatabaseAdapter.getAllRules(null);
		rAdapter = new RulesAdapter(this, rCursor);
		setListAdapter(rAdapter);
		rDatabaseAdapter.close();
	}

	private void enableAllRules() {
		new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				R.string.enable_rules).setMessage(
				getString(R.string.enable_rules_confirm)).setPositiveButton(
				R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface di, int i) {
						rDatabaseAdapter = new RulesDatabaseAdapter(
								RulesList.this);
						rDatabaseAdapter.open();
						rDatabaseAdapter.enableAllRules();
						rDatabaseAdapter.close();
						refreshCursor();
					}
				}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface di, int i) {
					}
				}).show();
	}

	private void disableAllRules() {
		new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				R.string.disable_rules).setMessage(
				getString(R.string.disable_rules_confirm)).setPositiveButton(
				R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface di, int i) {
						rDatabaseAdapter = new RulesDatabaseAdapter(
								RulesList.this);
						rDatabaseAdapter.open();
						rDatabaseAdapter.disableAllRules();
						rDatabaseAdapter.close();
						refreshCursor();
					}
				}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface di, int i) {
					}
				}).show();
	}

	private void createRule() {
		Intent i = new Intent(RulesList.this, EditRule.class);
		i.putExtra("editMode", false);
		startActivityForResult(i, Constants.RESULT_EDIT);
	}

	private void editRule(int ruleId) {
		Intent i = new Intent(RulesList.this, EditRule.class);
		i.putExtra("ruleId", ruleId);
		i.putExtra("editMode", true);
		startActivityForResult(i, Constants.RESULT_EDIT);
	}

	private void enableRule(int ruleId) {
		rDatabaseAdapter = new RulesDatabaseAdapter(this);
		rDatabaseAdapter.open();
		rDatabaseAdapter.enableRule(ruleId);
		rDatabaseAdapter.close();
		refreshCursor();
	}

	private void disableRule(int ruleId) {
		rDatabaseAdapter = new RulesDatabaseAdapter(this);
		rDatabaseAdapter.open();
		rDatabaseAdapter.disableRule(ruleId);
		rDatabaseAdapter.close();
		refreshCursor();
	}

	private void deleteRule(final int ruleId) {
		new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(R.string.delete)
				.setMessage(getString(R.string.delete_rule_confirm))
				.setPositiveButton(R.string.delete,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface di, int i) {
								rDatabaseAdapter = new RulesDatabaseAdapter(
										RulesList.this);
								rDatabaseAdapter.open();
								rDatabaseAdapter.deleteRule(ruleId);
								rDatabaseAdapter.close();
								refreshCursor();
							}
						}).setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface di, int i) {
							}
						}).show();

	}
}