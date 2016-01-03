/* varibles*/
//TODO need to add reply option to make the slider go back to the start and reload data from the start
//TODO try to remove all other collections possible and work with sections object

//Three js global varibles
var camera, scene, renderer, sprite, colors = [], particles = [], controls, light, currentParticles = [];
var container, stats;
var heus = [0.05, 0.3, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 0.05, 0.3, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95];
var scene3d;
var colors = [];
var colorlist = {};
var trueColorList = {};
var colorsLoaded = false;
var colorPickersLoaded = false;
var sections = [];
var sprites = {};
var xmeantotal = 0,ymeantotal = 0,zmeantotal = 0;
var xmean = 0,ymean = 0,zmean = 0, cameraCenter, calculatedmeans = false;
var colorSchemes = {};

//Single Plot Varibles
var clusterUrl;
var clusters;
var resultSetId;
var timeseriesId;
var resultData;
var fileName;
var uploader;

//Time Series Vars
var particleSets = {};
var sectionSets = {};
var fileNames = {};
var resultSets;
var removedclusters = [];
var recoloredclusters = [];
var timeSeriesLength;

var changedGlyphs = {};
var customclusters = {}
var pointLabelxKey = {}
var pointLabelxKeySets = {}
var maxClusterId = 0;
var bufferLoopStarted = false;
var plotDesc;

var plotPointsSets = {}
var plotPoints = {}

//Constants
var TIME_BETWEEN_PLOTS_IN_MILLS = 300;
var MAX_PLOTS_STORED = 20;
var LOAD_SIZE = 5;

var plotRangeSlider = {};

var speed = 300;
var glyphSize = 1.0;
var pointSize = 1.0;

var playEnum = {
    INIT: "init",
    PLAY: "play",
    PAUSE: "pause"
};

var playStatus = playEnum.INIT;

$(function () {
    $("#plot-slider").ionRangeSlider({
        grid: true,
        min: 0,
        max: 100,
        from: 0,
        hide_min_max: true,
        hide_from_to: true,
        onChange: function (data) {
            updatePlot(data.from);
        }
    });
    plotRangeSlider = $("#plot-slider").data("ionRangeSlider");
});

var totalItemsToLoad = 1;
var itemsLoaded = 1;

function progress() {
    var bar = 250;
    bar = Math.floor( bar * itemsLoaded / totalItemsToLoad);
    $("#bar").css({width : bar + "px"});
}

function generateClusterList(list, initcolors) {
    var keys = [];
    for (var k in trueColorList) {
        if (trueColorList.hasOwnProperty(k)) {
            keys.push(k);
        }
    }

    var emptyList = [];
    var nonEmptyList = [];
    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var aCol = trueColorList[key];
        if ($.isEmptyObject(aCol)) {
            emptyList.push(key);
        } else {
            nonEmptyList.push(key);
        }
    }
    var grid = "";

    if (list && nonEmptyList.length < 50) {
        for (var i = 0; i < nonEmptyList.length; i++) {
            var key = nonEmptyList[i];
            if (list.hasOwnProperty(key)) {
                var colorWithouthHash = initcolors[key].replace(/#/g, '')
                var sprite = getGlyphName(list[key]);
                if (sprite != null ) {
                    grid += "<div class='element-item transition metal' data-category='transition' style='background-color: #" + colorWithouthHash + " '>" +
                        "<p style='font-size: 0.8em'><span style='font-weight: bold'>" + list[key].label + "(" + sprite + ")" + "</span>:" + list[key].length + "</p></div>"
                } else {

                    grid += "<div class='element-item transition metal' data-category='transition' style='background-color: #" + colorWithouthHash + " '>" +
                        "<p style='font-size: 0.8em'><span style='font-weight: bold'>" + list[key].label + "</span>:" + list[key].length + "</p></div>"
                }
            }
        }
    }

    return grid;
}

function getGlyphName(key){
    var glyph = null;
    if (key.size > 1) {
        switch (parseInt(key.shape)) {
            case 0:
                glyph = "Disc";
                break;
            case 1:
                glyph = "Ball";
                break;
            case 2:
                glyph = "Star";
                break;
            case 3:
                glyph = "Cube";
                break;
            case 4:
                glyph = "Pyramid";
                break;
            case 5:
                glyph = "Cone";
                break;
            case 6:
                glyph = "Cylinder";
                break;
            default :
                glyph = "Cube";
        }
    }
    return glyph;
}

//function colorEnable(check) {
//    if (check) {
//        $('.color-pic1').colorpicker();
//    } else {
//        $('.color-pic1').colorpicker('destroy');
//    }
//}

