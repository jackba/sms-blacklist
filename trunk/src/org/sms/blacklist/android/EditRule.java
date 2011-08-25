package org.sms.blacklist.android;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class EditRule extends Activity {
	
	private Spinner mRuleType;
	private CheckBox mRuleEnabled;
	private EditText mEditRule;
	private Button mSave;
	private Button mDelete;
	
	private int RuleID;
	private boolean Edit;
	private Cursor mCursor;
	private String ruleEnabled;
	private int ruleType;
	private String ruleText;
	
	private SQLiteDatabase rDatabase;

	public void onCreate(Bundle icicle) {
	    super.onCreate(icicle);
	    final Intent intent = getIntent();
		RuleID = intent.getIntExtra("RuleID", 0);
		Edit = intent.getBooleanExtra("Edit", false);
		
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    
	    if (Edit) {
	    	setContentView(R.layout.edit_rule);
	    } else {
	    	setContentView(R.layout.create_rule);
	    }
	    
	    getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);


	    RulesDatabaseHelper rdh = new RulesDatabaseHelper(this);
		rDatabase = rdh.getWritableDatabase();

		mCursor = rDatabase.query(false, "rules", new String[] { "_id", "rule", "type", "enabled" }, "_id="+String.valueOf(RuleID), null, null, null, null, null);
		mCursor.moveToFirst();
	    
	    ruleEnabled = mCursor.getString(3);
	    mRuleEnabled = (CheckBox) findViewById(R.id.rule_enabled);
	    if (ruleEnabled.equals("true")) {
	    	mRuleEnabled.setChecked(true);
	    }
	    
	    ruleType = mCursor.getInt(2);
	    mRuleType = (Spinner) findViewById(R.id.rule_type);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.rule_type, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mRuleType.setAdapter(adapter);
	    mRuleType.setSelection(ruleType);
	    mRuleType.setOnItemSelectedListener(new RuleTypeSelectedListener());
	    
	    ruleText = mCursor.getString(1);
	    mEditRule = (EditText) findViewById(R.id.edit_rule);
	    if (ruleType == Constants.TYPE_K) {
	    	mEditRule.setInputType(InputType.TYPE_CLASS_TEXT);
	    } else {
	    	mEditRule.setInputType(InputType.TYPE_CLASS_NUMBER);
	    }
	    mEditRule.setText(ruleText);
	    
	    mSave = (Button) findViewById(R.id.save);
	    mSave.setOnClickListener(mOnSaveClickListener);
	    
	    if (Edit) {
	    	mDelete = (Button) findViewById(R.id.delete);
	    	mDelete.setOnClickListener(mOnDeleteClickListener);
	    }

	    ((Button)findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
	    
	}
	
	private class RulesDatabaseHelper extends SQLiteOpenHelper {
		public RulesDatabaseHelper(Context context) {
				super(context, "block_rules.db", null, 1);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
				String scripts = "create table rules (_id integer primary key, rule text not null, type integer not null, enabled text not null);";
				db.execSQL(scripts);
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}
	
	private class RuleTypeSelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	    	ruleType = pos;
	    	if (ruleType == Constants.TYPE_K) {
		    	mEditRule.setInputType(InputType.TYPE_CLASS_TEXT);
		    } else {
		    	mEditRule.setInputType(InputType.TYPE_CLASS_NUMBER);
		    }
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}
	
	private View.OnClickListener mOnSaveClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (ruleType == Constants.TYPE_K) {
		    	ruleText = mEditRule.getText().toString();
		    } else {
		    	ruleText = mEditRule.getText().toString().replaceAll("\\D", "");
		    	mEditRule.setText(ruleText);
		    }
			if (ruleText.length() > 0) {
				ContentValues cv = new ContentValues();
				cv.put("rule", ruleText);
				cv.put("type", ruleType);
				cv.put("enabled", ruleEnabled);
				if (Edit){
					rDatabase.update("rules", cv, "_id="+String.valueOf(RuleID), null);
				} else {
					rDatabase.insert("rules", "", cv);
				}
			} else {
				Toast toast = Toast.makeText(EditRule.this, getString(R.string.invaild_value), Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	};
	
	private View.OnClickListener mOnDeleteClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			
		}
	};
}