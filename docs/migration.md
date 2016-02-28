# Migration Notes

## from 1.x to 2.0

### Field configuration

This release add the ability to configure the requirement of a HTML form field
within the registration form. The result are the changed template files
`registration.html` and `htmlfield.html`. If you did not change anything by
yourself, just replace the files and no migration is needed. Also the
properties do not need to be changed, if you do not like a different behaviour.
If you like to change the requirement of a HTML form field within the browser,
please check the [documentation](user-registration.md#orgosiamhtmlformfields).
If you changed one of the mentioned template file, please read the following
migration:

In `registration.html`:

Before: 

    <div th:replace="htmlfield :: defaultInput('userName', 'text', true)"></div>
  
After:

    <div th:replace="htmlfield :: defaultInput('userName', 'text')"></div>
    
So just remove the last parameter of the `defaultInput` function. For sure this
is because the signature of the `defaultInput` function changed, so please also
remove the last parameter `required` in `htmlfield.html`.

Also the `extensioninput` changed. Please rename them to `extensionInput` in
both mentioned template files and remove the last parameter.

In `htmlfield.html` you need to change all `required` occurrences with
`requiredFieldMapping.get(attribute)`.

Before:

    th:required="${required}"
    
After:

    th:required="${requiredFieldMapping.get(attribute)}"

Other changes:

Before:

    th:if="${#arrays.contains(allowedFields, attribute)}"
    
After:

    th:if="${#maps.containsKey(requiredFieldMapping, attribute)}"

Please check against your changed template files and change line per line to
avoid overriding your specific changes.
