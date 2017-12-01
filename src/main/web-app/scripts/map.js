ymaps.ready(init);

var map;

function init() {

    map = new ymaps.Map("map", {
                    center: [0, 0],
                    zoom: 9

                });
//    setAddress(['Нижний Новгород ул Голубева д3',
//                'Нижний Новгород ул Голубева д3/2',
//                'Нижний Новгород ул Голубева д7'])

}

function setAddress(arrAddress) {
    arrAddress.forEach(addr => drawLocation(addr));
}

function drawLocation(address) {
    ymaps.geocode(address, {
        results: 1 }
    ).then(function (res) {

        var firstGeoObject = res.geoObjects.get(0);
                coords = firstGeoObject.geometry.getCoordinates(),
                bounds = firstGeoObject.properties.get('boundedBy');

            firstGeoObject.options.set('preset', 'islands#darkBlueDotIconWithCaption');
//            firstGeoObject.properties.set('iconCaption', firstGeoObject.getAddressLine());

            map.geoObjects.add(firstGeoObject);
            map.setBounds(bounds, {
                checkZoomRange: true
            });

//             var myPlacemark = new ymaps.Placemark(coords, {
//                 iconContent: 'моя метка',
//                 balloonContent: 'Содержимое балуна <strong>моей метки</strong>'
//                 }, {
//                 preset: 'islands#violetStretchyIcon'
//             });
//
//             map.geoObjects.add(myPlacemark);


    }, function(err) {
        console.error(err.message);
    });
}