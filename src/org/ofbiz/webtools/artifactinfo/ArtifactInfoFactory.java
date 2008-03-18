/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.webtools.artifactinfo;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entityext.eca.EntityEcaRule;
import org.ofbiz.entityext.eca.EntityEcaUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.eca.ServiceEcaRule;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.ofbiz.webapp.control.ConfigXMLReader;
import org.ofbiz.webapp.control.ConfigXMLReader.ControllerConfig;
import org.ofbiz.widget.form.FormFactory;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.screen.ModelScreen;
import org.ofbiz.widget.screen.ScreenFactory;
import org.xml.sax.SAXException;

/**
 *
 */
public class ArtifactInfoFactory {
    
    protected static UtilCache<String, ArtifactInfoFactory> artifactInfoFactoryCache = new UtilCache("ArtifactInfoFactory");
    
    protected String delegatorName;
    protected ModelReader entityModelReader;
    protected DispatchContext dispatchContext;
    protected Map<String, Map<String, List<EntityEcaRule>>> entityEcaCache;
    protected Map<String, Map<String, List<ServiceEcaRule>>> serviceEcaCache;
    
    public Map<String, EntityArtifactInfo> allEntityInfos = FastMap.newInstance();
    public Map<String, ServiceArtifactInfo> allServiceInfos = FastMap.newInstance();
    public Map<ServiceEcaRule, ServiceEcaArtifactInfo> allServiceEcaInfos = FastMap.newInstance();
    public Map<String, FormWidgetArtifactInfo> allFormInfos = FastMap.newInstance();
    public Map<String, ScreenWidgetArtifactInfo> allScreenInfos = FastMap.newInstance();
    public Map<String, ControllerRequestArtifactInfo> allControllerRequestInfos = FastMap.newInstance();
    public Map<String, ControllerViewArtifactInfo> allControllerViewInfos = FastMap.newInstance();

    // reverse-associative caches for walking backward in the diagram
    public Map<String, Set<ServiceEcaArtifactInfo>> allServiceEcaInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToServiceName = FastMap.newInstance();
    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToServiceName = FastMap.newInstance();
    
    public Map<String, Set<ServiceArtifactInfo>> allServiceInfosReferringToEntityName = FastMap.newInstance();
    public Map<String, Set<FormWidgetArtifactInfo>> allFormInfosReferringToEntityName = FastMap.newInstance();
    public Map<String, Set<ScreenWidgetArtifactInfo>> allScreenInfosReferringToEntityName = FastMap.newInstance();

    public Map<ServiceEcaRule, Set<ServiceArtifactInfo>> allServiceInfosReferringToServiceEcaRule = FastMap.newInstance();
    
    public Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToView = FastMap.newInstance(); 
    public Map<String, Set<ControllerRequestArtifactInfo>> allRequestInfosReferringToRequest = FastMap.newInstance(); 
    
    public static ArtifactInfoFactory getArtifactInfoFactory(String delegatorName) throws GeneralException {
        if (UtilValidate.isEmpty(delegatorName)) {
            delegatorName = "default";
        }
        
        ArtifactInfoFactory aif = artifactInfoFactoryCache.get(delegatorName);
        if (aif == null) {
            aif = new ArtifactInfoFactory(delegatorName);
            artifactInfoFactoryCache.put(delegatorName, aif);
        }
        return aif;
    }
    
    protected ArtifactInfoFactory(String delegatorName) throws GeneralException {
        this.delegatorName = delegatorName;
        this.entityModelReader = ModelReader.getModelReader(delegatorName);
        this.dispatchContext = new DispatchContext("ArtifactInfoDispCtx", null, this.getClass().getClassLoader(), null);
        this.entityEcaCache = EntityEcaUtil.getEntityEcaCache(EntityEcaUtil.getEntityEcaReaderName(delegatorName));
        this.serviceEcaCache = ServiceEcaUtil.ecaCache;
        
        this.prepareAll();
    }
    
    public void prepareAll() throws GeneralException {
        Set<String> entityNames = this.getEntityModelReader().getEntityNames();
        for (String entityName: entityNames) {
            this.getEntityArtifactInfo(entityName);
        }
        
        Set<String> serviceNames = this.getDispatchContext().getAllServiceNames();
        for (String serviceName: serviceNames) {
            this.getServiceArtifactInfo(serviceName);
        }
        
        // how to get all Service ECAs to prepare? don't worry about it, will be populated from service load, ie all ECAs for each service
        
        // TODO: how to get all forms to prepare?
        
        // TODO: how to get all screens to prepare?
        
        // TODO: get all controller requests and views to prepare
        Set<URL> controllerUrlSet = FastSet.newInstance();
        for (URL controllerUrl: controllerUrlSet) {
            ControllerConfig cc = ConfigXMLReader.getControllerConfig(controllerUrl);
            for (String requestUri: cc.requestMap.keySet()) {
                this.getControllerRequestArtifactInfo(controllerUrl, requestUri);
            }
            for (String viewUri: cc.viewMap.keySet()) {
                this.getControllerViewArtifactInfo(controllerUrl, viewUri);
            }
        }
    }
    
    public ModelReader getEntityModelReader() {
        return this.entityModelReader;
    }
    
    public DispatchContext getDispatchContext() {
        return this.dispatchContext;
    }
    
    public ModelEntity getModelEntity(String entityName) throws GenericEntityException {
        return this.getEntityModelReader().getModelEntity(entityName);
    }
    
    public ModelService getModelService(String serviceName) throws GenericServiceException {
        return this.getDispatchContext().getModelService(serviceName);
    }
    
    public ModelForm getModelForm(String formName, String formLocation) throws ParserConfigurationException, SAXException, IOException {
        return FormFactory.getFormFromLocation(formLocation, formName, this.entityModelReader, this.dispatchContext);
    }
    
