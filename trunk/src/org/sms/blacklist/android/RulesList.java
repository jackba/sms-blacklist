package org.sms.blacklist.android;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class RulesList extends ListActivity {
	
	LayoutInflater mInflater;
	ListView mListView;
	Cursor mCursor;
	SQLiteDatabase mDatabase;
	RulesAdapter mAdapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rules_list);
		
		mListView = (ListView) findViewById(android.R.id.list);
        mInflater = getLayoutInflater();
        
        RulesDatabaseHelper helper = new RulesDatabaseHelper(this);                 
        mDatabase = helper.getWritableDatabase(); 

        refreshCursor();
        
      //Listview clicked
		mListView.setOnItemClickListener(
			new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//Execute the selected script
					mCursor.moveToPosition(position);
					int RuleId = mCursor.getInt(0);
					Intent i = new Intent(RulesList.this, EditRule.class);
					i.putExtra("RuleID", RuleId);
					i.putExtra("Edit", true);
					startActivity(i);
				}
			}
		);
	}
	
	@Override
    public void onResume() {
	    refreshCursor();
	    super.onResume();
    }
	
	public void onStop() {
		mCursor.close();
		super.onStop();
	}
	
	private class RulesDatabaseHelper extends SQLiteOpenHelper {
		public RulesDatabaseHelper(Context context) {
				super(context, "block_rules.db", null, 1);
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

				TextView mRule = (TextView) view.findViewById(R.id.rule_text);
				TextView mType = (TextView) view.findViewById(R.id.rule_type);

				String ruleName = cursor.getString(1);
				int ruleType = cursor.getInt(2);
				String ruleEnabled = cursor.getString(3);
				
				switch (ruleType) {
					case Constants.TYPE_E:
						mType.setText(getString(R.string.type_e));
						break;
					case Constants.TYPE_S:
						mType.setText(getString(R.string.type_s));
						break;
					case Constants.TYPE_C:
						mType.setText(getString(R.string.type_c));
						break;
					case Constants.TYPE_K:
						mType.setText(getString(R.string.type_k));
						break;
					case Constants.TYPE_W:
						mType.setText(getString(R.string.type_w));
						break;
				
				}
				
				if(ruleEnabled.equals("true")) {
					mRule.setTypeface(null, Typeface.BOLD);
				} else {
					mRule.setTypeface(null, Typeface.NORMAL);
					mRule.setTextColor(Color.GRAY);
					}
				mRule.setText(ruleName);
			}

	   
			public View newView(Context context, Cursor cursor, ViewGroup parent) {
					View view = mInflater.inflate(R.layout.rules_items, null);
					bindView(view, context, cursor);
					return view;
			}
	}
	
	private void refreshCursor() {
        mCursor = mDatabase.query(false, "rules", new String[] { "_id", "rule", "type", "enabled" }, null, null, null, null, null, null);
        mAdapter = new RulesAdapter(this, mCursor);
        setListAdapter(mAdapter);
	}
}