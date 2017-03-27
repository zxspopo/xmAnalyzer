package com.yonyou.iuap.search.handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zengxs on 2016/9/20.
 */
public class AuthzSearchHandler extends SearchHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthzSearchHandler.class);

    @Override
    public void init(NamedList args) {
        super.init(args);
        TenantAuthzConfFactory.init();
    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        SolrParams params = req.getParams();
        boolean needAuthz = params.getBool(AuthzParams.IUAP_AUTHZ, false);
        if (needAuthz) {
            String authzValue = params.get(AuthzParams.IUAP_AUTHZ_VALUE);
            String tenantId = params.get(AuthzParams.IUAP_AUTHZ_TENANT_ID);
            String systemCode = params.get(AuthzParams.IUAP_AUTHZ_SYSTEM_CODE);
            if (StringUtils.isBlank(authzValue) || StringUtils.isBlank(tenantId) || StringUtils.isBlank(systemCode)) {
                throw new SolrException(
                        SolrException.ErrorCode.BAD_REQUEST,
                        "param[iuap.authz.value|iuap.authz.tenantid|iuap.authz.systemcode] can not be null or blank!Search requests cannot accept content streams.");
            }
            String q = TenantAuthzConfFactory.createQuery(params.get(CommonParams.Q), authzValue, tenantId, systemCode);
            Iterator<String> paramNameIterator = params.getParameterNamesIterator();
            Map<String, String[]> paramMap = new HashMap<>();
            while (paramNameIterator.hasNext()) {
                String paramName = paramNameIterator.next();
                if (CommonParams.Q.equalsIgnoreCase(paramName)) {
                    MultiMapSolrParams.addParam(paramName, q, paramMap);
                } else if (notAuthzParam(paramName)) {
                    Object value = params.get(paramName);
                    if (value.getClass().isArray()) {
                        paramMap.put(paramName, (String[]) value);
                    } else {
                        paramMap.put(paramName, new String[] {(String) value});
                    }
                }
            }
            MultiMapSolrParams multiMapSolrParams = new MultiMapSolrParams(paramMap);
            req = new SolrQueryRequestBase(req.getCore(), multiMapSolrParams) {};
            if (logger.isDebugEnabled()) {
                logger.debug("custom query resp is {}", req.getParams());
            }
        }
        super.handleRequestBody(req, rsp);
    }

    private boolean notAuthzParam(String paramName) {
        return !AuthzParams.IUAP_AUTHZ.equals(paramName) && !AuthzParams.IUAP_AUTHZ_VALUE.equals(paramName)
                && !AuthzParams.IUAP_AUTHZ_TENANT_ID.equals(paramName)
                && !AuthzParams.IUAP_AUTHZ_SYSTEM_CODE.equals(paramName);
    }

    @Override
    public String getDescription() {
        return "Solr authz handler";
    }

    @Override
    public String getSource() {
        return "iuap-solr-plugin";
    }
}
