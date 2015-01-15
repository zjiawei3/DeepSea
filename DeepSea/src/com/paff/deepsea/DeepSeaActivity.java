package com.paff.deepsea;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;


public class DeepSeaActivity extends Activity implements OnClickListener
{
	private static final String TAG = "DeepSeaActivity";
	private static final int SELECT_FILE_BY_FILEEXPLORER = 1;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// set up click listeners for all the buttons
		View continueButton = this.findViewById(R.id.continue_button);
		continueButton.setOnClickListener(this);
		View newButton = this.findViewById(R.id.new_button);
		newButton.setOnClickListener(this);
		View aboutButton = this.findViewById(R.id.about_button);
		aboutButton.setOnClickListener(this);
		View exitButton = this.findViewById(R.id.exit_button);
		exitButton.setOnClickListener(this);
		
		Log.i("Info", "Just first log message!");
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.continue_button:
				continueUploadFile();
				break;
			case R.id.about_button:
				//Intent i = new Intent(this, About.class);
				//startActivity(i);
				break;
			case R.id.new_button:
				openNewFileUploadDialog();
				break;
			case R.id.exit_button:
				finish();
				break;
			// more buttons go here (if any) ...
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		/*switch (item.getItemId())
		{
			case R.id.settings:
				startActivity(new Intent(this, Settings.class));
				return true;
				// More items go here (if any) ...
		}*/
		return true;
	}
	
	private void openNewFileUploadDialog()
	{
		startActivityForResult(new Intent(this, FileExplorerActivity.class), SELECT_FILE_BY_FILEEXPLORER);
	}
	
	private void continueUploadFile()
	{
		
	}
	
	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{  
		super.onActivityResult(requestCode, resultCode, data);  
		switch(requestCode) {    
			case (SELECT_FILE_BY_FILEEXPLORER) : 
			{      
				if (resultCode == Activity.RESULT_OK)
				{
					// TODO Extract the data returned from the child Activity.   
					Log.v(TAG, "Received Activity Result " + data.getDataString());
					break;
				}
			}
		}
	}
}