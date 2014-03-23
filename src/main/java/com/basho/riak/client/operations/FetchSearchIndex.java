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

import com.basho.riak.client.RiakCommand;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.operations.YzFetchIndexOperation;

import java.util.concurrent.ExecutionException;

 /*
 * @author Dave Rusek <drusuk at basho dot com>
 * @since 2.0
 */
public class FetchSearchIndex extends RiakCommand<YzFetchIndexOperation.Response>
{
	private final String index;

	FetchSearchIndex(Builder builder)
	{
		this.index = builder.index;
	}

	@Override
	protected final YzFetchIndexOperation.Response doExecute(RiakCluster cluster) throws ExecutionException, InterruptedException
	{
	    YzFetchIndexOperation.Builder builder = new YzFetchIndexOperation.Builder();
	    builder.withIndexName(index);
	    YzFetchIndexOperation operation = builder.build();
	    RiakFuture<YzFetchIndexOperation.Response, String> future =
            cluster.execute(operation);
        
        future.await();
        if (future.isSuccess())
        {
            return future.get();
        }
        else
        {
            throw new ExecutionException(future.cause().getCause());
        }
	}

	public static class Builder
	{
		private final String index;

		public Builder(String index)
		{
			this.index = index;
		}

		public FetchSearchIndex build()
		{
			return new FetchSearchIndex(this);
		}
	}
}
