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
import net.hasor.core.AppContextAware;
import net.hasor.data.ql.Query;
import net.hasor.data.ql.UDF;
import net.hasor.data.ql.dsl.QueryModel;
import net.hasor.data.ql.dsl.parser.DataQLParser;
import net.hasor.data.ql.dsl.parser.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * DataQL 插件上下文，提供 DataUDF 发现和管理以及 QL 执行任务调度协助线程的。
 * @author 赵永春(zyc@hasor.net)
 * @version : 2017-03-23
 */
public class GraphContext implements AppContextAware {
    private AppContext       appContext;
    private Map<String, UDF> udfMap;
    protected GraphContext() {
    }
    //
    //
    @Override
    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
        this.udfMap = new HashMap<String, UDF>();
        List<UDFDefine> udfList = appContext.findBindingBean(UDFDefine.class);
        for (UDFDefine define : udfList) {
            String defineName = define.getName();
            if (this.udfMap.containsKey(defineName)) {
                throw new IllegalStateException("udf name ‘" + defineName + "’ already exist.");
            }
            this.udfMap.put(defineName, define);
        }
    }
    //
    protected UDF findUDF(String udfName) {
        return this.udfMap.get(udfName);
    }
    //
    public Query createQuery(String graphQL) throws ParseException {
        QueryModel queryModel = DataQLParser.parserQL(graphQL);
        return new QueryImpl(this.appContext, queryModel);
    }
}