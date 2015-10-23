/* varibles*/
//TODO color changes not applied to data loaded later
//TODO need to add reply option to make the slider go back to the start and reload data from the start
//TODO Colors in the single view dont seem to work
//TODO the loop should not depend on the cluster sort

//Three js global varibles
var camera, scene, renderer, sprite, colors = [], particles = [], material, controls, light, currentParticles = [];
var container, stats;
var heus = [0.05, 0.3, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95];
var scene3d;
var colors = [];
var colorlist = [];
var sections = [];


//Single Plot Varibles
var clusterUrl;
var clusters;
var resultSetId;
var resultData;

//Time Series Vars
var particleSets = {};
var timeSeriesData = [];
var isPlaying = false;
var isPaused = false;
var resultSets;
var removedclusters = [];
var recoloredclusters = [];
var currentLoadedStart, currentLoadedEnd, timeSeriesLength;

//Constants
var TIME_BETWEEN_PLOTS_IN_MILLS = 300;
var MAX_PLOTS_STORED = 4;
var LOAD_SIZE = 2;

//Generate the check box list for clusters
function generateCheckList(list, initcolors) {

    var tabletop = "<table class='table table-striped' id='cluster_table' style='background-color: #EAEAEA; padding-top: 0px'>"
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

    for (i = 0; i < list.length; i++) {
        tablerows += "<tr class='even pointer'>"
            + "<td class='a-center'>"
            + "<input type='checkbox' class='flat' name='table_records' checked value='" + i + "'>"
            + "<label class='color-box-label'>" + i + "</label> "
            + "<div class='input-group color-pic1' style='width: 15px;height: 15px; display: inline-flex; padding-left: 20px;padding-top: 2px'>"
            + "<input value='" + initcolors[i] + "' class='form-control' type='hidden' id='" + i + "'>"
            + "<span class='input-group-addon'><i style='background-color: rgb(1, 343, 69);'></i></span>"
            + "</div>"
            + "</td>"
            + "<td class=' '>C" + i + "</span></td>"
            + "<td class=' '>121</td>"
            + "</tr>"
    }

    var tableend = "</tbody>"
        + "</table>"


    return tabletop + tablerows + tableend;
}

//Plot functions
function visualize(resultSetUrl, resultSet, id) {
    clusterUrl = resultSetUrl;
    clusters = resultSet.clusters;
    resultSetId = id;
    generateGraph();
    animate();
}

function visualizeTimeSeries(resultSetUrl, timeSeries, id) {
    resultSets = timeSeries.resultsets;
    timeSeriesLength = resultSets.length;
    generateTimeSeries(resultSets);
}


function generateGraph() {

    setupThreeJs();

    var cluster;
    var geometry = [];
    for (var i = 0; i < clusters.length; i++) {
        geometry.push(new THREE.Geometry());
        colors.push(new Array());
        particles.push(new Array());
    }
    var hsl;
    var sections = [];
    $.getJSON(clusterUrl, function (data) {
        for (var i = 0; i < clusters.length; i++) {
            var clusterdata = data.clusters[i];
            var clusterid = clusterdata.clusterid;

            hsl = [heus[clusterid], 1, 0.8];

            for (var k in clusterdata.points) {
                var p = clusterdata.points[k];

                //var hsl = [heus[data.cid], 1, 0.8];
                var vertex = new THREE.Vector3(p.x , p.y , p.z);
                geometry[clusterid].vertices.push(vertex);

                //TODO can cchange this
                if (sections.indexOf(clusterid) == -1)
                    sections.push(clusterid);

                colors[clusterid].push(new THREE.Color(0xffffff).setHSL(hsl[0], hsl[1], hsl[2]));

            }
        }
        for (var i in geometry) {
            colorlist.push(colors[i][0].getHexString());
            geometry[i].colors = colors[i];
            particles[i] = new THREE.PointCloud(geometry[i], material);
            scene3d.add(particles[i]);

        }
        window.document.getElementById("cluster_table_div").innerHTML = generateCheckList(sections, colorlist);
        stats.domElement.style.position = 'absolute';
        document.getElementById("stats").appendChild(stats.domElement);
        window.addEventListener('resize', onWindowResize, false);
        $('.color-pic1').colorpicker();
        render();
        animate();
    });
    setupMatrial()
    animate();

}

