package com.android.chatappwithfirebase.Model;

import java.util.List;

//todo 2 send notification (next IFCMService)
public class FCMResponse {
    private long multicats_id;
    private int success,failure,canonical_ids;
    private List<FCMResult> results;
    private long message_id;

    public FCMResponse() {
    }

    public long getMulticats_id() {
        return multicats_id;
    }

    public void setMulticats_id(long multicats_id) {
        this.multicats_id = multicats_id;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailure() {
        return failure;
    }

    public void setFailure(int failure) {
        this.failure = failure;
    }

    public int getCanonical_ids() {
        return canonical_ids;
    }

    public void setCanonical_ids(int canonical_ids) {
        this.canonical_ids = canonical_ids;
    }

    public List<FCMResult> getResults() {
        return results;
    }

    public void setResults(List<FCMResult> results) {
        this.results = results;
    }

    public long getMessage_id() {
        return message_id;
    }

    public void setMessage_id(long message_id) {
        this.message_id = message_id;
    }
}
