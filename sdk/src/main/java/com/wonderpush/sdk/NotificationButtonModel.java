package com.wonderpush.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class NotificationButtonModel {

    public int icon;
    public CharSequence label;
    public String targetUrl;
    public List<ActionModel> actions;

    public NotificationButtonModel() {
        actions = new ArrayList<>(0);
    }

    public NotificationButtonModel(AlertModel parentAlert, JSONObject data) {
        if (data == null) {
            return;
        }

        icon = parentAlert.resolveIconIdentifier(data.optString("icon", null));
        label = parentAlert.handleHtml(data.optString("label"));
        targetUrl = data.optString("targetUrl");
        JSONArray actions = data.optJSONArray("actions");
        int actionCount = actions != null ? actions.length() : 0;
        this.actions = new ArrayList<>(actionCount);
        for (int i = 0 ; i < actionCount ; ++i) {
            JSONObject action = actions.optJSONObject(i);
            this.actions.add(new ActionModel(action));
        }
    }

}