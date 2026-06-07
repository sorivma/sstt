package com.sstt.sources;

import jakarta.validation.constraints.NotBlank;

public class SourceMessageForm {
    private SourceType sourceType = SourceType.MANUAL;
    private String sender;

    @NotBlank
    private String rawText;

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
