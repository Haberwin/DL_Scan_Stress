package com.maidu.scanstress;

import android.content.Context;

public interface IScanner {
  void deInit();
  
  void init();
  
  void setDataListener(IDataListener paramIDataListener, Context paramContext);
  
  void start(int paramInt);
  
  void stop();
}


/* Location:              /home/levi/Tools/jd-gui/DatalogicScanTest-2.0-dex2jar.jar!/com/datalogicscantest/IScanner.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.0.6
 */