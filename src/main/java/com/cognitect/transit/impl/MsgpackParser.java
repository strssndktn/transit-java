// Copyright (c) Cognitect, Inc.
// All rights reserved.

package com.cognitect.transit.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.ValueType;

import com.cognitect.transit.ArrayReadHandler;
import com.cognitect.transit.ArrayReader;
import com.cognitect.transit.DefaultReadHandler;
import com.cognitect.transit.MapReadHandler;
import com.cognitect.transit.MapReader;
import com.cognitect.transit.ReadHandler;


public class MsgpackParser extends AbstractParser {
    private final MessageUnpacker mp;

    public MsgpackParser(MessageUnpacker mp,
                         Map<String, ReadHandler<?,?>> handlers,
                         DefaultReadHandler defaultHandler,
                         MapReader<?, Map<Object, Object>, Object, Object> mapBuilder,
                         ArrayReader<?, List<Object>, Object> listBuilder) {
        super(handlers, defaultHandler, mapBuilder, listBuilder);
        this.mp = mp;
    }

    private Object parseLong() throws IOException {
        ImmutableValue val = mp.unpackValue();
        try {
            return val.asIntegerValue().asLong();
        }
        catch (Exception ex) {
            BigInteger bi = new BigInteger(val.asRawValue().asString());
        }
        return val;
    }

    @Override
    public Object parse(ReadCache cache) throws IOException {
        return parseVal(false, cache);
    }

    @Override
    public Object parseVal(boolean asMapKey, ReadCache cache) throws IOException {
        switch (mp.getNextFormat().getValueType()) {
            case STRING:
                return cache.cacheRead(mp.unpackString(), asMapKey, this);
            case MAP:
                return parseMap(asMapKey, cache, null);
            case ARRAY:
                return parseArray(asMapKey, cache, null);
            case INTEGER:
                return parseLong();
            case FLOAT:
                return mp.unpackDouble();
            case BOOLEAN:
                return mp.unpackBoolean();
            case NIL:
                mp.unpackNil();
        }

        return null;
    }

    @Override
    public Object parseMap(boolean ignored, ReadCache cache, MapReadHandler<Object, ?, Object, Object, ?> handler) throws IOException {

	    int sz = this.mp.unpackMapHeader();

        MapReader<Object, ?, Object, Object> mr = (handler != null) ? handler.mapReader() : mapBuilder;

        Object mb = mr.init(sz);

        for (int remainder = sz; remainder > 0; remainder--) {
            Object key = parseVal(true, cache);
            if (key instanceof Tag) {
                String tag = ((Tag)key).getValue();
                ReadHandler<Object, Object> val_handler = getHandler(tag);
                Object val;
                if (val_handler != null) {
                    if (this.mp.getNextFormat().getValueType() == ValueType.MAP && val_handler instanceof MapReadHandler) {
                        // use map reader to decode value
                        val = parseMap(false, cache, (MapReadHandler<Object, ?, Object, Object, ?>) val_handler);
                    } else if (this.mp.getNextFormat().getValueType() == ValueType.ARRAY && val_handler instanceof ArrayReadHandler) {
                        // use array reader to decode value
                        val = parseArray(false, cache, (ArrayReadHandler<Object, ?, Object, ?>) val_handler);
                    } else {
                        // read value and decode normally
                        val = val_handler.fromRep(parseVal(false, cache));
                    }
                } else {
                    // default decode
                    val = this.decode(tag, parseVal(false, cache));
                }
                return val;
            } else {
                mb = mr.add(mb, key, parseVal(false, cache));
            }
        }

        return mr.complete(mb);
    }

    @Override
    public Object parseArray(boolean ignored, ReadCache cache, ArrayReadHandler<Object, ?, Object, ?> handler) throws IOException {

	    int sz = this.mp.unpackArrayHeader();

        ArrayReader<Object, ?, Object> ar = (handler != null) ? handler.arrayReader() : listBuilder;

        Object ab = ar.init(sz);

        for (int remainder = sz; remainder > 0; remainder--) {
            Object val = parseVal(false, cache);
            if ((val != null) && (val instanceof Tag)) {
                // it's a tagged value
                String tag = ((Tag) val).getValue();
                ReadHandler<Object, Object> val_handler = getHandler(tag);
                if (val_handler != null) {
                    if (this.mp.getNextFormat().getValueType() == ValueType.MAP && val_handler instanceof MapReadHandler) {
                        // use map reader to decode value
                        val = parseMap(false, cache, (MapReadHandler<Object, ?, Object, Object, ?>) val_handler);
                    } else if (this.mp.getNextFormat().getValueType() == ValueType.ARRAY && val_handler instanceof ArrayReadHandler) {
                        // use array reader to decode value
                        val = parseArray(false, cache, (ArrayReadHandler<Object, ?, Object, ?>) val_handler);
                    } else {
                        // read value and decode normally
                        val = val_handler.fromRep(parseVal(false, cache));
                    }
                } else {
                    // default decode
                    val = this.decode(tag, parseVal(false, cache));
                }
                return val;
            } else {
                // fall through to regular parse
                ab = ar.add(ab, val);
            }
        }

        return ar.complete(ab);
    }
}
