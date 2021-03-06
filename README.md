# FARM
The Field Automated Routing Module (FARM) allows users to track their location history while moving around a field. This helps to ensure that a field is properly and efficiently covered during fertilization, planting, etc.

<img src="./images/logo.png" width="176" height="176">

## Current functionality
* Tracks user's path as they move. Path is plotted as a semi-transparent rectangluar polygon with a right/left offset as defined by the user. (An offset allows for the user to account for scenarios where the phone/tablet is not placed in the center of the tractor, but on the left or right side of the tractor). Location tracking can be paused and restarted.
* User can define the width of the rectangle in feet so that the width of the sprayer, planter, implement, etc. is represented by the plotted path.
* User can define the boundaries of their field by tapping locations along the perimeter of their field on the map (optional). This area is then highlighted on the main map.
* Location method can be chosen as:
    * GPS (uses only GPS satellite signals)
    * Fused Location (uses a combination of GPS, cell tower, and Wi-Fi signals)
* Location refresh rate can be chosen.
## Screenshots
Settings            | Drawing Field     | Field Plotted     | Plotting Path     |
:------------------:|:-----------------:|:-----------------:|:------------------:
<img src="./images/settings.png" width="189" height="393"> | <img src="./images/drawField.png" width="189" height="393"> | <img src="./images/Ready.png" width="189" height="393"> | <img src="./images/Path.png" width="189" height="393">

## Downloading
To download a runnable version of the FARM app, click the ["release"](https://github.com/sheppardd1/FARM/releases) tab near the top of this page, and download the latest "FARM.apk" file Once this file is downloaded on your Android device, you can tap the file and the app will install on your device. This app is offered as-is with no guarantees about its functionality whatsoever. Use it at your own risk.
## TODO
* Track stats such as acreage covered and time elapsed.
* Give user ability to save defined field for later use.
* Give user to save progress so they can resume later.
* Allow user to choose colors of field area and path.
## Dependencies
* The app is dependent on the android-maps-utils library
* Link to documentation: 
    * https://developers.google.com/maps/documentation/android-sdk/utility/setup
    * https://github.com/googlemaps/android-maps-utils
* To import this library, use the 3rd set of instructions given at https://o7planning.org/en/10525/how-to-add-external-libraries-to-android-project-in-android-studio
## Changelog
* 16MAY2020 [version 1.01]: Added ability for user to offset the recieved GPS values. This can be used when the phone is being held on the side of the tractor instead of the center.
