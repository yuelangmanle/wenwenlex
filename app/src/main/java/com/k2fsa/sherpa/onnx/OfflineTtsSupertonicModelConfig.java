package com.k2fsa.sherpa.onnx;

public final class OfflineTtsSupertonicModelConfig {
    private final String durationPredictor;
    private final String textEncoder;
    private final String vectorEstimator;
    private final String vocoder;
    private final String ttsJson;
    private final String unicodeIndexer;
    private final String voiceStyle;

    private OfflineTtsSupertonicModelConfig(Builder builder) {
        this.durationPredictor = builder.durationPredictor;
        this.textEncoder = builder.textEncoder;
        this.vectorEstimator = builder.vectorEstimator;
        this.vocoder = builder.vocoder;
        this.ttsJson = builder.ttsJson;
        this.unicodeIndexer = builder.unicodeIndexer;
        this.voiceStyle = builder.voiceStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String durationPredictor = "";
        private String textEncoder = "";
        private String vectorEstimator = "";
        private String vocoder = "";
        private String ttsJson = "";
        private String unicodeIndexer = "";
        private String voiceStyle = "";

        public Builder setDurationPredictor(String durationPredictor) {
            this.durationPredictor = durationPredictor;
            return this;
        }

        public Builder setTextEncoder(String textEncoder) {
            this.textEncoder = textEncoder;
            return this;
        }

        public Builder setVectorEstimator(String vectorEstimator) {
            this.vectorEstimator = vectorEstimator;
            return this;
        }

        public Builder setVocoder(String vocoder) {
            this.vocoder = vocoder;
            return this;
        }

        public Builder setTtsJson(String ttsJson) {
            this.ttsJson = ttsJson;
            return this;
        }

        public Builder setUnicodeIndexer(String unicodeIndexer) {
            this.unicodeIndexer = unicodeIndexer;
            return this;
        }

        public Builder setVoiceStyle(String voiceStyle) {
            this.voiceStyle = voiceStyle;
            return this;
        }

        public OfflineTtsSupertonicModelConfig build() {
            return new OfflineTtsSupertonicModelConfig(this);
        }
    }
}
