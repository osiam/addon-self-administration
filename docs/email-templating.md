# Email templating
We integrated [thymeleaf](http://www.thymeleaf.org) template engine for
creating email templates. Thymeleaf has a great integration with the actual version 4.0 of the Spring Framework which we use.

## Template resolving

For the three mechanisms mentioned below, we created default templates in english and german as HTML files. The default template language is english. You can have a look to the templates in the [resources folder](https://github.com/osiam/server/tree/5eab9fc54e2327b58f4a15673a79d66419bea511/registration-module/src/main/resources/registration-module/template/mail).

The template file retrieving will be explained by the example of the registration email. First we check the classpath for template files resolved by the following order:
 * **registration-*.html**: Your registration email template with a specified language. You have to set the language code in the locale of the SCIM User Object and choose a standard language code (e.g. 'en') which is listed here: [Supported Locales](http://www.oracle.com/technetwork/java/javase/javase7locales-334809.html) e.g. registration-en.html
 * **registration.html**: Your registration email template which will be taken, if the locale of the user is not set or the file with the language code not found. Could be your default language template, so you have to care only about one language file.
 * **registration-default-de.html**: The default template provided by OSIAM in german. Make sure you don't name your file like this since it can't be predicted which file will be taken from the container.
 * **registration-default.html**: The default template provided by OSIAM in english, this file should also not be overriden.

If you like to change also the other three email templates, you have to create files with the same concept mentioned above, but with the specific file name:

 * **changeemail.html** and **changeemail-*.html** (* locale e.g. de)
 * **changeemailinfo.html** and **changeemailinfo-*.html** (* locale e.g. de)
 * **lostpassword.html** and **lostpassword-*.html** (* locale e.g. de)

You have to copy all your template files with the folder: ```addon-self-administration/template/mail``` to /etc/osiam (or the folder you choose). You have to configure your container like mentioned above, we recommend to take the /etc/osiam folder and added to 'shared.loader' in catalina.properties of the tomcat.

## Template engine
Thymeleaf provides a wide range of functionality, which is explained in the [documentation](http://www.thymeleaf.org/doc/html/Using-Thymeleaf.html). So we just explain a few things, so you can handle your own templates.

Thymeleaf is a engine that render your java objects which referenced in your templates into readable text and also provide a wide range of logical functionality. You write standard HTML, but with some extensions of the thymeleaf engine. With thymeleaf you add th:* attributes to HTML elements and have access to the java object attributes by the ${object.attribute}-expression. This will just be rendered, if this expression is in an th:*-tag ([short introduction](http://www.thymeleaf.org/standarddialect5minutes.html) to the tags).
To show you an example, we will have a look at the [userinfos template file](https://github.com/osiam/addon-self-administration/blob/server/src/main/resources/registration-module/template/mail/userinfos-en.html) There we have access to the attributes of an object or in this example to the SCIM user object:

`<span id="greeting" th:remove="tag" th:text="${user.name} ? (${user.name.formatted}?: 'Sir or Madam') : 'Sir or Madam'"></span>`

You have access to all attributes of the user which have a getter and also with an infinity depth of object graph (e.g. (java)user.getName().getFormatted() >> (thymeleaf)user.name.formatted). In the above example, the complex name object attribute of the SCIM user will be null checked and also the formatted attribute of the name with [elvis operator](http://www.thymeleaf.org/doc/html/Using-Thymeleaf.html#default-expressions-elvis-operator)

We provide access in the template to the SCIM user, so all attributes of the user could be used to extract in your own template. You also could access the specific links in the email which you can set in the osiam-self-administration.properties. Just add this thymeleaf magic in your template file:

`<a th:href="@{${registerlink}}" th:text="@{${registerlink}}"></a>`
-> The registerlink will be generated and set by the add-on and link to itself (/registration/activation...)

For the other mechanisms and their emails, you have access to the following links, which you can [set in the properties file](configuration.md#orgosiammaillinkprefix).
* [activatelink](https://github.com/osiam/addon-self-administration/blob/master/src/main/resources/addon-self-administration/template/mail/changeemail-default.html): Change email
* [lostpasswordlink](https://github.com/osiam/addon-self-administration/blob/master/src/main/resources/addon-self-administration/template/mail/lostpassword-default-de.html): Password lost
