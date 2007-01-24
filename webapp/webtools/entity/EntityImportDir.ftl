<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<div class="head1">${uiLabelMap.WebtoolsImportToDataSource}</div>
<div class="tabletext">${uiLabelMap.WebtoolsMessage5}.</div>
<hr/>
  <div class="head2">${uiLabelMap.WebtoolsImport}:</div>

  <form method="post" action="<@ofbizUrl>entityImportDir</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsAbsolutePath}:</div>
    <div><input type="text" class="inputBox" size="60" name="path" value="${path?if_exists}"/></div>
    <div class="tabletext"><input type="checkbox" name="mostlyInserts" <#if mostlyInserts?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMostlyInserts}</div>
    <div class="tabletext"><input type="checkbox" name="maintainTimeStamps" <#if keepStamps?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMaintainTimestamps}</div>
    <div class="tabletext"><input type="checkbox" name="createDummyFks" <#if createDummyFks?exists>"checked"</#if>/>${uiLabelMap.WebtoolsCreateDummyFks}</div>
    <div class="tabletext"><input type="checkbox" name="deleteFiles" <#if (deleteFiles?exists || !path?has_content)>"checked"</#if>/>${uiLabelMap.WebtoolsDeleteFiles}</div>
    <div class="tabletext">${uiLabelMap.WebtoolsTimeoutSeconds}:<input type="text" size="6" value="${txTimeoutStr?default("7200")}" name="txTimeout"/></div>
    <div class="tabletext">${uiLabelMap.WebtoolsPause}:<input type="text" size="6" value="${filePauseStr?default("0")}" name="filePause"/></div>
    <div><input type="submit" value="${uiLabelMap.WebtoolsImportFile}"/></div>
  </form>
  <hr/>
  <#if messages?exists>
    <div class="head1">${uiLabelMap.WebtoolsResults}:</div>
    <#list messages as message>
        <div class="tabletext">${message}</div>
    </#list>
  </#if>