function generateTimeSeries(resultSets) {
    setupThreeJs();
    currentLoadedStart = 0;

    if(timeSeriesLength > MAX_PLOTS_STORED){
        loadPlotData(0,MAX_PLOTS_STORED)
        currentLoadedEnd = MAX_PLOTS_STORED
    }else{
        loadPlotData(0,timeSeriesLength)
        currentLoadedEnd = timeSeriesLength
    }
    setupMatrial();
    initBufferAndLoad();

}

function loadPlotData(start,end){
    var cluster;
    var geometry = [];
    var hsl;

    for (var i = start; i < end; i++) {

        clusterUrl = "/resultssetall/" + resultSets[i].id;
        $.getJSON(clusterUrl, function (data) {
            particles = [];
            colors = [];
            geometry = [];
            sections = [];
            clusters = data.clusters;
            for (var i = 0; i < clusters.length; i++) {
                geometry.push(new THREE.Geometry());
                colors.push(new Array());
                particles.push(new Array());
                var clusterdata = data.clusters[i];
                var clusterid = clusterdata.clusterid;

                hsl = [heus[clusterid], 1, 0.8];

                for (var k in clusterdata.points) {
                    var p = clusterdata.points[k];

                    //var hsl = [heus[data.cid], 1, 0.8];
                    var vertex = new THREE.Vector3(p.x, p.y, p.z);
                    geometry[clusterid].vertices.push(vertex);

                    //TODO can change this
                    if (sections.indexOf(clusterid) == -1)
                        sections.push(clusterid);

                    colors[clusterid].push(new THREE.Color(0xffffff).setHSL(hsl[0], hsl[1], hsl[2]));

                }
            }

            for (var i in geometry) {
                colorlist.push(colors[i][0].getHexString());
                geometry[i].colors = colors[i];
                particles[i] = new THREE.PointCloud(geometry[i], material);
                //scene3d.add(particles[i]);

            }

            particleSets[data.timeSeriesSeqNumber] = particles;


        });

    }
}


//Util functions

function initPlotData(){
    $("#slider").slider("option", "max", timeSeriesLength-1);
    $("#slider").slider("option", "value", $("#slider").slider("value"));
    currentParticles = particleSets["0"];
    for (var i = 0; i < currentParticles.length; i++) {
        scene3d.add(currentParticles[i]);
    }
    window.document.getElementById("cluster_table_div").innerHTML = generateCheckList(sections, colorlist);
    $('.color-pic1').colorpicker();
}

function initBufferAndLoad(){

    setTimeout(function () {
        if(Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < MAX_PLOTS_STORED){
            initBufferAndLoad();
        }else{
            if (currentLoadedStart in particleSets) {
                initPlotData();
                render();
                animate();
            }
        }
    }, 1000);
}

function gotoBufferAndLoad(event, ui){

    setTimeout(function () {
        if(Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < MAX_PLOTS_STORED){
            gotoBufferAndLoad(event, ui);
        }else{
            if (currentLoadedStart in particleSets) {
                updatePlot(event, ui)
                render();
                animate();
            }
        }
    }, 1000);
}

function setupThreeJs(){
    renderer = null;
    particles = [];
    colors = [];
    controls = null;
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
    camera = new THREE.PerspectiveCamera(45, canvasWidth / canvasHeight, 0.1, 10000);
    camera.name = 'camera';
    camera.position.set(1, 1, 1);
    scene3d.add(camera);
    controls = new THREE.OrbitControls(camera, renderer.domElement);
    stats.domElement.style.position = 'absolute';
    document.getElementById("stats").appendChild(stats.domElement);
    window.addEventListener('resize', onWindowResize, false);

}

function setupMatrial(){
    material = new THREE.PointCloudMaterial({
        size: 0.003,
        map: sprite,
        vertexColors: THREE.VertexColors,
        transparent: true
    });
    material.color.setHSL(1.0, 1, 1);
}

