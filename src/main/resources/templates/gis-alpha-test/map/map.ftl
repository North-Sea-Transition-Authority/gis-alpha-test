<#include '../layout/layout.ftl'>

<#assign pageTitle = "Feature Map" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  backLinkEnabled=true
>
  <div data-module='map' data-feature-ids="${featureIds?join(",")}"></div>
</@defaultPage>
