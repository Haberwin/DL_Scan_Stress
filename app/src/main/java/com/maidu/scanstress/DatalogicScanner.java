package com.maidu.scanstress;

import android.content.Context;
import android.util.Log;

import com.datalogic.decode.BarcodeManager;
import com.datalogic.decode.DecodeException;
import com.datalogic.decode.DecodeResult;
import com.datalogic.decode.ReadListener;
import com.datalogic.decode.TimeoutListener;

public class DatalogicScanner implements IScanner, ReadListener, TimeoutListener {
  private BarcodeManager bm;
  
  private int mErrorCount = 0;
  
  private IDataListener mListener;
  
  public void deInit() throws DecodeException {
    if (this.bm != null)
      this.bm.release();
  }
  
  public void init() {
    try {
      //this();
      Log.d("liuwenhua","DatalogicScanner init");
      this.bm = new BarcodeManager();
    } catch (DecodeException decodeException) {
      decodeException.printStackTrace();
    } 
    try {
      this.bm.addReadListener(this);
      this.bm.addTimeoutListener(this);
    } catch (DecodeException decodeException) {
      decodeException.printStackTrace();
    } 
  }
  @Override
  public void onRead(DecodeResult paramDecodeResult) {
    if (paramDecodeResult != null) {
      this.mListener.notifyData();
      this.mListener.sendData(paramDecodeResult.getText());
    } 
  }
  
  public void onScanTimeout() {
    //this.mListener.notifyData();
    IDataListener iDataListener = this.mListener;
    StringBuilder stringBuilder = (new StringBuilder()).append("TIMEOUT[");
    int i = this.mErrorCount;
    this.mErrorCount = i + 1;
    iDataListener.sendData(stringBuilder.append(i).append("]").toString());
  }
  
  public void setDataListener(IDataListener paramIDataListener, Context paramContext) {
    this.mListener = paramIDataListener;
  }
  
  public void start(int paramInt) {
    try {
      this.bm.startDecode(paramInt);
    } catch (DecodeException decodeException) {
      decodeException.printStackTrace();
    } 
  }
  
  public void stop() {
    try {
      this.bm.stopDecode();
    } catch (DecodeException ignored) {}
  }
}


/* Location:              /home/levi/Tools/jd-gui/DatalogicScanTest-2.0-dex2jar.jar!/com/datalogicscantest/DatalogicScanner.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.0.6
 */