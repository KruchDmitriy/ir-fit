ymaps.ready(init);

var map, objects, polygons = [], index = {}, taskId;
var marker = undefined;

const defaultColor = '#ffff00';
const goodColor = '#0000ff';
const badColor = '#ff0000';
const selectedColor = '#ffffff';
const selectedGoodColor = '#bbbbff';
const selectedBadColor = '#ffbbbb';

function init() {
    ymaps.geocode('Нижний Новгород ул.Голубева д3/1', { results: 1 })
    .then(function (res) {
        var firstGeoObject = res.geoObjects.get(0);
        map = new ymaps.Map("map", {
            center: firstGeoObject.geometry.getCoordinates(),
            type: "yandex#hybrid",
            zoom: 18,
            controls: ['zoomControl']
        });

        map.container.fitToViewport();
//        map.events.add('boundschange', function (e) {
//            if (e.get('newZoom') !== e.get('oldZoom')) {
//                if (marker != undefined) {
//                    if (e.get('newZoom') > 14) {
//                        map.geoObjects.remove(marker)
//                    }
//                    else {
//                        map.geoObjects.add(marker)
//                    }
//                }
//            }
//        });


        // Задаем изображение для иконок меток.
        res.geoObjects.options.set('preset', 'islands#redCircleIcon');
        res.geoObjects.events
            // При наведении на метку показываем хинт с названием станции метро.
            .add('mouseenter', function (event) {
                var geoObject = event.get('target');
                myMap.hint.open(geoObject.geometry.getCoordinates(), geoObject.getPremise());
            })
            // Скрываем хинт при выходе курсора за пределы метки.
            .add('mouseleave', function (event) {
                myMap.hint.close(true);
            });
        // Добавляем коллекцию найденных геообъектов на карту.
        myMap.geoObjects.add(res.geoObjects);
        // Масштабируем карту на область видимости коллекции.
        myMap.setBounds(res.geoObjects.getBounds());
    }, function(err) {
        console.error(err.message);
    });
}