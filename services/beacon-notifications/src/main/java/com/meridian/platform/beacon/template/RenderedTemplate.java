package com.meridian.platform.beacon.template;

public final class RenderedTemplate {

    private final String templateCode;
    private final String subject;
    private final String body;

    public RenderedTemplate(String templateCode, String subject, String body) {
        this.templateCode = templateCode;
        this.subject = subject;
        this.body = body;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
