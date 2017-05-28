package com.github.mostroverkhov.firebase_rsocket.internal.codec.gson.read;

import com.github.mostroverkhov.firebase_rsocket.internal.codec.gson.util.GsonSerializer;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.read.NonTypedReadResponse;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.read.ReadRequest;
import com.github.mostroverkhov.firebase_rsocket_data.common.model.read.ReadResponse;
import com.google.gson.*;
import io.reactivex.Flowable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class DataWindowTransformer<T> implements Function<NonTypedReadResponse, Flowable<ReadResponse<T>>> {
    private final GsonSerializer gsonSerializer;
    private final Class<T> itemsType;

    public DataWindowTransformer(GsonSerializer gsonSerializer, Class<T> itemsType) {
        this.gsonSerializer = gsonSerializer;
        this.itemsType = itemsType;
    }

    @Override
    public Flowable<ReadResponse<T>> apply(NonTypedReadResponse nonTyped) {
        return Flowable.just(typedWindow(nonTyped));
    }

    private ReadResponse<T> typedWindow(NonTypedReadResponse nonTyped) {
        ReadRequest readRequest = nonTyped.getReadRequest();
        String data = nonTyped.getData();
        List<T> typedData = typedItems(data);
        return new ReadResponse<>(readRequest, typedData);
    }

    private List<T> typedItems(String items) {
        Gson gson = gsonSerializer.getGson();
        return typedItems(gson, itemsType, asArray(gson, items));
    }

    private static <T> List<T> typedItems(Gson gson,
                                          Class<T> itemType,
                                          JsonArray jsonArray) {
        List<T> data = new ArrayList<>();
        for (JsonElement dataItemJson : jsonArray) {
            T t = gson.fromJson(dataItemJson, itemType);
            data.add(t);
        }
        return data;
    }

    private static JsonArray asArray(Gson gson, String items) {
        TypeAdapter<JsonElement> adapter = gson.getAdapter(JsonElement.class);
        Reader reader = new BufferedReader(new StringReader(items));
        try {
            return adapter.fromJson(reader).getAsJsonArray();
        } catch (IOException e) {
            throw new RuntimeException("Error while reading json input ", e);
        }
    }
}
