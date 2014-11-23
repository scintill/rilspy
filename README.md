# rilspy

The goal of this project is to develop an easy-to-use method of instrumenting closed-source vendor RIL libraries on Android.

Currently it is an app that requires root. This app will inject code into your running `rild`, which will log calls to libc's `read()` and `write()` from any part of rild, including the vendor libraries.

## Usage

Have a rooted phone. Install the app, launch it, approve the root request. If you've already run the app before, it's best to kill `rild` (it will be re-started by `init`) and the app, to start from a clean state.  The app itself currently doesn't have much feedback -- you have to look at `logcat` output.  Here's a command to filter to the most relevant log tags:

    adb logcat -s librilspy,lib__hijack.bin__.so,LibrilspyInjector,CMDProcessor,Rilspy

Logs about `read()`/`write()` have the `librilspy` tag.  Currently they give the file descriptor, number of bytes, and raw buffer. For example, a short extract from my Qualcomm device:

    D/librilspy( 2694): write 5 bytes to fd 15: qcril
    D/librilspy( 2694): write 15 bytes to fd 15: radio-interface

In this case, the written data was nice ASCII bytes.  My simple test to to produce readable output is to run a USSD code to check my account balance, and I can see the return message logged.  More sensible logging of binary data (such as a hex dump) will be added later.

**After running the app, your rild will be logging like this for the rest of its life, which has privacy, stability, and performance implications.** It's probably best to kill rild and let it be restarted without the spy code, when you're not actively using the logging.

## Building

Git-clone, then "git submodule init; git submodule update."  Should build out-of-the-box in Android Studio, but requires the NDK.  You may have to put `ndk.dir` into the local.properties file, similar to how the SDK is.  [This StackOverflow question](https://stackoverflow.com/questions/23321680/android-studio-ndk-dir-issue) demonstrates it (the questioner's issue was simply having an incorrect path.)

## License

See LICENSE.txt
