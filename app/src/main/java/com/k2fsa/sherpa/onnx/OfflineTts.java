package com.k2fsa.sherpa.onnx;

public final class OfflineTts {
    private long ptr = 0L;

    public OfflineTts(OfflineTtsConfig config) {
        SherpaOnnxLibraryLoader.loadIfNeeded();
        ptr = newFromFile(config);
        if (ptr == 0L) {
            throw new IllegalArgumentException("Invalid OfflineTtsConfig: failed to create native OfflineTts");
        }
    }

    public int getSampleRate() {
        return getSampleRate(ptr);
    }

    public int getNumSpeakers() {
        return getNumSpeakers(ptr);
    }

    public GeneratedAudio generate(String text) {
        return generate(text, 0, 1.0f);
    }

    public GeneratedAudio generate(String text, int sid) {
        return generate(text, sid, 1.0f);
    }

    public GeneratedAudio generate(String text, int sid, float speed) {
        return generateImpl(ptr, text, sid, speed);
    }

    public void release() {
        if (ptr == 0L) {
            return;
        }
        delete(ptr);
        ptr = 0L;
    }

    @Override
    protected void finalize() throws Throwable {
        release();
    }

    private native long newFromFile(OfflineTtsConfig config);

    private native void delete(long ptr);

    private native int getSampleRate(long ptr);

    private native int getNumSpeakers(long ptr);

    private native GeneratedAudio generateImpl(long ptr, String text, int sid, float speed);
}
