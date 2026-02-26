<#include './layout/layout.ftl'>

<#assign pageTitle = "Reset shapes" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>
  <@fdsForm.htmlForm>
    <@fdsTextInput.textInput labelText="Token" path="form.resetToken" />
    <@fdsAction.button buttonClass="govuk-button govuk-button--warning" buttonText="Reset shapes"/>
  </@fdsForm.htmlForm>

</@defaultPage>