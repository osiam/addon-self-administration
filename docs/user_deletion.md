<h1>User deletion</h1>
<h2>Metadata</h2>
<table>
<tr>
<td>Path</td>
<td><i>addon-self-administration/deletion/</i></td>
</tr>
<tr>
<td>Method</td>
<td><i>POST</i></td>
</tr>
<tr>
<td>Param(s)</td>
<td><i>user id</i></td>
</tr>
</table>
<h2>Description</h2>
<p>
On action the API shall:
<ul>
<li>remove all user data permanently</li>
<li>send a confirmation email to the primary ore first email address</li>
<li>revoke active access tokens from user</li>
</ul>
</p>
<p>
A user can register with same user data then the deleted user.
</p>

<h2>cURL example</h2>
<pre>
curl -X POST -H "Accept: application/json" -H "Authorization: Bearer &lt;Access_Token&gt;" http://osiam-test.lan.tarent.de:8080/addon-self-administration/deletion/&lt;User_Id&gt;</pre>