function updatePlot(event, ui) {
    var sliderValue = ui.value;
    if(sliderValue >= currentLoadedStart && sliderValue < currentLoadedEnd){
        if (sliderValue in particleSets) {
            scene3d = new THREE.Scene();
            scene3d.add(camera);
            $("#slider").slider("option", "value", $("#slider").slider("value"));
            currentParticles = particleSets[sliderValue];
            for (var i = 0; i < currentParticles.length; i++) {
                if (!(removedclusters.hasOwnProperty(i))) {
                    scene3d.add(currentParticles[i]);
                }
            }
            window.addEventListener('resize', onWindowResize, false);
            render();
            animate();
            $("#amount").val(sliderValue);
        }
    }else {
        for(var k = 0; k < (currentLoadedEnd - currentLoadedStart); k++){
            delete particleSets[currentLoadedStart + k];
        }
        currentLoadedStart = sliderValue;
        currentLoadedEnd = timeSeriesLength
        if(timeSeriesLength > (currentLoadedStart + MAX_PLOTS_STORED)){
            currentLoadedEnd = currentLoadedStart + MAX_PLOTS_STORED;
        }
        //TODO check if this might fail in edge cases
        loadPlotData(currentLoadedStart, currentLoadedEnd)
        gotoBufferAndLoad(event, ui);
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
//TODO WInodow rezise does not work yet need to fix
function onWindowResize() {
    //var width = $('#canvas3d').width();
    //var height = $('#canvas3d').height();
    //camera.aspect = width / height;
    //camera.updateProjectionMatrix();
    //renderer.setSize(width, height);
    //controls.handleResize();
    //render();
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

    for (var i in colors[id]) {
        colors[id][i] = new THREE.Color(color);
    }
    recoloredclusters[id] = colors[id];
    currentParticles[id].geometry.colors = colors[id];
    currentParticles[id].geometry.colorsNeedUpdate = true;
}

function animateTimeSeriesPlay(){
    isPlaying = true;
    isPaused = false;
    playLoop();
}

function playLoop() {
    var currentValue = $("#slider").slider("value");
    var maxValue = currentLoadedEnd-1;
    checkAndBufferData(currentValue+1)
    if(currentValue == maxValue){
        $('#slider-play').removeClass("fa fa-pause").addClass("fa fa-play-circle");
        return
    }
    if(isBufferNeeded(currentValue)){
        isPaused = true;
        isPlaying = false;
    }else {
        setTimeout(function () {
            scene3d = new THREE.Scene();
            scene3d.add(camera);
            $("#slider").slider("option", "value", currentValue + 1);
            currentParticles = particleSets[currentValue + 1];
            for (var i = 0; i < currentParticles.length; i++) {
                if (recoloredclusters.hasOwnProperty(i)) {
                    currentParticles[i].geometry.colors = recoloredclusters[i];
                    currentParticles[i].geometry.colorsNeedUpdate = true;
                }
                if (!(removedclusters.hasOwnProperty(i))) {
                    scene3d.add(currentParticles[i]);
                }

            }
            window.addEventListener('resize', onWindowResize, false);
            $("#amount").val(currentValue + 1);
            render();
            if (maxValue > currentValue + 1 && !isPaused) {
                playLoop();
            }else{
                isPaused = true;
                isPlaying = false;
                $('#slider-play').removeClass("fa fa-pause").addClass("fa fa-play-circle");
            }
        }, TIME_BETWEEN_PLOTS_IN_MILLS);
    }
}


function isBufferNeeded(currentval){
    if(currentLoadedEnd == timeSeriesLength){
        return false;
    }
    return (currentval == (currentLoadedStart + Math.floor((currentLoadedEnd - currentLoadedStart)/2))) ?  true : false;
}
function checkAndBufferData(currentval){
    if(isBufferNeeded(currentval)){
        var loadend = timeSeriesLength
        if(timeSeriesLength > currentLoadedEnd + LOAD_SIZE){
            loadend = currentLoadedEnd + LOAD_SIZE;
        }
        loadPlotData(currentLoadedEnd,loadend)
        for(var i =0; i < (loadend - currentLoadedEnd); i++){
            delete particleSets[currentLoadedStart+i];
        }
        checkIfBuffered();
    }
}

function checkIfBuffered(){
    setTimeout(function () {
        if(Object.keys(particleSets).length < timeSeriesLength && Object.keys(particleSets).length < MAX_PLOTS_STORED){
            checkIfBuffered();
        }else{
            currentLoadedStart += LOAD_SIZE;
            currentLoadedEnd += LOAD_SIZE;
            if(currentLoadedEnd > timeSeriesLength){
                currentLoadedEnd = timeSeriesLength
            }
            if(isPaused){
                isPaused = false;
                isPlaying = true;
                playLoop();

            }
        }
    }, 1000);
}

function  animateTimeSeriesPause(){
    isPaused = true
}