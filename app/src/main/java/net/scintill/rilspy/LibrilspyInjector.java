/**
 * Copyright (c) 2014 Joey Hewitt <joey@joeyhewitt.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.scintill.rilspy;

import android.content.Context;
import android.util.Log;

import com.SecUpwN.AIMSICD.utils.CMDProcessor;
import com.SecUpwN.AIMSICD.utils.CommandResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibrilspyInjector {
    protected static final String TAG = LibrilspyInjector.class.getSimpleName();

    private Context mContext;

    public LibrilspyInjector(Context context) {
        mContext = context;
    }

    public boolean inject() {
        boolean shouldInject = false;
        int rilPid = 0;
        try {
            rilPid = getPid("/rild", "radio");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String libraryDir = mContext.getApplicationInfo().nativeLibraryDir;
        String libraryPath = libraryDir + "/librilspy.so";

        try {
            shouldInject = !checkIfLibraryAlreadyLoaded(rilPid, "/librilspy.so");
        } catch (IOException e) {
            Log.e(TAG, "Error trying to determine if library is loaded. Not injecting.", e);
        }

        if (shouldInject) {
            Log.d(TAG, "Installing rilspy");

            String cmd = "logwrapper " + libraryDir + "/lib__hijack.bin__.so -d -p " + rilPid +
                    " -l " + libraryPath;
            Log.d(TAG, "Executing cmd: "+cmd);
            CommandResult result = CMDProcessor.runSuCommand(cmd);

            if (!result.success()) {
                throw new RuntimeException("unable to inject phone process: " + result);
            }
            return true;
        } else {
            Log.e(TAG, "Library was already injected.");
        }

        return false;
    }

    private boolean checkIfLibraryAlreadyLoaded(int phonePid, String libraryPath) throws IOException {
        BufferedReader in = null;
        boolean sawStack = false, sawLib = false;
        try {
            String filePath = "/proc/"+phonePid+"/maps";
            CommandResult result = CMDProcessor.runSuCommand("cat "+filePath);
            if (!result.success()) {
                throw new IOException("error reading "+filePath);
            }

            in = new BufferedReader(new StringReader(result.getStdout()));
            String line;

            while ((line = in.readLine()) != null) {
                // sanity-check that we are reading correctly
                if (line.endsWith("[stack]")) {
                    sawStack = true;
                    if (sawLib) break;
                } else if (line.contains(libraryPath)) {
                    sawLib = true;
                    if (sawStack) break;
                }
            }
        } finally {
            if (in != null) in.close();
        }

        if (!sawStack) {
            throw new IOException("did not find stack; is the file being read wrong?");
        }

        return sawLib;
    }

    private int getPid(String cmdName, String user) throws IOException {
        BufferedReader in = null;
        try {
            CommandResult result = CMDProcessor.runShellCommand("\\ps");
            if (!result.success()) {
                throw new IOException("error running ps");
            }

            in = new BufferedReader(new StringReader(result.getStdout()));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith(user) && line.endsWith(cmdName)) {
                    String lineRemainder = line.substring(user.length());
                    Matcher m = Pattern.compile("\\s*([0-9]+)\\s+").matcher(lineRemainder);
                    m.find();
                    String pidStr = m.group(1);
                    return Integer.parseInt(pidStr, 10);
                }
            }
        } finally {
            if (in != null) in.close();
        }

        throw new IOException("couldn't find pid");
    }

}
