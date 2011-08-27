package org.sms.blacklist.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OpenMessage extends Activity {
	
	private Button mDelete;
	
	private int messageId;
	private TextView mNumber;
	private TextView mBody;
	private TextView mDate;

	private MessagesDatabaseAdapter mDatabaseAdapter;
	private Cursor mCursor;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		final Intent intent = getIntent();
		messageId = intent.getIntExtra("messageId", 0);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
		setContentView(R.layout.open_message);

		mDatabaseAdapter = new MessagesDatabaseAdapter(this);
		mDatabaseAdapter.open();
		mCursor = mDatabaseAdapter.getMessage(messageId);
		mDatabaseAdapter.close();

		long timestamp = mCursor.getLong(0);
		
		String messageDate = formatTimeStampString(this, timestamp);
		String messageNumber = mCursor.getString(1);
		String messageBody = mCursor.getString(2);

		mCursor.close();
		
		mNumber = (TextView) findViewById(R.id.number);
		mBody = (TextView) findViewById(R.id.message);
		mDate = (TextView) findViewById(R.id.date);
		
		mNumber.setText(messageNumber);
		mDate.setText(messageDate);
		mBody.setText(messageBody);
		
		mDelete = (Button) findViewById(R.id.delete);
		mDelete.setOnClickListener(mOnDeleteClickListener);

		((Button)findViewById(R.id.ok)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDatabaseAdapter = new MessagesDatabaseAdapter(OpenMessage.this);
				mDatabaseAdapter.open();
				mDatabaseAdapter.markRead(messageId);
				mDatabaseAdapter.close();
				setResult(RESULT_OK);
				finish();
			}
		});
	}
	
	public void onStop(){
		mDatabaseAdapter = new MessagesDatabaseAdapter(OpenMessage.this);
		mDatabaseAdapter.open();
		mDatabaseAdapter.markRead(messageId);
		mDatabaseAdapter.close();
		super.onStop();
	}
	
	private View.OnClickListener mOnDeleteClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			new AlertDialog.Builder(OpenMessage.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.delete)
			.setMessage(getString(R.string.delete_message_confirm))
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int i) {
					mDatabaseAdapter = new MessagesDatabaseAdapter(OpenMessage.this);
					mDatabaseAdapter.open();
					mDatabaseAdapter.deleteMessage(messageId);
					mDatabaseAdapter.close();
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
	
	public String formatTimeStampString(Context context, long timestamp) {
        Time then = new Time();
        then.set(timestamp);
        Time now = new Time();
        now.setToNow();

        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;

		format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);

        return DateUtils.formatDateTime(context, timestamp, format_flags);
    }
}