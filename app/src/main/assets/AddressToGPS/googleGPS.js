function initialize() {
    var address = document.getElementById('address_input');
    autocomplete = new google.maps.places.Autocomplete(address);

    google.maps.event.addListener(autocomplete, 'place_changed', autocompleteCallback);
}

function autocompleteCallback() {
    var place = autocomplete.getPlace();

    if (place.geometry) {
        var locationFromPlace = place.geometry.location;
        var address = document.getElementById('address_input');
        new google.maps.Map(document.getElementById('map_canvas')).setCenter(locationFromPlace);

        window.JSInterface.coordinates(address.value, locationFromPlace.toUrlValue(),
                                        place.formatted_phone_number);
        google.maps.event.clearListeners(autocomplete, 'place_changed');
    }
}

function setFocus(lookup) {
    var address = document.getElementById('address_input');

    address.value = lookup.toString();
    address.focus();
}

try {
    google.maps.event.addDomListener(window, 'load', initialize);
} catch (err) {
    window.JSInterface.completeReset()
}