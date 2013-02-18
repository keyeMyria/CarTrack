<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="TrackAnimation.aspx.cs"
    Inherits="CarTrackService.TrackAnimation" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <title>轨迹动画</title>
    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.5/jquery.min.js" type="text/javascript"></script>
    <script src="http://www.google.com/jsapi?key=ABQIAAAAWnH52xN-emJ_1CCCph0mahQYrgrkeICy7YyoN5VEbQ7TybAgdRQnzbsN63I6d7E1JaRJyr8R6RIXmA"
        type="text/javascript"></script>
    <script src="http://maps.google.com/maps/api/js?sensor=false&region=cn" type="text/javascript"></script>
    <%--<script src="http://ditu.google.com/maps?file=api&amp;v=3&amp;key=ABQIAAAAWnH52xN-emJ_1CCCph0mahT2_nlfRAGNaLTeU_vebTNiW5pRUBQ5J0lYX1A2xoRwsn64ym062mLUKQ" type="text/javascript"></script>--%>
    <script type="text/javascript">
        var ge; 		//hold instance of google earth plugin
        var model; 	//hold 3D model to be placed on earth
        var marker; 	//hold marker on the google map
        var map; 		//hold instance of google map
        var panorama;

        var initLat = 48.85893528;//29.87;
        var initLng = 2.2933412;//121.63;

        google.load("earth", "1");
        google.setOnLoadCallback(init);

        function init() {
            google.earth.createInstance('div_map3d', initialise3dMap, failed);
        }

        function failed(errorCode) { alert('Unable to load google earth ' + errorCode);ge = null;}

        function initialise3dMap(instance) {
            //initialise google earth plugin instance for the very first time.
            ge = instance;

            ge.getWindow().setVisibility(true);
            ge.getLayerRoot().enableLayerById(ge.LAYER_ROADS, true);
            ge.getLayerRoot().enableLayerById(ge.LAYER_BUILDINGS, true);
            ge.getLayerRoot().enableLayerById(ge.LAYER_TERRAIN, true);
            ge.getOptions().setFlyToSpeed(ge.SPEED_TELEPORT);

            var lookAt = ge.createLookAt('lookat');  	// Create a new LookAt (i.e., point where we 
            // are looking )
            lookAt.setLatitude(initLat);    	// Set the coordinates values of the point 
            // where we looking at.
            lookAt.setLongitude(initLng);
            lookAt.setRange(10.0);            	// Set zoom range i.e., from how far are we 
            // looking at the point.
            lookAt.setAltitude(10);             	// Set altitude from ground of the point 
            // where we looking at.
            //lookAt.setAltitudeMode(ge.ALTITUDE_CLAMP_TO_GROUND);
            lookAt.setTilt(80);                	// Set the tilt angle where we looking at
            // i.e., how many degrees we need to down 
            // our head to look at the point.
            ge.getView().setAbstractView(lookAt);  	// update Google earth to new 'lookAt' point.

            // 3D Model placement
            model = ge.createModel('');            // create the model geometry
            var loc = ge.createLocation('');
            var scale = ge.createScale('');
            var orientation = ge.CreateOrientation('');
            loc.setLatitude(lookAt.getLatitude()); // fetch location of the current lookat point.
            loc.setLongitude(lookAt.getLongitude());
            model.setLocation(loc);                // set the location of the model.
            scale.Set(5, 5, 5);
            model.setScale(scale);                 // set scale for the model along x,y,z axis
            orientation.setHeading(-180);
            model.setOrientation(orientation);     // rotate the model x degrees from north.
            var link = ge.createLink('');          // defining link
            model.setLink(link);                   // setting the collada file for the model
            link.setHref('us_police_car.dae11');
            var modelPlacemark = ge.createPlacemark('');   // define the model placemark
            modelPlacemark.setGeometry(model);             // set model in to the placemark
            ge.getFeatures().appendChild(modelPlacemark); // add placemark to Earth
        }

        function initialise2dMap() {
            var mapCenter = new google.maps.LatLng(initLat, initLng);
            var myOptions = {
                zoom: 16,
                center: mapCenter,
                mapTypeId: google.maps.MapTypeId.ROADMAP
            };
            map = new google.maps.Map(document.getElementById("div_map2d"), myOptions);
            marker = new google.maps.Marker({ map: map, draggable: true,
                animation: google.maps.Animation.DROP, position: mapCenter
            });

            // options for the panorama
            panoramaOptions = {
                addressControl: true,
                addressControlOptions: {
                    style: { backgroundColor: 'grey', color: 'yellow' }
                },
                position: mapCenter,
                pov: {
                    heading: 140,
                    pitch: +10,
                    zoom: 1
                }
            };
            // show the panorama in the map_canvas2 div element with the options that were set
            panorama = new google.maps.StreetViewPanorama(document.getElementById("div_streetview"), panoramaOptions);
            // set street view for map
            map.setStreetView(panorama);
        }
        $(window).load(function () {
            initialise2dMap();
        });

        var plat = 0, plon = 0, pdir = 0; // previous latitude and longitude
        function MoveTo(lat, lon, dir, latc, lonc) {
            if (plat != lat || plon != lon) { // if new location is not same as previous location

                if (dir == 0) dir = GetDirection(plat, plon, lat, lon);

                if (ge != null) {
                    try {
                        var oldLa = ge.getView().copyAsLookAt(ge.ALTITUDE_RELATIVE_TO_GROUND); 	// get current 

                        var curHeading = oldLa.getHeading();
                        var desiredHeading = dir;

                        var curRange = oldLa.getRange();
                        var desiredRange = Math.max(20.0, 5 * 10); // this.currentSpeed

                        var la = this.ge.createLookAt('');
                        la.set(lat, lon,
                      0, // altitude
                      this.ge.ALTITUDE_RELATIVE_TO_GROUND,
                      desiredHeading,  //curHeading + this.getTurnToDirection_(curHeading, desiredHeading),
                      60, // tilt
                      curRange + (desiredRange - curRange) * 0.1 // range (inverse of zoom)
                      );

                        ge.getView().setAbstractView(la);

                        var position = new google.maps.LatLng(lat, lon);
                        model.getLocation().setLatLngAlt(lat, lon, 0);  // get 3d model location 
                        // and update it to new location
                        var orientation = ge.CreateOrientation('');
                        orientation.setHeading(dir - 180);   	// set model orientation to the same 
                        // heading direction (x degrees from north)
                        model.setOrientation(orientation);
                    }
                    catch (err) {
                    }
                }

                var positionc = new google.maps.LatLng(latc, lonc);    	// move the 2d map marker 
                // to the new location
                marker.setPosition(positionc);
                map.setCenter(positionc);    // set this new location as the center of the map

                panorama.setPosition(position);
                panorama.setPov({ heading: dir, pitch: +10, zoom: 1 });
            }
            plat = lat; plon = lon; pdir = dir; //make current location as previous location before leaving.
        }

        var trackId = null;
        function MoveToLatest() {
            if (trackId == null)
                return;

            var currentTime = new Date();
            // calling the rest web service
            $.getJSON('GpsDataService.svc/GetLocation/' + trackId
                 + '?time=' + currentTime.getTime(),
                function (data) {
                    if (data != null) {
                        MoveTo(data.Latitude, data.Longitude, data.Heading, data.LatitudeChina, data.LongitudeChina);
                    }
                });
        }

        var interval = 0;
        function startTracking() {
            var form1 = document.getElementById('form1');
            if (form1.txtTrackId.value == null || form1.txtTrackId.value == "")
                return;

            trackId = form1.txtTrackId.value;
            form1.btnStartTrack.disabled = true;
            form1.btnStopTrack.disabled = false;
            form1.btnIncreaseSpeed.disabled = false;
            form1.btnDecreaseSpeed.disabled = false;

            //MoveTo(-37.824661, 144.979607);
            //MoveToLatest();
            interval = setInterval("MoveToLatest()", speed); 	// call moveToLatest in every 
            // 300 ms
        }

        function stopTracking() {
            var form1 = document.getElementById('form1');
            clearInterval(interval);

            form1.btnStartTrack.disabled = false;
            form1.btnStopTrack.disabled = true;
            form1.btnIncreaseSpeed.disabled = true;
            form1.btnDecreaseSpeed.disabled = true;
        }

        function GetDirection(lat1, lon1, lat2, lon2) {
            var br = 0;
            br = Math.atan2(Math.sin(lon2 - lon1) *
	Math.cos(lat2), Math.cos(lat1) * Math.sin(lat2) -
    	Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)) % (2 * Math.PI);
            br = (br * 180) / Math.PI;
            return br;
        }
        function getTurnToDirection_(heading1, heading2) {
            if (Math.abs((heading1) - (heading2)) < 1)
                return heading2 - heading1;

            return (fixAngle(heading2 - heading1) < 0) ? -1 : 1;
        }
        function fixAngle(a) {
            while (a < -180)
                a += 360;

            while (a > 180)
                a -= 360;

            return a;
        }
        function toggle2d() {
            var form1 = document.getElementById('form1');
            if (form1.show2d.value == "Show 2-D Map") {
                $("#td2dmap").animate({ width: "50%" }, 500);
                $("#td3dmap").animate({ width: "50%" }, 500);
                form1.show2d.value = "Hide 2-D Map";
            }
            else {
                $("#td2dmap").animate({ width: "0%" }, 500);
                $("#td3dmap").animate({ width: "100%" }, 500);
                form1.show2d.value = "Show 2-D Map";
            }
        }
        function togglesv() {
            var form1 = document.getElementById('form1');
            if (form1.showsv.value == "Show StreetView") {
                document.getElementById("div_map3d").style.height = "0%";
                document.getElementById("div_streetview").style.height = "100%"; ;
                form1.showsv.value = "Hide StreetView";
            }
            else {
                document.getElementById("div_map3d").style.height = "100%";
                document.getElementById("div_streetview").style.height = "0%";
                form1.showsv.value = "Show StreetView";
            }
        }
        var speed = 4000;
        function changeSpeed(btnName) {
            if (btnName == "increase") {
                speed = speed / 2;
            }
            else {
                speed = speed * 2;
            }
            clearInterval(interval);
            interval = setInterval("MoveToLatest()", speed);
        }

        function onresize() {
            var clientHeight = document.documentElement.clientHeight;
            $('#holder').each(function () {
                $(this).css({
                    height: (clientHeight -
			$(this).position().top - 50).toString() + 'px'
                });
            });
        }

        $(window).resize(onresize);
        onresize();

        function enbalebuildings() {
            if (buildings.checked)
                ge.getLayerRoot().enableLayerById(ge.LAYER_BUILDINGS, true);
            else
                ge.getLayerRoot().enableLayerById(ge.LAYER_BUILDINGS, false);
        }
    </script>