    public ModelScreen getModelScreen(String screenName, String screenLocation) throws ParserConfigurationException, SAXException, IOException {
        return ScreenFactory.getScreenFromLocation(screenLocation, screenName);
    }
    
    public Map<String, String> getControllerRequestInfoMap(URL controllerXmlUrl, String requestUri) {
        return ConfigXMLReader.getControllerConfig(controllerXmlUrl).requestMap.get(requestUri);
    }

    public Map<String, String> getControllerViewInfoMap(URL controllerXmlUrl, String viewUri) {
        return ConfigXMLReader.getControllerConfig(controllerXmlUrl).viewMap.get(viewUri);
    }

    public EntityArtifactInfo getEntityArtifactInfo(String entityName) throws GeneralException {
        EntityArtifactInfo curInfo = this.allEntityInfos.get(entityName);
        if (curInfo == null) {
            curInfo = new EntityArtifactInfo(entityName, this);
            this.allEntityInfos.put(entityName, curInfo);
        }
        return curInfo;
    }
    
    public ServiceArtifactInfo getServiceArtifactInfo(String serviceName) throws GeneralException {
        ServiceArtifactInfo curInfo = this.allServiceInfos.get(serviceName);
        if (curInfo == null) {
            curInfo = new ServiceArtifactInfo(serviceName, this);
            this.allServiceInfos.put(serviceName, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }
    
    public ServiceEcaArtifactInfo getServiceEcaArtifactInfo(ServiceEcaRule ecaRule) throws GeneralException {
        ServiceEcaArtifactInfo curInfo = this.allServiceEcaInfos.get(ecaRule);
        if (curInfo == null) {
            curInfo = new ServiceEcaArtifactInfo(ecaRule, this);
            this.allServiceEcaInfos.put(ecaRule, curInfo);
            curInfo.populateAll();
        }
        return curInfo;
    }
    
    public FormWidgetArtifactInfo getFormWidgetArtifactInfo(String formName, String formLocation) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        FormWidgetArtifactInfo curInfo = this.allFormInfos.get(formLocation + "#" + formName);
        if (curInfo == null) {
            curInfo = new FormWidgetArtifactInfo(formName, formLocation, this);
            this.allFormInfos.put(curInfo.getUniqueId(), curInfo);
        }
        return curInfo;
    }
    
    public ScreenWidgetArtifactInfo getScreenWidgetArtifactInfo(String screenName, String screenLocation) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        ScreenWidgetArtifactInfo curInfo = this.allScreenInfos.get(screenLocation + "#" + screenName);
        if (curInfo == null) {
            curInfo = new ScreenWidgetArtifactInfo(screenName, screenLocation, this);
            this.allScreenInfos.put(curInfo.getUniqueId(), curInfo);
        }
        return curInfo;
    }
    
    public ControllerRequestArtifactInfo getControllerRequestArtifactInfo(URL controllerXmlUrl, String requestUri) {
        ControllerRequestArtifactInfo curInfo = this.allControllerRequestInfos.get(controllerXmlUrl.toExternalForm() + "#" + requestUri);
        if (curInfo == null) {
            curInfo = new ControllerRequestArtifactInfo(controllerXmlUrl, requestUri, this);
            this.allControllerRequestInfos.put(curInfo.getUniqueId(), curInfo);
        }
        return curInfo;
    }
    
    public ControllerViewArtifactInfo getControllerViewArtifactInfo(URL controllerXmlUrl, String viewUri) {
        ControllerViewArtifactInfo curInfo = this.allControllerViewInfos.get(controllerXmlUrl.toExternalForm() + "#" + viewUri);
        if (curInfo == null) {
            curInfo = new ControllerViewArtifactInfo(controllerXmlUrl, viewUri, this);
            this.allControllerViewInfos.put(curInfo.getUniqueId(), curInfo);
        }
        return curInfo;
    }
    
    public Set<ArtifactInfoBase> getAllArtifactInfosByNamePartial(String artifactNamePartial) {
        Set<ArtifactInfoBase> aiBaseSet = FastSet.newInstance();
        
        for (Map.Entry<String, EntityArtifactInfo> curEntry: allEntityInfos.entrySet()) {
            if (curEntry.getKey().contains(artifactNamePartial)) {
                aiBaseSet.add(curEntry.getValue());
            }
        }
        for (Map.Entry<String, ServiceArtifactInfo> curEntry: allServiceInfos.entrySet()) {
            if (curEntry.getKey().contains(artifactNamePartial)) {
                aiBaseSet.add(curEntry.getValue());
            }
        }
        
        for (Map.Entry<String, FormWidgetArtifactInfo> curEntry: allFormInfos.entrySet()) {
            if (curEntry.getKey().contains(artifactNamePartial)) {
                aiBaseSet.add(curEntry.getValue());
            }
        }
        for (Map.Entry<String, ScreenWidgetArtifactInfo> curEntry: allScreenInfos.entrySet()) {
            if (curEntry.getKey().contains(artifactNamePartial)) {
                aiBaseSet.add(curEntry.getValue());
            }
        }
        
        for (Map.Entry<String, ControllerRequestArtifactInfo> curEntry: allControllerRequestInfos.entrySet()) {
            if (curEntry.getKey().contains(artifactNamePartial)) {
                aiBaseSet.add(curEntry.getValue());
            }
        }
        for (Map.Entry<String, ControllerViewArtifactInfo> curEntry: allControllerViewInfos.entrySet()) {
            if (curEntry.getKey().contains(artifactNamePartial)) {
                aiBaseSet.add(curEntry.getValue());
            }
        }
        
        return aiBaseSet;
    }
}
