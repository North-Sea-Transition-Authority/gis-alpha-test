<#include '../layout/layout.ftl'>

<#assign pageTitle = "Feature selection" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
>
    <@fdsForm.htmlForm>
        <@fdsCheckbox.checkboxes
          fieldsetHeadingText="Which features should be displayed on the map?"
          path="form.featureIds"
          checkboxes=features
        />
        <@fdsAction.button buttonText="Show on map"/>
    </@fdsForm.htmlForm>
</@defaultPage>
