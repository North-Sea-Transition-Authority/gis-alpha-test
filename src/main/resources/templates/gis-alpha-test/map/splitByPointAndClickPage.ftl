<#include '../layout/layout.ftl'>

<@defaultPage
  htmlTitle="Feature map"
  pageHeading=""
  pageSize=PageSize.FULL_COLUMN
  backLinkEnabled=true
  backLinkUrl=backUrl
>
  <div data-module='split-by-point-and-click-page'
    data-journey-id="${journeyId}"
    data-srs-wkid="${srsWkid?c}"
  >
  </div>
</@defaultPage>
