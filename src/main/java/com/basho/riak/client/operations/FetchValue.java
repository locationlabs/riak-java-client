/*
 * Copyright 2013 Basho Technologies Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basho.riak.client.operations;

import com.basho.riak.client.cap.Quorum;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.convert.PassThroughConverter;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.operations.FetchOperation;
import com.basho.riak.client.util.ByteArrayWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.basho.riak.client.convert.Converters.convert;

/**
 * Command used to fetch a value from Riak, referenced by it's key.
 */
public class FetchValue<T> extends RiakCommand<FetchValue.Response<T>>
{

    private final Location location;
    private final Map<FetchOption<?>, Object> options;
    private final Converter<T> converter;

    public FetchValue(Location location, Converter<T> converter)
    {
        this.options = new HashMap<FetchOption<?>, Object>();
        this.converter = converter;
        this.location = location;
    }

    /**
     * Factory method for a FetchValue command with a conversion step
     *
     * @param location  the key to fetch
     * @param converter a domain object converter
     * @return a response object
     */
    public FetchValue(Key location, Converter<T> converter)
    {
        this((Location) location, converter);
    }

    /**
     * Factory method for a FetchValue command returning raw RiakObjects
     *
     * @param location the key to fetch
     * @return a response object
     */
    public FetchValue(Key location)
    {
        this((Location) location, (Converter<T>) new PassThroughConverter());
    }

    /**
     * Add an optional setting for this command. This will be passed along with the
     * request to Riak to tell it how to behave when servicing the request.
     *
     * @param option
     * @param value
     * @param <U>
     * @return
     */
    public <U> FetchValue<T> withOption(FetchOption<U> option, U value)
    {
        options.put(option, value);
        return this;
    }

    @Override
    Response<T> execute(RiakCluster cluster) throws ExecutionException, InterruptedException
    {

        ByteArrayWrapper type = location.getType();
        ByteArrayWrapper bucket = location.getBucket();
        ByteArrayWrapper key = location.getKey();

        FetchOperation.Builder builder = new FetchOperation.Builder(bucket, key);
        builder.withBucketType(type);

        for (Map.Entry<FetchOption<?>, Object> opPair : options.entrySet())
        {

            RiakOption<?> option = opPair.getKey();

            if (option == FetchOption.R)
            {
                builder.withR(((Quorum) opPair.getValue()).getIntValue());
            }
            else if (option == FetchOption.DELETED_VCLOCK)
            {
                builder.withReturnDeletedVClock((Boolean) opPair.getValue());
            }
            else if (option == FetchOption.TIMEOUT)
            {
                builder.withTimeout((Integer) opPair.getValue());
            }
            else if (option == FetchOption.HEAD)
            {
                builder.withHeadOnly((Boolean) opPair.getValue());
            }
            else if (option == FetchOption.BASIC_QUORUM)
            {
                builder.withBasicQuorum((Boolean) opPair.getValue());
            }
            else if (option == FetchOption.IF_MODIFIED)
            {
                VClock clock = (VClock) opPair.getValue();
                builder.withIfNotModified(clock.getBytes());
            }
            else if (option == FetchOption.N_VAL)
            {
                builder.withNVal((Integer) opPair.getValue());
            }
            else if (option == FetchOption.PR)
            {
                builder.withPr(((Quorum) opPair.getValue()).getIntValue());
            }
            else if (option == FetchOption.SLOPPY_QUORUM)
            {
                builder.withSloppyQuorum((Boolean) opPair.getValue());
            }
            else if (option == FetchOption.NOTFOUND_OK)
            {
                builder.withNotFoundOK((Boolean) opPair.getValue());
            }

        }

        FetchOperation operation = builder.build();

        FetchOperation.Response response = cluster.execute(operation).get();
        List<T> converted = convert(converter, response.getObjectList());

        return new Response<T>(response.isNotFound(), response.isUnchanged(), converted, response.getVClock());

    }

    /**
     * A response from Riak including the vector clock.
     *
     * @param <T> the type of the returned object, if no converter given this will be RiakObject
     */
    public static class Response<T>
    {

        private final boolean notFound;
        private final boolean unchanged;
        private final VClock vClock;
        private final List<T> value;

        Response(boolean notFound, boolean unchanged, List<T> value, VClock vClock)
        {
            this.notFound = notFound;
            this.unchanged = unchanged;
            this.value = value;
            this.vClock = vClock;
        }

        public boolean isNotFound()
        {
            return notFound;
        }

        public boolean isUnchanged()
        {
            return unchanged;
        }

        public boolean hasvClock()
        {
            return vClock != null;
        }

        public VClock getvClock()
        {
            return vClock;
        }

        public boolean hasValue()
        {
            return value != null;
        }

        public List<T> getValue()
        {
            return value;
        }

    }
}