//Generate the check box list for clusters
function generateCheckList(list, initcolors) {
    var keys = [];
    for (var k in trueColorList) {
        if (trueColorList.hasOwnProperty(k)) {
            keys.push(k);
        }
    }

    var emptyList = [];
    var nonEmptyList = [];
    var glyphList = [];
    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var sprite = getGlyphName(list[key])
        if(sprite != null){
            glyphList.push(key);
        }else{
            var aCol = trueColorList[key];
            if ($.isEmptyObject(aCol)) {
                emptyList.push(key);
            } else {
                nonEmptyList.push(key);
            }
        }

    }

    keys = glyphList.concat(nonEmptyList);
    keys = keys.concat(emptyList);

    var tabletop = "<table class='table table-striped table-bordered responsive-utilities jambo_table bulk_action' id='cluster_table'>"
        + "<thead>"
        + "<tr class='headings'>"
        + "<th>"
        + "<input type='checkbox' id='check-all' class='flat' checked> Cluster"
        + "</th>"
        + "<th class='column-title' >Label</th>"
        + "<th class='column-title'>Size</th>"
        + "</tr>"
        + "</thead>"
        + "<tbody>";

    var tablerows = "";

    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        //if (list.hasOwnProperty(key)) {
            tablerows += "<tr class='even pointer' id='" + key + "'>"
                + "<td class='a-center'>";
            if (!(removedclusters.hasOwnProperty(key))){
                tablerows += "<input type='checkbox' class='flat' name='table_records' checked value='" + key + "'>";
            }else{
                tablerows += "<input type='checkbox' class='flat' name='table_records' value='" + key + "'>";
            }
            var sprite = getGlyphName(list[key]);
            tablerows += "<label class='color-box-label'>" + key + "</label> "
                + "<div class='input-group' style='width: 15px;height: 15px; display: inline-flex; padding-left: 20px;padding-top: 2px'>"
                + "<input value='" + initcolors[key] + "' class='form-control color-pic1' type='hidden' key='" + key + "' id='color-box" + key + "'>"
                + "<span id='color-picker-addon' value='"+ key + "' class='color-picker-addon' style='background-color:#" + initcolors[key] +"'></span>"
                + "</div>"
                + "</td>";
            if(sprite != null){
                tablerows += "<td class=' '><span>" + list[key].label + "</span>"
                    + "<select name='glyphs' class='select-glyph' id='" + key + "'>"
                    + "<option value='0'" + checkIfSelected("0", list[key].shape, key) + ">Disc</option>"
                    + "<option value='1'" + checkIfSelected("1", list[key].shape, key) + ">Ball</option>"
                    + "<option value='2'" + checkIfSelected("2", list[key].shape, key) + ">Star</option>"
                    + "<option value='3'" + checkIfSelected("3", list[key].shape, key) + ">Cube</option>"
                    + "<option value='4'" + checkIfSelected("4", list[key].shape, key) + ">Pyramid</option>"
                    + "<option value='5'" + checkIfSelected("5", list[key].shape, key) + ">Cone</option>"
                    + "<option value='6'" + checkIfSelected("6", list[key].shape, key) + ">Cylinder</option>"
                    + "</select>"
                    "</td>";
            }else{
                tablerows += "<td class=' '><span>" + list[key].label +"</span></td>";
            }
                tablerows += "<td class='l1'>" + list[key].length + "</td>"
                    + "</tr>";
        //}
    }

    var tableend = "</tbody>"
        + "</table>";


    return tabletop + tablerows + tableend;
}

function checkIfSelected(key, shape, clusterkey){
    if(changedGlyphs.hasOwnProperty(clusterkey)){
        shape = changedGlyphs[clusterkey]
    }
    if (key == shape) {
        return "selected"
    }else{
        return ""
    }
}
function updateClusterList(list, initcolors) {
    for (var key in list) {
        if (list.hasOwnProperty(key)) {
            //var x=$("#" + key);
            //x[2].html(list[key].length);
            $("#cluster_table #" + key + " td.l1").html(list[key].length);
        }
    }
}

//Plot functions
function visualize(resultSetUrl, artifact, fid, tid) {
    clusterUrl = resultSetUrl;
    //clusters = resultSet.clusters;
    resultSetId = fid;
    timeseriesId = tid;
    setupThreeJs();
    intialSetup(artifact);
    generateGraph();
    setupGuiSingle();
    animate();
}

var publicUrl = false;

function visualizeTimeSeries(resultSetUrl, artifact, id, pub) {
    clusterUrl = resultSetUrl;
    publicUrl = pub;
    timeseriesId = id;
    resultSets = artifact.files;
    timeSeriesLength = resultSets.length;
    setupThreeJs();
    intialSetup(artifact);
    initPlotData();
    generateTimeSeries(resultSets);
    setupGuiTimeSeries();
}

function intialSetup(artifact) {
    $("#progress").css({ display: "block" });
    // check weather we have camera
    if (artifact.settings) {
        if (artifact.settings.glyphSize) {
            glyphSize = artifact.settings.glyphSize;
        }
        if (artifact.settings.pointSize) {
            pointSize = artifact.settings.pointSize;
        }
        if (artifact.settings.speed) {
            speed = artifact.settings.speed;
        }
        if (artifact.settings.camera) {
            var c = artifact.settings.camera;
            //camera.aspect = c.aspect;
            //camera.far = c.far;
            //camera.near = c.near;
            //camera.zoom = c.zoom;
            //camera.updateProjectionMatrix ();
        }
        if (artifact.settings.cameraup) {
            var up = artifact.settings.cameraup;
            camera.up.set(up.x, up.y, up.z);
        }
        if(artifact.settings.glyphs){
            changedGlyphs =  artifact.settings.glyphs;
        }
        if(artifact.settings.customclusters){
            customclusters = artifact.settings.customclusters;
        }
        if(artifact.settings.camerastate){
            var cameraState = artifact.settings.camerastate;
            camera.matrix.fromArray(JSON.parse(cameraState));
            camera.matrix.decompose(camera.position, camera.quaternion, camera.scale);
        }
        camera.updateProjectionMatrix();
        var colors = artifact.settings.clusterColors;
        if (colors) {
            var count = 0;
            for (var key in colors) {
                if (colors.hasOwnProperty(key)) {
                    var clustercolor = colors[key];
                    colorlist[key] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();
                    trueColorList[key] = clustercolor;
                    count += 1;
                }
            }
            colorsLoaded = true;
        }
    }

    controlers.pointsize = pointSize;
    controlers.glyphsize = glyphSize;
    controlers.delay = speed;
}

