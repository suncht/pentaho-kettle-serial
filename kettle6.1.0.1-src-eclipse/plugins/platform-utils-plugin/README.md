BA Server Utils
===============

Collection of kettle steps to interact with BA Server services.

RELEASE-0.1
* __Get session variables__: reads session variables from the current user session in the BA Server
* __Set session variables__: writes session variables to the current user session in the BA Server
RELEASE-0.2
* __Call endpoint__: inspects the BA Server to list available endpoints / invokes a BA Server endpoint


## Additional info

The steps **Get session variables** and **Set session variables** read and write user session variables. In order to
simulate user session variables in Spoon (for testing purposes) the variables will be read from and written to internal
variables with a prefix `_FAKE_SESSION_`.