</head>
<body>
    <form id="form1" runat="server">
    <table id="holder" style="height: 700px; width: 100%; border-collapse: collapse"
        cellspacing="0" cellpadding="1">
        <tr>
            <td colspan="2" style="height: 30px;">
                <div style="background-color: Gray; color: white">
                    <input type="checkbox" id="buildings" title="Toggle 3-D Buildings" checked="checked"
                        onclick="enbalebuildings()" />3-D Buildings |
                    <input type="button" id="show2d" title="Toggle 2-D Maps" onclick="toggle2d()" value="Hide 2-D Map" />
                    <input type="button" id="showsv" title="Toggle StreetView" onclick="togglesv()" value="Show StreetView" />
                    <input type="button" id="btnStartTrack" title="Start tracking GPS" onclick="startTracking()"
                        value="start" />
                    <input type="button" id="btnStopTrack" title="Stop tracking GPS" onclick="stopTracking()"
                        value="stop" disabled="true"/>
                    <input type="button" id="btnIncreaseSpeed" value="Increase Speed" onclick="changeSpeed('increase')" disabled="true"/>
                    <input type="button" id="btnDecreaseSpeed" value="Decrease Speed" onclick="changeSpeed('decrease')" disabled="true"/>
                    <input id="txtTrackId" type="text" value="" readonly="readonly" runat="server" />
                </div>
            </td>
        </tr>
        <tr>
            <td id="td3dmap" style="height: 100%; width: 50%">
                <div id="div_map3d" style="height: 100%; width: 100%;">
                </div>
                <div id="div_streetview" style="height: 0%; width: 100%;">
                </div>
            </td>
            <td id="td2dmap" style="height: 100%; width: 50%">
                <div id="div_map2d" style="height: 100%; width: 100%;">
                </div>
            </td>
        </tr>
    </table>
    </form>
</body>
</html>
