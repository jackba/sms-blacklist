package org.sms.blacklist.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
	
	private Spinner rType;
	private CheckBox rEnabled;
	private EditText rEditRule;
	private Button rSave;
	private Button rDelete;
	
	private RulesDatabaseAdapter rDatabaseAdapter;
	
	private int ruleId;
	private boolean editMode;
	private Cursor rCursor;
	private String enabled;
	private int type;
	private String rule;
	
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		final Intent intent = getIntent();
		ruleId = intent.getIntExtra("ruleId", 0);
		editMode = intent.getBooleanExtra("editMode", false);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
		
		if (editMode) {
			setContentView(R.layout.edit_rule);
			rDatabaseAdapter = new RulesDatabaseAdapter(this);
			rDatabaseAdapter.open();
			rCursor = rDatabaseAdapter.getRule(ruleId);
			rDatabaseAdapter.close();
			
			enabled = rCursor.getString(2);
			rEnabled = (CheckBox) findViewById(R.id.rule_enabled);
			if (enabled.equals("true")) {
				rEnabled.setChecked(true);
			}
			rEnabled.setOnCheckedChangeListener(rEnabledChangeListener);
		} else {
			setContentView(R.layout.create_rule);
		}

		rType = (Spinner) findViewById(R.id.rule_type);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.rule_type, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		rType.setAdapter(adapter);
		if (editMode) {
			type = rCursor.getInt(1);
			rType.setSelection(type);
		}
		rType.setOnItemSelectedListener(new typeSelectedListener());
		
		
		rEditRule = (EditText) findViewById(R.id.edit_rule);
		if (type == Constants.TYPE_BLOCKED_KEYWORD) {
			rEditRule.setInputType(InputType.TYPE_CLASS_TEXT);
			rEditRule.setHint(R.string.rule_hint_keyword);
		} else {
			rEditRule.setInputType(InputType.TYPE_CLASS_PHONE);
			rEditRule.setHint(R.string.rule_hint_number);
		}
		if (editMode) {
			rule = rCursor.getString(0);
			rEditRule.setText(rule);
		}
		
		rSave = (Button) findViewById(R.id.save);
		rSave.setOnClickListener(mOnSaveClickListener);
		
		if (editMode) {
			
			rCursor.close();
			
			rDelete = (Button) findViewById(R.id.delete);
			rDelete.setOnClickListener(rOnDeleteClickListener);
		}

		((Button)findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private class typeSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			type = pos;
			if (type == Constants.TYPE_BLOCKED_KEYWORD) {
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
			if (type == Constants.TYPE_BLOCKED_KEYWORD) {
				rule = rEditRule.getText().toString();
			} else {
				rule = rEditRule.getText().toString().replaceAll("[^0-9N#?*]", "").replaceAll("[N#]", "?");
				rEditRule.setText(rule);
			}
			if (rule.length() > 0) {
				if (!editMode) {
					enabled = "true";
				}
				rDatabaseAdapter = new RulesDatabaseAdapter(EditRule.this);
				rDatabaseAdapter.open();
				if (editMode){
					rDatabaseAdapter.updateRule(ruleId, rule, type, enabled);
				} else {
					rDatabaseAdapter.insertRule(rule, type, enabled);
				}
				rDatabaseAdapter.close();
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
					rDatabaseAdapter = new RulesDatabaseAdapter(EditRule.this);
					rDatabaseAdapter.open();
					rDatabaseAdapter.deleteRule(ruleId);
					rDatabaseAdapter.close();
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
			enabled = String.valueOf(checked);
		}
	};
}