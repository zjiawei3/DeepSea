package com.paff.deepsea.intents;

public final class FileManagerIntents
{

	/**
	 * Activity Action: Pick a file through the file manager, or let user
	 * specify a custom file name. Data is the current file name or file name
	 * suggestion. Returns a new file name as file URI in data.
	 * 
	 * <p>
	 * Constant Value: "com.paff.deepsea.action.PICK_FILE"
	 * </p>
	 */
	public static final String ACTION_PICK_FILE = "com.paff.deepsea.action.PICK_FILE";

	/**
	 * Activity Action: Pick a directory through the file manager, or let user
	 * specify a custom file name. Data is the current directory name or
	 * directory name suggestion. Returns a new directory name as file URI in
	 * data.
	 * 
	 * <p>
	 * Constant Value: "com.paff.deepsea.action.PICK_DIRECTORY"
	 * </p>
	 */
	public static final String ACTION_PICK_DIRECTORY = "com.paff.deepsea.action.PICK_DIRECTORY";

	/**
	 * Activity Action: Move, copy or delete after select entries. Data is the
	 * current directory name or directory name suggestion.
	 * 
	 * <p>
	 * Constant Value: "com.paff.deepsea.action.MULTI_SELECT"
	 * </p>
	 */
	public static final String ACTION_MULTI_SELECT = "com.paff.deepsea.action.MULTI_SELECT";

	/**
	 * The title to display.
	 * 
	 * <p>
	 * This is shown in the title bar of the file manager.
	 * </p>
	 * 
	 * <p>
	 * Constant Value: "com.paff.deepsea.extra.TITLE"
	 * </p>
	 */
	public static final String EXTRA_TITLE = "com.paff.deepsea.extra.TITLE";

	/**
	 * The text on the button to display.
	 * 
	 * <p>
	 * Depending on the use, it makes sense to set this to "Open" or "Save".
	 * </p>
	 * 
	 * <p>
	 * Constant Value: "com.paff.deepsea.extra.BUTTON_TEXT"
	 * </p>
	 */
	public static final String EXTRA_BUTTON_TEXT = "com.paff.deepsea.extra.BUTTON_TEXT";

	/**
	 * Flag indicating to show only writeable files and folders.
	 * 
	 * <p>
	 * Constant Value: "com.paff.deepsea.extra.WRITEABLE_ONLY"
	 * </p>
	 */
	public static final String EXTRA_WRITEABLE_ONLY = "com.paff.deepsea.extra.WRITEABLE_ONLY";

}
