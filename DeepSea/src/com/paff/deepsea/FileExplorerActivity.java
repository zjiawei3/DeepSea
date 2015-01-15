package com.paff.deepsea;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import com.paff.deepsea.util.MimeTypeParser;
import com.paff.deepsea.util.MimeTypes;
import com.paff.deepsea.util.FileUtils;
import com.paff.deepsea.intents.FileManagerIntents;
import com.paff.deepsea.localfile.IconifiedText;
import com.paff.deepsea.localfile.DirectoryScanner;
import com.paff.deepsea.localfile.ThumbnailLoader;
import com.paff.deepsea.localfile.DirectoryContents;
import com.paff.deepsea.localfile.IconifiedTextListAdapter;

public class FileExplorerActivity extends ListActivity
{
	private static final String TAG = "FileExplorerActivity";
	private static final String NOMEDIA_FILE = ".nomedia";
	private static final Character FILE_EXTENSION_SEPARATOR = '.';

	static final public int MESSAGE_SHOW_DIRECTORY_CONTENTS = 500; // List of
																	// contents
																	// is ready,
																	// obj =
																	// DirectoryContents
	static final public int MESSAGE_SET_PROGRESS = 501; // Set progress bar,
														// arg1 = current value,
														// arg2 = max value
	static final public int MESSAGE_ICON_CHANGED = 502; // View needs to be
														// redrawn, obj =
														// IconifiedText

	private int m_State;
	private static final int STATE_BROWSE = 1;
	private static final int STATE_PICK_FILE = 2;
	private static final int STATE_PICK_DIRECTORY = 3;
	private static final int STATE_MULTI_SELECT = 4;

	private Button m_ButtonMove;

	private Button m_ButtonCopy;

	private Button m_ButtonDelete;

	private LinearLayout m_DirectoryInput;
	private EditText m_EditDirectory;
	private ImageButton m_ButtonDirectoryPick;

	private LinearLayout m_ActionNormal;
	private LinearLayout m_ActionMultiselect;
	private TextView m_EmptyText;
	private ProgressBar m_ProgressBar;

	private EditText m_EditFilename;
	private Button m_ButtonPick;
	private LinearLayout m_DirectoryButtons;

	// There's a ".nomedia" file here
	private boolean mNoMedia;

	private File currentDirectory = new File("");

	private String m_SdCardPath = "";

	private MimeTypes m_MimeTypes;

	private boolean m_WritableOnly;

	private static final String BUNDLE_CURRENT_DIRECTORY = "current_directory";
	private static final String BUNDLE_CONTEXT_FILE = "context_file";
	private static final String BUNDLE_CONTEXT_TEXT = "context_text";
	private static final String BUNDLE_SHOW_DIRECTORY_INPUT = "show_directory_input";
	private static final String BUNDLE_STEPS_BACK = "steps_back";

	private String m_ContextText;
	private File m_ContextFile = new File("");

	/** How many steps one can make back using the back key. */
	private int m_StepsBack;

	private File m_PreviousDirectory;

	private Handler msgCurrentHandler;
	private DirectoryScanner m_DirectoryScanner;
	private ThumbnailLoader m_ThumbnailLoader;

	/** Contains directories and files together */
	private ArrayList<IconifiedText> m_DirectoryEntries = new ArrayList<IconifiedText>();

	/** Dir separate for sorting */
	List<IconifiedText> m_ListDir = new ArrayList<IconifiedText>();

	/** Files separate for sorting */
	List<IconifiedText> m_ListFile = new ArrayList<IconifiedText>();