function generateGraph() {
    var cluster;
    var geometry = {};
    var hsl;

    $.getJSON(clusterUrl, function (data) {
        fileName = data.file;
        plotDesc =  data.desc;
        uploader = data.uploader;
        maplabelstokeys(data.points)
        //temp only till data change
        var points = {};
        var pointcolors = {};
        $("#plot-title").text(data.file);
        clusters = data.clusters;
        var clusterCount = 0;
        for (var cid in clusters) {
            if (data.clusters.hasOwnProperty(cid)) {
                clusterCount++;
                var clusterdata = data.clusters[cid];
                var clusterid = parseInt(cid);
                setMaxClusterId(clusterid);
                var clustercolor;
                if (!colorsLoaded) {
                    clustercolor = {"r": 255, "g": 255, "b": 255};
                    if (clusterdata.r) {
                        clustercolor["r"] = clusterdata.r[3];
                        clustercolor["g"] = clusterdata.r[2];
                        clustercolor["b"] = clusterdata.r[1];
                        trueColorList[clusterid] = clustercolor;
                    } else {
                        trueColorList[clusterid] = {};
                    }
                } else {
                    clustercolor = trueColorList[clusterid];
                }

                if (!sections.hasOwnProperty(clusterid))
                    sections[clusterid] = {
                        "length": clusterdata.p.length,
                        "size": clusterdata.s,
                        "shape": clusterdata.f,
                        "visible": clusterdata.v,
                        "color": clustercolor,
                        "label": clusterdata.l
                    };

                if (!geometry.hasOwnProperty(clusterid)) {
                    geometry[clusterid] = new THREE.BufferGeometry();
                    currentParticles[clusterid] = [];
                }
                if (!colorlist.hasOwnProperty(clusterid))
                    colorlist[clusterid] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();

                var positions = new Float32Array(clusterdata.p.length * 3);
                var colorarray = new Float32Array(clusterdata.p.length * 3);
                var sizes = new Float32Array(clusterdata.p.length);
                xmean = 0;
                ymean = 0;
                zmean = 0;
                for (var k = 0; k < clusterdata.p.length; k++) {
                    var p = findPoint(data, clusterdata.p[k]);
                    if (!p) {
                        continue;
                    }
                    var p0 = parseFloat(p[0]);
                    var p1 = parseFloat(p[1]);
                    var p2 = parseFloat(p[2]);
                    positions[k * 3 + 0] = p0;
                    positions[k * 3 + 1] = p1;
                    positions[k * 3 + 2] = p2;

                    xmean += p0;
                    ymean += p1;
                    zmean += p2;

                    var tempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")");
                    colorarray[k * 3 + 0] = tempcolor.r;
                    colorarray[k * 3 + 1] = tempcolor.g;
                    colorarray[k * 3 + 2] = tempcolor.b;


                    points[clusterdata.p[k]] = [p0, p1, p2];
                    pointcolors[clusterdata.p[k]] = tempcolor;
                }

                xmean = xmean / clusterdata.p.length;
                ymean = ymean / clusterdata.p.length;
                zmean = zmean / clusterdata.p.length;
                geometry[clusterid].addAttribute('position', new THREE.BufferAttribute(positions, 3));
                geometry[clusterid].addAttribute('color', new THREE.BufferAttribute(colorarray, 3));

                xmeantotal += xmean;
                ymeantotal += ymean;
                zmeantotal += zmean;
            }
        }

        xmeantotal = xmeantotal/clusterCount;
        ymeantotal = ymeantotal/clusterCount;
        zmeantotal = zmeantotal/clusterCount;

        scene3d = new THREE.Scene();

        scene3d.add(camera);

        for (var key in geometry) {
            if (geometry.hasOwnProperty(key)) {
                geometry[key].translate(-xmeantotal,-ymeantotal,-zmeantotal);
                currentParticles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size,sections[key].shape, false));
            }
        }

        renderCustomCluster();
        addParticlesToScence();

        drawEdges(data.edges,points,pointcolors);
        document.getElementById('cluster_table_div').innerHTML = generateCheckList(sections, colorlist);
        document.getElementById('plot-clusters').innerHTML = generateClusterList(sections, colorlist);
        populatePlotInfo();
        //$("#cluster_table_div").html(generateCheckList(sections, colorlist));
        //$("#plot-clusters").html(generateClusterList(sections, colorlist));
        var cls = $("#plot-clusters").isotope({
            itemSelector: '.element-item',
            layoutMode: 'fitRows',
            containerStyle: null
        });

        //clusters.on( 'arrangeComplete', function(){
        //    var height = window.innerHeight - 57 - 40 - 40 - 10 - $("#plot-clusters").height();
        //    $('#canvas3d').width(window.innerWidth - 45);
        //    $('#canvas3d').height(height);
        //    var canvasWidth = $('#canvas3d').width();
        //    var canvasHeight = $('#canvas3d').height();
        //    camera.aspect = width / height;
        //    camera.updateProjectionMatrix();
        //    renderer.setSize(canvasWidth, canvasHeight);
        //});
        // stats.domElement.style.position = 'absolute';
        // document.getElementById("stats").appendChild(stats.domElement);
        window.addEventListener('resize', onWindowResize, true);
        //if (clusters && clusterCount < 100) {
        //    $('.color-pic1').colorpicker();
        //    $('.color_enable').prop('checked', true);
        //}
        changeGlyphSize();
        changePointSize();
        animate();
        itemsLoaded = totalItemsToLoad;
        $( "#progress" ).css({display : "none"});
    });


    animate();
}

function addParticlesToScence(){
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            scene3d.remove(currentParticles[key]);
            scene3d.add(currentParticles[key]);
        }
    }
}
function maplabelstokeys(points){
    pointLabelxKey = {};
    plotPoints = {}
    for (var key in points) {
        if (points.hasOwnProperty(key)) {
            var p = points[key.toString()]
            pointLabelxKey[p[3]] = key;
            plotPoints[key] =  [p[0], p[1], p[2]];
        }
    }
}

function findPoint(data, key) {
    return data.points[key.toString()];
}

