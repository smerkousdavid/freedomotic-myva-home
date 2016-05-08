# freedomotic-myva-home
A modified version of myva home (Android app) for the freedomotic framework

###ALPHA (Currently being developed)

##What can it currently do?

Sample commands(Provided by Rest and freedomotic)
Any user command (Currently no params)

Available commands:

Freedomotic version (key vers <<= freedomotic)
Shutdown Freedomotic (key shutdown >>= freedomotic)

ANY user command...
based of the name (uuid not required and is auto calculated)
example

    Turn on kitchen lights
    
    ACTUAL COMMAND NAME(S): Turn on kitchen light-145 (AND) Turn on Kitchen Light
    Key codes auto calc Turn, kitchen, on, light( % > 145)
    That command passed are iterated through multiple commands(So just like a google search the more
    specifi you input a query the more specific the results will turn out to be...)
