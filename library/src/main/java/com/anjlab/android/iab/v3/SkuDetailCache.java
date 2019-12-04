package com.anjlab.android.iab.v3;

import android.content.Context;
import android.text.TextUtils;

import com.android.billingclient.api.SkuDetails;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author linkzhang
 * @describe
 * @date 2019-12-04 11:30
 */
public class SkuDetailCache extends BillingBase {

    private static final String ENTRY_DELIMITER = "#####";
    private static final String LINE_DELIMITER = ">>>>>";
    private static final String VERSION_KEY = ".version";

    private HashMap<String, SkuDetails> data;
    private String cacheKey;
    private String version;

    SkuDetailCache(Context context, String key) {
        super(context);
        data = new HashMap<>();
        cacheKey = key;
        load();
    }

    private String getPreferencesCacheKey() {
        return getPreferencesBaseKey() + cacheKey;
    }

    private void load() {
        String[] entries = loadString(getPreferencesCacheKey(), "").split(Pattern.quote(ENTRY_DELIMITER));
        for (String entry : entries) {
            if (!TextUtils.isEmpty(entry)) {
                String[] parts = entry.split(Pattern.quote(LINE_DELIMITER));
                SkuDetails skuDetails = null;
                if (parts.length > 1) {
                    try {
                        skuDetails = new SkuDetails(parts[1]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                if (skuDetails != null) {
                    data.put(parts[0], skuDetails);
                }
            }
        }
        version = getCurrentVersion();
    }

    private void reloadDataIfNeeded() {
        if (!version.equalsIgnoreCase(getCurrentVersion())) {
            data.clear();
            load();
        }
    }

    void put(String productId, SkuDetails purchase) {
        reloadDataIfNeeded();
        data.put(productId, purchase);
        flush();

    }

    void remove(String productId) {
        reloadDataIfNeeded();
        data.remove(productId);
        flush();

    }


    boolean includesProduct(String productId) {
        reloadDataIfNeeded();
        return data.containsKey(productId);
    }

    SkuDetails getDetails(String productId) {
        reloadDataIfNeeded();
        return data.containsKey(productId) ? data.get(productId) : null;
    }

    void put(String productId, String details) {
        reloadDataIfNeeded();
        SkuDetails purchase = null;
        try {
            purchase = new SkuDetails(details);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (purchase != null) {
            data.put(productId, purchase);
            flush();
        }

    }


    void clear() {
        reloadDataIfNeeded();
        data.clear();
        flush();
    }

    List<String> getContents() {
        return new ArrayList<>(data.keySet());
    }

    @Override
    public String toString() {
        return TextUtils.join(", ", data.keySet());
    }

    private void flush() {
        ArrayList<String> output = new ArrayList<>();
        for (String productId : data.keySet()) {
            SkuDetails info = data.get(productId);
            output.add(productId + LINE_DELIMITER + info.getOriginalJson());
        }
        saveString(getPreferencesCacheKey(), TextUtils.join(ENTRY_DELIMITER, output));
        version = Long.toString(new Date().getTime());
        saveString(getPreferencesVersionKey(), version);
    }

    private String getPreferencesVersionKey() {
        return getPreferencesCacheKey() + VERSION_KEY;
    }

    private String getCurrentVersion() {
        return loadString(getPreferencesVersionKey(), "0");
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void put(List<SkuDetails> skuDetails) {
        reloadDataIfNeeded();
        if (skuDetails!=null && !skuDetails.isEmpty()){
            for (SkuDetails details:skuDetails){
                data.put(details.getSku(), details);
            }
            flush();
        }
    }
}