function drawEdges(edges,points,pointcolors){

   if(edges == null || edges == undefined)
        return;

    var geometry = new THREE.BufferGeometry();
    var material = new THREE.LineBasicMaterial({ vertexColors: THREE.VertexColors });
        var positions = [];
    var colorarray = [];
    for (var key in edges) {
        if (edges.hasOwnProperty(key)) {
            var edge = edges[key];
            var vertices = edge.v;
            var previousvertex = null;
            var previouscolor = null;
            var isFirst = true;
            for (var i = 0; i < vertices.length; i++) {
                var verkey = vertices[i];
                //if (vertices.hasOwnProperty(verkey)) {
                    var pointkey = verkey;
                    var point = points[parseInt(pointkey)];
                    var pointcolor = pointcolors[parseInt(pointkey)];
                    var vertex = new THREE.Vector3(point[0],point[1],point[2]);
                    if(previousvertex != null){
                        if(isFirst){
                            isFirst = false
                        }else{
                            colorarray.push(previouscolor.r);
                            colorarray.push(previouscolor.g);
                            colorarray.push(previouscolor.b);

                            positions.push(previousvertex.x);
                            positions.push(previousvertex.y);
                            positions.push(previousvertex.z);
                        }
                    }
                    positions.push(vertex.x);
                    positions.push(vertex.y);
                    positions.push(vertex.z);
                    previousvertex = vertex;
                    
                    previouscolor = pointcolor;
                    colorarray.push(pointcolor.r);
                    colorarray.push(pointcolor.g);
                    colorarray.push(pointcolor.b);
                //}
            }
        }
    }


    var positions32 = new Float32Array(positions.length);
    var colorarray32 = new Float32Array(colorarray.length);
    positions32.set(positions);
    colorarray32.set(colorarray);
    geometry.addAttribute('position', new THREE.BufferAttribute(positions32, 3));
    geometry.addAttribute('color', new THREE.BufferAttribute(colorarray32, 3));
    geometry.translate(-xmeantotal,-ymeantotal,-zmeantotal);
    var linesegs = line = new THREE.LineSegments( geometry, material );
    scene3d.add(linesegs)
}

function generateTimeSeries(resultSets) {
    initBufferAndLoad();
    playStatus = playEnum.PAUSE;
    playLoop();
}

//TODO WInodow rezise does not work yet need to fix
function onWindowResize() {
    var width = window.innerWidth;
    var height = window.innerHeight - 57 - 40 - 40 - 10 ;
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width - 45, height);
    //controls.handleResize();
    render();
}

function randomRBG() {
    return (Math.floor(Math.random() * (255 - 0 + 1)) + 0);
}

function loadPlotData(start, end) {
    for (var i = start; i < end; i++) {
        // check weather we already have a value
        if (particleSets[i] || bufferRequestMade[i]) {
            continue;
        }
        if (!publicUrl) {
            clusterUrl = "/resultssetall/" + resultSets[i].tId + "/file/" + resultSets[i].id;
        } else {
            clusterUrl = "/public/resultssetall/" + resultSets[i].tId + "/file/" + resultSets[i].id;
        }
        bufferRequestMade[i] = true;
        (function(i){
        $.getJSON(clusterUrl, function (data) {
            particles = {};
            colors = {};
            plotPoints = {}
            pointLabelxKey = {}
            var hsl;
            var geometry = {};
            clusters = data.clusters;
            fileName = data.file;
            plotDesc =  data.desc;
            uploader = data.uploader;

            var localSections = [];
            var clusterCount = 0;
            for (var cid in clusters) {
                if (data.clusters.hasOwnProperty(cid)) {
                    clusterCount++;
                    var clusterdata = data.clusters[cid];
                    var clusterid =  parseInt(cid)
                    setMaxClusterId(clusterid);
                    var clustercolor;
                    if (!colorsLoaded) {
                        clustercolor = {"r": 0, "g": 0, "b": 0};
                        if (clusterdata.r) {
                            clustercolor["r"] = clusterdata.r[3];
                            clustercolor["g"] = clusterdata.r[2];
                            clustercolor["b"] = clusterdata.r[1];
                            trueColorList[clusterid] = clustercolor;
                        } else {
                            trueColorList[clusterid] = {};
                        }
                    } else {
                        clustercolor = trueColorList[clusterid];
                    }
                    if (clustercolor == null)
                        clustercolor = {"a": randomRBG(), "b": randomRBG(), "g": randomRBG(), "r": randomRBG()};

                    hsl = [heus[clusterid], 1, 0.8];
                    if (!geometry.hasOwnProperty(clusterid)) {
                        geometry[clusterid] = new THREE.BufferGeometry();
                        particles[clusterid] = [];
                    }

                    if (!colorlist.hasOwnProperty(clusterid))
                        colorlist[clusterid] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();

                    var localSection = {
                        "length": clusterdata.p.length,
                        "size": clusterdata.s,
                        "shape": clusterdata.f,
                        "visible": clusterdata.v,
                        "color": clustercolor,
                        "label": clusterdata.l
                    };
                    if (!sections.hasOwnProperty(clusterid)) {
                        sections[clusterid] = localSection;
                    }
                    localSections[clusterid] = localSection;

                    var positions = new Float32Array(clusterdata.p.length * 3);
                    var colorarray = new Float32Array(clusterdata.p.length * 3);
                    var sizes = new Float32Array(clusterdata.p.length);
                    xmean = 0;
                    ymean = 0;
                    zmean = 0;
                    for (var k = 0; k < clusterdata.p.length; k++) {
                        var p = findPoint(data, clusterdata.p[k]);
                        if (!p) {
                            continue;
                        }
                        var p0 = parseFloat(p[0]);
                        var p1 = parseFloat(p[1]);
                        var p2 = parseFloat(p[2]);
                        plotPoints[clusterdata.p[k]] =  [p[0], p[1], p[2]];
                        pointLabelxKey[p[3]] = clusterdata.p[k];
                        positions[k * 3 + 0] = p0;
                        positions[k * 3 + 1] = p1;
                        positions[k * 3 + 2] = p2;

                        if (!calculatedmeans) {
                            xmean += p0;
                            ymean += p1;
                            zmean += p2;
                        }

                        var tempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")");
                        colorarray[k * 3 + 0] = tempcolor.r;
                        colorarray[k * 3 + 1] = tempcolor.g;
                        colorarray[k * 3 + 2] = tempcolor.b;

                    }

                    if (!calculatedmeans) {
                        xmean = xmean / clusterdata.p.length;
                        ymean = ymean / clusterdata.p.length;
                        zmean = zmean / clusterdata.p.length;

                        xmeantotal += xmean;
                        ymeantotal += ymean;
                        zmeantotal += zmean;
                    }

                    geometry[clusterid].addAttribute('position', new THREE.BufferAttribute(positions, 3));
                    geometry[clusterid].addAttribute('color', new THREE.BufferAttribute(colorarray, 3));
                }
            }


            if(!calculatedmeans) {
                xmeantotal = xmeantotal / clusterCount;
                ymeantotal = ymeantotal / clusterCount;
                zmeantotal = zmeantotal / clusterCount;
                calculatedmeans = true;
            }
            for (var key in geometry) {
                if (geometry.hasOwnProperty(key)) {
                    geometry[key].translate(-xmeantotal,-ymeantotal,-zmeantotal);
                    particles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size,sections[key].shape, false));
                }
            }

            particleSets[data.seq] = particles;
            for (var key in particles) {
                if (particles.hasOwnProperty(key)) {
                    if (controlers.pointsize != 1 || controlers.glyphsize != 1) {
                        if (sections[key].size == 1) {
                            particles[key].material.size = (localSections[key].size / 200) * controlers.pointsize;
                        } else {
                            particles[key].material.size = (localSections[key].size / 200) * controlers.glyphsize;
                        }
                        particles[key].material.needsUpdate = true;
                    }
                }
            }
            plotPointsSets[data.seq] = plotPoints;
            pointLabelxKeySets[data.seq] = pointLabelxKey;
            sectionSets[data.seq] = localSections;
            fileNames[data.seq] = data.file;
        }).fail(function() {
            bufferRequestMade[i] = false;
        });
        })(i);

    }
}


