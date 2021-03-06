/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.data.ql.ctx;
import net.hasor.core.AppContext;
import net.hasor.data.ql.UDF;
import net.hasor.data.ql.Query;
import net.hasor.data.ql.QueryResult;
import net.hasor.data.ql.dsl.QueryModel;
import net.hasor.data.ql.result.ValueModel;
import net.hasor.data.ql.runtime.QueryContext;
import net.hasor.data.ql.runtime.TaskParser;
import net.hasor.data.ql.runtime.TaskStatus;
import net.hasor.data.ql.runtime.task.AbstractPrintTask;
import net.hasor.data.ql.runtime.task.AbstractTask;

import java.util.*;
/**
 * 用于封装 QL 查询接口。
 * @author 赵永春(zyc@hasor.net)
 * @version : 2017-03-23
 */
class QueryImpl implements Query {
    private final GraphContext     graphContext;
    private final QueryModel       queryModel;
    private final Map<String, UDF> temporaryUDF;
    //
    public QueryImpl(AppContext appContext, QueryModel queryModel) {
        this.graphContext = appContext.getInstance(GraphContext.class);
        this.queryModel = queryModel;
        this.temporaryUDF = new HashMap<String, UDF>();
    }
    //
    @Override
    public String getQueryString(boolean useFragment) {
        if (useFragment) {
            return this.queryModel.buildQuery();
        } else {
            return this.queryModel.buildQueryWithoutFragment();
        }
    }
    //
    //    @Override
    //    public <T> T doQuery(Map<String, Object> queryContext, Class<?> toType) {
    //        QueryResult result = this.query(queryContext);
    //        return null;
    //    }
    @Override
    public QueryResult doQuery(Map<String, Object> queryContext) {
        AbstractPrintTask queryTask = new TaskParser().doParser(this.queryModel.getDomain());
        if (queryContext == null) {
            queryContext = Collections.EMPTY_MAP;
        }
        this.runTasks(new QueryContextImpl(queryContext) {
            @Override
            public UDF findUDF(String udfName) {
                if (temporaryUDF.containsKey(udfName)) {
                    return temporaryUDF.get(udfName);
                }
                return graphContext.findUDF(udfName);
            }
        }, queryTask);
        //
        try {
            Object value = queryTask.getValue();
            return new ValueModel(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void runTasks(QueryContext context, AbstractTask queryTask) {
        List<AbstractTask> allTask = queryTask.getAllTask();
        while (true) {
            List<AbstractTask> toTask = new ArrayList<AbstractTask>();
            for (AbstractTask t : allTask) {
                if (TaskStatus.Failed.equals(queryTask.getTaskStatus())) {
                    return;
                }
                //
                if (t.isWaiting()) {
                    toTask.add(t);
                }
            }
            //
            if (toTask.isEmpty()) {
                return;
            }
            //
            toTask.get(0).run(context, null);
            //            toTask.get(new Random(System.currentTimeMillis()).nextInt(toTask.size())).run(context, null);
            //
        }
    }
}