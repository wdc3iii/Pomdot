package com.example.pomdot1;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static final HashMap<String, String> attributes = new HashMap<>();

    public static final String DETACHED_CHAR_UUID       = "00002A19-0000-1000-8000-00805f9b34fb";
    public static final String DETACHED_DESC_UUID       = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String DETACHED_SERV_UUID       = "0000180F-0000-1000-8000-00805f9b34fb";

    public static final String POMDOT_CHAR_UUID       = "00002A00-0000-1000-8000-00805f9b34fb";
    public static final String POMDOT_DESC_UUID       = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String POMDOT_SERV_UUID       = "7b4bffd6-1b31-4af5-9ef5-afa2972ced14";

    public static final String LOCATION_LAT_CHAR_UUID = "71dc81c7-c2a5-4296-bf47-83490b51e9f8";
    public static final String LOCATION_LAT_DESC_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String LOCATION_LON_CHAR_UUID = "e5b7f683-d622-499d-93e8-60f0969f94a6";
    public static final String LOCATION_LON_DESC_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String LOCATION_SERV_UUID     = "74b93a57-aa7b-4b7e-93d7-ea5f58b19364";
    public static final String DATE_CHAR_UUID         = "b9152bc4-31af-4012-8f22-0cd93299ab3a";
    public static final String DATE_DESC_UUID         = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String TIME_CHAR_UUID         = "1f418979-4776-440a-b0ff-57af57594086";
    public static final String TIME_DESC_UUID         = "00002902-0000-1000-8000-00805f9b34fb";

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