// this function is used to create new cluster id's for custom clusters
function setMaxClusterId(clusterid){
    if(clusterid > maxClusterId){
        maxClusterId = clusterid;
    }
}

//Util functions
function initPlotData() {
    plotRangeSlider.update({max: timeSeriesLength - 1, min: 0, from: 0});
    //$("#plot-slider").attr("max", timeSeriesLength - 1);
    //$("#plot-slider").attr("value", 0);
    currentParticles = particleSets["0"];
    camera.lookAt(scene3d.position);
    camera.updateProjectionMatrix();
    //camera.target.position.copy(cameraCenter);
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            scene3d.add(currentParticles[key]);
        }
    }
}



function setupThreeJs() {
    renderer = null;
    particles = [];
    colors = [];
    controls = null;
    var height = window.innerHeight - 57 - 40 - 40 - 10;
    $('#canvas3d').width(window.innerWidth - 45);
    $('#canvas3d').height(height);
    var canvasWidth = $('#canvas3d').width();
    var canvasHeight = $('#canvas3d').height();

    //new THREE.Scene
    scene3d = new THREE.Scene();
    stats = new Stats();
    //set the scene
    var canvas3d = $('#canvas3d');
    //new THREE.WebGLRenderer
    renderer = new THREE.WebGLRenderer({canvas: canvas3d.get(0), antialias: true});
    renderer.setSize(canvasWidth, canvasHeight);
    renderer.setClearColor(0x121224, 1);

    //new THREE.PerspectiveCamera
    cameraCenter = new THREE.Vector3(0,0,0);
    camera = new THREE.PerspectiveCamera(45, canvasWidth / canvasHeight, 0.1, 10000);
    camera.name = 'camera';
    camera.position.set(1, 1, 1);
    camera.lookAt(cameraCenter);
    scene3d.add(camera);
    controls = new THREE.TrackballControls(camera, renderer.domElement);
    controls.staticMoving = true;
    controls.rotateSpeed = 20.0;
    controls.dynamicDampingFactor = 0.3;
    initColorSchemes()
    // stats.domElement.style.position = 'absolute';
    //document.getElementById("stats").appendChild(stats.domElement);
    sprites["0"] =  THREE.ImageUtils.loadTexture(ImageEnum.DISC);
    sprites["1"] =  THREE.ImageUtils.loadTexture(ImageEnum.BALL);
    sprites["2"] =  THREE.ImageUtils.loadTexture(ImageEnum.STAR);
    sprites["3"] =  THREE.ImageUtils.loadTexture(ImageEnum.CUBE);
    sprites["4"] =  THREE.ImageUtils.loadTexture(ImageEnum.PYRAMID);
    sprites["5"] =  THREE.ImageUtils.loadTexture(ImageEnum.CONE);
    sprites["6"] =  THREE.ImageUtils.loadTexture(ImageEnum.CYLINDER);
    window.addEventListener('resize', onWindowResize, false);
}

function initColorSchemes() {
    colorSchemes['mathlab50'] = ["#ffffff","#ff0000","#00ff00","#ff1ab9","#ffd300","#0084f6","#008d46","#a7613e","#00fff6","#3e7b8d","#eda7ff","#d3ff95","#b94fff","#e51a58","#848400","#00ff95","#ffedff","#f68412","#caff00","#0035c1","#ffca84","#9e728d","#4fb912","#9ec1ff","#959e7b","#ff7bb0","#9e0900","#ffb9b9","#8461ca","#9e0072","#84dca7","#ff00f6","#00d3ff","#ff7258","#583e35","#d3d3d3","#dc61dc","#6172b0","#b9ca2c","#545454","#5800ca","#95c1ca","#d39e23","#84b058","#e5edb9","#f6d3ff","#8d09a7","#6a4f00","#003e9e","#7b3e7b"]
}

function loadMatrial(size, shape, isglyph) {
    var sprite;
    if (!isglyph) {
        sprite = null;
    }else{
     //   sprite = THREE.ImageUtils.loadTexture(ImageEnum.CONE);
    }

    if(size>1){
        switch (parseInt(shape)){
            case 0: sprite = sprites["0"];
                break;
            case 1: sprite = sprites["1"];
                break;
            case 2: sprite = sprites["2"];
                break;
            case 3: sprite = sprites["3"];
                break;
            case 4: sprite = sprites["4"];
                break;
            case 5: sprite = sprites["5"];
                break;
            case 6: sprite = sprites["6"];
                break;
            default : sprite = sprites["3"];
        }
    }

    var material = new THREE.PointsMaterial({
        size: size / 200,
        map: sprite,
        vertexColors: THREE.VertexColors,
        transparent: true,
        opacity: 0.8
    });
    return material;
}

