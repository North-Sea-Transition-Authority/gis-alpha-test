<#include '../layout/layout.ftl'>

<#assign pageTitle = "Feature Map" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  backLinkEnabled=true
  backLinkUrl=backUrl
>
  <@fdsAction.link
    linkClass="fds-link-button"
    linkText="Switch to split by coordinate entry"
    linkUrl=springUrl(coordinateEntryMapUrl)
  />
  <div data-module='split-by-point-and-click-page'
    data-feature-ids="${featureIds?join(",")}"
    data-srs-wkid="${srsWkid?c}"
    <#if journeyId??>data-journey-id="${journeyId}"</#if>
  >
  </div>
</@defaultPage>
