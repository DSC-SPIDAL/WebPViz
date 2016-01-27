var submitUrl;

function addRowToExp(key, val) {
    var tr =
        '<tr>'
        + ' <td>' + key + '</td>'
        + '<td>' + val + '</td>'
        //+ '<td align="center"><a href="javascript:void(0);" class="edit-file-link"><i class="glyphicon glyphicon-edit"></i></a></td>'
        + '<td align="center"><a class="delete-link" ><i class="glyphicon glyphicon-trash"></i></a></td>'
        + '</tr>';
    $('#expTable > tbody:last-child').append(tr);
}

function addDescToExp(val) {
    $("#exp_desc").text(val);
}

function expToJson() {
    var expFull = {};
    var exp = {};
    var props = {};
    var table = document.getElementById("expTable");
    for (var i = 0, row; row = table.rows[i]; i++) {
        //iterate through rows
        //rows would be accessed using the "row" variable assigned in the for loop
        props[row.cells[0].innerHTML] =  row.cells[1].innerHTML;
    }
    exp['desc'] = $("#exp_desc").text();
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
            $('#editExpDescModal').modal('hide');
        },
        error: function (data) {
            $('#addExpPropModal').modal('hide');
            $('#editExpDescModal').modal('hide');
        }
    });
}

function populateTable(data) {
    var props = data.props;
    if (props) {
        for (var key in props) {
            if (props.hasOwnProperty(key)) {
                addRowToExp(key, props[key]);
            }
        }
    }
    var desc = data.desc;
    if (desc) {
        addDescToExp(desc);
    }
    registerExpDelete();
}

function loadExp(url, updateUrl) {
    submitUrl = updateUrl;
    $.getJSON(url, function (data) {
        populateTable(data);
    });
}


function registerExpDelete() {
    $('.delete-link').click(function() {
        var tr = $(this).closest('tr');
        tr.css("background-color","#FF3700");
        tr.fadeOut(400, function(){
            tr.remove();
            var exp = expToJson();
            submitExp(submitUrl, exp);
        });
        return false;
    });
}