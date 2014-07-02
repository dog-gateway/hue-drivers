hue-drivers
===========

Drivers (Experimental) for the Philips Hue bridge and devices


### Changelog ###

*2014/06/27*
--------------

* Tested successfully the Hue Bridge discovery and the PushLink authentication
* Found a bug on light-strip handling: CT_COLOR_LAMP is wrongly mapped to OnOffLight instead of being mapped to ColorDimmableLight

*2014/06/18*
--------------

* Added support to Hue Bridge discovery and PushLink authentication (Experimental)
* Device discovery fully supported
* Completed color control 
* Completed On/Off control

*2014/05/26*
--------------

* HueGateway driver now connects to the Hue bridge
* Bridge discovery to be implemented
* Device (Lamp) discovery to be implemented