ImageEnum = {
    BALL : "/assets/images/textures/ball.png",
    CUBE : "/assets/images/textures/cube.png",
    DISC : "/assets/images/textures/disc.png",
    STAR : "/assets/images/textures/star.png",
    PYRAMID : "/assets/images/textures/pyramid.png",
    CONE : "/assets/images/textures/cone.png",
    CYLINDER : "/assets/images/textures/cylinder.png"
};

/**
 * This function will try to render the plot with the index
 * If this plot is not loaded yet, it will simply do nothing.
 * It is the loading functions responsibility to load the data required
 * @param index
 */
function updatePlot(index) {

        if (index in particleSets && particleSets[index]) {
            scene3d = new THREE.Scene();
            scene3d.add(camera);
            //$("#plot-slider").attr("value", $("#plot-slider").attr("value"));
          //  maplabelstokeys(plotPointsSets[index])
            currentParticles = particleSets[index];
            plotPoints = plotPointsSets[index];
            pointLabelxKey = pointLabelxKeySets[index];
            var localSection = sectionSets[index];
            sections = localSection
            renderCustomCluster();
            for (var key in currentParticles) {
                if (currentParticles.hasOwnProperty(key)) {

                    if(controlers.pointsize != 1 || controlers.glyphsize != 1) {
                        if (sections[key].size == 1) {
                            currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
                        } else {
                            currentParticles[key].material.size = (sections[key].size / 200) * controlers.glyphsize;
                        }
                        currentParticles[key].material.needsUpdate = true;
                    }

                    if (recoloredclusters.hasOwnProperty(key)) {
                        var tempcolor = recoloredclusters[key]
                        var colorattri = currentParticles[key].geometry.getAttribute('color');
                        var colorsd = new Float32Array(colorattri.length);
                        for (var k = 0; k < colorattri.length / 3; k++) {
                            colorsd[k * 3 + 0] = tempcolor.r;
                            colorsd[k * 3 + 1] = tempcolor.g;
                            colorsd[k * 3 + 2] = tempcolor.b;
                        }
                        currentParticles[key].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
                        currentParticles[key].geometry.colorsNeedUpdate = true;
                    }

                    if(changedGlyphs.hasOwnProperty(key)){
                        currentParticles[key].material.map = sprites[changedGlyphs[key]];
                        currentParticles[key].material.needsUpdate = true;
                    }
                    if (!(removedclusters.hasOwnProperty(key))) {
                        scene3d.add(currentParticles[key]);
                    }
                }
            }
            // change only when the setting dispaly is on
            if (settingOn) {
                document.getElementById('cluster_table_div').innerHTML = generateCheckList(sections, colorlist);
            }
            document.getElementById('plot-clusters').innerHTML = generateClusterList(sections, colorlist);
            fileName = fileNames[index]
            populatePlotInfo()
            //$("#cluster_table_div").html(generateCheckList(sections, colorlist));
            //$("#plot-clusters").html(generateClusterList(sections, colorlist));
            // $('.color_enable').prop('checked', false);
            sections = localSection;
            window.addEventListener('resize', onWindowResize, false);
            render();
            animate();
            $("#plot-title").text(fileNames[index]);
            //if (!colorPickersLoaded) {
            //    if (clusters && Object.keys(clusters).length < 100) {
            //        $('.color-pic1').colorpicker();
            //        $('.color_enable').prop('checked', true);
            //        colorPickersLoaded = true;
            //    }
            //}
            return true;
    } else {
        return false;
    }
    return false;
}

var settingOn = false;

function showSettings() {
    settingOn = true;
    document.getElementById('cluster_table_div').innerHTML = generateCheckList(sections, colorlist);
    //$('.color_enable').prop('checked', true);
}

function hideSettings() {
    settingOn = false;
}

function animate() {
    requestAnimationFrame(animate);
    controls.update();
    stats.update();
    render();
}

function render() {
    var camera = scene3d.getObjectByName('camera');
    renderer.render(scene3d, camera);
}

function removeSection(id) {
    scene3d.remove(currentParticles[id]);
    removedclusters[id] = id;
}

function removeAllSection(){
    removedclusters = [];
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            scene3d.remove(currentParticles[key]);
            removedclusters[key] = key;
        }
    }
}
function addAllSections(){
    for (var key in removedclusters) {
        if (removedclusters.hasOwnProperty(key)) {
            scene3d.add(currentParticles[key]);
        }
    }
    removedclusters = [];
}

function addSection(id) {
    scene3d.add(currentParticles[id]);
    delete removedclusters[id];
}

function changeGlyph(id,shape){
    changedGlyphs[id] = shape;
    currentParticles[id].material.map = sprites[shape];
    currentParticles[id].material.needsUpdate = true;
}

function recolorSection(id, color) {
    if(id == "cccolor") return;
    colorlist[id] = color;
    color = "#"+color
    var tempcolor = new THREE.Color(color);
    trueColorList[id] = {"r": tempcolor.toArray()[0] * 255, "g": tempcolor.toArray()[1] * 255, "b": tempcolor.toArray()[2] * 255};
    var colorattri = currentParticles[id].geometry.getAttribute('color');
    var colorsd = new Float32Array(colorattri.length);
    for (var k = 0; k < colorattri.length / 3; k++) {
        colorsd[k * 3 + 0] = tempcolor.r;
        colorsd[k * 3 + 1] = tempcolor.g;
        colorsd[k * 3 + 2] = tempcolor.b;
    }
    currentParticles[id].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
    // update the lables
    $("#plot-clusters").html(generateClusterList(sections, colorlist));
    //for (var i in colors[id]) {
    //    colors[id][i] = new THREE.Color(color);
    //}
    recoloredclusters[id] = new THREE.Color(color);
    currentParticles[id].geometry.colorsNeedUpdate = true;

}

