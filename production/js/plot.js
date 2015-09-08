function generateCheckList(list){

    var tabletop = "<table class='table table-striped' id='cluster_table' style='background-color: #EAEAEA; padding-top: 0px'>"
                       + "<thead>"
                           + "<tr class='headings'>"
                           +     "<th>"
                           +     "<input type='checkbox' id='check-all' disabled='true' class='flat'> Cluster"
                           +     "</th>"
                           +     "<th class='column-title'>Label</th>"
                           +     "<th class='column-title'>Size</th>"
                       +     "</tr>"
                       + "</thead>"
                       +"<tbody>"

    var tablerows = "";

    for(i =0; i< list.length; i++){
        tablerows +="<tr class='even pointer'>"
                    + "<td class='a-center'>"
                        +"<input type='checkbox' class='flat' name='table_records' value='" + i + "'><label class='color-box-label'>" + i + "</label> <div class='color-box' style='background-color: #FF850A;'></div>"
                    +"</td>"
                    +"<td class=' '>C2</td>"
                    +"<td class=' '>121</td>"
                   +"</tr>"
    }

    var tableend =  "</tbody>"
               + "</table>"


    return tabletop+tablerows+tableend;
}
/*
<thead>
<tr class="headings">
    <th>
    <input type="checkbox" id="check-all" disabled="true" class="flat"> Cluster
    </th>
    <th class="column-title">Label</th>
    <th class="column-title">Size</th>
    </th>
    </tr>
    </thead>

    */