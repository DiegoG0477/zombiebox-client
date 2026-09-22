# Native DIAL Home entry

Whisperplay metadata uses a development `ZombieBox` DIAL ID and the documented
`authorizedOrigins` package entry for Zombie Cast. A custom DIAL_HOME action targets
a separate exported Activity, **disabled by default**. Advanced → Native DIAL launch
locally enables/disables that component. No model/manufacturer branches or native
libraries are used. Disabled state is checked again when an Intent arrives.

Only empty payload or `screen=home` opens the existing Home. Payloads, URLs,
credentials and additionalDataUrl are never forwarded or fetched. This is not
YouTube registration, pairing approval, autoplay, a DIAL media transport or proof
of an OEM service. Existing paired control/receiver ownership remains independent.
There is no proprietary Vizio launch contract in the supplied package inventory.

Whisperplay can read static metadata even while the launch component is disabled;
the local policy gates execution, not an assertion that OEM advertisements disappear.
Public DIAL registry approval, Cast-side native DIAL discovery/launch and actual Fire
TV lifecycle evidence remain open. The development ID must not be presented as registered.

Contract: [Amazon DIAL integration](https://developer.amazon.com/docs/fire-tv/dial-integration.html).
