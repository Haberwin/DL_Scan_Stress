package com.maidu.scanstress;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import static java.lang.Thread.sleep;


public class MainActivity extends Activity implements IDataListener {

  private EditText editAvg;

  private EditText editCurrentCycle;

  private EditText editMax;

  private EditText editMin;

  private EditText editMisread;

  private EditText editNumTimes;

  private EditText editTimeoutCount;

  private boolean initialized;

  private CheckBox keepAwakeBox;

  private int mCurrentCycle = 0;
  private int mReadCount=0;
  private int mTimeoutCount=0;
  private int mMissReadCount=0;
  public static final int UPDATE_TEXT = 1;

  private long mEndTime;

  private long mMaxTime;

  private long mMinTime;

  private IScanner mScanner;
  private  Thread mRunable;

  private long mStartTime;

  private long mTotalTimes;

  private PowerManager.WakeLock mWakelock = null;

  private boolean mustStop;

  private String previousLabel;

  private ResultHandler resultHandler;

  private EditText sleepTimeText;

  private Button startBtn;

  private Button stopBtn;

  private EditText timeoutText;
  private KeyListener keylistener;


  protected void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);
    setContentView(R.layout.activity_main);
    this.keylistener= DigitsKeyListener.getInstance();
    this.editNumTimes = findViewById(R.id.editNumTimes);
    //this.editNumTimes = (EditText)findViewById(2131165188);
    //this.editCurrentCycle = (EditText)findViewById(2131165200);
    this.editCurrentCycle = findViewById(R.id.editCurrentCycle);
    this.editMin = findViewById(R.id.editMin);
    this.editMax = findViewById(R.id.editMax);
    this.editAvg = findViewById(R.id.editAvg);
    this.startBtn = findViewById(R.id.startButton);
    this.stopBtn = findViewById(R.id.stopButton);
    this.sleepTimeText = findViewById(R.id.editSleepTime);
    this.timeoutText = findViewById(R.id.editTimeout);
    this.keepAwakeBox = findViewById(R.id.checkBoxAwake);
    this.editMisread = findViewById(R.id.editMisread);
    this.editTimeoutCount = findViewById(R.id.editTimeoutCount);
    this.initialized = false;
    this.startBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        runTest();
      }
    });
    this.stopBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        endTest();
      }
    });
    new ArrayAdapter(this, R.layout.spinner_row, new String[]{"Default Scanner"});
    this.resultHandler = new ResultHandler();
  }

  private void endTest() {
    this.mustStop = true;
    if(mRunable !=null && mRunable.isAlive()){
      mRunable.interrupt();
      Log.d("liuwenhua",""+mRunable.isAlive());
      try {
        mRunable.join();
        Log.d("liuwenhua",""+mRunable.isAlive());
      } catch (InterruptedException e) {
        Log.d("liuwenhua","mRunable interupt!");
      }
    }
    this.startBtn.setEnabled(true);
    this.stopBtn.setEnabled(false);
    this.timeoutText.setKeyListener(keylistener);
    this.sleepTimeText.setKeyListener(keylistener);
    this.editNumTimes.setKeyListener(keylistener);
    this.keepAwakeBox.setEnabled(true);
    if (this.mWakelock != null && this.mWakelock.isHeld())
      this.mWakelock.release();
    if (this.mScanner != null && this.initialized)
      this.mScanner.stop();


    System.gc();
  }

  private void initializeScanner() {
    this.mScanner = new DatalogicScanner();
    this.mScanner.setDataListener(this, getApplicationContext());
    this.mScanner.init();
    this.initialized = true;
  }

  private void runTest() {
    this.mTotalTimes = 0L;
    this.mMinTime = Long.MAX_VALUE;
    this.mMaxTime = Long.MIN_VALUE;
    this.mCurrentCycle = 0;
    int mMisreadTimes = 0;
    int mTimeoutTimes = 0;
    this.previousLabel = null;
    this.editCurrentCycle.setText("" + this.mCurrentCycle);
    this.editMisread.setText("" + mMisreadTimes);
    this.editTimeoutCount.setText("" + mTimeoutTimes);


    String str1;
    str1 = this.editNumTimes.getText().toString();
    try {
      int mTotDesiredTimes = Integer.parseInt(str1);
      if (mTotDesiredTimes <= 0) {
        NumberFormatException numberFormatException = new NumberFormatException();
        Toast.makeText(MainActivity.this, "Desired times must be positive!", Toast.LENGTH_LONG).show();
        //this("Desired times must be positive!");
        throw numberFormatException;
      }
    } catch (NumberFormatException str) {
      Toast toast = Toast.makeText(this, "Iteration must be a positive number!", Toast.LENGTH_SHORT);
      toast.setGravity(17, 0, 0);
      toast.show();
      return;
    }
    if (!this.initialized)
      initializeScanner();
    str1 = this.sleepTimeText.getText().toString();
    final int sleepTime;
    if (!str1.isEmpty()) {
      try {
        sleepTime = Integer.parseInt(str1);
        if (sleepTime < 0) {
          NumberFormatException numberFormatException = new NumberFormatException();

          Toast.makeText(MainActivity.this, "Timeout must be a positive number!", Toast.LENGTH_LONG).show();
          //this("Timeout must be a positive number!");
          throw numberFormatException;
        }
      } catch (NumberFormatException str) {
        Toast toast = Toast.makeText(this, "Provide a valid sleep timeout", Toast.LENGTH_LONG);
        toast.setGravity(17, 0, 0);
        toast.show();
        return;
      }
    } else {
      sleepTime = 1000;
    }
    str1 = this.timeoutText.getText().toString();
    final int timeout;
    if (str1.isEmpty()) {
      timeout = 1000;
    } else {
      try {
        timeout = Integer.parseInt(str1);
        if (timeout < 500 || timeout > 10000) {
          NumberFormatException numberFormatException = new NumberFormatException();
          Toast.makeText(MainActivity.this, "Timeout must be a positive number!", Toast.LENGTH_LONG).show();
          throw numberFormatException;
        }
      } catch (NumberFormatException str) {
        Toast toast = Toast.makeText(this, "Provide a valid timeout(500~10000ms)", Toast.LENGTH_LONG);
        toast.setGravity(17, 0, 0);
        toast.show();
        return;
      }
    }
    if (this.keepAwakeBox.isChecked()) {
      //this.mWakelock = ((PowerManager)getSystemService("power")).newWakeLock(26, "Scan Test wakelock");
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      String flag = "ScanStress:wakelock acquire!";
      assert pm != null;
      this.mWakelock = pm.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.PARTIAL_WAKE_LOCK, flag);

      this.mWakelock.acquire(10000 * 60 * 1000L /*10 minutes*/);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    System.gc();
    this.timeoutText.setKeyListener(null);
    this.sleepTimeText.setKeyListener(null);
    this.editNumTimes.setKeyListener(null);
    this.startBtn.setEnabled(false);
    this.stopBtn.setEnabled(true);
    this.keepAwakeBox.setEnabled(false);
    this.mustStop = false;
    final int mTotDesiredTimes =Integer.parseInt(editNumTimes.getText().toString());

    mRunable=new Thread(new Runnable() {
      @Override
      public void run() {
        while (!mustStop && mCurrentCycle < mTotDesiredTimes) {
          mCurrentCycle += 1;
          mStartTime = System.currentTimeMillis();

          try {
          mScanner.start(timeout);
          Log.d("liuwenhua",String.format("Scan Times:%d",mCurrentCycle));

            sleep(sleepTime);
            //handler.sendMessage();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          catch (Exception exp){
            initializeScanner();
            Log.d("liuwenhua","reInitialize Scanner!");
          }

        }
        //endTest();
        Message message = new Message();
        message.what = UPDATE_TEXT;
        message.obj="Finish test!";
        resultHandler.sendMessage(message);
      }
    });
    mRunable.start();


  }

  private void updateStatistics() {
    long l = this.mEndTime - this.mStartTime;
    this.mTotalTimes += l;
    if (l < this.mMinTime)
      this.mMinTime = l;
    if (l > this.mMaxTime)
      this.mMaxTime = l;
    l = this.mTotalTimes/ this.mCurrentCycle;
    this.editAvg.setText("" + (int) l);
    this.editMax.setText("" + (int) this.mMaxTime);
    this.editMin.setText("" + (int) this.mMinTime);
  }

  public void notifyData() {
    Log.d("liuwenhua","notifyData");
    this.mEndTime = System.currentTimeMillis();
  }


  protected void onDestroy() {
    super.onDestroy();
    endTest();
    if (this.mScanner != null && this.initialized) {
      this.mScanner.deInit();
      this.initialized = false;
    }
  }

  public void sendData(String paramString) {
    Log.d("liuwenhua","MainAcitivty SendDate+"+paramString);
    if (paramString == null)
    {
      mMissReadCount+=1;
      return;
    }
    if (paramString.startsWith("TIMEOUT")) {
      this.resultHandler.sendEmptyMessage(2);
      mTimeoutCount+=1;
      return;
    }
    if (this.previousLabel == null) {
      this.resultHandler.sendEmptyMessage(0);
      this.previousLabel = paramString;
      return;
    }
    if (this.previousLabel.equals(paramString)) {
      this.resultHandler.sendEmptyMessage(0);
      updateStatistics();
      mReadCount+=1;
      return;
    }
    this.resultHandler.sendEmptyMessage(1);

  }

  class ResultHandler extends Handler {
    public void handleMessage(Message param1Message) {

    editCurrentCycle.setText(""+mReadCount);
    editTimeoutCount.setText(""+mTimeoutCount);
    editMisread.setText(""+mMissReadCount);
    switch (param1Message.what){
      case UPDATE_TEXT:
        endTest();
    }

      // Byte code:
      //   0: aload_1
      //   1: getfield what : I
      //   4: tableswitch default -> 32, 0 -> 234, 1 -> 260, 2 -> 279
      //   32: aload_0
      //   33: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   36: invokestatic -get3 : (Lcom/datalogicscantest/MainActivity;)I
      //   39: aload_0
      //   40: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   43: invokestatic -get4 : (Lcom/datalogicscantest/MainActivity;)I
      //   46: iadd
      //   47: aload_0
      //   48: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   51: invokestatic -get6 : (Lcom/datalogicscantest/MainActivity;)I
      //   54: iadd
      //   55: aload_0
      //   56: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   59: invokestatic -get7 : (Lcom/datalogicscantest/MainActivity;)I
      //   62: if_icmpge -> 298
      //   65: aload_0
      //   66: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   69: invokestatic -get8 : (Lcom/datalogicscantest/MainActivity;)Z
      //   72: iconst_1
      //   73: ixor
      //   74: ifeq -> 298
      //   77: aload_0
      //   78: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   81: invokestatic -get9 : (Lcom/datalogicscantest/MainActivity;)I
      //   84: ifle -> 98
      //   87: aload_0
      //   88: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   91: invokestatic -get9 : (Lcom/datalogicscantest/MainActivity;)I
      //   94: i2l
      //   95: invokestatic sleep : (J)V
      //   98: aload_0
      //   99: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   102: invokestatic currentTimeMillis : ()J
      //   105: invokestatic -set2 : (Lcom/datalogicscantest/MainActivity;J)J
      //   108: pop2
      //   109: aload_0
      //   110: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   113: invokestatic -get5 : (Lcom/datalogicscantest/MainActivity;)Lcom/datalogicscantest/IScanner;
      //   116: aload_0
      //   117: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   120: invokestatic -get10 : (Lcom/datalogicscantest/MainActivity;)I
      //   123: invokeinterface start : (I)V
      //   128: aload_0
      //   129: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   132: invokestatic -get0 : (Lcom/datalogicscantest/MainActivity;)Landroid/widget/EditText;
      //   135: new java/lang/StringBuilder
      //   138: dup
      //   139: invokespecial <init> : ()V
      //   142: ldc ''
      //   144: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   147: aload_0
      //   148: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   151: invokestatic -get3 : (Lcom/datalogicscantest/MainActivity;)I
      //   154: invokevirtual append : (I)Ljava/lang/StringBuilder;
      //   157: invokevirtual toString : ()Ljava/lang/String;
      //   160: invokevirtual setText : (Ljava/lang/CharSequence;)V
      //   163: aload_0
      //   164: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   167: invokestatic -get1 : (Lcom/datalogicscantest/MainActivity;)Landroid/widget/EditText;
      //   170: new java/lang/StringBuilder
      //   173: dup
      //   174: invokespecial <init> : ()V
      //   177: ldc ''
      //   179: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   182: aload_0
      //   183: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   186: invokestatic -get4 : (Lcom/datalogicscantest/MainActivity;)I
      //   189: invokevirtual append : (I)Ljava/lang/StringBuilder;
      //   192: invokevirtual toString : ()Ljava/lang/String;
      //   195: invokevirtual setText : (Ljava/lang/CharSequence;)V
      //   198: aload_0
      //   199: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   202: invokestatic -get2 : (Lcom/datalogicscantest/MainActivity;)Landroid/widget/EditText;
      //   205: new java/lang/StringBuilder
      //   208: dup
      //   209: invokespecial <init> : ()V
      //   212: ldc ''
      //   214: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuilder;
      //   217: aload_0
      //   218: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   221: invokestatic -get6 : (Lcom/datalogicscantest/MainActivity;)I
      //   224: invokevirtual append : (I)Ljava/lang/StringBuilder;
      //   227: invokevirtual toString : ()Ljava/lang/String;
      //   230: invokevirtual setText : (Ljava/lang/CharSequence;)V
      //   233: return
      //   234: aload_0
      //   235: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   238: astore_1
      //   239: aload_1
      //   240: aload_1
      //   241: invokestatic -get3 : (Lcom/datalogicscantest/MainActivity;)I
      //   244: iconst_1
      //   245: iadd
      //   246: invokestatic -set0 : (Lcom/datalogicscantest/MainActivity;I)I
      //   249: pop
      //   250: aload_0
      //   251: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   254: invokestatic -wrap2 : (Lcom/datalogicscantest/MainActivity;)V
      //   257: goto -> 32
      //   260: aload_0
      //   261: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   264: astore_1
      //   265: aload_1
      //   266: aload_1
      //   267: invokestatic -get4 : (Lcom/datalogicscantest/MainActivity;)I
      //   270: iconst_1
      //   271: iadd
      //   272: invokestatic -set1 : (Lcom/datalogicscantest/MainActivity;I)I
      //   275: pop
      //   276: goto -> 32
      //   279: aload_0
      //   280: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   283: astore_1
      //   284: aload_1
      //   285: aload_1
      //   286: invokestatic -get6 : (Lcom/datalogicscantest/MainActivity;)I
      //   289: iconst_1
      //   290: iadd
      //   291: invokestatic -set3 : (Lcom/datalogicscantest/MainActivity;I)I
      //   294: pop
      //   295: goto -> 32
      //   298: aload_0
      //   299: getfield this$0 : Lcom/datalogicscantest/MainActivity;
      //   302: invokestatic -wrap0 : (Lcom/datalogicscantest/MainActivity;)V
      //   305: goto -> 128
      //   308: astore_1
      //   309: goto -> 98
      // Exception table:
      //   from	to	target	type
      //   87	98	308	java/lang/InterruptedException }
    }
  }

}


/* Location:              /home/levi/Tools/jd-gui/DatalogicScanTest-2.0-dex2jar.jar!/com/datalogicscantest/MainActivity.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.0.6
 */