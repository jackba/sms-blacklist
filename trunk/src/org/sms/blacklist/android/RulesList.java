package org.sms.blacklist.android;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
	
	LayoutInflater rInflater;
	ListView rListView;
	Cursor rCursor;
	SQLiteDatabase rDatabase;
	RulesAdapter rAdapter;	
	
	private static final int createRule = Menu.FIRST + 1;
	private static final int enableRules = Menu.FIRST + 2;
	private static final int disableRules = Menu.FIRST + 3;
	private static final int contextEdit = 1;
	private static final int contextDelete = 2;
	private static final int contextToggle = 3;
	private static final int contextCancel = 4;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rules_list);

		
		rListView = (ListView) findViewById(android.R.id.list);
		rInflater = getLayoutInflater();
		
		RulesDatabaseHelper rhelper = new RulesDatabaseHelper(this);				 
		rDatabase = rhelper.getWritableDatabase(); 

		refreshCursor();
		
	  //Listview clicked
		rListView.setOnItemClickListener(
			new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//Execute the selected script
					rCursor.moveToPosition(position);
					int ruleId = rCursor.getInt(0);
					editRule(ruleId);
				}
			}
		);
		
		rListView.setOnCreateContextMenuListener(
			new OnCreateContextMenuListener() {
				public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
					menu.add(0, contextEdit, 0, getString(R.string.edit_rule));
					menu.add(0, contextToggle, 0, getString(R.string.toggle));
					menu.add(0, contextDelete, 0, getString(R.string.delete));
					menu.add(0, contextCancel, 0, getString(R.string.cancel));
				}
			}
		);
	}
	
	@Override
	public void onResume() {
		refreshCursor();
		super.onResume();
	}
	
	@Override
	public void onStop() {
		rCursor.close();
		super.onStop();
	}

	@Override 
	public boolean onContextItemSelected(MenuItem item) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			int itemId = Integer.valueOf(String.valueOf(info.position));
			rCursor.moveToPosition(itemId);
			int ruleId = rCursor.getInt(0);
			
			switch(item.getItemId())
			{
			case contextEdit:
				editRule(ruleId);
				break;
			case contextToggle:
				toggleRule(ruleId);
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
			MenuItem itemAdd = menu.add(0, createRule, 0, getString(R.string.create_rule));
			{
				itemAdd.setAlphabeticShortcut('n');
				itemAdd.setIcon(android.R.drawable.ic_menu_add);
			}
			
			MenuItem itemEnableAll = menu.add(0, enableRules, 0, getString(R.string.enable_rules));
			{
				itemEnableAll.setAlphabeticShortcut('n');
				itemEnableAll.setIcon(R.drawable.ic_menu_enable_rules);
			}
			MenuItem itemDisableAll = menu.add(0, disableRules, 0, getString(R.string.disable_rules));
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
				enableRules();
				break;
			case disableRules:
				disableRules();
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
	
	private class RulesDatabaseHelper extends SQLiteOpenHelper {
		public RulesDatabaseHelper(Context context) {
				super(context, "rules.sqlite", null, 1);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
				String scripts = "create table rules (_id integer primary key, rule text not null, type integer not null, description text, enabled text not null);";
				db.execSQL(scripts);
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
					case Constants.TYPE_BLACKLIST:
						rType.setText(getString(R.string.type_blacklist));
						break;
					case Constants.TYPE_WHITELIST:
						rType.setText(getString(R.string.type_whitelist));
						break;
					case Constants.TYPE_KEYWORD:
						rType.setText(getString(R.string.type_keyword));
						break;
				
				}
				
				if(ruleEnabled.equals("true")) {
					rRule.setTypeface(null, Typeface.BOLD);
				} else {
					rRule.setTypeface(null, Typeface.NORMAL);
					rRule.setTextColor(Color.GRAY);
					}
				rRule.setText(ruleName);
			}

	   
			public View newView(Context context, Cursor cursor, ViewGroup parent) {
					View view = rInflater.inflate(R.layout.rules_items, null);
					bindView(view, context, cursor);
					return view;
			}
	}
	
	private void refreshCursor() {
		rCursor = rDatabase.query(false, "rules", new String[] { "_id", "rule", "type", "enabled" }, null, null, null, null, null, null);
		rAdapter = new RulesAdapter(this, rCursor);
		setListAdapter(rAdapter);
	}
	
	private void enableRules() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.enable_rules)
		.setMessage(getString(R.string.enable_rules_confirm))
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
				rCursor.moveToFirst();
				ContentValues cv = new ContentValues();
				cv.put("enabled", "true");
				while (!rCursor.isAfterLast()) {
					int ruleId=rCursor.getInt(0); 
					rDatabase.update("rules", cv, "_id="+String.valueOf(ruleId), null);
					rCursor.moveToNext(); 
				  }
				refreshCursor();
			}
		})
		.setNegativeButton(R.string.cancel , new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
			}
		})
		.show();
	}
	
	private void disableRules() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.disable_rules)
		.setMessage(getString(R.string.disable_rules_confirm))
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
				rCursor.moveToFirst();
				ContentValues cv = new ContentValues();
				cv.put("enabled", "false");
				while (!rCursor.isAfterLast()) {
					int ruleId=rCursor.getInt(0); 
					rDatabase.update("rules", cv, "_id="+String.valueOf(ruleId), null);
					rCursor.moveToNext(); 
				  }
				refreshCursor();
			}
		})
		.setNegativeButton(R.string.cancel , new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
			}
		})
		.show();
	}
	
	private void createRule() {
		Intent i = new Intent(RulesList.this, EditRule.class);
		i.putExtra("editMode", false);
		startActivityForResult(i, Constants.RESULT_EDIT);
	}
	
	private void toggleRule(int ruleId) {
		rCursor = rDatabase.query("rules", new String[] { "enabled" }, "_id="+String.valueOf(ruleId), null, null, null, null);
		rCursor.moveToFirst();
		String ruleEnabled = rCursor.getString(0);
		if(ruleEnabled.equals("true")) {
			ruleEnabled = "false";
		} else {
			ruleEnabled = "true";
			}
		ContentValues cv = new ContentValues();
		cv.put("enabled", ruleEnabled);
		rDatabase.update("rules", cv, "_id="+String.valueOf(ruleId), null);
		refreshCursor();
	}
	
	private void editRule(int ruleId) {
		Intent i = new Intent(RulesList.this, EditRule.class);
		i.putExtra("ruleId", ruleId);
		i.putExtra("editMode", true);
		startActivityForResult(i, Constants.RESULT_EDIT);
	}
	
	private void deleteRule(int ruleId) {
		rDatabase.delete("rules", "_id="+String.valueOf(ruleId), null);
		refreshCursor();
	}
}