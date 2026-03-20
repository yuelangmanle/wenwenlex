package com.k2fsa.sherpa.onnx;

final class SherpaOnnxLibraryLoader {
    private static volatile boolean loaded = false;

    private SherpaOnnxLibraryLoader() {
    }

    static synchronized void loadIfNeeded() {
        if (loaded) {
            return;
        }
        System.loadLibrary("sherpa-onnx-jni");
        loaded = true;
    }
}