function addCustomCluster(isSingle){
    var clusterLabel = $('#cclabel').val();
    var shape = $('#ccshape').val();
    var size = $('#ccsize').val();
    var color = $('#cccolor').val();
    var points = ($('#ccpoints').val()).split(",");

    var p = points;
   // var point
    //for(var i=0, len=points.length; i < len; i++){
    //    p[i] = pointLabelxKey[points[i]]
    //}
    var clusterkey = maxClusterId + 1;
    setMaxClusterId(clusterkey);
    var cluster = {
        l: clusterLabel,
        f: shape,
        s: size,
        c: color,
        p: p
    }
    customclusters[clusterkey.toString()] = cluster;

    if(!isSingle){
        var currentValue = parseInt($("#plot-slider").prop("value"));
        updatePlot(currentValue)
    }else{
        renderCustomCluster()
        document.getElementById('cluster_table_div').innerHTML = generateCheckList(sections, colorlist);
        document.getElementById('plot-clusters').innerHTML = generateClusterList(sections, colorlist);
        //$("#cluster_table_div").html(generateCheckList(sections, colorlist));
        //$("#plot-clusters").html(generateClusterList(sections, colorlist));
        addParticlesToScence()
    }
}



function renderCustomCluster(){
    var geometry = {};
    var localSections = [];
    for (var cid in customclusters) {
        if (customclusters.hasOwnProperty(cid)) {
            var clusterdata = customclusters[cid];
            var clusterid =  parseInt(cid)
            setMaxClusterId(clusterid);
            var clustercolor;

            if (!geometry.hasOwnProperty(clusterid)) {
                geometry[clusterid] = new THREE.BufferGeometry();
                particles[clusterid] = [];
            }

            if (!colorlist.hasOwnProperty(clusterid)){
                var tempcolor = new THREE.Color(clusterdata.c);
                clustercolor = {"r": tempcolor.toArray()[0] * 255, "g": tempcolor.toArray()[1] * 255, "b": tempcolor.toArray()[2] * 255};
                colorlist[clusterid] = clusterdata.c.substring(1);
                if(!trueColorList.hasOwnProperty(clusterid)) {
                    trueColorList[clusterid] = clustercolor;
                }
            }

            var localSection = {
                "length": clusterdata.p.length,
                "size": clusterdata.s,
                "shape": clusterdata.f,
                "visible": 1,
                "color": clustercolor,
                "label": clusterdata.l
            };
            if (!sections.hasOwnProperty(clusterid)) {
                sections[clusterid] = localSection;
            }
            localSections[clusterid] = localSection;

            var positions = new Float32Array(clusterdata.p.length * 3);
            var colorarray = new Float32Array(clusterdata.p.length * 3);
            var sizes = new Float32Array(clusterdata.p.length);
            for (var k = 0; k < clusterdata.p.length; k++) {
                var key = pointLabelxKey[clusterdata.p[k]]
                var p = plotPoints[key]
                if (!p) {
                    continue;
                }
                var p0 = parseFloat(p[0]);
                var p1 = parseFloat(p[1]);
                var p2 = parseFloat(p[2]);
                positions[k * 3 + 0] = p0;
                positions[k * 3 + 1] = p1;
                positions[k * 3 + 2] = p2;

                var tempc = trueColorList[clusterid]
                tempcolor = new THREE.Color("rgb(" + tempc.r + "," + tempc.g + "," + tempc.b + ")");

                colorarray[k * 3 + 0] = tempcolor.r;
                colorarray[k * 3 + 1] = tempcolor.g;
                colorarray[k * 3 + 2] = tempcolor.b;

            }

            geometry[clusterid].addAttribute('position', new THREE.BufferAttribute(positions, 3));
            geometry[clusterid].addAttribute('color', new THREE.BufferAttribute(colorarray, 3));
        }
    }
    var tempparticles;
    for (var key in geometry) {
        if (geometry.hasOwnProperty(key)) {
            geometry[key].translate(-xmeantotal,-ymeantotal,-zmeantotal);
            tempparticles = new THREE.Points(geometry[key], loadMatrial(sections[key].size,sections[key].shape, false));

            if (controlers.pointsize != 1 || controlers.glyphsize != 1) {
                if (sections[key].size == 1) {
                    tempparticles.material.size = (localSections[key].size / 200) * controlers.pointsize;
                } else {
                    tempparticles.material.size = (localSections[key].size / 200) * controlers.glyphsize;
                }
                tempparticles.material.needsUpdate = true;
            }
            currentParticles[key] = tempparticles;

        }
    }

}

function animateTimeSeriesPlay() {
    playStatus = playEnum.PLAY;
}

function playLoop() {
    var currentValue = parseInt($("#plot-slider").prop("value"));
    var maxValue = timeSeriesLength - 1;
    setTimeout(function () {
        if (particleSets[currentValue] && playStatus == playEnum.PLAY) {
            if (updatePlot(currentValue + 1)) {
                plotRangeSlider.update({from: currentValue + 1});
                render();
            }
        }

        if (!(maxValue > currentValue && playStatus == playEnum.PLAY)) {
            playStatus = playEnum.PAUSE;
            if (maxValue == currentValue) {
                $('#play-span').removeClass("glyphicon-pause").addClass("glyphicon-repeat");
            }
        }
        playLoop();
    }, controlers.delay);
}

function initBufferAndLoad() {
    var currentValue = parseInt($("#plot-slider").prop("value"));
    setTimeout(function () {
        if (Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < MAX_PLOTS_STORED) {
            if (timeSeriesLength > MAX_PLOTS_STORED) {
                loadPlotData(0, MAX_PLOTS_STORED);
            }else{
                loadPlotData(0, timeSeriesLength);
            }
            initBufferAndLoad();
        } else {
            updatePlot(0);
            $( "#progress" ).css({display : "none"});
            if (!bufferLoopStarted) {
                bufferLoopStarted = true;
                bufferLoop();
            }
        }
    }, controlers.delay);
}