	/** SD card separate for sorting */
	List<IconifiedText> m_ListSdCard = new ArrayList<IconifiedText>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		msgCurrentHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				FileExplorerActivity.this.handleMessage(msg);
			}
		};

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.fileexplorer);

		m_EmptyText = (TextView) findViewById(R.id.empty_text);
		m_ProgressBar = (ProgressBar) findViewById(R.id.scan_progress);

		getListView().setOnCreateContextMenuListener(this);
		getListView().setEmptyView(findViewById(R.id.empty));
		getListView().setTextFilterEnabled(true);
		getListView().requestFocus();
		getListView().requestFocusFromTouch();

		m_DirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
		m_ActionNormal = (LinearLayout) findViewById(R.id.action_normal);
		m_ActionMultiselect = (LinearLayout) findViewById(R.id.action_multiselect);
		m_EditFilename = (EditText) findViewById(R.id.filename);

		m_ButtonPick = (Button) findViewById(R.id.button_pick);

		m_ButtonPick.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View arg0)
			{
				pickFileOrDirectory();
			}
		});

		// Initialize only when necessary:
		m_DirectoryInput = null;

		// Create map of extensions:
		getMimeTypes();

		getSdCardPath();

		m_State = STATE_BROWSE;

		Intent intent = getIntent();
		String action = intent.getAction();

		File browseto = new File("/");

		if (!TextUtils.isEmpty(m_SdCardPath))
		{
			browseto = new File(m_SdCardPath);
		}

		// Default state
		m_State = STATE_BROWSE;
		m_WritableOnly = false;

		if (action != null)
		{
			if (action.equals(FileManagerIntents.ACTION_PICK_FILE))
			{
				m_State = STATE_PICK_FILE;
			}
			else if (action.equals(FileManagerIntents.ACTION_PICK_DIRECTORY))
			{
				m_State = STATE_PICK_DIRECTORY;
				m_WritableOnly = intent.getBooleanExtra(FileManagerIntents.EXTRA_WRITEABLE_ONLY, false);

				// Remove edit text and make button fill whole line
				m_EditFilename.setVisibility(View.GONE);
				m_ButtonPick.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT));
			}
			else if (action.equals(FileManagerIntents.ACTION_MULTI_SELECT))
			{
				m_State = STATE_MULTI_SELECT;

				// Remove buttons
				m_DirectoryButtons.setVisibility(View.GONE);
				m_ActionNormal.setVisibility(View.GONE);

				// Multi select action: move
				m_ButtonMove = (Button) findViewById(R.id.button_move);
				m_ButtonMove.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View arg0)
					{
						// TODO: Click on 'MOVE' button
					}
				});

				// Multi select action: copy
				m_ButtonCopy = (Button) findViewById(R.id.button_copy);
				m_ButtonCopy.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View arg0)
					{
						// TODO: Click on 'COPY' button
					}
				});

				// Multi select action: delete
				m_ButtonDelete = (Button) findViewById(R.id.button_delete);
				m_ButtonDelete.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View arg0)
					{
						// TODO: Click on 'DELETE' button
					}
				});
			}
		}

		if (m_State == STATE_BROWSE)
		{
			// Remove edit text and button.
			m_EditFilename.setVisibility(View.GONE);
			m_ButtonPick.setVisibility(View.GONE);
		}

		if (m_State != STATE_MULTI_SELECT)
		{
			// Remove multiselect action buttons
			m_ActionMultiselect.setVisibility(View.GONE);
		}

		// Set current directory and file based on intent data.
		File file = FileUtils.getFile(intent.getData());
		if (file != null)
		{
			File dir = FileUtils.getPathWithoutFilename(file);
			if (dir.isDirectory())
			{
				browseto = dir;
			}
			if (!file.isDirectory())
			{
				m_EditFilename.setText(file.getName());
			}
		}

		String title = intent.getStringExtra(FileManagerIntents.EXTRA_TITLE);
		if (title != null)
		{
			setTitle(title);
		}

		String buttontext = intent.getStringExtra(FileManagerIntents.EXTRA_BUTTON_TEXT);
		if (buttontext != null)
		{
			m_ButtonPick.setText(buttontext);
		}

		m_StepsBack = 0;

		if (savedInstanceState != null)
		{
			browseto = new File(savedInstanceState.getString(BUNDLE_CURRENT_DIRECTORY));
			m_ContextFile = new File(savedInstanceState.getString(BUNDLE_CONTEXT_FILE));
			m_ContextText = savedInstanceState.getString(BUNDLE_CONTEXT_TEXT);

			boolean show = savedInstanceState.getBoolean(BUNDLE_SHOW_DIRECTORY_INPUT);
			showDirectoryInput(show);

			m_StepsBack = savedInstanceState.getInt(BUNDLE_STEPS_BACK);
		}

		browseTo(browseto);
	}

	public void onDestroy()
	{
		super.onDestroy();

		// Stop the scanner.
		DirectoryScanner scanner = m_DirectoryScanner;

		if (scanner != null)
		{
			scanner.setCancel(true);
		}

		m_DirectoryScanner = null;

		ThumbnailLoader loader = m_ThumbnailLoader;

		if (loader != null)
		{
			loader.setCancel(true);
			m_ThumbnailLoader = null;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);

		// remember file name
		outState.putString(BUNDLE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
		outState.putString(BUNDLE_CONTEXT_FILE, m_ContextFile.getAbsolutePath());
		outState.putString(BUNDLE_CONTEXT_TEXT, m_ContextText);
		boolean show = isDirectoryInputVisible();
		outState.putBoolean(BUNDLE_SHOW_DIRECTORY_INPUT, show);
		outState.putInt(BUNDLE_STEPS_BACK, m_StepsBack);
	}

	private void getMimeTypes()
	{
		MimeTypeParser mtp = new MimeTypeParser();

		XmlResourceParser in = getResources().getXml(R.xml.mimetypes);

		try
		{
			m_MimeTypes = mtp.fromXmlResource(in);
		}
		catch (XmlPullParserException e)
		{
			Log.e(TAG, "PreselectedChannelsActivity: XmlPullParserException", e);
			throw new RuntimeException("PreselectedChannelsActivity: XmlPullParserException");
		}
		catch (IOException e)
		{
			Log.e(TAG, "PreselectedChannelsActivity: IOException", e);
			throw new RuntimeException("PreselectedChannelsActivity: IOException");
		}
	}

	private void getSdCardPath()
	{
		m_SdCardPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	/**
	 * Show the directory line as input box instead of button row. If Directory
	 * input does not exist yet, it is created. Since the default is show ==
	 * false, nothing is created if it is not necessary (like after
	 * savedInstanceState).
	 * 
	 * @param show
	 */
	private void showDirectoryInput(boolean show)
	{
		if (show)
		{
			if (m_DirectoryInput == null)
			{
				onCreateDirectoryInput();
			}
		}
		if (m_DirectoryInput != null)
		{
			m_DirectoryInput.setVisibility(show ? View.VISIBLE : View.GONE);
			m_DirectoryButtons.setVisibility(show ? View.GONE : View.VISIBLE);
		}

		refreshDirectoryPanel();
	}

	private void onCreateDirectoryInput()
	{
		m_DirectoryInput = (LinearLayout) findViewById(R.id.directory_input);
		m_EditDirectory = (EditText) findViewById(R.id.directory_text);

		m_ButtonDirectoryPick = (ImageButton) findViewById(R.id.button_directory_pick);

		m_ButtonDirectoryPick.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View arg0)
			{
				goToDirectoryInEditText();
			}
		});
	}

	private void refreshDirectoryPanel()
	{
		if (isDirectoryInputVisible())
		{
			// Set directory path
			String path = currentDirectory.getAbsolutePath();
			m_EditDirectory.setText(path);

			// Set selection to last position so user can continue to type:
			m_EditDirectory.setSelection(path.length());
		}
		else
		{
			setDirectoryButtons();
		}
	}

	// private boolean mHaveShownErrorMessage;
	private File mHaveShownErrorMessageForFile = null;

	private void goToDirectoryInEditText()
	{
		File browseto = new File(m_EditDirectory.getText().toString());
		if (browseto.equals(currentDirectory))
		{
			showDirectoryInput(false);
		}
		else
		{
			if (mHaveShownErrorMessageForFile != null && mHaveShownErrorMessageForFile.equals(browseto))
			{
				// Don't let user get stuck in wrong directory.
				mHaveShownErrorMessageForFile = null;
				showDirectoryInput(false);
			}
			else
			{
				if (!browseto.exists())
				{
					// browseTo() below will show an error message,
					// because file does not exist.
					// It is ok to show this the first time.
					mHaveShownErrorMessageForFile = browseto;
				}
				browseTo(browseto);
			}
		}
	}

	private boolean isDirectoryInputVisible()
	{
		return ((m_DirectoryInput != null) && (m_DirectoryInput.getVisibility() == View.VISIBLE));
	}

	private void setDirectoryButtons()
	{
		String[] parts = currentDirectory.getAbsolutePath().split("/");

		m_DirectoryButtons.removeAllViews();

		int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;

		// Add home button separately
		ImageButton ib = new ImageButton(this);
		ib.setImageResource(R.drawable.icn_home_small);
		ib.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
		ib.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				jumpTo(new File("/"));
			}
		});
		m_DirectoryButtons.addView(ib);

		// Add other buttons

		String dir = "";

		for (int i = 1; i < parts.length; i++)
		{
			dir += "/" + parts[i];
			if (dir.equals(m_SdCardPath))
			{
				// Add SD card button
				ib = new ImageButton(this);
				ib.setImageResource(R.drawable.icn_sdcard_small);
				ib.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
				ib.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View view)
					{
						jumpTo(new File(m_SdCardPath));
					}
				});
				m_DirectoryButtons.addView(ib);
			}
			else
			{
				Button b = new Button(this);
				b.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
				b.setText(parts[i]);
				b.setTag(dir);
				b.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View view)
					{
						String dir = (String) view.getTag();
						jumpTo(new File(dir));
					}
				});
				m_DirectoryButtons.addView(b);
			}
		}

		checkButtonLayout();
	}

	private void checkButtonLayout()
	{
		// Let's measure how much space we need:
		int spec = View.MeasureSpec.UNSPECIFIED;
		m_DirectoryButtons.measure(spec, spec);
		int count = m_DirectoryButtons.getChildCount();

		int requiredwidth = m_DirectoryButtons.getMeasuredWidth();
		int width = getWindowManager().getDefaultDisplay().getWidth();

		if (requiredwidth > width)
		{
			int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;

			// Create a new button that shows that there is more to the left:
			ImageButton ib = new ImageButton(this);
			ib.setImageResource(R.drawable.fe_button_back);
			ib.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
			//
			ib.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View view)
				{
					// Up one directory.
					upOneLevel();
				}
			});
			m_DirectoryButtons.addView(ib, 0);

			// New button needs even more space
			ib.measure(spec, spec);
			requiredwidth += ib.getMeasuredWidth();

			// Need to take away some buttons
			// but leave at least "back" button and one directory button.
			while (requiredwidth > width && m_DirectoryButtons.getChildCount() > 2)
			{
				View view = m_DirectoryButtons.getChildAt(1);
				requiredwidth -= view.getMeasuredWidth();

				m_DirectoryButtons.removeViewAt(1);
			}
		}
	}

	private void upOneLevel()
	{
		if (m_StepsBack > 0)
		{
			m_StepsBack--;
		}
		if (currentDirectory.getParent() != null)
			browseTo(currentDirectory.getParentFile());
	}

	private void browseTo(final File aDirectory)
	{
		// setTitle(aDirectory.getAbsolutePath());
		if (aDirectory.isDirectory())
		{
			if (aDirectory.equals(currentDirectory))
			{
				// Switch from button to directory input
				showDirectoryInput(true);
			}
			else
			{
				m_PreviousDirectory = currentDirectory;
				currentDirectory = aDirectory;
				refreshList();
				// selectInList(previousDirectory);
				// refreshDirectoryPanel();
			}
		}
		else
		{
			if (m_State == STATE_BROWSE || m_State == STATE_PICK_DIRECTORY)
			{
				// Lets start an intent to View the file, that was clicked...
				openFile(aDirectory);
			}
			else if (m_State == STATE_PICK_FILE)
			{
				// Pick the file
				m_EditFilename.setText(aDirectory.getName());
			}
		}
	}

	private void jumpTo(final File aDirectory)
	{
		m_StepsBack = 0;
		browseTo(aDirectory);
	}

	private void refreshList()
	{
		boolean directoriesOnly = m_State == STATE_PICK_DIRECTORY;

		// Cancel an existing scanner, if applicable.
		DirectoryScanner scanner = m_DirectoryScanner;
		if (scanner != null)
		{
			scanner.setCancel(true);
		}

		ThumbnailLoader loader = m_ThumbnailLoader;

		if (loader != null)
		{
			loader.setCancel(true);
			m_ThumbnailLoader = null;
		}

		m_DirectoryEntries.clear();
		m_ListDir.clear();
		m_ListFile.clear();
		m_ListSdCard.clear();

		setProgressBarIndeterminateVisibility(true);

		// Don't show the "folder empty" text since we're scanning.
		m_EmptyText.setVisibility(View.GONE);

		// Also DON'T show the progress bar - it's kind of lame to show that
		// for less than a second.
		m_ProgressBar.setVisibility(View.GONE);
		setListAdapter(null);

		m_DirectoryScanner = new DirectoryScanner(currentDirectory, this, msgCurrentHandler, m_MimeTypes, m_SdCardPath,
				m_WritableOnly, directoriesOnly);
		m_DirectoryScanner.start();

		// Add the "." == "current directory"
		/*
		 * directoryEntries.add(new IconifiedText(
		 * getString(R.string.current_dir),
		 * getResources().getDrawable(R.drawable.ic_launcher_folder)));
		 */
		// and the ".." == 'Up one level'
		/*
		 * if(currentDirectory.getParent() != null) directoryEntries.add(new
		 * IconifiedText( getString(R.string.up_one_level),
		 * getResources().getDrawable(R.drawable.ic_launcher_folder_open)));
		 */
	}

	private void openFile(File aFile)
	{
		if (!aFile.exists())
		{
			Toast.makeText(this, R.string.error_file_does_not_exists, Toast.LENGTH_SHORT).show();
			return;
		}

		Intent resultIntent = new Intent();
		// TODO Add extras or a data URI to this intent as appropriate.
		resultIntent.setData(Uri.parse(FileExplorerProvider.FILE_PROVIDER_PREFIX + aFile));
		setResult(RESULT_OK, resultIntent);
		finish();
		
		/*
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		Uri data = FileUtils.getUri(aFile);
		String type = m_MimeTypes.getMimeType(aFile.getName());
		intent.setDataAndType(data, type);

		// Were we in GET_CONTENT mode?
		Intent originalIntent = getIntent();

		if (originalIntent != null && originalIntent.getAction() != null
				&& originalIntent.getAction().equals(Intent.ACTION_GET_CONTENT))
		{
			// In that case, we should probably just return the requested data.
			intent.setData(Uri.parse(FileExplorerProvider.FILE_PROVIDER_PREFIX + aFile));
			setResult(RESULT_OK, intent);
			finish();
			return;
		}

		try
		{
			startActivity(intent);
		}
		catch (ActivityNotFoundException e)
		{
			Toast.makeText(this, R.string.application_not_available, Toast.LENGTH_SHORT).show();
		}
		*/
	}

	private void handleMessage(Message message)
	{
		// Log.v(TAG, "Received message " + message.what);
		switch (message.what)
		{
			case MESSAGE_SHOW_DIRECTORY_CONTENTS:
				showDirectoryContents((DirectoryContents) message.obj);
				break;

			case MESSAGE_SET_PROGRESS:
				setProgress(message.arg1, message.arg2);
				break;

			case MESSAGE_ICON_CHANGED:
				notifyIconChanged((IconifiedText) message.obj);
				break;
		}
	}

	private void addAllElements(List<IconifiedText> addTo, List<IconifiedText> addFrom)
	{
		int size = addFrom.size();
		for (int i = 0; i < size; i++)
		{
			addTo.add(addFrom.get(i));
		}
	}

	private void selectInList(File selectFile)
	{
		String filename = selectFile.getName();
		IconifiedTextListAdapter la = (IconifiedTextListAdapter) getListAdapter();
		int count = la.getCount();
		for (int i = 0; i < count; i++)
		{
			IconifiedText it = (IconifiedText) la.getItem(i);
			if (it.getText().equals(filename))
			{
				getListView().setSelection(i);
				break;
			}
		}
	}

	private void showDirectoryContents(DirectoryContents contents)
	{
		m_DirectoryScanner = null;

		m_ListSdCard = contents.getListSdCard();
		m_ListDir = contents.getListDirectory();
		m_ListFile = contents.getListFile();
		mNoMedia = contents.getNoMedia();

		m_DirectoryEntries.ensureCapacity(m_ListSdCard.size() + m_ListDir.size() + m_ListFile.size());

		addAllElements(m_DirectoryEntries, m_ListSdCard);
		addAllElements(m_DirectoryEntries, m_ListDir);
		addAllElements(m_DirectoryEntries, m_ListFile);

		IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
		itla.setListItems(m_DirectoryEntries, getListView().hasTextFilter());
		setListAdapter(itla);
		getListView().setTextFilterEnabled(true);

		selectInList(m_PreviousDirectory);
		refreshDirectoryPanel();
		setProgressBarIndeterminateVisibility(false);

		m_ProgressBar.setVisibility(View.GONE);
		m_EmptyText.setVisibility(View.VISIBLE);

		m_ThumbnailLoader = new ThumbnailLoader(currentDirectory, m_ListFile, msgCurrentHandler, this, m_MimeTypes);
		m_ThumbnailLoader.start();
	}

	private void setProgress(int progress, int maxProgress)
	{
		m_ProgressBar.setMax(maxProgress);
		m_ProgressBar.setProgress(progress);
		m_ProgressBar.setVisibility(View.VISIBLE);
	}

	private void notifyIconChanged(IconifiedText text)
	{
		if (getListAdapter() != null)
		{
			((BaseAdapter) getListAdapter()).notifyDataSetChanged();
		}
	}

	private void pickFileOrDirectory()
	{
		File file = null;
		if (m_State == STATE_PICK_FILE)
		{
			String filename = m_EditFilename.getText().toString();
			file = FileUtils.getFile(currentDirectory.getAbsolutePath(), filename);
		}
		else if (m_State == STATE_PICK_DIRECTORY)
		{
			file = currentDirectory;
		}

		Intent intent = getIntent();
		intent.setData(FileUtils.getUri(file));
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) getListAdapter();

		if (adapter == null)
		{
			return;
		}

		IconifiedText text = (IconifiedText) adapter.getItem(position);

		if (m_State == STATE_MULTI_SELECT)
		{
			text.setSelected(!text.isSelected());
			adapter.notifyDataSetChanged();
			return;
		}

		String file = text.getText();
		/*
		 * if (selectedFileString.equals(getString(R.string.up_one_level))) {
		 * upOneLevel(); } else {
		 */
		String curdir = currentDirectory.getAbsolutePath();
		File clickedFile = FileUtils.getFile(curdir, file);
		if (clickedFile != null)
		{
			if (clickedFile.isDirectory())
			{
				// If we click on folders, we can return later by the "back"
				// key.
				m_StepsBack++;
			}
			browseTo(clickedFile);
		}
	}
	
	// This code seems to work for SDK 2.3 (target="9")
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (m_StepsBack > 0) {
				upOneLevel();
				return true;
			}
		}
		
		return super.onKeyDown(keyCode, event);
	}
}
