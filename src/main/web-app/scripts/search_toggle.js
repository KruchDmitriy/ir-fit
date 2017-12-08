$.fn.extend({
    animateCss: function (animationName) {
        var animationEnd = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
        this.addClass('animated ' + animationName).one(animationEnd, function () {
            $(this).removeClass('animated ' + animationName);
        });
    }
});

//<!--<div class="container">-->
//    <!--<div class="row" style="margin:10px;">-->
//        <!--<div class="panel panel-primary panel-body">-->
//            <!--<div class="pull-left">-->
//                <!--<span  >-->
//                    <!--<i class="glyphicon glyphicon-star" style="color:gold"></i>-->
//                <!--</span>-->
//                <!---->
//            <!--</div>-->
//        <!--</div>-->
//    <!--</div>-->
//<!--</div>-->

function getStartCount(countGold) {
    var countStars = 5;
    var stars = '<span>';
    for (i = 0; i < countGold; i++) {
        stars += '<i class="glyphicon glyphicon-star" style="color:gold">';
    }
    for (i = countGold; i < countStars; i++) {
        stars += '<i class="glyphicon glyphicon-star" style="color:white">';
    }
    stars += '</span>';
}

function processDocument(doc, id) {
    $(".search-output").show();
    $(".results-for").show();
    nothing = true;
    var containerTitle =  '<div class="container">';



    var docTitle = '<h3 id="doc' + id + '"><a href="' + doc.url + '" class="doc-title" target="_blank"></a></h3>';

    containerTitle += docTitle;
    containerTitle += getStartCount(doc.starsCount);

    var docLink = '<h4><a href="' + doc.url + '" target="_blank" class="doc-link">' + doc.url + '</a></h4>';
    var docDesc = '<p class="doc-desc">' + doc.text + '</p>';


    var searchResults = containerTitle + docLink + docDesc;
    $(".search-output").prepend(searchResults + '</div>'  /*+ '<hr>'*/);
    $(".nothing").hide();
    drawAdresses(doc.addresses);
//    setUrlTitle(doc.url, $("#doc"+id)[0]);
}

$('.search-query').each(function () {
    var elem = $(this);

    var searchQuery = elem.val();
    var flag = true;
    var out = true;
    var nothing = true;

    elem.bind("keyup", function (event) {
        if (event.keyCode !== 13) {
            return;
        }

        if (searchQuery !== elem.val()) {
            searchQuery = elem.val();

            $.ajax({
                type: "POST",
                url: "/search",
                contentType: "application/json;charset=UTF-8",
                data: JSON.stringify({
                    'query': searchQuery
                }),
                dataType: "json"
            }).done(function (documents) {
                console.log(documents);
                removeObjectsFromMap();

                if (documents.length >= 1) {
                    for (var i = 0; i < documents.length; i++) {
                        if (window.CP.shouldStopExecution(1)) {
                            break;
                        }
                        processDocument(documents[i], i);
                    }

                    window.CP.exitedLoop(1);
                    $(".results-for").html("Search results for " + "<strong>" + searchQuery + "</strong>  -  " +
                        "Showing <strong>" + documents.length + " </strong>results");
                } else {
                    $(".results-for").hide();

                    if (nothing == true) {
                        nothing = false;
                        $(".nothing").show();//.animateCss("fadeInRight").addClass(".delay-1");
                        flag = true;
                        out = false;
                        $(".search-output").hide();
                    }
                }
            });

            if (flag == true) {
                flag = false;
                out = true;
                $(".home").hide();
                $(".nothing").hide();
                $(".results-for").show();
                $(".search-output").show();//.animateCss("fadeInRight").addClass(".delay-1");
            }
        } else if (!searchQuery) {
            $(".results-for").hide();
            if (out === true) {
                searchQuery = elem.val();
                flag = true;
                out = false;

                $(".search-output").hide();
                $(".nothing").hide();
                $(".home").show().animateCss("fadeInRight").addClass(".delay-1");
            }
        }
    });
});