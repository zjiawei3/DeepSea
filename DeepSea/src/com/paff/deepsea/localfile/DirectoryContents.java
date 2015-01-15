package com.paff.deepsea.localfile;

import java.util.List;

public class DirectoryContents 
{
    List<IconifiedText> listDir;
    List<IconifiedText> listFile;
    List<IconifiedText> listSdCard;
    
    // If true, there's a ".nomedia" file in this directory.
    boolean noMedia;
    
    public List<IconifiedText> getListDirectory()
    {
    	return listDir;
    }
    
    public List<IconifiedText> getListFile()
    {
    	return listFile;
    }
    
    public List<IconifiedText> getListSdCard()
    {
    	return listSdCard;
    }
    
    public boolean getNoMedia()
    {
    	return noMedia;
    }
}
