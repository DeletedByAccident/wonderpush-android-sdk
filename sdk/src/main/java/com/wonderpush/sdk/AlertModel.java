package com.wonderpush.sdk;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import android.util.Base64InputStream;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class AlertModel implements Cloneable {

    private static final String TAG = WonderPush.TAG;

    interface Builder {
        AlertModel build(JSONObject inputJSON);
    }

    enum Type {
        // No specific style configured
        NULL(null, new Builder() {
            @Override
            public AlertModel build(JSONObject inputJSON) {
                return new AlertModel(inputJSON);
            }
        }),
        // Explicitly no particular style
        NONE("none", new Builder() {
            @Override
            public AlertModel build(JSONObject inputJSON) {
                return new AlertModel(inputJSON);
            }
        }),
        BIG_TEXT("bigText", new Builder() {
            @Override
            public AlertBigTextModel build(JSONObject inputJSON) {
                return new AlertBigTextModel(inputJSON);
            }
        }),
        BIG_PICTURE("bigPicture", new Builder() {
            @Override
            public AlertModel build(JSONObject inputJSON) {
                return new AlertBigPictureModel(inputJSON);
            }
        }),
        INBOX("inbox", new Builder() {
            @Override
            public AlertInboxModel build(JSONObject inputJSON) {
                return new AlertInboxModel(inputJSON);
            }
        }),
        ;

        private final String type;
        private final Builder builder;

        Type(String type, Builder builder) {
            this.type = type;
            this.builder = builder;
        }

        @Override
        public String toString() {
            return type;
        }

        public Builder getBuilder() {
            return builder;
        }

        public static Type fromString(String type) {
            if (type == null) {
                return NULL;
            }
            for (Type notificationType : Type.values()) {
                if (type.equals(notificationType.type)) {
                    return notificationType;
                }
            }
            throw new IllegalArgumentException("Constant \"" + type + "\" is not a known notification type");
        }
    }

    private static final boolean defaultVibrate = true;
    private static final boolean defaultSound = true;
    private static final boolean defaultLight = true;

    // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
    private static final int defaultNotificationColor;
    private static final int defaultNotificationLedOn;
    private static final int defaultNotificationLedOff;

    static {
        // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
        int _defaultNotificationColor = Color.WHITE;
        try {
            int config_defaultNotificationColor = Resources.getSystem().getIdentifier("config_defaultNotificationColor", "color", "android");
            if (config_defaultNotificationColor != 0)
                _defaultNotificationColor = WonderPushCompatibilityHelper.getColorResource(Resources.getSystem(), config_defaultNotificationColor);
        } catch (Exception ex) {
            WonderPush.logError("Failed to read config_defaultNotificationColor", ex);
        }
        defaultNotificationColor = _defaultNotificationColor;

        // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
        int _defaultNotificationLedOn = 1000;
        try {
            int config_defaultNotificationLedOn = Resources.getSystem().getIdentifier("config_defaultNotificationLedOn", "integer", "android");
            _defaultNotificationLedOn = Resources.getSystem().getInteger(config_defaultNotificationLedOn);
        } catch (Exception ex) {
            WonderPush.logError("Failed to read config_defaultNotificationLedOn", ex);
        }
        defaultNotificationLedOn = _defaultNotificationLedOn;

        // https://android.googlesource.com/device/lge/mako/+/master/overlay/frameworks/base/core/res/res/values/config.xml
        int _defaultNotificationLedOff = 9000;
        try {
            int config_defaultNotificationLedOff = Resources.getSystem().getIdentifier("config_defaultNotificationLedOff", "integer", "android");
            _defaultNotificationLedOff = Resources.getSystem().getInteger(config_defaultNotificationLedOff);
        } catch (Exception ex) {
            WonderPush.logError("Failed to read config_defaultNotificationLedOff", ex);
        }
        defaultNotificationLedOff = _defaultNotificationLedOff;
    }

    private final JSONObject inputJson;

    private String channel;
    private Type type; // cannot be changed if foreground for easier coding
    private boolean html;
    // Modify forCurrentSettings() and clone() when adding a field below
    private CharSequence title;
    private CharSequence text;
    private CharSequence subText;
    private CharSequence info;
    private CharSequence ticker;
    private String tag;
    private boolean tagPresent;
    private Integer priority;
    private Boolean autoOpen;
    private Boolean autoDrop;
    private List<String> persons;
    private String category;
    private Integer color;
    private String group;
    private String groupTargetUrl;
    private boolean groupTargetUrlPresent;
    private String sortKey;
    private Boolean localOnly;
    private Integer number;
    private Boolean onlyAlertOnce;
    private Long when;
    private Boolean showWhen;
    private Boolean usesChronometer;
    private Integer visibility;
    private Boolean vibrate;
    private long[] vibratePattern;
    private Boolean lights;
    private Integer lightsColor;
    private Integer lightsOn;
    private Integer lightsOff;
    private Boolean sound;
    private Uri soundUri;
    private Boolean ongoing;
    private Integer progress; // negative for "indeterminate"
    private Integer smallIcon;
    private Bitmap largeIcon;
    private List<NotificationButtonModel> buttons;
    private Boolean allowSystemGeneratedContextualActions;
    private String badgeIconType; // one of NotificationCompat.BADGE_ICON_* constants
    private Boolean chronometerCountDown;
    private Boolean colorized;
    private JSONObject extras;
    private Long timeoutAfter;
    // Modify forCurrentSettings() and clone() when adding a field above
    private AlertModel foreground;

    private Collection<CacheUtil.FetchWork> resourcesToFetch = new ArrayList<>();

    public static AlertModel fromOldFormatStringExtra(String alert) {
        if (alert == null) return null;
        try {
            JSONObject wpAlert = new JSONObject();
            wpAlert.putOpt("text", alert);
            return fromJSON(wpAlert);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while parsing a notification alert with string input " + alert, e);
        }
        return null;
    }

    public static AlertModel fromJSON(JSONObject wpAlert) {
        if (wpAlert == null) return null;
        try {
            // Get the alert type
            Type type = null;
            try {
                type = Type.fromString(JSONUtil.getString(wpAlert, "type"));
            } catch (Exception ex) {
                WonderPush.logError("Failed to read notification alert type", ex);
            }
            if (type == null) {
                type = Type.NONE;
            }

            // Instantiate the appropriate non-abstract subclass
            return type.getBuilder().build(wpAlert);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while parsing a notification alert with JSON input " + wpAlert.toString(), e);
        }
        return null;
    }

    protected void fromJSONToplevel(JSONObject wpAlert) {
        setChannel(JSONUtil.getString(wpAlert, "channel"));
        setType(JSONUtil.getString(wpAlert, "type")); // must be done before fromJSONCommon()
        setHtml(wpAlert.optBoolean("html", false)); // must be done before fromJSONCommon()
        fromJSONCommon(wpAlert);

        JSONObject wpAlertForeground = wpAlert.optJSONObject("foreground");
        if (wpAlertForeground == null) {
            wpAlertForeground = new JSONObject();
        }
        AlertModel foreground = new AlertModel(wpAlertForeground, false);
        foreground.fromJSONForeground(wpAlertForeground);
        setForeground(foreground);
    }

    protected void fromJSONForeground(JSONObject wpAlert) {
        fromJSONCommon(wpAlert);

        if (!wpAlert.has("priority")) {
            setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        setForeground(null);
    }

    protected void fromJSONCommon(JSONObject wpAlert) {
        setTitle(JSONUtil.getString(wpAlert, "title"));
        setText(JSONUtil.getString(wpAlert, "text"));
        setSubText(JSONUtil.getString(wpAlert, "subText"));
        setInfo(JSONUtil.getString(wpAlert, "info"));
        setTicker(JSONUtil.getString(wpAlert, "ticker"));
        setHasTag(wpAlert.has("tag"));
        setTag(JSONUtil.getString(wpAlert, "tag"));
        if (!wpAlert.isNull("autoOpen")) {
            setAutoOpen(wpAlert.optBoolean("autoOpen", false));
        } else {
            setAutoOpen(null);
        }
        if (!wpAlert.isNull("autoDrop")) {
            setAutoDrop(wpAlert.optBoolean("autoDrop", false));
        } else {
            setAutoDrop(null);
        }
        setPersons(wpAlert.optJSONArray("persons"));
        setCategory(JSONUtil.getString(wpAlert, "category"));
        if (wpAlert.opt("priority") instanceof String) {
            setPriority(wpAlert.optString("priority"));
        } else if (wpAlert.opt("priority") instanceof Number) {
            setPriority(wpAlert.optInt("priority", NotificationCompat.PRIORITY_DEFAULT));
        }
        setColor(JSONUtil.getString(wpAlert, "color"));
        setGroup(JSONUtil.getString(wpAlert, "group"));
        setHasGroupTargetUrl(wpAlert.has("groupTargetUrl"));
        setGroupTargetUrl(JSONUtil.getString(wpAlert, "groupTargetUrl"));
        setSortKey(JSONUtil.getString(wpAlert, "sortKey"));
        if (!wpAlert.isNull("localOnly")) {
            setLocalOnly(wpAlert.optBoolean("localOnly", false));
        } else {
            setLocalOnly(null);
        }
        if (!wpAlert.isNull("number")) {
            setNumber(wpAlert.optInt("number", 1));
        } else {
            setNumber(null);
        }
        if (!wpAlert.isNull("onlyAlertOnce")) {
            setOnlyAlertOnce(wpAlert.optBoolean("onlyAlertOnce", false));
        } else {
            setOnlyAlertOnce(null);
        }
        if (!wpAlert.isNull("when")) {
            setWhen(wpAlert.optLong("when", System.currentTimeMillis()));
        } else {
            setWhen(null);
        }
        if (!wpAlert.isNull("showWhen")) {
            setShowWhen(wpAlert.optBoolean("showWhen", false));
        } else {
            setShowWhen(null);
        }
        if (!wpAlert.isNull("usesChronometer")) {
            setUsesChronometer(wpAlert.optBoolean("usesChronometer", false));
        } else {
            setUsesChronometer(null);
        }
        if (wpAlert.isNull("visibility")) {
            setVisibility((Integer) null);
        } else if (wpAlert.opt("visibility") instanceof String) {
            setVisibility(wpAlert.optString("visibility"));
        } else {
            setVisibility(wpAlert.optInt("visibility", NotificationCompat.VISIBILITY_PRIVATE));
        }
        if (wpAlert.isNull("lights")) {
            setLights(null);
            setLightsColor((Integer) null);
            setLightsOn(null);
            setLightsOff(null);
        } else if (wpAlert.optJSONObject("lights") != null) {
            JSONObject lights = wpAlert.optJSONObject("lights");
            setLights(null);
            setLightsColor(JSONUtil.getString(lights, "color"));
            if (!(lights.opt("on") instanceof Number)) {
                setLightsOn(null);
            } else {
                setLightsOn(lights.optInt("on", defaultNotificationLedOn));
            }
            if (!(lights.opt("off") instanceof Number)) {
                setLightsOff(null);
            } else {
                setLightsOff(lights.optInt("off", defaultNotificationLedOff));
            }
        } else {
            setLights(wpAlert.optBoolean("lights", defaultLight));
            setLightsColor((Integer) null);
            setLightsOn(null);
            setLightsOff(null);
        }
        if (wpAlert.isNull("vibrate")) {
            setVibrate(null);
            setVibratePattern(null);
        } else if (wpAlert.optJSONArray("vibrate") != null) {
            setVibrate(null);
            JSONArray vibrate = wpAlert.optJSONArray("vibrate");
            long[] pattern = new long[vibrate.length()];
            for (int i = 0; i < vibrate.length(); ++i) {
                pattern[i] = vibrate.optLong(i, 0);
            }
            setVibratePattern(pattern);
        } else {
            setVibrate(wpAlert.optBoolean("vibrate", defaultVibrate));
            setVibratePattern(null);
        }
        if (wpAlert.isNull("sound")) {
            setSound(null);
            setSoundUri((Uri) null);
        } else if (wpAlert.opt("sound") instanceof String) {
            setSound(null);
            setSoundUri(JSONUtil.getString(wpAlert, "sound"));
        } else {
            setSound(wpAlert.optBoolean("sound", defaultSound));
            setSoundUri((Uri) null);
        }
        if (!wpAlert.isNull("ongoing")) {
            setOngoing(wpAlert.optBoolean("ongoing", false));
        } else {
            setOngoing(null);
        }
        if (wpAlert.isNull("progress")) {
            setProgress(null);
        } else if (wpAlert.opt("progress") instanceof Boolean) {
            setProgress(wpAlert.optBoolean("progress", false));
        } else {
            setProgress(wpAlert.optInt("progress"));
        }
        if (wpAlert.isNull("smallIcon")) {
            setSmallIcon((Integer) null);
        } else {
            setSmallIcon(JSONUtil.getString(wpAlert, "smallIcon"));
        }
        if (wpAlert.isNull("largeIcon")) {
            setLargeIcon((Bitmap) null);
        } else {
            setLargeIcon(JSONUtil.getString(wpAlert, "largeIcon"));
        }
        setButtons(wpAlert.optJSONArray("buttons"));
        if (wpAlert.isNull("allowSystemGeneratedContextualActions")) {
            setAllowSystemGeneratedContextualActions(null);
        } else {
            setAllowSystemGeneratedContextualActions(wpAlert.optBoolean("allowSystemGeneratedContextualActions", true));
        }
        if (wpAlert.isNull("badgeIconType")) {
            setBadgeIconType(null);
        } else {
            setBadgeIconType(JSONUtil.getString(wpAlert, "badgeIconType"));
        }
        if (wpAlert.isNull("chronometerCountDown")) {
            setChronometerCountDown(null);
        } else {
            setChronometerCountDown(wpAlert.optBoolean("chronometerCountDown", true));
        }
        if (wpAlert.isNull("colorized")) {
            setColorized(null);
        } else {
            setColorized(wpAlert.optBoolean("colorized", true));
        }
        if (wpAlert.isNull("extras")) {
            setExtras(null);
        } else {
            setExtras(wpAlert.optJSONObject("extras"));
        }
        if (wpAlert.isNull("timeoutAfter")) {
            setTimeoutAfter(null);
        } else {
            setTimeoutAfter(wpAlert.optLong("timeoutAfter", 0));
        }
    }

    protected AlertModel(JSONObject inputJSON) {
        this(inputJSON, true);
    }

    protected AlertModel(JSONObject inputJSON, boolean initTopLevel) {
        this.inputJson = inputJSON;
        if (initTopLevel) {
            fromJSONToplevel(inputJSON);
        }
    }

    public AlertModel forCurrentSettings(boolean applicationIsForeground) {
        AlertModel rtn;
        try {
            rtn = (AlertModel) clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Failed to clone an " + this.getClass().getSimpleName(), e);
            return null;
        }

        if (applicationIsForeground && getForeground() != null) {
            rtn.forCurrentSettingsInternal(getForeground());
        }
        rtn.setForeground(null);

        return rtn;
    }

    protected void forCurrentSettingsInternal(AlertModel from) {
        if (from.getText() != null) {
            setText(from.getText());
        }
        if (from.getTitle() != null) {
            setTitle(from.getTitle());
        }
        if (from.getSubText() != null) {
            setSubText(from.getSubText());
        }
        if (from.getInfo() != null) {
            setInfo(from.getInfo());
        }
        if (from.getTicker() != null) {
            setTicker(from.getTicker());
        }
        if (from.hasPriority()) {
            setPriority(from.getPriority());
        }
        if (from.hasAutoOpen()) {
            setAutoOpen(from.getAutoOpen());
        }
        if (from.hasAutoDrop()) {
            setAutoDrop(from.getAutoDrop());
        }
        if (from.getPersons() != null) {
            setPersons(from.getPersons());
        }
        if (from.getCategory() != null) {
            setCategory(from.getCategory());
        }
        if (from.hasColor()) {
            setColor(from.getColor());
        }
        if (from.getGroup() != null) {
            setGroup(from.getGroup());
        }
        setHasGroupTargetUrl(from.hasGroupTargetUrl());
        if (from.getGroupTargetUrl() != null) {
            setGroupTargetUrl(from.getGroupTargetUrl());
        }
        if (from.getSortKey() != null) {
            setSortKey(from.getSortKey());
        }
        if (from.hasLocalOnly()) {
            setLocalOnly(from.getLocalOnly());
        }
        if (from.hasNumber()) {
            setNumber(from.getNumber());
        }
        if (from.hasOnlyAlertOnce()) {
            setOnlyAlertOnce(from.getOnlyAlertOnce());
        }
        if (from.hasWhen()) {
            setWhen(from.getWhen());
        }
        if (from.hasShowWhen()) {
            setShowWhen(from.getShowWhen());
        }
        if (from.hasUsesChronometer()) {
            setUsesChronometer(from.getUsesChronometer());
        }
        if (from.hasVisibility()) {
            setVisibility(from.getVisibility());
        }
        if (from.hasLights()) {
            setLights(from.getLights());
        }
        if (from.hasLightsColor()) {
            setLightsColor(from.getLightsColor());
        }
        if (from.hasLightsOn()) {
            setLightsOn(from.getLightsOn());
        }
        if (from.hasLightsOff()) {
            setLightsOff(from.getLightsOff());
        }
        if (from.hasVibrate()) {
            setVibrate(from.getVibrate());
        }
        if (from.getVibratePattern() != null) {
            setVibratePattern(from.getVibratePattern());
        }
        if (from.hasSound()) {
            setSound(from.getSound());
        }
        if (from.getSoundUri() != null) {
            setSoundUri(from.getSoundUri());
        }
        if (from.hasOngoing()) {
            setOngoing(from.getOngoing());
        }
        if (from.hasProgress()) {
            if (from.isProgressIndeterminate()) {
                setProgress(true);
            } else {
                setProgress(from.getProgress());
            }
        }
        if (from.hasSmallIcon()) {
            setSmallIcon(from.getSmallIcon());
        }
        if (from.getLargeIcon() != null) {
            setLargeIcon(from.getLargeIcon());
        }
        // DO NOT vary buttons
        if (from.getAllowSystemGeneratedContextualActions() != null) {
            setAllowSystemGeneratedContextualActions(from.getAllowSystemGeneratedContextualActions());
        }
        if (from.getBadgeIconType() != null) {
            setBadgeIconType(from.getBadgeIconType());
        }
        if (from.getChronometerCountDown() != null) {
            setChronometerCountDown(from.getChronometerCountDown());
        }
        if (from.getColorized() != null) {
            setColorized(from.getColorized());
        }
        if (from.getExtras() != null) {
            try {
                setExtras(new JSONObject(from.getExtras().toString()));
            } catch (JSONException ex) {
                Log.e(WonderPush.TAG, "Failed to clone notification extras, ignoring them", ex);
            }
        }
        if (from.getTimeoutAfter() != null) {
            setTimeoutAfter(from.getTimeoutAfter());
        }
    }

    /**
     * Return another AlertModel to replace the current one, if need be.
     * @return null if no alternative is needed
     */
    public AlertModel getAlternativeIfNeeded() {
        return null;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        AlertModel rtn = (AlertModel) super.clone();
        if (foreground != null) {
            rtn.foreground = (AlertModel) foreground.clone();
        }
        if (persons != null) {
            rtn.persons = new LinkedList<>(persons);
        }
        if (vibratePattern != null) {
            rtn.vibratePattern = Arrays.copyOf(vibratePattern, vibratePattern.length);
        }
        if (buttons != null) {
            rtn.buttons = new LinkedList<>();
            for (NotificationButtonModel button : buttons) {
                rtn.buttons.add((NotificationButtonModel) button.clone());
            }
        }
        if (extras != null) {
            try {
                rtn.extras = new JSONObject(extras.toString());
            } catch (JSONException ex) {
                Log.e(WonderPush.TAG, "Failed to clone notification extras, ignoring them", ex);
            }
        }
        return rtn;
    }

    /**
     * @return Transforms the invalid resource identifier 0 into null
     */
    protected Integer resourceIdOrNull(int resId) {
        return resId == 0 ? null : resId;
    }

    /**
     * @return A valid resource integer, or 0
     */
    protected int resolveResourceIdentifier(String resName, String resType) {
        if (resName == null || resType == null) {
            return 0;
        }
        int resId = WonderPush.getApplicationContext().getResources().getIdentifier(resName, resType, WonderPush.getApplicationContext().getPackageName());
        WonderPush.logDebug("Resolving " + resName + " as " + resType + " resource of " + WonderPush.getApplicationContext().getPackageName() + ": " + resId);
        if (resId == 0) {
            resId = Resources.getSystem().getIdentifier(resName, resType, "android");
            WonderPush.logDebug("Resolving " + resName + " as " + resType + " resource of android: " + resId);
        }
        return resId;
    }

    protected int resolveIconIdentifier(String resName) {
        if (resName == null) {
            return 0;
        }
        int rtn;
        // Try as is
        rtn = resolveResourceIdentifier(resName, "drawable");
        if (rtn == 0) {
            // Try fixing spaces
            resName = resName.replaceAll(" ", "_");
            rtn = resolveResourceIdentifier(resName, "drawable");
        }
        if (rtn == 0) {
            // Try bundled icon (ic_XXX_white_24dp)
            rtn = resolveResourceIdentifier("ic_" + resName + "_white_24dp", "drawable");
        }
        if (rtn == 0) {
            // Try system icon (ic_XXX)
            rtn = resolveResourceIdentifier("ic_" + resName, "drawable");
        }
        return rtn;
    }

    protected InputStream decodeDataUri(Uri uri, String logPrefix) {
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        if ("data".equals(scheme)) {
            WonderPush.logDebug(logPrefix + ": Resolved as data URI");
            String sspEnc = uri.getEncodedSchemeSpecificPart(); // we must decode ourself to binary, not to text
            String ssp;
            try {
                ssp = URLDecoder.decode(sspEnc, "ISO-8859-1");
            } catch (UnsupportedEncodingException ex) {
                WonderPush.logDebug(logPrefix + ": Failed to decode scheme-specific part: " + sspEnc, ex);
                ssp = sspEnc;
            }
            byte[] bytes = null;
            try {
                bytes = ssp.getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException ignored) {
            }
            if (bytes != null) {
                int offset = ssp.indexOf(',') + 1;
                InputStream in = new ByteArrayInputStream(bytes, offset, bytes.length - offset);
                String info = ssp.substring(0, offset - 1);
                if (info.endsWith(";base64")) {
                    in = new Base64InputStream(in, 0);
                }
                return in;
            }
        }
        return null;
    }

    protected Bitmap resolveLargeIconFromString(String value, String logPrefix) {
        if (value == null) {
            return null;
        }
        return resolveBitmapFromStringWithFetchResult(value, CacheUtil.fetchLargeIcon(Uri.parse(value), logPrefix), logPrefix);
    }

    protected Bitmap resolveBigPictureFromString(String value, String logPrefix) {
        if (value == null) {
            return null;
        }
        return resolveBitmapFromStringWithFetchResult(value, CacheUtil.fetchBigPicture(Uri.parse(value), logPrefix), logPrefix);
    }

    private Bitmap resolveBitmapFromStringWithFetchResult(String value, CacheUtil.FetchResult fetchResult, String logPrefix) {
        if (!fetchResult.needsWork()) {
            return resolveBitmapFromString(value, fetchResult.getResult(), logPrefix);
        } else {
            resourcesToFetch.add(fetchResult.getWork());
            return null;
        }
    }

    private Bitmap resolveBitmapFromString(String value, File httpResolvedFile, String logPrefix) {
        if (value == null) {
            return null;
        }
        Uri uri = Uri.parse(value);
        InputStream stream;
        int resId;
        if (httpResolvedFile != null) {
            return BitmapFactory.decodeFile(httpResolvedFile.getAbsolutePath());
        } else if ((stream = decodeDataUri(uri, logPrefix)) != null) {
            return BitmapFactory.decodeStream(stream);
        } else if ((resId = resolveResourceIdentifier(value, "drawable")) != 0) {
            WonderPush.logDebug(logPrefix + ": Resolved as drawable");
            return BitmapFactory.decodeResource(WonderPush.getApplicationContext().getResources(), resId);
        } else if ((resId = resolveResourceIdentifier(value, "mipmap")) != 0) {
            WonderPush.logDebug(logPrefix + ": Resolved as mipmap");
            return BitmapFactory.decodeResource(WonderPush.getApplicationContext().getResources(), resId);
        } else {
            for (String suffix : new String[]{"", ".webp", ".png", ".jpg", ".jpeg", ".gif", ".bmp"}) {
                try {
                    Bitmap bm = BitmapFactory.decodeStream(WonderPush.getApplicationContext().getResources().getAssets().open(value + suffix));
                    if (bm != null) {
                        WonderPush.logDebug(logPrefix + ": Resolved as asset with suffix: \"" + suffix + "\"");
                        return bm;
                    }
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    protected CharSequence handleHtml(CharSequence input) {
        if (isHtml() && input instanceof String) {
            return WonderPushCompatibilityHelper.fromHtml((String) input); // images are unsupported in text, but unicode smileys are
        } else {
            return input;
        }
    }

    public Collection<CacheUtil.FetchWork> getResourcesToFetch() {
        return Collections.unmodifiableCollection(resourcesToFetch);
    }

    public JSONObject getInputJson() {
        return inputJson;
    }

    public boolean hasChannel() {
        return channel != null;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean hasType() {
        return type != null;
    }

    public Type getType() {
        return type == null ? Type.NULL : type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setType(String type) {
        setType(Type.fromString(type));
    }

    public boolean isHtml() {
        return html;
    }

    public void setHtml(boolean html) {
        this.html = html;
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(CharSequence title) {
        this.title = handleHtml(title);
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = handleHtml(text);
    }

    public CharSequence getSubText() {
        return subText;
    }

    public void setSubText(CharSequence subText) {
        this.subText = handleHtml(subText);
    }

    public CharSequence getInfo() {
        return info;
    }

    public void setInfo(CharSequence info) {
        this.info = handleHtml(info);
    }

    public CharSequence getTicker() {
        return ticker;
    }

    public void setTicker(CharSequence ticker) {
        this.ticker = handleHtml(ticker);
    }

    public boolean hasTag() {
        return tagPresent;
    }

    public void setHasTag(boolean tagPresent) {
        this.tagPresent = tagPresent;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean hasPriority() {
        return this.priority != null;
    }

    public int getPriority() {
        return priority == null ? NotificationCompat.PRIORITY_DEFAULT : priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setPriority(String priority) {
        if (priority == null) {
            this.priority = null;
        } else {
            // Use the value of the field with matching name
            try {
                setPriority(Notification.class.getField("PRIORITY_" + priority.toUpperCase(Locale.ROOT)).getInt(null));
            } catch (Exception ignored) {
                this.priority = null;
            }
        }
    }

    public boolean hasAutoOpen() {
        return autoOpen != null;
    }

    public boolean getAutoOpen() {
        return autoOpen != null && autoOpen;
    }

    public void setAutoOpen(Boolean autoOpen) {
        this.autoOpen = autoOpen;
    }

    public boolean hasAutoDrop() {
        return autoDrop != null;
    }

    public boolean getAutoDrop() {
        return autoDrop != null && autoDrop;
    }

    public void setAutoDrop(Boolean autoDrop) {
        this.autoDrop = autoDrop;
    }

    public AlertModel getForeground() {
        return foreground;
    }

    public void setForeground(AlertModel foreground) {
        this.foreground = foreground;
    }

    public List<String> getPersons() {
        return persons;
    }

    public void setPersons(List<String> persons) {
        this.persons = persons;
    }

    public void setPersons(JSONArray personsJson) {
        if (personsJson != null) {
            List<String> persons = new LinkedList<>();
            for (int i = 0; i < personsJson.length(); ++i) {
                String person = personsJson.optString(i);
                if (person != null) {
                    persons.add(person);
                }
            }
            setPersons(persons);
        }

    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        boolean valid = category == null;
        if (!valid) {
            // Accept the value if it corresponds to one of the category constants' value
            for (Field field : Notification.class.getFields()) {
                try {
                    if (field.getName().startsWith("CATEGORY_") && category.equals(field.get(null))) {
                        // A category field has the provided value, keep it
                        valid = true;
                        break;
                    }
                } catch (Exception ignored) { // IllegalAccessException
                }
            }
        }
        if (!valid) {
            // Use the value of the field with matching name
            try {
                category = (String) Notification.class.getField("CATEGORY_" + category.toUpperCase(Locale.ROOT)).get(null);
                valid = true;
            } catch (Exception ignored) {} // IllegalAccessException | ClassCastException | NullPointerException
        }
        // Valid or not, keep the given value
        this.category = category;
    }

    public boolean hasColor() {
        return color != null;
    }

    public int getColor() {
        return color == null ? NotificationCompat.COLOR_DEFAULT : color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColor(String color) {
        if (color == null) {
            this.color = null;
        } else {
            try {
                setColor(Color.parseColor(color));
            } catch (Exception ignored) { // IllegalArgumentException
                this.color = null; // don't set any color (instead of forcing the default color for instance), especially for parsing AlertModel.foreground
            }
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroupTargetUrl() {
        return groupTargetUrl;
    }

    public void setGroupTargetUrl(String groupTargetUrl) {
        this.groupTargetUrl = groupTargetUrl;
    }

    public boolean hasGroupTargetUrl() {
        return groupTargetUrlPresent;
    }

    public void setHasGroupTargetUrl(boolean groupTargetUrlPresent) {
        this.groupTargetUrlPresent = groupTargetUrlPresent;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public boolean hasLocalOnly() {
        return localOnly != null;
    }

    public boolean getLocalOnly() {
        return localOnly != null && localOnly;
    }

    public void setLocalOnly(Boolean localOnly) {
        this.localOnly = localOnly;
    }

    public boolean hasNumber() {
        return number != null;
    }

    public int getNumber() {
        return number == null ? 0 : number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public boolean hasOnlyAlertOnce() {
        return onlyAlertOnce != null;
    }

    public boolean getOnlyAlertOnce() {
        return onlyAlertOnce != null && onlyAlertOnce;
    }

    public void setOnlyAlertOnce(Boolean onlyAlertOnce) {
        this.onlyAlertOnce = onlyAlertOnce;
    }

    public boolean hasWhen() {
        return when != null;
    }

    public long getWhen() {
        return when == null ? System.currentTimeMillis() : when;
    }

    public void setWhen(Long when) {
        this.when = when;
    }

    public boolean hasShowWhen() {
        return showWhen != null;
    }

    public boolean getShowWhen() {
        return showWhen != null && showWhen;
    }

    public void setShowWhen(Boolean showWhen) {
        this.showWhen = showWhen;
    }

    public boolean hasUsesChronometer() {
        return usesChronometer != null;
    }

    public boolean getUsesChronometer() {
        return usesChronometer != null && usesChronometer;
    }

    public void setUsesChronometer(Boolean usesChronometer) {
        this.usesChronometer = usesChronometer;
    }

    public boolean hasVisibility() {
        return visibility != null;
    }

    public int getVisibility() {
        return visibility == null ? 0 : visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public void setVisibility(String visibility) {
        if (visibility == null) {
            setVisibility((Integer) null);
        } else {
            // Use the value of the field with matching name
            try {
                setVisibility(Notification.class.getField("VISIBILITY_" + visibility.toUpperCase(Locale.ROOT)).getInt(null));
            } catch (Exception ignored) {
            }
        }
    }

    public boolean hasVibrate() {
        return vibrate != null;
    }

    public boolean getVibrate() {
        return vibrate == null ? defaultVibrate : vibrate;
    }

    public void setVibrate(Boolean vibrate) {
        this.vibrate = vibrate;
    }

    public long[] getVibratePattern() {
        return vibratePattern;
    }

    public void setVibratePattern(long[] vibratePattern) {
        this.vibratePattern = vibratePattern;
    }

    public boolean hasLights() {
        return lights != null;
    }

    public boolean getLights() {
        return lights == null ? defaultLight : lights;
    }

    public void setLights(Boolean lights) {
        this.lights = lights;
    }

    public boolean hasLightsColor() {
        return lightsColor != null;
    }

    public int getLightsColor() {
        return lightsColor == null ? defaultNotificationColor : lightsColor;
    }

    public void setLightsColor(Integer lightsColor) {
        this.lightsColor = lightsColor;
    }

    public void setLightsColor(String lightsColor) {
        try {
            setLightsColor(Color.parseColor(lightsColor));
        } catch (Exception ignored) { // IllegalArgumentException | NullPointerException
            setLightsColor((Integer) null);
        }
    }

    public boolean hasLightsOn() {
        return lightsOn != null;
    }

    public int getLightsOn() {
        return lightsOn == null ? defaultNotificationLedOn : lightsOn;
    }

    public void setLightsOn(Integer lightsOn) {
        this.lightsOn = lightsOn;
    }

    public boolean hasLightsOff() {
        return lightsOff != null;
    }

    public int getLightsOff() {
        return lightsOff == null ? defaultNotificationLedOff : lightsOff;
    }

    public void setLightsOff(Integer lightsOff) {
        this.lightsOff = lightsOff;
    }

    public boolean hasSound() {
        return sound != null;
    }

    public boolean getSound() {
        return sound == null ? defaultSound : sound;
    }

    public void setSound(Boolean sound) {
        this.sound = sound;
    }

    public Uri getSoundUri() {
        return soundUri;
    }

    public void setSoundUri(Uri soundUri) {
        if (soundUri != null) {
            String scheme = soundUri.getScheme() == null ? null : soundUri.getScheme().toLowerCase(Locale.ROOT);
            if ("http".equals(scheme) || "https".equals(scheme)) {
                // Fetch external file (as the system RingtoneManager has no INTERNET permission, unlike us)
                // and convert it to a URI
                CacheUtil.FetchResult fetchResult = CacheUtil.fetchSound(soundUri, "Sound");
                if (fetchResult.needsWork()) {
                    resourcesToFetch.add(fetchResult.getWork());
                } // let the following code set the default sound meanwhile
                File soundCached = fetchResult.getResult();
                if (soundCached != null) {
                    soundUri = FileProvider.getUriForFile(
                            WonderPush.getApplicationContext(),
                            WonderPush.getApplicationContext().getPackageName() + ".wonderpush.fileprovider",
                            soundCached);
                    WonderPush.getApplicationContext().grantUriPermission("com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    WonderPush.logDebug("Sound: new URI: " + soundUri);
                } else {
                    setSound(true);
                    setSoundUri((Uri) null);
                }
            }
        }
        this.soundUri = soundUri;
    }

    public void setSoundUri(String soundUri) {
        if (soundUri == null) {
            setSoundUri((Uri) null);
        } else {
            // Resolve as a raw resource
            int resId = resolveResourceIdentifier(soundUri, "raw");
            if (resId != 0) {
                setSoundUri(new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(WonderPush.getApplicationContext().getPackageName())
                        .path(String.valueOf(resId))
                        .build());
            } else {
                // Parse as Uri
                try {
                    setSoundUri(Uri.parse(soundUri));
                } catch (Exception ex) {
                    Log.e(WonderPush.TAG, "Failed to parse sound as uri: " + soundUri);
                    setSoundUri((Uri) null);
                    setSound(defaultSound);
                }
            }
        }
    }

    public boolean hasOngoing() {
        return ongoing != null;
    }

    public boolean getOngoing() {
        return ongoing != null && ongoing;
    }

    public void setOngoing(Boolean ongoing) {
        this.ongoing = ongoing;
    }

    public boolean hasProgress() {
        return progress != null;
    }

    public boolean isProgressIndeterminate() {
        return progress != null && progress < 0;
    }

    public int getProgress() {
        return progress;
    }

    public int getProgressMax() {
        return 100;
    }

    public void setProgress(Integer progress) { // number between 0 and 100, or -1 for indeterminate
        this.progress = progress;
    }

    public void setProgress(boolean progress) { // true for indeterminate, false for no progress
        if (progress) {
            setProgress(-1);
        } else {
            setProgress(null);
        }
    }

    public boolean hasSmallIcon() {
        return smallIcon != null && smallIcon != 0;
    }

    public int getSmallIcon() {
        return smallIcon == null ? 0 : smallIcon;
    }

    public void setSmallIcon(Integer smallIcon) {
        this.smallIcon = smallIcon;
    }

    public void setSmallIcon(String smallIcon) {
        if (smallIcon == null) {
            setSmallIcon((Integer) null);
        } else {
            setSmallIcon(resourceIdOrNull(resolveIconIdentifier(smallIcon)));
        }
    }

    public Bitmap getLargeIcon() {
        return largeIcon;
    }

    public void setLargeIcon(Bitmap largeIcon) {
        this.largeIcon = largeIcon;
        if (largeIcon != null) {
            WonderPush.logDebug("Large icon: " + largeIcon.getWidth() + "x" + largeIcon.getHeight());
        }
    }

    public void setLargeIcon(String largeIcon) {
        setLargeIcon(resolveLargeIconFromString(largeIcon, "Large icon"));
    }

    public List<NotificationButtonModel> getButtons() {
        return buttons;
    }

    public void setButtons(List<NotificationButtonModel> buttons) {
        this.buttons = buttons;
    }

    public void setButtons(JSONArray buttonsJson) {
        if (buttonsJson == null) {
            setButtons((List<NotificationButtonModel>) null);
        } else {
            List<NotificationButtonModel> buttons = new LinkedList<>();
            for (int i = 0; i < buttonsJson.length(); ++i) {
                JSONObject button = buttonsJson.optJSONObject(i);
                if (button == null) continue;
                buttons.add(new NotificationButtonModel(this, button));
            }
            setButtons(buttons);
        }
    }

    public Boolean getAllowSystemGeneratedContextualActions() {
        return allowSystemGeneratedContextualActions;
    }

    public void setAllowSystemGeneratedContextualActions(Boolean allowSystemGeneratedContextualActions) {
        this.allowSystemGeneratedContextualActions = allowSystemGeneratedContextualActions;
    }

    public String getBadgeIconType() {
        return badgeIconType;
    }

    public int getBadgeIconTypeInt() {
        if ("none".equalsIgnoreCase(badgeIconType)) {
            return NotificationCompat.BADGE_ICON_NONE;
        } else if ("small".equalsIgnoreCase(badgeIconType)) {
            return NotificationCompat.BADGE_ICON_SMALL;
        } else if ("large".equalsIgnoreCase(badgeIconType)) {
            return NotificationCompat.BADGE_ICON_LARGE;
        } else {
            return NotificationCompat.BADGE_ICON_NONE;
        }
    }

    public void setBadgeIconType(String badgeIconType) {
        this.badgeIconType = badgeIconType;
    }

    public Boolean getChronometerCountDown() {
        return chronometerCountDown;
    }

    public void setChronometerCountDown(Boolean chronometerCountDown) {
        this.chronometerCountDown = chronometerCountDown;
    }

    public Boolean getColorized() {
        return colorized;
    }

    public void setColorized(Boolean colorized) {
        this.colorized = colorized;
    }

    public JSONObject getExtras() {
        return extras;
    }

    public void setExtras(JSONObject extras) {
        this.extras = extras;
    }

    public Long getTimeoutAfter() {
        return timeoutAfter;
    }

    public void setTimeoutAfter(Long timeoutAfter) {
        this.timeoutAfter = timeoutAfter;
    }
}
