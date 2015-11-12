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
var sections = [];
var sprites = {};
var xmeantotal = 0,ymeantotal = 0,zmeantotal = 0;
var xmean = 0,ymean = 0,zmean = 0, cameraCenter, calculatedmeans = false;

//Single Plot Varibles
var clusterUrl;
var clusters;
var resultSetId;
var resultData;

//Time Series Vars
var particleSets = {};
var sectionSets = {};
var currentPointSize = 0;
var fileNames = {};
var timeSeriesData = [];
var isPlaying = false;
var isPaused = false;
var resultSets;
var removedclusters = [];
var recoloredclusters = [];
var currentLoadedStart, currentLoadedEnd, timeSeriesLength;
var precurrentLoadedStart, precurrentLoadedEnd;

//Constants
var TIME_BETWEEN_PLOTS_IN_MILLS = 300;
var MAX_PLOTS_STORED = 20;
var LOAD_SIZE = 5;

var plotRangeSlider = {};

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

function generateClusterList(list, initcolors) {
    var grid = "";

    for(var key in list) {
        if(list.hasOwnProperty(key)) {
            var colorWithouthHash = initcolors[key].replace(/#/g, '')
            grid += "<div class='element-item transition metal' data-category='transition' style='background-color: #"+ colorWithouthHash +" '>"+
                "<p style='font-size: 0.8em'><span style='font-weight: bold'>" + list[key].label + "</span>:" + list[key].length +"</p></div>"
        }
    }

    return grid;
}

//Generate the check box list for clusters
function generateCheckList(list, initcolors) {

    var tabletop = "<table class='table' id='cluster_table'>"
        + "<thead>"
        + "<tr class='headings'>"
        + "<th>"
        + "<input type='checkbox' id='check-all' disabled='true' class='flat'> Cluster"
        + "</th>"
        + "<th class='column-title'>Label</th>"
        + "<th class='column-title'>Size</th>"
        + "</tr>"
        + "</thead>"
        + "<tbody>"

    var tablerows = "";

    for (var key in list) {
        if (list.hasOwnProperty(key)) {
            tablerows += "<tr class='even pointer' id='" + key + "'>"
                + "<td class='a-center'>"
            if (!(removedclusters.hasOwnProperty(key))){
                tablerows += "<input type='checkbox' class='flat' name='table_records' checked value='" + key + "'>"
            }else{
                tablerows += "<input type='checkbox' class='flat' name='table_records' value='" + key + "'>"
            }
            tablerows += "<label class='color-box-label'>" + key + "</label> "
                + "<div class='input-group color-pic1' style='width: 15px;height: 15px; display: inline-flex; padding-left: 20px;padding-top: 2px'>"
                + "<input value='" + initcolors[key] + "' class='form-control' type='hidden' id='" + key + "'>"
                + "<span class='input-group-addon color-picker-addon'><i style='background-color: rgb(1, 343, 69);'></i></span>"
                + "</div>"
                + "</td>"
                + "<td class=' '>" + list[key].label + "</span></td>"
                + "<td class='l1'>" + list[key].length + "</td>"
                + "</tr>"
        }
    }

    var tableend = "</tbody>"
        + "</table>"


    return tabletop + tablerows + tableend;
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
function visualize(resultSetUrl, resultSet, id) {
    clusterUrl = resultSetUrl;
    clusters = resultSet.clusters;
    resultSetId = id;
    generateGraph();
    setupGuiSingle();
    animate();
}

function visualizeTimeSeries(resultSetUrl, timeSeries, id) {
    resultSets = timeSeries.resultsets;
    timeSeriesLength = resultSets.length;
    generateTimeSeries(resultSets);
    setupGuiTimeSeries();
}


function generateGraph() {

    setupThreeJs();

    var cluster;
    var geometry = {};
    var hsl;
    var sections = {};
    $.getJSON(clusterUrl, function (data) {
        fileName = data.fileName;
        $("#plot-title").text(data.fileName)
        for (var i = 0; i < clusters.length; i++) {
            var clusterdata = data.clusters[i];
            var clusterid = clusterdata.clusterid;

            var clustercolor = clusterdata.color
            if (clustercolor == null)
                clustercolor = {"a": randomRBG(), "b": randomRBG(), "g": randomRBG(), "r": randomRBG()}

            if (!sections.hasOwnProperty(clusterdata.clusterid))
                sections[clusterdata.clusterid] = {
                    "length": clusterdata.points.length,
                    "size": clusterdata.size,
                    "shape": clusterdata.shape,
                    "visible": clusterdata.visible,
                    "color": clustercolor,
                    "label": clusterdata.label
                }

            if (!geometry.hasOwnProperty(clusterdata.clusterid)) {
                geometry[clusterdata.clusterid] = new THREE.BufferGeometry()
                currentParticles[clusterdata.clusterid] = new Array();
            }
            if(!colorlist.hasOwnProperty(clusterdata.clusterid))
            colorlist[clusterdata.clusterid] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString()

            var positions = new Float32Array(clusterdata.points.length * 3);
            var colorarray = new Float32Array(clusterdata.points.length * 3);
            var sizes = new Float32Array(clusterdata.points.length);
            xmean = 0
            ymean = 0
            zmean = 0
            for (var k = 0; k < clusterdata.points.length; k++) {
                var p = clusterdata.points[k];

                positions[k * 3 + 0] = p.x;
                positions[k * 3 + 1] = p.y;
                positions[k * 3 + 2] = p.z;

                xmean += p.x;
                ymean += p.y;
                zmean += p.z;

                var tempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")")
                colorarray[k * 3 + 0] = tempcolor.r;
                colorarray[k * 3 + 1] = tempcolor.g;
                colorarray[k * 3 + 2] = tempcolor.b;



            }

            xmean = xmean/clusterdata.points.length;
            ymean = ymean/clusterdata.points.length;
            zmean = zmean/clusterdata.points.length;
            geometry[clusterdata.clusterid].addAttribute('position', new THREE.BufferAttribute(positions, 3));
            geometry[clusterdata.clusterid].addAttribute('color', new THREE.BufferAttribute(colorarray, 3));

            xmeantotal += xmean;
            ymeantotal += ymean;
            zmeantotal += zmean;

        }

        xmeantotal = xmeantotal/clusters.length;
        ymeantotal = ymeantotal/clusters.length;
        zmeantotal = zmeantotal/clusters.length;

        for (var key in geometry) {
            if (geometry.hasOwnProperty(key)) {
                geometry[key].translate(-xmeantotal,-ymeantotal,-zmeantotal);
                currentParticles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size,sections[key].shape, false));
                scene3d.add(currentParticles[key]);
            }
        }
        $("#cluster_table_div").html(generateCheckList(sections, colorlist));
        $("#plot-clusters").html(generateClusterList(sections, colorlist));
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
        $('.color-pic1').colorpicker();
        render();
        animate();
    });

    animate();

}

