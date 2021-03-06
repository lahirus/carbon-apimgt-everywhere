
/*
 * Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.everywhere.interceptor.valve.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.apimgt.core.APIManagerConstants;
import org.wso2.carbon.apimgt.everywhere.interceptor.UsageStatConfiguration;
import org.wso2.carbon.apimgt.everywhere.interceptor.valve.APIManagerInterceptorValve;
import org.wso2.carbon.apimgt.usage.publisher.APIMgtUsageDataPublisher;
import org.wso2.carbon.apimgt.usage.publisher.DataPublisherUtil;
import org.wso2.carbon.apimgt.usage.publisher.internal.UsageComponent;
import org.wso2.carbon.apimgt.usage.publisher.service.APIMGTConfigReaderService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.tomcat.ext.valves.TomcatValveContainer;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.ArrayList;

/**

 * @scr.component name="org.wso2.carbon.apimgt.interceptor.valve.internal" immediate="true"
 
 * @scr.reference name="config.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"
 
 * cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * 
 * @scr.reference name="registry.service" interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 *
 * @scr.reference name="usage.service" interface="org.wso2.carbon.apimgt.usage.publisher.service.APIMGTConfigReaderService"
 * cardinality="1..1" policy="dynamic" bind="setAPIMGTConfigReaderService" unbind="unsetAPIMGTConfigReaderService"
 *
 */
public class APIManagerInterceptorComponent {
	private static final Log log = LogFactory.getLog(APIManagerInterceptorComponent.class);
    private static String apiManagementEnabled;
    private static String externalAPIManagerGatewayURL;

    protected void activate(ComponentContext ctx) {
    	if (log.isDebugEnabled()) {
    		log.info("Activating API Manager Interceptor Component");
    	}

        try {
            apiManagementEnabled = CarbonUtils.getServerConfiguration().getFirstProperty(APIManagerConstants.API_MANGEMENT_ENABLED);
            externalAPIManagerGatewayURL = CarbonUtils.getServerConfiguration().getFirstProperty(APIManagerConstants.EXTERNAL_API_GATEWAY);

    	/* Register the valves with Tomcat, if apimgt is enabled & external api manager is not configured */
            if (apiManagementEnabled.equalsIgnoreCase("true") && externalAPIManagerGatewayURL == null) {
                ArrayList<CarbonTomcatValve> valves = new ArrayList<CarbonTomcatValve>();
                valves.add(new APIManagerInterceptorValve());
                TomcatValveContainer.addValves(valves);
            }
            setStatPublishingConf();

        }catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }
    
	private void setStatPublishingConf() {
		boolean statsPublishingEnabled = UsageComponent.getApiMgtConfigReaderService().isEnabled();
		String statsPublisherClass = UsageComponent.getApiMgtConfigReaderService()
		                                           .getPublisherClass();
		String hostName = DataPublisherUtil.getHostAddress();
		APIMgtUsageDataPublisher publisher = null;

		try {
			publisher = (APIMgtUsageDataPublisher) Class.forName(statsPublisherClass)
			                                                                     .newInstance();
		} catch (InstantiationException e) {
			String msg = "Error instantiating";
			log.error(msg + statsPublisherClass);
		} catch (IllegalAccessException e) {
			String msg = "Illegal access to";
			log.error(msg + statsPublisherClass);
		} catch (ClassNotFoundException e) {
			String msg = "Class not found";
			log.error(msg + statsPublisherClass);
		}
		UsageStatConfiguration statconf = new UsageStatConfiguration();
		statconf.setHostName(hostName);		
		statconf.setStatsPublishingEnabled(statsPublishingEnabled);
		statconf.setPublisher(publisher);
	}

    protected void deactivate(ComponentContext componentContext) {
    	
    }
    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.setServerConfigContext(contextService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.setServerConfigContext(null);
    }
    
    protected void setAPIMGTConfigReaderService(APIMGTConfigReaderService configReaderService) {
//        DataHolder.setServerConfigContext(contextService.getServerConfigContext());
    }

    protected void unsetAPIMGTConfigReaderService(APIMGTConfigReaderService configReaderService) {
//        DataHolder.setServerConfigContext(null);
    }

    protected void setRegistryService(RegistryService registryService) {
        if (registryService != null && log.isDebugEnabled()) {
            log.debug("Registry service initialized");
        }
        DataHolder.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
    	DataHolder.setRegistryService(null);
    }

    public static String getAPIManagementEnabled() {
        return apiManagementEnabled;
    }

    public static String getExternalAPIManagerGatewayURL() {
        return externalAPIManagerGatewayURL;
    }

}

