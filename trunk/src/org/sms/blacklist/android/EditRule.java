package org.sms.blacklist.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class EditRule extends Activity {
	
	private Spinner rRuleType;
	private CheckBox rRuleEnabled;
	private EditText rEditRule;
	private Button rSave;
	private Button rDelete;
	
	private int ruleId;
	private boolean editMode;
	private Cursor rCursor;
	private String ruleEnabled;
	private int ruleType;
	private String ruleText;
	
	private SQLiteDatabase rDatabase;
	
	public void onCreate(Bundle icicle) {
	    super.onCreate(icicle);
	    final Intent intent = getIntent();
		ruleId = intent.getIntExtra("ruleId", 0);
		editMode = intent.getBooleanExtra("editMode", false);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
	    
	    RulesDatabaseHelper rhelper = new RulesDatabaseHelper(this);
		rDatabase = rhelper.getWritableDatabase();
	    
	    if (editMode) {
	    	setContentView(R.layout.edit_rule);
	    	rCursor = rDatabase.query("rules", new String[] { "rule", "type", "enabled" }, "_id="+String.valueOf(ruleId), null, null, null, null);
			rCursor.moveToFirst();
	    	
			ruleEnabled = rCursor.getString(2);
		    rRuleEnabled = (CheckBox) findViewById(R.id.rule_enabled);
		    if (ruleEnabled.equals("true")) {
		    	rRuleEnabled.setChecked(true);
		    }
		    rRuleEnabled.setOnCheckedChangeListener(rEnabledChangeListener);
	    } else {
	    	setContentView(R.layout.create_rule);
	    }

	    rRuleType = (Spinner) findViewById(R.id.rule_type);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.rule_type, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    rRuleType.setAdapter(adapter);
	    if (editMode) {
		    ruleType = rCursor.getInt(1);
		    rRuleType.setSelection(ruleType);
	    }
	    rRuleType.setOnItemSelectedListener(new RuleTypeSelectedListener());
	    
	    
	    rEditRule = (EditText) findViewById(R.id.edit_rule);
	    if (ruleType == Constants.TYPE_KEYWORD) {
	    	rEditRule.setInputType(InputType.TYPE_CLASS_TEXT);
	    	rEditRule.setHint(R.string.rule_hint_keyword);
	    } else {
	    	rEditRule.setInputType(InputType.TYPE_CLASS_PHONE);
	    	rEditRule.setHint(R.string.rule_hint_number);
	    }
	    if (editMode) {
	    	ruleText = rCursor.getString(0);
		    rEditRule.setText(ruleText);
	    }
	    
	    
	    rSave = (Button) findViewById(R.id.save);
	    rSave.setOnClickListener(mOnSaveClickListener);
	    
	    if (editMode) {
	    	rDelete = (Button) findViewById(R.id.delete);
	    	rDelete.setOnClickListener(rOnDeleteClickListener);
	    }

	    ((Button)findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
	    
	}
	
	private class RulesDatabaseHelper extends SQLiteOpenHelper {
		public RulesDatabaseHelper(Context context) {
				super(context, "rules.sqlite", null, 1);
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
	    	if (ruleType == Constants.TYPE_KEYWORD) {
		    	rEditRule.setInputType(InputType.TYPE_CLASS_TEXT);
		    	rEditRule.setHint(R.string.rule_hint_keyword);
		    } else {
		    	rEditRule.setInputType(InputType.TYPE_CLASS_PHONE);
		    	rEditRule.setHint(R.string.rule_hint_number);
		    }
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}
	
	
	private View.OnClickListener mOnSaveClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (ruleType == Constants.TYPE_KEYWORD) {
		    	ruleText = rEditRule.getText().toString();
		    } else {
		    	ruleText = rEditRule.getText().toString().replaceAll("[^0-9N*]", "").replaceAll("N", "?");
		    	rEditRule.setText(ruleText);
		    }
			if (ruleText.length() > 0) {
				if (!editMode) {
					ruleEnabled = "true";
				}
				ContentValues cv = new ContentValues();
				cv.put("rule", ruleText);
				cv.put("type", ruleType);
				cv.put("enabled", ruleEnabled);
				if (editMode){
					rDatabase.update("rules", cv, "_id="+String.valueOf(ruleId), null);
				} else {
					rDatabase.insert("rules", "", cv);
				}
				rDatabase.close();
				setResult(RESULT_OK);
				finish();
			} else {
				Toast toast = Toast.makeText(EditRule.this, getString(R.string.invaild_value), Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	};
	
	private View.OnClickListener rOnDeleteClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			new AlertDialog.Builder(EditRule.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.delete)
			.setMessage(getString(R.string.delete_rule_confirm))
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int i) {
					rDatabase.delete("rules", "_id="+String.valueOf(ruleId), null);
					rDatabase.close();
					setResult(RESULT_OK);
					finish();
				}
			})
			.setNegativeButton(R.string.cancel , new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int i) {
				}
			})
			.show();

		}
	};
	
	private OnCheckedChangeListener rEnabledChangeListener = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(CompoundButton button, boolean checked) {
			ruleEnabled = String.valueOf(checked);
		}
	};
}