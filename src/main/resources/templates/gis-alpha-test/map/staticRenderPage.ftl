<#include '../layout/layout.ftl'>

<#assign pageTitle = "Static Render" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
>
  <#assign shapesJson>
    [<#list shapes as shape>{"esriJsonPolygon": "${shape.esriJsonPolygon()?j_string}", "depthStart": ${shape.depthStart()?c}, "depthEnd": ${shape.depthEnd()?c}, "name": "${shape.name()?j_string}"}<#sep>,</#sep></#list>]
  </#assign>
  <div data-module='static-render-page'
       data-shapes='${shapesJson}'
  >
  </div>
</@defaultPage>
