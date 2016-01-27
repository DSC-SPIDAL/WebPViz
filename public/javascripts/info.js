
function addRowToExp(key, val) {
    var tr =
        '<tr>'
        + ' <td>' + key + '</td>'
        + '<td>' + val + '</td>'
        + '<td align="center"><a href="javascript:void(0);" class="edit-file-link"><i class="glyphicon glyphicon-edit"></i></a></td>'
        + '<td align="center"><a href="" ><i class="glyphicon glyphicon-trash"></i></a></td>'
        + '</tr>';
    $('#expTable > tbody:last-child').append(tr);
}

function expToJson() {
    var expFull = {};
    var exp = {};
    var props = {};
    $('#expTable tr').each(function(){
        var key = "";
        var val = "";
        var count = 0;
        $(this).find('td').each(function(){
            //do your stuff, you can use $(this) to get current cell
            if (count == 0) {
                key = $(this).html()
            } else if (count == 1) {
                val = $(this).html();
            }
        });
        props[key] = val;
    });

    exp['props'] = props;
    expFull['tid'] = $('#artifactId').val();
    expFull['exp'] = exp;
    return expFull;
}

function submitExp(url, exp) {
    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        data: JSON.stringify(exp),
        url: url,
        success: function (data) {
            $('#addExpPropModal').modal('hide');
        },
        error: function (data) {
            $('#addExpPropModal').modal('hide');
        }
    });
}

function populateTable(data) {
    for (var key in data) {
        if (data.hasOwnProperty(key)) {
            addRowToExp(key, data[key]);
        }
    }
}

function loadExp(url) {
    $.getJSON(url, function (data) {
        populateTable(data);
    });
}