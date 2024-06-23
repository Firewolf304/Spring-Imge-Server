let isLoggedIn = false;
let currentPage = 0;

$(document).ready(() => {
    createPagination();
    if(document.cookie !== "") {
        checkSession().then(
            v => updateUI()
        );
    }
    $('#login-button').click(function() {
        updateUI();
    });
    $('#uploadForm').submit((event) => {
        event.preventDefault();
        const formData = new FormData(document.getElementById("uploadForm"));
        try {
            const response = fetch('/apis/upload', {
                method: 'POST',
                body: formData
            }).then(response => {
                if (response.ok) {
                    alert('Success');
                    updateUI();
                } else {
                    alert('Something wrong: ' + response.statusText);
                }
            });
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка при отправке изображения.');
        }
    })

});
var checkSession = () => {
    var id = $.cookie("id");
    return fetch(`session?id=${id}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            }
        }).then(response =>
    {
        if(response.ok) {
            return response.text();
        } else {
            //document.getElementById("status").innerText = "error";
        }
    }).then( data => {
        if(data !== undefined ) {
            isLoggedIn = true;
        } else {
            isLoggedIn = false;
        }
        console.log("Response: ", data);
        return data;
    }).catch(error => {
        console.error('error:', error);
    });
}
var updateUI = () => {
    if (isLoggedIn) {
        $('#login-button').text('Logout');
        $('.sidebar').show();
        $('#username').text($.cookie('user'))

        $('#login-button').click(() => {
            $.ajax({
                    url: "/logout",
                    method: 'POST',
                    success: function(data) {
                        console.log("Exited");
                    },
                    error: function(error) {
                        console.error('Error fetching images:', error);
                    }
                });
            window.location.reload();
            });
        loadImages();

        /**/
    } else {
        window.location.href = "/login/index.html";
    }
}
var loadImages = () => {
    $.ajax({
        url: `/images?offset=${currentPage}&count=${$('#count-input').val()}`,
        method: 'GET',
        success: function(data) {
            if($('#self-images').is(':checked')) {
                displayYourImages();
            } else {
                displayImages(JSON.parse(data) );
            }
        },
        error: function(error) {
            console.error('Error fetching images:', error);
        }
    });

    //totalPages = Math.ceil(docum / imagesPerPage);
}
var displayImages = (images) => {
    $('#image-gallery').empty();
    /*for(var image in images["images"]) {
        console.log(`loading ${images["images"][image]}`)
        requestImage(image, images["images"][image]);
        //const imgElement = $('<img>').attr('src', image.url).attr('alt', image.title);
        //$('#image-gallery').append(imgElement);
    }*/
    const imagePromises = images["images"].map(imageName => requestImage(imageName, images["delete_access"]));
    Promise.all(imagePromises).then(imageElements => {
        imageElements.forEach(element => {
            if (element) {
                $('#image-gallery').append(element);
            }
        });
    });
}
var displayYourImages = () => {
    $.ajax({
        url: `/privateimages?user_id=${$.cookie("user_id")}&offset=${currentPage}&count=${$('#count-input').val()}`,
        method: 'GET',
        success: function(data) {
            var images = JSON.parse(data);
            $('#image-gallery').empty();
            /*for(var image in images["images"]) {
                console.log(`loading ${images["images"][image]}`)
                requestImage(image, images["images"][image]);
                //const imgElement = $('<img>').attr('src', image.url).attr('alt', image.title);
                //$('#image-gallery').append(imgElement);
            }*/
            const imagePromises = images["images"].map(imageName => requestImage(imageName, images["delete_access"]));
            Promise.all(imagePromises).then(imageElements => {
                imageElements.forEach(element => {
                    if (element) {
                        $('#image-gallery').append(element);
                    }
                });
            });
        },
        error: function(error) {
            console.error('Error fetching images:', error);
        }
    });

}
var requestImage = (image, access) => {
    return fetch(`/getfile/${image}`, {method: 'GET'})
        .then( response => {
            if (!response.ok) {
                throw new Error('Something wrong');
            }
            return response.blob(); // hehe blob
        }).then(blob => {
            const imgUrl = URL.createObjectURL(blob);
            const imgElement = $('<img>').attr('src', imgUrl).attr('alt', image);
            var name = image.split("--");
            const labelUsername = $('<label>').attr('class', 'label-username-element').text("User: " + name[2]);
            const labelDate = $('<label>').attr('class', 'label-date-element').text("Date: " + name[0]);
            const labelId = $('<label>').attr('class', 'label-id-element').text("User_id: " + name[1]);
            const labelName = $('<label>').attr('class', 'label-name-element').text("Name: " + name[3]);
            const buttonDelete = $('<button>').attr('class', 'delete-button').text("Delete");
            buttonDelete.click((event) => {
                    var p = event.target.parentElement;
                    var img = p.querySelector('img');
                    $.ajax({
                        url: `/apis/remove?name=${img.alt}`,
                        method: 'GET',
                        success: (data) => {
                            updateUI();
                        },
                        error: function(error) {
                            console.error('Error fetching images:', error);
                        }
                    });
                });
            const divElement = $('<div>').addClass('image-item');

            divElement.append(imgElement).append($('<br>')).append(labelName).append($('<br>')).append(labelUsername).append($('<br>')).append(labelDate);
            if(access.includes(image)) {
                divElement.append($('<br>')).append(buttonDelete);
            }
            return divElement;
        })
        .catch(error => console.error('Error fetching images:', error));
}
var createPagination = () => {
    $('#pagination').empty();
    for (let i = 0; i <= 10; i++) {
        const pageItem = $('<div>').addClass('page-item');
        const pageButton = $('<button>').text(i+1).click(() => {
            currentPage = i;
            updateUI();
            createPagination();
        });

        if (i === currentPage) {
            pageButton.addClass('active');
        }

        pageItem.append(pageButton);
        $('#pagination').append(pageItem);
    }
}