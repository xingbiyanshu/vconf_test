package com.example.sissi.vconftest;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class PcTrace {

    private static boolean isEnabled = true;
    
    public static final int VERBOSE = -1;
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    public static final int FATAL = 4;
    private static int level = INFO;

    private static boolean isFileTraceInited = false;
    private static boolean isFileTraceEnabled = false;
    private static BufferedWriter bufWriter;
    private static BufferedWriter bufWriter1;
    private static BufferedWriter curBw;
    private static final int WRITER_BUF_SIZE = 1024;
    private static File traceFile;
    private static File traceFile1;
    private static File curTf;
	private static final String TRACE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ File.separator + "kedacom" + File.separator + "trace";
    private static final String TRACE_FILE = "trace.txt";
    private static final String TRACE_FILE1 = "trace1.txt";
    private static final int TRACE_FILE_SIZE_LIMIT = 1024 * 1024 * 1024;
    private static Object lock = new Object();

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");

    private static final String TAG = "PcTrace";

    private PcTrace() {

    }

    public static void enable(boolean isEnable) {
        if (isEnable) {
            log(INFO, TAG, "==================PcTrace enabled!");
        } else {
            log(INFO, TAG, "==================PcTrace disabled!");
        }
        isEnabled = isEnable;
    }

    public static void setTraceLevel(int lv) {
        log(INFO, TAG, "==================Set PcTrace level to " + lv);
        level = lv;
    }


    /*Print*/
    public static void p(int lev, String tag, String format, Object... para){
        if (!isEnabled || lev < level || null == tag || null == format || null == para) {
            return;
        }

        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        if (tag.contains("%") && !format.contains("%")){
            /* 用户期望调用方法p(int lev, String format, Object... para)，但是运行时机制错误匹配到了本方法。比如：
            * PcTrace.p(PcTrace.ERROR, "%s", "Mismatch Test"); 故此处纠正以正常输出。*/
            /*注意：此法仍有缺陷，当用户确实想调用p(int lev, String tag, String format, Object... para)，
              而恰好又使用了一个包含“%”的tag跟一个普通字符串，则该调用会被“错误地”纠正！故Tag中不要包含“%”！*/
            String fmt = tag;
            ArrayList<Object> paras = new ArrayList<Object>();
            paras.add(format);
            for (Object obj : para) {
                paras.add(obj);
            }
            log(lev, getClassName(ste.getClassName()), simplePrefix(ste)+ String.format(fmt, paras.toArray()));
            return;
        }
        log(lev, tag, prefix(ste)+ String.format(format, para));
    }

    public static void p(int lev, String format, Object... para){
        if (!isEnabled || lev < level || null == format || null == para) {
            return;
        }

        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        log(lev, getClassName(ste.getClassName()), simplePrefix(ste)+ String.format(format, para));
    }

    public static void p(String format, Object... para){
        if (!isEnabled || INFO < level || null == format || null == para) {
            return;
        }

        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        log(INFO, getClassName(ste.getClassName()), simplePrefix(ste) + String.format(format, para));
    }


    /* Stack Trace */
    public static void st(String msg) {
        if (!isEnabled) {
            return;
        }

        StackTraceElement stes[] = Thread.currentThread().getStackTrace();
        StackTraceElement ste = stes[3];
        StringBuffer trace = new StringBuffer();

        trace.append(prefix(ste)).append(msg).append("\n");

        for (int i = 3, j = 0; i < stes.length; ++i, ++j) {
            trace.append("#" + j + " " + stes[i] + "\n");
        }

        System.out.println(trace.toString());
    }


    /* File trace */
    public static void ft(String msg) {
        if (!isEnabled) {
            return;
        }
        synchronized (lock) {
            fileTrace(msg, false);
        }
    }

    
    /* Flush file trace */
    public static void fft(String msg) {
        if (!isEnabled) {
            return;
        }
        synchronized (lock) {
            fileTrace(msg, true);
        }
    }



    private static void initFileTrace() {
        if (isFileTraceInited) {
            return;
        }

        isFileTraceInited = true;

        traceFile = createTraceFile(TRACE_DIR, TRACE_FILE);
        traceFile1 = createTraceFile(TRACE_DIR, TRACE_FILE1);
        if (null == traceFile || null == traceFile1) {
            return;
        }
        curTf = traceFile;

        try {
			bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(traceFile, true)),
					WRITER_BUF_SIZE);
			bufWriter1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(traceFile1, true)),
					WRITER_BUF_SIZE);
            curBw = bufWriter;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        isFileTraceEnabled = true;
    }

    
    private static File createTraceFile(String dir, String filename) {
        File traceDir = new File(dir);
        if (!traceDir.exists()) {
            if (!traceDir.mkdirs()) {
                return null;
            }
        }

        File traceFile = new File(dir + File.separator + filename);
        if (!traceFile.exists()) {
            try {
                traceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        try {
            FileOutputStream fos;
            fos = new FileOutputStream(traceFile);
            fos.write((sdf.format(new Date()) + " ================================== Start Tracing... \n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return traceFile;
    }

    
    private static void rechooseTraceFile() {
        try {
            curBw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        curBw = (curTf == traceFile) ? bufWriter1 : bufWriter;
    }

    
    private static void fileTrace(String msg, boolean isFlush) {
        if (!isFileTraceInited) {
            initFileTrace();
        }

        if (!isFileTraceEnabled) {
            return;
        }

        if (curTf.length() >= TRACE_FILE_SIZE_LIMIT) {
            rechooseTraceFile();
        }

        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
		String trace = prefix(ste) + msg + "\n";

        try {
            curBw.write(trace);
            if (isFlush) {
                curBw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String prefix(StackTraceElement ste){
		return "[" + getClassName(ste.getClassName()) + ":" + ste.getMethodName() + ":" + ste.getLineNumber() + "] ";
    }


    private static String simplePrefix(StackTraceElement ste){
        return  "[" + ste.getMethodName() + ":" + ste.getLineNumber() + "] ";
    }

	private static String getClassName(String classFullName) {
		String className = "";
		int lastSlashIndx = classFullName.lastIndexOf(".");
		if (-1 == lastSlashIndx) {
			className = classFullName;
		} else {
			className = classFullName.substring(lastSlashIndx + 1, classFullName.length());
		}
		return className;
	}

    private static void log(int lev, String tag, String content){
        switch (lev){
            case VERBOSE:
                Log.v(tag, content);
                break;
            case DEBUG:
                Log.d(tag, content);
                break;
            case INFO:
                Log.i(tag, content);
                break;
            case WARN:
                Log.w(tag, content);
                break;
            case ERROR:
                Log.e(tag, content);
                break;
            case FATAL:
                Log.wtf(tag, content);
                break;
        }
    }
}
