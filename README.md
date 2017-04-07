# ProximityBand-Android

Connects with the Proximity Band over Bluetooth Low Energy to send and recieve data.

## Usage:

<b>Connecting with the Proximity Band:</b><br>
Turn on the Proximity Band.<br>
Start the Proximity Band Andoid App.<br>
If Bluetooth is not turned on, accept the prompt to turn it on.<br>
Using the "Bluetooth LE Device" dropdown, select the Proximity Band device.<br>
Tap the "CONNECT" button.<br>
If successful, the button text will chage to "DISCONNECT".<br>
The app will attempt the automatically re-connect to the last connected device on the next launch.<br>

<b>Setting alarm settings:</b><br>
Threshold slider: determines how low the signal strength must be for the alarm to activate.<br>
Filtering slider: determines how smooth the filtered signal strength is.<br>
Delay slider: determines how long the signal strength must remain below the threshold for the alarm to activate.<br>

<b>Setting the notification groups:</b><br>
While the device is connected, notifications are forwarded to the Proximity Band.<br>
Each notification can activate the red, green, or blue LEDs.<br>
To choose which app's notifications will activate the red LED, tap on the red box under "Notification Groups" and select them from the list of all apps on the Android device.<br>
Similarly, the same process is used for the green and blue notification groups.<br>

<b>Turning off the alarm:</b><br>
In any case while the alarm is turned on, simply press the "Ok" or "Cancel" on the alarm dialog that pops up.<br>
<br>

## TODO:
See issues.

## Technical details:
<pre>
<b>Getting data from the Proximity Band's Bluetooth LE:</b>
Switch Position 1 Command  		=	"S1"
Switch Position 2 Command  		=	"S2"
Switch Position 3 Command  		=	"S3"

Button Pressed Command    		=	"B1"
Button Unpressed Command  		=	"B0"

Acknowledged Command                    =	'A'

<b>Sending data to the Proximity Band's Bluetooth LE:</b>
Red LED On Command         		=	'R'
Green LED On Command       		=	'G'
Blue LED On Command        		=	'B'
Red LED Off Command        		=	'r'
Green LED Off Command      		=	'g'
Blue LED Off Command       		=	'b'

Red LED Flash On Command    	        =       'I'
Green LED Flash On Command  	        =       'J'
Blue LED Flash On Command   	        =       'K'
Red LED Flash Off Command   	        =       'i'
Green LED Flash Off Command 	        =       'j'
Blue LED Flash Off Command  	        =       'k'

Motor Short Command      	 	=	'1'
Motor Medium Command     	 	=	'2'
Motor Long Command       	 	=	'3'

Alarm On Command           	 	=	'X'
Alarm Off Command           	        =    	'x'
Alarm Supress Command                   =	'Y'
</pre>