var bufferRequestMade = {};        // track the requests made to get data to be buffered
var currentPlotUpdated = false;    // make sure we don't render the same plot multiple times

function bufferLoop(){
    setTimeout(function () {
        var currentIndex = parseInt($("#plot-slider").prop("value"));
        var loadend = timeSeriesLength;
        if (timeSeriesLength > currentIndex + controlers.loadSize) {
            loadend = currentIndex + controlers.loadSize;
        }
        var loadStartIndex = 0;
        if (currentIndex - controlers.loadSize > 0) {
            loadStartIndex = currentIndex - controlers.loadSize;
        }
        for (var i = 0; i < loadStartIndex; i++) {
            if (bufferRequestMade[i]) {
                delete bufferRequestMade[i];
            }
            delete particleSets[i];
            if (particleSets[i]) {
                delete bufferRequestMade[i];
                particleSets[i] = null;
            }
            if (plotPointsSets[i]){
                delete plotPointsSets[i];
                plotPointsSets[i] = null
            }
            if (pointLabelxKeySets[i]){
                delete pointLabelxKeySets[i];
                pointLabelxKeySets[i] = null
            }
            if (sectionSets[i]) {
                delete sectionSets[i];
                sectionSets[i] = null;
            }
        }
        for (var i = loadend + 1; i < timeSeriesLength; i++) {
            if (bufferRequestMade[i]) {
                delete bufferRequestMade[i];
            }
            if (particleSets[i]) {
                delete particleSets[i];
            }
            if (sectionSets[i]) {
                delete sectionSets[i];
            }
        }

        loadPlotData(loadStartIndex, loadend);
        if (playStatus == playEnum.PAUSE && !currentPlotUpdated) {
            updatePlot(currentIndex);
            currentPlotUpdated = true;
        }
        bufferLoop();

    }, controlers.delay / 2);
}

function animateTimeSeriesPause() {
    playStatus = playEnum.PAUSE;
}

function resetView() {
    controls.reset();
}

function resetSlider() {
    playStatus = playEnum.PAUSE;
    //$("#plot-slider").attr("value", 0);
    plotRangeSlider.update({from: 0});
}

var controlers = {
    delay: 300,
    pointsize: 1,
    glyphsize: 1,
    loadSize: 10,
    maxPlotsStored: 20
};


function setupGuiSingle() {
    var gui = new dat.GUI({ autoPlace: false });
    var customContainer = document.getElementById('plot-controls');
    customContainer.appendChild(gui.domElement);
    gui.add(controlers, 'pointsize', 0.01, 5.0, 1.0).name("Point Size").onFinishChange(changePointSize);
    gui.add(controlers, 'glyphsize', 0.01, 5.0, 1.0).name("Glyph Size").onFinishChange(changeGlyphSize);
    //h.add( effectController, "shininess", 1.0, 400.0, 1.0 ).name( "shininess" ).onChange( render );
}

function setupGuiTimeSeries() {
    var gui = new dat.GUI({ autoPlace: false });
    var customContainer = document.getElementById('plot-controls');
    customContainer.appendChild(gui.domElement);
    gui.add(controlers, 'delay', 10.0, 2000.0, speed).name( "Play Delay(ms)");
    gui.add(controlers, 'pointsize', 0.01, 5.0, pointSize).name("Point Size").onFinishChange(changePointSize);
    gui.add(controlers, 'glyphsize', 0.01, 5.0, glyphSize).name("Glyph Size").onFinishChange(changeGlyphSize);
}

function changePointSize(){
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            if (sections[key].size == 1) {
                currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
            } else {
                currentParticles[key].material.size = (sections[key].size / 200) * controlers.glyphsize;
            }
            currentParticles[key].material.needsUpdate = true;
        }
    }
    render();
}

function changeGlyphSize(){
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            if (sections[key].size == 1) {
                currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
            } else {
                currentParticles[key].material.size = (sections[key].size / 200) * controlers.glyphsize;
            }
            currentParticles[key].material.needsUpdate = true;
        }
    }
    render();
}

function populatePlotInfo(){
    document.getElementById('plot-info-description').innerHTML = "<b>Name: </b>" + fileName + "</br>" +
            "<b>Desc: </b>" + plotDesc +"</br>" +
            "<b>Uploader: </b>" + uploader
}

function savePlot() {
    var res = false;
    bootbox.confirm("You are about to save the current plot configuration. Continue?", function(result) {
        res = result;
        if (res) {
            var url = '/timeseries/save ';
            var c = camera.toJSON();
            var obj = {};
            obj['camera'] = camera.toJSON();
            obj['tid'] = timeseriesId;
            obj['fid'] = resultSetId;
            obj['pointSize'] = controlers.pointsize;
            obj['glyphSize'] = controlers.glyphsize;
            obj['speed'] = controlers.delay;
            obj['glyphs'] = changedGlyphs;
            obj['customclusters'] = customclusters;
            var lookAtVector = new THREE.Vector3(0, 0, 0);
            lookAtVector.applyQuaternion(camera.quaternion);
            var lookAtJson = {};
            lookAtJson.x = lookAtVector.x;
            lookAtJson.y = lookAtVector.y;
            lookAtJson.z = lookAtVector.z;
            obj['clusterColors'] = trueColorList;
            obj['lookVector'] = lookAtJson;
            obj['cameraPosition'] = camera.position;
            obj['cameraup'] = camera.up;
            obj['rotation'] = camera.rotation;
            obj['zoom'] = camera.zoom;
            obj['camerastate'] = JSON.stringify(camera.matrix.toArray());
            obj['controltarget'] = controls.target;
            $.ajax({
                type: "POST",
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                data: JSON.stringify(obj),
                url: url,
                success: function (data) {
                    console.log(data);
                }
            });
        }
    });
}

