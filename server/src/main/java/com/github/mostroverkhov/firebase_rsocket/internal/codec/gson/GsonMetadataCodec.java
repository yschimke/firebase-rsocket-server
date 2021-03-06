package com.github.mostroverkhov.firebase_rsocket.internal.codec.gson;

import com.github.mostroverkhov.firebase_rsocket.servercommon.KeyValue;
import com.github.mostroverkhov.firebase_rsocket.internal.codec.MetadataCodec;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.github.mostroverkhov.firebase_rsocket.servercommon.Conversions.bytesToReader;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class GsonMetadataCodec implements MetadataCodec {

    private final Gson gson;
    private Charset charset;

    public GsonMetadataCodec(Gson gson, Charset charset) {
        this.gson = gson;
        this.charset = charset;
    }

    @Override
    public byte[] encode(KeyValue keyValue) {
        String s = gson.toJson(keyValue);
        return s.getBytes(charset);
    }

    @Override
    public KeyValue decode(byte[] metadata) {
        try (JsonReader reader = new JsonReader(bytesToReader(metadata, charset))) {
            KeyValue kv = new KeyValue();
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                String val = reader.nextString();
                kv.put(key, val);
            }
            return kv;
        } catch (IOException e) {
            throw mapMetadataError(metadata, e);
        }
    }

    private static IllegalStateException mapMetadataError(byte[] metaData, IOException e) {
        return new IllegalStateException("Error reading metadata op: " + bytesToMessage(metaData), e);
    }

    private static String bytesToMessage(byte[] data) {
        String s;
        try {
            s = IOUtils.toString(data, "UTF-8");
        } catch (IOException e) {
            s = "(error reading message as UTF-8 string)";
        }
        return s;
    }
}