function generateTimeSeries(resultSets) {
    setupThreeJs();
    currentLoadedStart = 0;
    precurrentLoadedStart = 0;

    if (timeSeriesLength > MAX_PLOTS_STORED) {
        loadPlotData(0, MAX_PLOTS_STORED)
        currentLoadedEnd = MAX_PLOTS_STORED
        precurrentLoadedEnd = MAX_PLOTS_STORED
    }else{
        loadPlotData(0,timeSeriesLength)
        currentLoadedEnd = timeSeriesLength
        precurrentLoadedEnd = timeSeriesLength
    }
    initBufferAndLoad();

}

//TODO WInodow rezise does not work yet need to fix
function onWindowResize() {
    var width = window.innerWidth
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
    var cluster;
    var geometry = [];
    var hsl;

    for (var i = start; i < end; i++) {
        clusterUrl = "/resultssetall/" + resultSets[i].timeSeriesId + "/file/" + resultSets[i].id;
        $.getJSON(clusterUrl, function (data) {
            particles = {};
            colors = {};
            geometry = {};
            clusters = data.clusters;
            fileName = data.fileName;

            var localSections = [];
            for (var i = 0; i < clusters.length; i++) {

                var clusterdata = data.clusters[i];
                var clusterid = clusterdata.clusterid;
                var clustercolor = clusterdata.color;
                if (clustercolor == null)
                    clustercolor = {"a": randomRBG(), "b": randomRBG(), "g": randomRBG(), "r": randomRBG()}

                hsl = [heus[clusterid], 1, 0.8];
                if (!geometry.hasOwnProperty(clusterdata.clusterid)) {
                    geometry[clusterdata.clusterid] = new THREE.BufferGeometry()
                    particles[clusterdata.clusterid] = new Array();
                }

                if(!colorlist.hasOwnProperty(clusterdata.clusterid))
                colorlist[clusterdata.clusterid] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString()

                var localSection = {
                    "length": clusterdata.points.length,
                    "size": clusterdata.size,
                    "shape": clusterdata.shape,
                    "visible": clusterdata.visible,
                    "color": clustercolor,
                    "label": clusterdata.label
                };
                if (!sections.hasOwnProperty(clusterdata.clusterid)) {
                    sections[clusterdata.clusterid] = localSection;
                }
                localSections[clusterdata.clusterid] = localSection;

                var positions = new Float32Array(clusterdata.points.length * 3);
                var colorarray = new Float32Array(clusterdata.points.length * 3);
                var sizes = new Float32Array(clusterdata.points.length);
                xmean = 0
                ymean = 0
                zmean = 0
                for (var k = 0; k < clusterdata.points.length; k++) {
                    var p = clusterdata.points[k];

                    positions[k * 3 + 0] = p.x;
                    positions[k * 3 + 1] = p.y;
                    positions[k * 3 + 2] = p.z;

                    if(!calculatedmeans){
                        xmean += p.x;
                        ymean += p.y;
                        zmean += p.z;
                    }

                    var tempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")")
                    colorarray[k * 3 + 0] = tempcolor.r;
                    colorarray[k * 3 + 1] = tempcolor.g;
                    colorarray[k * 3 + 2] = tempcolor.b;

                }

                if(!calculatedmeans) {
                    xmean = xmean / clusterdata.points.length;
                    ymean = ymean / clusterdata.points.length;
                    zmean = zmean / clusterdata.points.length;

                    xmeantotal += xmean;
                    ymeantotal += ymean;
                    zmeantotal += zmean;
                }

                geometry[clusterdata.clusterid].addAttribute('position', new THREE.BufferAttribute(positions, 3));
                geometry[clusterdata.clusterid].addAttribute('color', new THREE.BufferAttribute(colorarray, 3));
            }
            if(!calculatedmeans) {
                xmeantotal = xmeantotal / clusters.length;
                ymeantotal = ymeantotal / clusters.length;
                zmeantotal = zmeantotal / clusters.length;
                calculatedmeans = true;
            }
            for (var key in geometry) {
                if (geometry.hasOwnProperty(key)) {
                    geometry[key].translate(-xmeantotal,-ymeantotal,-zmeantotal);
                    particles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size,sections[key].shape, false));
                }
            }

            particleSets[data.timeSeriesSeqNumber] = particles;
            sectionSets[data.timeSeriesSeqNumber] = localSections;
            fileNames[data.timeSeriesSeqNumber] = data.fileName;
            console.log(data.fileName)
        });

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
    $("#cluster_table_div").html(generateCheckList(sections, colorlist));
    $("#plot-clusters").html(generateClusterList(sections, colorlist));
    var clusters = $("#plot-clusters").isotope({
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

    $('.color-pic1').colorpicker();
}

function initBufferAndLoad() {

    setTimeout(function () {
        if (Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < MAX_PLOTS_STORED) {
            initBufferAndLoad();
        } else {
            if (currentLoadedStart in particleSets) {
                initPlotData();
                render();
                animate();
            }
        }
    }, 1000);
}

function gotoBufferAndLoad(sliderValue) {

    setTimeout(function () {
        if (Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < MAX_PLOTS_STORED && !(currentLoadedEnd == timeSeriesLength)) {
                gotoBufferAndLoad(sliderValue);
        } else {
            if (currentLoadedStart in particleSets) {
                updatePlot(sliderValue)
                render();
                animate();
            }
        }
    }, 1000);
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
    cameraCenter = new THREE.Vector3(0,0,0)
    camera = new THREE.PerspectiveCamera(45, canvasWidth / canvasHeight, 0.1, 10000);
    camera.name = 'camera';
    camera.position.set(1, 1, 1);
    camera.lookAt(cameraCenter);
    scene3d.add(camera);
    controls = new THREE.OrbitControls(camera, renderer.domElement);
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
        opacity: 0.9
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
    CYLINDER : "/assets/images/textures/cylinder.png",
}

function updatePlot(sliderValue) {
    if($("#play-span").hasClass("glyphicon-repeat")){
        $("#play-span").removeClass("glyphicon-repeat").addClass("glyphicon-play");
    }
    if(sliderValue >= currentLoadedStart && sliderValue < currentLoadedEnd){
        if (sliderValue in particleSets) {
            scene3d = new THREE.Scene();
            scene3d.add(camera);
            //$("#plot-slider").attr("value", $("#plot-slider").attr("value"));

            currentParticles = particleSets[sliderValue];
            var localSection = sectionSets[sliderValue];
            for (var key in currentParticles) {
                if (currentParticles.hasOwnProperty(key)) {

                    if(controlers.pointsize != 1) {
                        currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
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
                    if (!(removedclusters.hasOwnProperty(key))) {
                        scene3d.add(currentParticles[key]);
                    }
                }
            }
            $("#cluster_table_div").html(generateCheckList(localSection, colorlist));
            $("#plot-clusters").html(generateClusterList(localSection, colorlist));
            sections = localSection;
            window.addEventListener('resize', onWindowResize, false);
            render();
            animate();
            $("#plot-title").text(fileNames[sliderValue]);
            $('.color-pic1').colorpicker();
        }
    } else {
        for (var k = 0; k < (currentLoadedEnd - currentLoadedStart); k++) {
            delete particleSets[currentLoadedStart + k];
            delete sectionSets[currentLoadedStart + k];
        }
        currentLoadedStart = sliderValue;
        precurrentLoadedStart = sliderValue;
        currentLoadedEnd = timeSeriesLength
        precurrentLoadedEnd = timeSeriesLength
        if(timeSeriesLength > (currentLoadedStart + MAX_PLOTS_STORED)){
            currentLoadedEnd = currentLoadedStart + MAX_PLOTS_STORED;
            precurrentLoadedEnd = precurrentLoadedStart + MAX_PLOTS_STORED;
        }
        //TODO check if this might fail in edge cases
        loadPlotData(currentLoadedStart, currentLoadedEnd)
        gotoBufferAndLoad(sliderValue);
    }
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

function addSection(id) {
    scene3d.add(currentParticles[id]);
    delete removedclusters[id];
}

function recolorSection(id, color) {
    colorlist[id] = color;
    var tempcolor = new THREE.Color(color);
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

function animateTimeSeriesPlay() {
    isPlaying = true;
    isPaused = false;
    playLoop();
}

function playLoop() {

    var currentValue = parseInt($("#plot-slider").prop("value"));
    var maxValue = timeSeriesLength - 1;
    checkAndBufferData(currentValue+1)
    //if(currentValue == maxValue){
    //    $('#slider-play').removeClass("fa fa-pause").addClass("fa fa-play-circle");
    //    return
    //}
    if((currentValue + 1) >= currentLoadedEnd){
        isPaused = true;
    } else {
        setTimeout(function () {
            scene3d = new THREE.Scene();
            scene3d.add(camera);
            //$("#plot-slider").attr("value", currentValue + 1);
            plotRangeSlider.update({from: currentValue + 1});
            currentParticles = particleSets[currentValue + 1];
            var localSection = sectionSets[currentValue + 1];
            for (var key in currentParticles) {
                if (currentParticles.hasOwnProperty(key)) {

                    if(controlers.pointsize != 1) {
                        currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
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
                    if (!(removedclusters.hasOwnProperty(key))) {
                        scene3d.add(currentParticles[key]);
                    }
                }
            }
            $("#cluster_table_div").html(generateCheckList(localSection, colorlist));
            $("#plot-clusters").html(generateClusterList(localSection, colorlist));
            sections = localSection;
            window.addEventListener('resize', onWindowResize, false);
            $("#plot-title").text(fileNames[currentValue + 1]);
            $('.color-pic1').colorpicker();
            render();
            if (maxValue > currentValue + 1 && !isPaused) {
                playLoop();
            } else {
                isPaused = true;
                isPlaying = false;
                if (maxValue == currentValue + 1) {
                    $('#play-span').removeClass("glyphicon-pause").addClass("glyphicon-repeat");
                }
            }
        }, controlers.delay);
    }
}


function isBufferNeeded(currentval) {
    if (currentLoadedEnd == timeSeriesLength) {
        return false;
    }
    return (currentval == (precurrentLoadedStart + Math.floor((precurrentLoadedEnd - precurrentLoadedStart)/2))) ?  true : false;
}
function checkAndBufferData(currentval){
    if(isBufferNeeded(currentval)){
        bufferData();
    }
}


function bufferData(){
    var loadend = timeSeriesLength
    if(timeSeriesLength > precurrentLoadedEnd + controlers.loadSize){
        loadend = precurrentLoadedEnd + controlers.loadSize;
    }
    loadPlotData(precurrentLoadedEnd,loadend)
    for(var i =0; i < (loadend - precurrentLoadedEnd); i++){
        delete particleSets[precurrentLoadedStart+i];
        delete sectionSets[precurrentLoadedStart+i];
    }
    precurrentLoadedStart += controlers.loadSize;
    precurrentLoadedEnd += controlers.loadSize;
    if(precurrentLoadedEnd > timeSeriesLength){
        precurrentLoadedEnd = timeSeriesLength
    }
    checkIfBuffered();
}

function checkIfBuffered() {
    setTimeout(function () {
        if (Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < controlers.maxPlotsStored) {
            checkIfBuffered();
        } else {
            currentLoadedStart += controlers.loadSize;
            currentLoadedEnd += controlers.loadSize;
            if (currentLoadedEnd > timeSeriesLength) {
                currentLoadedEnd = timeSeriesLength
            }
            var currentValue = $("#slider").slider("value");
            if(currentValue + 1 > (currentLoadedStart + Math.floor((currentLoadedEnd - currentLoadedStart)/2))){
                bufferData();
            }
            if(isPlaying && isPaused){
                isPaused = false;
                playLoop();

            }
        }
    }, 1000);
}

function animateTimeSeriesPause() {
    isPaused = true
    isPlaying = false
}

function resetView() {
    controls.reset();
}

function resetSlider() {
    isPaused = true;
    isPlaying = false;
    //$("#plot-slider").attr("value", 0);
    plotRangeSlider.update({from: 0});
    updatePlot(0);
}

var controlers = {
    delay: 300,
    pointsize: 1,
    loadSize: 5,
    maxPlotsStored: 20
};



function setupGuiSingle() {
    var gui = new dat.GUI({ autoPlace: false });
    var customContainer = document.getElementById('plot-controls');
    customContainer.appendChild(gui.domElement);
    gui.add(controlers, 'pointsize', 0.01, 5.0, 1.0).name("Point Size").onFinishChange(changePointSize)
    gui.add(controlers, "position", 1.0, 40.0, 1.0)
    gui.add(controlers, 'loadsize', 1.0, 50.0, 1.0)
    //h.add( effectController, "shininess", 1.0, 400.0, 1.0 ).name( "shininess" ).onChange( render );
}

function setupGuiTimeSeries() {
    var gui = new dat.GUI({ autoPlace: false });
    var customContainer = document.getElementById('plot-controls');
    customContainer.appendChild(gui.domElement);

    gui.add(controlers, 'delay', 10.0, 2000.0, 50.0).name( "Play Delay(ms)")
    gui.add(controlers, 'pointsize', 0.01, 5.0, 1.0).name("Point Size").onFinishChange(changePointSize)

    //var storage = gui.addFolder('Storage Controls');
    //
    //storage.add(controlers, "loadSize", 1.0, 20.0, 1.0).name("Load Size").di
    //storage.add(controlers, 'maxPlotsStored', 10.0, 200.0, 1.0).name("Storage Size")

    //h.add( effectController, "shininess", 1.0, 400.0, 1.0 ).name( "shininess" ).onChange( render );
}

function changePointSize(){
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            currentParticles[key].material.size = (sections[key].size/200) * controlers.pointsize;
            currentParticles[key].material.needsUpdate = true;
        }
    }
    render();
}