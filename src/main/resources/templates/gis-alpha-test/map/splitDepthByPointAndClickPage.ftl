<#include '../layout/layout.ftl'>

<#assign pageTitle = "Feature Map" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  backLinkEnabled=true
  backLinkUrl=backUrl
>
  <div data-module='depth-split-by-point-and-click-page'
    data-feature-id-and-depths="${(featureIdAndDepthsJson!'[]')}"
    data-srs-wkid="${srsWkid?c}"
    data-feature-id="${featureId}"
  >
  </div>
</@defaultPage>
