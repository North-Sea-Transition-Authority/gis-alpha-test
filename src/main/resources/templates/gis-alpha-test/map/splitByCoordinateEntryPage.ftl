<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle="Feature map"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
backLinkEnabled=true
backLinkUrl=backUrl
>
  <div
    data-module='split-by-coordinate-entry-page'
    data-feature-ids="${featureIds?join(",")}"
    data-srs-wkid="${srsWkid?c}"
    <#if journeyId??>data-journey-id="${journeyId}"</#if>
    <#if userTestingExtentText??>data-user-testing-extent-text="${userTestingExtentText}"</#if>
  >
  </div>
</@defaultPage>
