<#include '../layout/layout.ftl'>

<#assign pageTitle = "Map" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
>
  <div data-module='map'></div>
</@defaultPage>
