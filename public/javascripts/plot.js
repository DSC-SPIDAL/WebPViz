//Constants
var MAX_PLOTS_STORED = 20;

/* varibles*/
//Three js global varibles
var camera, scene, renderer, controls, light;
var container, stats;
var scene3d;
var publicUrl = false;
// keep the settings object around
var allSettings = {};
var settingOn = false;

// Color controls
var colors = [];
var colorlist = {};
var trueColorList = {};
var colorsLoaded = false;
var colorSchemes = {};
var currentCustomColorScheme = null;

// Particle
var sections = [], particles = [], currentParticles = [];

var sprites = {};
var trajSprites = {};
var particleSets = {};
var lineSets = {};
var sectionSets = {};
var changedGlyphs = {};
var changedSizes ={};
var customclusters = {};
var customclusternotadded = false;
var customclusternotaddedtolist = false;
var pointLabelxKey = {};
var pointLabelxKeySets = {};
var maxClusterId = 0;
var plotPointsSets = {};
var plotPoints = {};

var xmeantotal = 0, ymeantotal = 0, zmeantotal = 0;
var xmean = 0, ymean = 0, zmean = 0, cameraCenter, calculatedmeans = false;

//Single Plot Varibles
var clusterUrl;
var clusters;
var resultSetId;
var timeseriesId;
var fileName;
var uploader;

//Time Series Vars
var fileNames = {};
var resultSets;
var removedclusters = [];
var recoloredclusters = [];
var realpaedclusters = [];
var timeSeriesLength;
var plotDesc;

//Play controls
var bufferLoopStarted = false;
var plotRangeSlider = {};
var speed = 300;
var glyphSize = 1.0;
var pointSize = 1.0;

var bufferRequestMade = {};        // track the requests made to get data to be buffered
var currentPlotUpdated = false;    // make sure we don't render the same plot multiple times

var infoPage = false;
var artifactName = "";
var desc = "";
var group = "";

// save the scenes so that we can free them
var scenes = {};

// keep track of the information needed to do trajectories
var trajectoryPointLabels = [];
var trajectoryPoints = {};
var trajectoryLimit = 10;
var totalTrajectoryPoints = 5;
var trajectoryPointSizeRatio = 10;
// keep track of the cluster IDs created for the trajectory
var trajectoryToClusterId = {};

// raw data sets coming from back-end. these will be converted to threejs format
var dataSets = {};

var playEnum = {
    INIT: "init",
    PLAY: "play",
    PAUSE: "pause"
};
var playStatus = playEnum.INIT;

ImageEnum = {
    BALL: "/assets/images/textures1/ball.png",
    CUBE: "/assets/images/textures1/cube.png",
    DISC: "/assets/images/textures1/disc.png",
    STAR: "/assets/images/textures1/star.png",
    PYRAMID: "/assets/images/textures1/pyramid.png",
    CONE: "/assets/images/textures1/cone.png",
    CYLINDER: "/assets/images/textures1/cylinder.png"
};

ImageTrajEnum = {
    BALL: "/assets/images/textures1/traj/ball.png",
    CUBE: "/assets/images/textures1/traj/cube.png",
    DISC: "/assets/images/textures1/traj/disc.png",
    STAR: "/assets/images/textures1/traj/star.png",
    PYRAMID: "/assets/images/textures1/traj/pyramid.png",
    CONE: "/assets/images/textures1/traj/cone.png",
    CYLINDER: "/assets/images/textures1/traj/cylinder.png"
};

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
var isTimeSeries = false;
var reInitialize = false;

//Plot functions - These are the methods that are first called when a plot is generated
function visualize(resultSetUrl, artifact, fid, tid, info) {
    info = typeof info !== 'undefined' ? info : false;
    infoPage = info;
    clusterUrl = resultSetUrl;
    resultSetId = fid;
    timeseriesId = tid;
    artifactName = artifact.name;
    desc = artifact.desc;
    group = artifact.group;
    isTimeSeries = false;
    setupThreeJs();
    $("#progress").css({display: "block"});
    intialSetup(artifact.settings, false);
    generateGraph();
    setupGuiSingle();
}


function visualizeTimeSeries(resultSetUrl, artifact, id, pub, info) {
    info = typeof info !== 'undefined' ? info : false;
    infoPage = info;
    clusterUrl = resultSetUrl;
    publicUrl = pub;
    timeseriesId = id;
    resultSets = artifact.files;
    timeSeriesLength = resultSets.length;
    artifactName = artifact.name;
    desc = artifact.desc;
    group = artifact.group;
    isTimeSeries = true;

    setupThreeJs();
    $("#progress").css({display: "block"});
    intialSetup(artifact.settings, false);
    initPlotData();
    generateTimeSeries(resultSets);
    setupGuiTimeSeries();
}

// we will move the time series to begining
function reInitGraph() {
    $("#progress").css({display: "block"});
    currentParticles = [];
    colorlist = {};
    trueColorList = {};
    setupThreeJs();
    intialSetup(allSettings, true);
    // initPlotData();
    generateGraph();
    updateSingleGui();
    reInitialize = false;
}

// we will move the time series to begining
function reInitTimeSeries() {
    plotRangeSlider.update({from: 0});
    var span = $("#play-span");
    span.removeClass("glyphicon-pause").addClass("glyphicon-play");
    $("#progress").css({display: "block"});
    currentParticles = [];
    colorlist = [];
    trueColorList = [];
    setupThreeJs();
    intialSetup(allSettings, true);
    initPlotData();
    updateTimeSeriesGui();
    generateTimeSeries(resultSets, true);
    reInitialize = false;
}

function animate1() {
    if (!reInitialize) {
        requestAnimationFrame(animate1);
        controls.update();
        stats.update();
        var camera = scene3d.getObjectByName('camera');
        renderer.render(scene3d, camera);
    } else {
        if (isTimeSeries) {
            reInitTimeSeries();
        } else {
            reInitGraph();
        }
    }
}

//Init methods

function setupThreeJs() {
    renderer = null;
    particles = [];
    colors = [];
    controls = null;
    if (!infoPage) {
        var height = window.innerHeight - 57 - 40 - 40 - 10;
        $('#canvas3d').width(window.innerWidth - 45);
        $('#canvas3d').height(height);
    } else {
        var height = (window.innerHeight - 57 - 40 - 40 - 10)/2;
        $('#canvas3d').width((window.innerWidth - 45)/2 -10);
        $('#canvas3d').height(height);
    }
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
    cameraCenter = new THREE.Vector3(0, 0, 0);
    camera = new THREE.PerspectiveCamera(45, canvasWidth / canvasHeight, .01, 10000);
    camera.name = 'camera';
    camera.position.set(1, 1, 1);
    camera.lookAt(cameraCenter);
    scene3d.add(camera);
    controls = new THREE.TrackballControls(camera, renderer.domElement);
    controls.staticMoving = true;
    controls.rotateSpeed = 20.0;
    controls.dynamicDampingFactor = 0.3;
    initColorSchemes();
    sprites["0"] = THREE.ImageUtils.loadTexture(ImageEnum.DISC);
    sprites["1"] = THREE.ImageUtils.loadTexture(ImageEnum.BALL);
    sprites["2"] = THREE.ImageUtils.loadTexture(ImageEnum.STAR);
    sprites["3"] = THREE.ImageUtils.loadTexture(ImageEnum.CUBE);
    sprites["4"] = THREE.ImageUtils.loadTexture(ImageEnum.PYRAMID);
    sprites["5"] = THREE.ImageUtils.loadTexture(ImageEnum.CONE);
    sprites["6"] = THREE.ImageUtils.loadTexture(ImageEnum.CYLINDER);

    trajSprites["0"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.DISC);
    trajSprites["1"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.BALL);
    trajSprites["2"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.STAR);
    trajSprites["3"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.CUBE);
    trajSprites["4"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.PYRAMID);
    trajSprites["5"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.CONE);
    trajSprites["6"] = THREE.ImageUtils.loadTexture(ImageTrajEnum.CYLINDER);
    window.addEventListener('resize', onWindowResize, false);
}

function intialSetup(settings, reinit) {
    colorsLoaded = false;
    // check weather we have camera
    if (settings) {
        var sett;
        if (settings.selected) {
            if (settings.selected == "original" && !reinit) {
                // clear the originals
                settings.settings['original'] = {};
                sett = settings.settings['original'];
            } else {
                // new settings
                sett = settings.settings[settings.selected];
            }
            allSettings = settings;
        } else {
            allSettings['tid'] = timeseriesId;
            allSettings['fid'] = resultSetId;
            allSettings['settings'] = {};
            allSettings['settings']['original'] = settings;
            allSettings['selected'] = 'original';
            sett = settings;
        }

        if (sett.glyphSize) {
            glyphSize = sett.glyphSize;
        }
        if (sett.pointSize) {
            pointSize = sett.pointSize;
        }
        if (sett.speed) {
            speed = sett.speed;
        }
        if (sett.camera) {
            var c = sett.camera;
        }
        if (sett.cameraup) {
            var up = sett.cameraup;
            camera.up.set(up.x, up.y, up.z);
        }
        if (sett.glyphs) {
            changedGlyphs = sett.glyphs;
        }
        if (sett.changedSizes) {
            changedSizes = sett.changedSizes;
        }
        if (sett.customclusters) {
            customclusters = sett.customclusters;
        }
        if (sett.camerastate) {
            var cameraState = sett.camerastate;
            camera.matrix.fromArray(JSON.parse(cameraState));
            camera.matrix.decompose(camera.position, camera.quaternion, camera.scale);
        }
        camera.updateProjectionMatrix();
        var colors = sett.clusterColors;
        if (colors) {
            for (var key in colors) {
                if (colors.hasOwnProperty(key)) {
                    var clustercolor = colors[key];
                    if (clustercolor) {
                        colorlist[key] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();
                        trueColorList[key] = clustercolor;
                    }
                }
            }
            colorsLoaded = true;
        }
    } else {
        allSettings['tid'] = timeseriesId;
        allSettings['fid'] = resultSetId;
        allSettings['settings'] = {};
        allSettings['settings']['original'] = {};
        allSettings['selected'] = 'original';
    }

    controlers.pointsize = pointSize;
    controlers.glyphsize = glyphSize;
    controlers.delay = speed;
    controlers.settings = allSettings.selected;
}

function initPlotData() {
    plotRangeSlider.update({max: timeSeriesLength - 1, min: 0, from: 0});
    currentParticles = particleSets["0"];
    camera.lookAt(scene3d.position);
    camera.updateProjectionMatrix();
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            scene3d.add(currentParticles[key]);
        }
    }
}

function initColorSchemes() {
    colorSchemes['mathlab50'] = ["#ffffff", "#ff0000", "#00ff00", "#ff1ab9", "#ffd300", "#0084f6", "#008d46", "#a7613e", "#00fff6", "#3e7b8d", "#eda7ff", "#d3ff95", "#b94fff",
        "#e51a58", "#848400", "#00ff95", "#ffedff", "#f68412", "#caff00", "#0035c1", "#ffca84", "#9e728d", "#4fb912", "#9ec1ff", "#959e7b", "#ff7bb0", "#9e0900", "#ffb9b9",
        "#8461ca", "#9e0072", "#84dca7", "#ff00f6", "#00d3ff", "#ff7258", "#583e35", "#d3d3d3", "#dc61dc", "#6172b0", "#b9ca2c", "#545454", "#5800ca", "#95c1ca", "#d39e23",
        "#84b058", "#e5edb9", "#f6d3ff", "#8d09a7", "#6a4f00", "#003e9e", "#7b3e7b"]
    colorSchemes['colorbrewer9'] = ["#e41a1c", "#377eb8", "#4daf4a", "#984ea3", "#ff7f00", "#ffff33", "#a65428", "#f781bf", "#999999"]
    colorSchemes['colorbrewerpaired12'] = ["#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928"]
    colorSchemes['salsa17'] = ["#0000ff", "#ffaaff", "#aa5500", "#aa55ff", "#00ffdb", "#ffff7f", "#778899", "#55aa7f", "#49ff00", "#550000", "#dbff00", "#ffdb00", "#ff9200",
        "#aaffff", "#ff0000", "#c0c0c0", "#ffffff"]
}

// Generates Single plot graphs
function generateGraph() {
    var geometry = {};

    $.getJSON(clusterUrl, function (data) {
        fileName = data.file;
        plotDesc = data.desc;
        uploader = data.uploader;
        maplabelstokeys(data.points);
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
                    clustercolor = {"r": 255, "g": 255, "b": 255, "a": 255};
                    if (clusterdata.r) {
                        clustercolor["r"] = clusterdata.r[3];
                        clustercolor["g"] = clusterdata.r[2];
                        clustercolor["b"] = clusterdata.r[1];
                        clustercolor["a"] = clusterdata.r[0];
                        trueColorList[clusterid] = clustercolor;
                    } else {
                        trueColorList[clusterid] = {};
                    }
                } else {
                    clustercolor = trueColorList[clusterid];
                }

                if (!sections.hasOwnProperty(clusterid)) {
                    sections[clusterid] = {
                        "length": clusterdata.p.length,
                        "size": clusterdata.s,
                        "shape": clusterdata.f,
                        "visible": clusterdata.v,
                        "color": clustercolor,
                        "label": clusterdata.l,
                        'traj': false
                    };
                }

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

        xmeantotal = xmeantotal / clusterCount;
        ymeantotal = ymeantotal / clusterCount;
        zmeantotal = zmeantotal / clusterCount;
        scene3d = new THREE.Scene();
        scene3d.add(camera);

        for (var key in geometry) {
            if (geometry.hasOwnProperty(key)) {
                geometry[key].translate(-xmeantotal, -ymeantotal, -zmeantotal);
                currentParticles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size, sections[key].shape, false,sections[key].color.a, false));
                if (changedGlyphs.hasOwnProperty(key)) {
                    currentParticles[key].material.map = sprites[changedGlyphs[key]];
                    currentParticles[key].material.needsUpdate = true;
                }
            }
        }

        renderCustomCluster();
        addParticlesToScence();
        var linesegs = drawEdges(data.edges, points, pointcolors);
        scene3d.add(linesegs);
        generateClusterList(sections, colorlist);
        populatePlotInfo();
        var cls = $("#plot-clusters").isotope({
            itemSelector: '.element-item',
            layoutMode: 'fitRows',
            containerStyle: null
        });

        window.addEventListener('resize', onWindowResize, true);
        changeGlyphSize();
        changePointSize();
        animate1();
        savePlotSettings(controlers.settings);
        itemsLoaded = totalItemsToLoad;
        $("#progress").css({display: "none"});
    });
}

//Generates TimeSeries Plots
function generateTimeSeries(resultSets, reInit) {
    playStatus = playEnum.PAUSE;
    if (reInit) {
        clearThreeJS(timeSeriesLength, timeSeriesLength);
    }
    initBufferAndLoad();
    if (!reInit) {
        playLoop();
    }
}

function drawEdges(edges, points, pointcolors) {
    if (edges == null || edges == undefined)
        return;

    var geometry = new THREE.BufferGeometry();
    var material = new THREE.LineBasicMaterial({vertexColors: THREE.VertexColors});
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
                var pointkey = verkey;
                var point = points[parseInt(pointkey)];
                var pointcolor = pointcolors[parseInt(pointkey)];
                var vertex = new THREE.Vector3(point[0], point[1], point[2]);
                if (previousvertex != null) {
                    if (isFirst) {
                        isFirst = false
                    } else {
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
            }
        }
    }


    var positions32 = new Float32Array(positions.length);
    var colorarray32 = new Float32Array(colorarray.length);
    positions32.set(positions);
    colorarray32.set(colorarray);
    geometry.addAttribute('position', new THREE.BufferAttribute(positions32, 3));
    geometry.addAttribute('color', new THREE.BufferAttribute(colorarray32, 3));
    geometry.translate(-xmeantotal, -ymeantotal, -zmeantotal);
    var linesegs = line = new THREE.LineSegments(geometry, material);
    return linesegs;
    //scene3d.add(linesegs)
}

function convertDataToThreeJsFormat2(data) {
    particles = {};
    colors = {};
    var plotPoints = {};
    pointLabelxKey = {};
    var geometry = {};
    clusters = data.clusters;
    fileName = data.file;
    plotDesc = data.desc;
    uploader = data.uploader;
    var points = {};
    var pointcolors = {};
    var edgeIndex = 0;
    var edges = {};
    // we will create a separate point list for trajectories
    var trajectoryPointIndex = 0;

    var localSections = [];
    var clusterCount = 0;
    var currentHighestClusterId = 0;
    for (var cid in clusters) {
        if (data.clusters.hasOwnProperty(cid)) {
            var tempCid = parseInt(cid);
            if (tempCid > currentHighestClusterId) {
                currentHighestClusterId = tempCid;
            }
        }
    }
    var upperCaseTrajectoryPointLabels = trajectoryPointLabels.map(function(value) {
        return value.toUpperCase();
    });

    var highetClusterId = currentHighestClusterId;
    for (var cid in clusters) {
        if (data.clusters.hasOwnProperty(cid)) {
            clusterCount++;
            var clusterdata = data.clusters[cid];
            var clusterid = parseInt(cid);
            setMaxClusterId(clusterid);
            var clustercolor;
            if (!colorsLoaded) {
                clustercolor = {"r": 0, "g": 0, "b": 0, "a": 255};
                if (clusterdata.r) {
                    clustercolor["r"] = clusterdata.r[3];
                    clustercolor["g"] = clusterdata.r[2];
                    clustercolor["b"] = clusterdata.r[1];
                    clustercolor["a"] = clusterdata.r[0];
                    trueColorList[clusterid] = clustercolor;
                } else {
                    trueColorList[clusterid] = {};
                }
            } else {
                clustercolor = trueColorList[clusterid];
            }
            if (clustercolor == null)
                clustercolor = {"a": 255, "b": randomRBG(), "g": randomRBG(), "r": randomRBG()};

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
                "label": clusterdata.l,
                'traj': false
            };
            if (!sections.hasOwnProperty(clusterid)) {
                sections[clusterid] = localSection;
            }
            localSections[clusterid] = localSection;

            xmean = 0;
            ymean = 0;
            zmean = 0;
            var k = 0;
            var trajectoryNumber = 0;
            if (trajectoryLimit >= 0) {
                trajectoryNumber = trajectoryLimit;
            } else {
                trajectoryNumber = -1;
            }
            var count = clusterdata.p.length;
            var positions = new Float32Array(count * 3);
            var colorarray = new Float32Array(count * 3);

            for (var pointIndex = 0; pointIndex < clusterdata.p.length; pointIndex++) {
                var p = findPoint(data, clusterdata.p[pointIndex]);
                if (!p) {
                    continue;
                }
                var p0 = parseFloat(p[0]);
                var p1 = parseFloat(p[1]);
                var p2 = parseFloat(p[2]);
                var label = p[3];
                plotPoints[clusterdata.p[pointIndex]] = [p[0], p[1], p[2]];
                pointLabelxKey[p[3]] = clusterdata.p[pointIndex];
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

                var pointPerElements = 1;
                if (trajectoryLimit < 0) {
                    pointPerElements = Math.round(Math.ceil(timeSeriesLength / totalTrajectoryPoints));
                } else if (totalTrajectoryPoints > trajectoryLimit) {
                    pointPerElements = 1;
                } else if (totalTrajectoryPoints < trajectoryLimit) {
                    pointPerElements = Math.round(Math.ceil(trajectoryLimit / totalTrajectoryPoints));
                }

                if (upperCaseTrajectoryPointLabels.indexOf(label.toUpperCase()) >= 0) {
                    var trajectoryList = trajectoryPoints[label];
                    var edge = {};
                    var edgeVerteces = [];
                    var currentClusterId = 0;
                    // check weather there is a cluster id for this trajectory
                    if (!trajectoryToClusterId[label.toUpperCase()]) {
                        currentHighestClusterId = currentHighestClusterId + 1;
                        currentClusterId = currentHighestClusterId;
                        trajectoryToClusterId[label.toUpperCase()] = currentHighestClusterId;

                        if (!geometry.hasOwnProperty(currentClusterId)) {
                            geometry[currentClusterId] = new THREE.BufferGeometry();
                            particles[currentClusterId] = [];
                        }
                        // we will add some extra points to cluster
                        localSection = {
                            "length": clusterdata.p.length,
                            "size": trajectoryPointSizeRatio,
                            "shape": clusterdata.f,
                            "visible": clusterdata.v,
                            "color": clustercolor,
                            "label": label,
                            'traj': true
                        };
                        if (!sections.hasOwnProperty(currentClusterId)) {
                            sections[currentClusterId] = localSection;
                        }
                        trueColorList[currentClusterId] = clustercolor;
                        if (!colorlist.hasOwnProperty(currentClusterId)) {
                            colorlist[currentClusterId] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();
                        }
                        localSections[currentClusterId] = localSection;
                    } else {
                        currentClusterId = trajectoryToClusterId[label.toUpperCase()];
                        clustercolor = trueColorList[currentClusterId];
                    }

                    if (!trajectoryList) {
                        trajectoryList = [];
                        trajectoryPoints[label] = trajectoryList;
                    } else {
                        if (!geometry.hasOwnProperty(currentClusterId)) {
                            geometry[currentClusterId] = new THREE.BufferGeometry();
                            particles[currentClusterId] = [];
                        }
                        // we will add some extra points to cluster
                        localSection = {
                            "length": clusterdata.p.length,
                            "size": trajectoryPointSizeRatio,
                            "shape": clusterdata.f,
                            "visible": clusterdata.v,
                            "color": clustercolor,
                            "label": label,
                            'traj': true
                        };
                        if (!sections.hasOwnProperty(currentClusterId)) {
                            sections[currentClusterId] = localSection;
                        }
                        trueColorList[currentClusterId] = clustercolor;
                        if (!colorlist.hasOwnProperty(currentClusterId)) {
                            colorlist[currentClusterId] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();
                        }
                        localSections[currentClusterId] = localSection;
                        var c = 0;
                        var pointPlaceStartingIndex = Math.round(Math.ceil(pointPerElements / 2));
                        var indexStarted = false;
                        var startingIndex = 0;
                        var trajectoryPointCount = 0;
                        for (var z = 0; z < trajectoryList.length; z++) {
                            if (trajectoryNumber >= 0 && z < trajectoryList.length - trajectoryNumber) {
                                continue;
                            } else if (!indexStarted) {
                                indexStarted = true;
                                startingIndex = z;
                            }
                            var tp = trajectoryList[z];
                            if (!tp) {
                                continue;
                            }
                            if (((z - startingIndex) - pointPlaceStartingIndex) % pointPerElements == 0) {
                                trajectoryPointCount++;
                            }
                        }
                        var positionsTrajec = new Float32Array(trajectoryPointCount * 3);
                        var colorarrayTrajec = new Float32Array(trajectoryPointCount * 3);
                        for (var z = 0; z < trajectoryList.length; z++) {
                            if (trajectoryNumber >= 0 && z < trajectoryList.length - trajectoryNumber) {
                                continue;
                            } else if (!indexStarted) {
                                indexStarted = true;
                                startingIndex = z;
                            }
                            var tp = trajectoryList[z];
                            if (!tp) {
                                continue;
                            }
                            var pp0 = parseFloat(tp.p[0]);
                            var pp1 = parseFloat(tp.p[1]);
                            var pp2 = parseFloat(tp.p[2]);
                            var ptempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")");
                            if (((z - startingIndex) - pointPlaceStartingIndex) % pointPerElements == 0) {
                                positionsTrajec[c * 3 + 0] = pp0;
                                positionsTrajec[c * 3 + 1] = pp1;
                                positionsTrajec[c * 3 + 2] = pp2;

                                colorarrayTrajec[c * 3 + 0] = ptempcolor.r;
                                colorarrayTrajec[c * 3 + 1] = ptempcolor.g;
                                colorarrayTrajec[c * 3 + 2] = ptempcolor.b;
                                c++;
                            }

                            points[trajectoryPointIndex] = [pp0, pp1, pp2];
                            pointcolors[trajectoryPointIndex] = ptempcolor;
                            edgeVerteces.push(trajectoryPointIndex);

                            trajectoryPointIndex++;
                        }
                        geometry[currentClusterId].addAttribute('position', new THREE.BufferAttribute(positionsTrajec, 3));
                        geometry[currentClusterId].addAttribute('color', new THREE.BufferAttribute(colorarrayTrajec, 3));
                    }

                    points[trajectoryPointIndex] = [p0, p1, p2];
                    pointcolors[trajectoryPointIndex] = tempcolor;
                    edgeVerteces.push(trajectoryPointIndex + "");
                    trajectoryPointIndex++;

                    var trajectory = {};
                    trajectory['p'] = p;
                    trajectory['c'] = clustercolor;
                    trajectoryList[data.seq] = trajectory;

                    edge['v'] = edgeVerteces;
                    edges[edgeIndex] = edge;
                    edgeIndex++;
                }
                k++;
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


    if (!calculatedmeans) {
        xmeantotal = xmeantotal / clusterCount;
        ymeantotal = ymeantotal / clusterCount;
        zmeantotal = zmeantotal / clusterCount;
        calculatedmeans = true;
    }

    for (var key in geometry) {
        if (geometry.hasOwnProperty(key)) {
            geometry[key].translate(-xmeantotal, -ymeantotal, -zmeantotal);
            if (key > highetClusterId) {
                particles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size, sections[key].shape, false, sections[key].color.a, true));
            } else {
                particles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size, sections[key].shape, false, sections[key].color.a, false));
            }
        }
    }

    lineSets[data.seq] = drawEdges(edges, points, pointcolors);

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
}

function convertDataToThreeJsFormat(data) {
    particles = {};
    colors = {};
    var plotPoints = {};
    pointLabelxKey = {};
    var geometry = {};
    clusters = data.clusters;
    fileName = data.file;
    plotDesc = data.desc;
    uploader = data.uploader;
    var points = {};
    var pointcolors = {};
    var edgeIndex = 0;
    var edges = {};
    // we will create a separate point list for trajectories
    var trajectoryPointIndex = 0;

    var localSections = [];
    var clusterCount = 0;
    var currentHighestClusterId = 0;
    for (var cid in clusters) {
        if (data.clusters.hasOwnProperty(cid)) {
            var tempCid = parseInt(cid);
            if (tempCid > currentHighestClusterId) {
                currentHighestClusterId = tempCid;
            }
        }
    }
    var upperCaseTrajectoryPointLabels = trajectoryPointLabels.map(function(value) {
        return value.toUpperCase();
    });

    var pointPerElements = 1;
    if (trajectoryLimit < 0) {
        pointPerElements = Math.round(Math.ceil(timeSeriesLength / totalTrajectoryPoints));
    } else if (totalTrajectoryPoints > trajectoryLimit) {
        pointPerElements = 1;
    } else if (totalTrajectoryPoints < trajectoryLimit) {
        pointPerElements = Math.round(Math.ceil(trajectoryLimit / totalTrajectoryPoints));
    }

    var highetClusterId = currentHighestClusterId;
    for (var cid in clusters) {
        if (data.clusters.hasOwnProperty(cid)) {
            clusterCount++;
            var clusterdata = data.clusters[cid];
            var clusterid = parseInt(cid);
            setMaxClusterId(clusterid);
            var clustercolor;
            if (!colorsLoaded) {
                clustercolor = {"r": 0, "g": 0, "b": 0, "a": 255};
                if (clusterdata.r) {
                    clustercolor["r"] = clusterdata.r[3];
                    clustercolor["g"] = clusterdata.r[2];
                    clustercolor["b"] = clusterdata.r[1];
                    clustercolor["a"] = clusterdata.r[0];
                    trueColorList[clusterid] = clustercolor;
                } else {
                    trueColorList[clusterid] = {};
                }
            } else {
                clustercolor = trueColorList[clusterid];
            }
            if (clustercolor == null)
                clustercolor = {"a": 255, "b": randomRBG(), "g": randomRBG(), "r": randomRBG()};

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
                "label": clusterdata.l,
                'traj': false
            };
            if (!sections.hasOwnProperty(clusterid)) {
                sections[clusterid] = localSection;
            }
            localSections[clusterid] = localSection;

            xmean = 0;
            ymean = 0;
            zmean = 0;
            var k = 0;
            var trajectoryNumber = 0;
            if (trajectoryLimit >= 0) {
                trajectoryNumber = trajectoryLimit;
            } else {
                trajectoryNumber = -1;
            }
            var count = clusterdata.p.length;
            // reduce the points that belong to a trajectory
            for (var pointIndex = 0; pointIndex < clusterdata.p.length; pointIndex++) {
                var p = findPoint(data, clusterdata.p[pointIndex]);
                if (!p) {
                    continue;
                }
                var label = p[3];
                if (upperCaseTrajectoryPointLabels.indexOf(label.toUpperCase()) >= 0) {
                    count--;
                }
            }
            var positions = new Float32Array(count * 3);
            var colorarray = new Float32Array(count * 3);
            for (var pointIndex = 0; pointIndex < clusterdata.p.length; pointIndex++) {
                var p = findPoint(data, clusterdata.p[pointIndex]);
                if (!p) {
                    continue;
                }
                var p0 = parseFloat(p[0]);
                var p1 = parseFloat(p[1]);
                var p2 = parseFloat(p[2]);
                var label = p[3];
                var tempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")");

                // regular point
                if (upperCaseTrajectoryPointLabels.indexOf(label.toUpperCase()) < 0) {
                    plotPoints[clusterdata.p[pointIndex]] = [p[0], p[1], p[2]];
                    pointLabelxKey[p[3]] = clusterdata.p[pointIndex];
                    positions[k * 3 + 0] = p0;
                    positions[k * 3 + 1] = p1;
                    positions[k * 3 + 2] = p2;

                    if (!calculatedmeans) {
                        xmean += p0;
                        ymean += p1;
                        zmean += p2;
                    }
                    colorarray[k * 3 + 0] = tempcolor.r;
                    colorarray[k * 3 + 1] = tempcolor.g;
                    colorarray[k * 3 + 2] = tempcolor.b;
                    k++;
                } else {
                    // trajectory point
                    var trajectoryList = trajectoryPoints[label];
                    var edge = {};
                    var edgeVerteces = [];
                    var currentClusterId = 0;
                    // check weather there is a cluster id for this trajectory
                    if (!trajectoryToClusterId[label.toUpperCase()]) {
                        currentHighestClusterId = currentHighestClusterId + 1;
                        currentClusterId = currentHighestClusterId;
                        trajectoryToClusterId[label.toUpperCase()] = currentHighestClusterId;

                        if (!geometry.hasOwnProperty(currentClusterId)) {
                            geometry[currentClusterId] = new THREE.BufferGeometry();
                            particles[currentClusterId] = [];
                        }
                        // we will add some extra points to cluster
                        localSection = {
                            "length": clusterdata.p.length,
                            "size": trajectoryPointSizeRatio,
                            "shape": clusterdata.f,
                            "visible": clusterdata.v,
                            "color": clustercolor,
                            "label": label,
                            'traj': true
                        };
                        if (!sections.hasOwnProperty(currentClusterId)) {
                            sections[currentClusterId] = localSection;
                        }
                        trueColorList[currentClusterId] = clustercolor;
                        if (!colorlist.hasOwnProperty(currentClusterId)) {
                            colorlist[currentClusterId] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();
                        }
                        localSections[currentClusterId] = localSection;
                    } else {
                        currentClusterId = trajectoryToClusterId[label.toUpperCase()];
                        clustercolor = trueColorList[currentClusterId];
                    }

                    if (!trajectoryList) {
                        trajectoryList = [];
                        trajectoryPoints[label] = trajectoryList;
                    }

                    var trajectory = {};
                    trajectory['p'] = p;
                    trajectory['c'] = clustercolor;
                    trajectoryList[data.seq] = trajectory;

                    if (!geometry.hasOwnProperty(currentClusterId)) {
                        geometry[currentClusterId] = new THREE.BufferGeometry();
                        particles[currentClusterId] = [];
                    }
                    // we will add some extra points to cluster
                    localSection = {
                        "length": clusterdata.p.length,
                        "size": trajectoryPointSizeRatio,
                        "shape": clusterdata.f,
                        "visible": clusterdata.v,
                        "color": clustercolor,
                        "label": label,
                        'traj': true
                    };
                    if (!sections.hasOwnProperty(currentClusterId)) {
                        sections[currentClusterId] = localSection;
                    }
                    trueColorList[currentClusterId] = clustercolor;
                    if (!colorlist.hasOwnProperty(currentClusterId)) {
                        colorlist[currentClusterId] = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")").getHexString();
                    }
                    localSections[currentClusterId] = localSection;
                    var c = 0;
                    var pointPlaceStartingIndex = Math.round(Math.ceil(pointPerElements / 2));
                    var indexStarted = false;
                    var startingIndex = 0;
                    var trajectoryPointCount = 0;
                    for (var z = 0; z < trajectoryList.length; z++) {
                        if (trajectoryNumber >= 0 && z < trajectoryList.length - trajectoryNumber) {
                            continue;
                        } else if (!indexStarted) {
                            indexStarted = true;
                            startingIndex = z;
                        }
                        var tp = trajectoryList[z];
                        if (!tp) {
                            continue;
                        }
                        if (((z - startingIndex) - pointPlaceStartingIndex) % pointPerElements == 0) {
                            trajectoryPointCount++;
                        }
                    }
                    var positionsTrajec = new Float32Array(trajectoryPointCount * 3);
                    var colorarrayTrajec = new Float32Array(trajectoryPointCount * 3);
                    var ptempcolor = new THREE.Color("rgb(" + clustercolor.r + "," + clustercolor.g + "," + clustercolor.b + ")");

                    var startingColor = Color.rgb(clustercolor.r, clustercolor.g, clustercolor.b);
                    var hsl = startingColor.hslData();
                    var h = hsl[0];
                    var s = hsl[1];
                    var l = hsl[2];
                    var saturationIncrement = 1.0 / trajectoryPointCount;
                    for (var z = 0; z < trajectoryList.length; z++) {
                        if (trajectoryNumber >= 0 && z < trajectoryList.length - trajectoryNumber) {
                            continue;
                        } else if (!indexStarted) {
                            indexStarted = true;
                            startingIndex = z;
                        }
                        var tp = trajectoryList[z];
                        if (!tp) {
                            continue;
                        }
                        var pp0 = parseFloat(tp.p[0]);
                        var pp1 = parseFloat(tp.p[1]);
                        var pp2 = parseFloat(tp.p[2]);
                        s = c * saturationIncrement;
                        var newColor = Color.hsl(h, s, l);
                        var newRgb = newColor.rgbData();

                        if (((z - startingIndex) - pointPlaceStartingIndex) % pointPerElements == 0) {
                            positionsTrajec[c * 3 + 0] = pp0;
                            positionsTrajec[c * 3 + 1] = pp1;
                            positionsTrajec[c * 3 + 2] = pp2;
                            colorarrayTrajec[c * 3 + 0] = newRgb[0];
                            colorarrayTrajec[c * 3 + 1] = newRgb[1];
                            colorarrayTrajec[c * 3 + 2] = newRgb[2];
                            c++;
                        }

                        points[trajectoryPointIndex] = [pp0, pp1, pp2];
                        pointcolors[trajectoryPointIndex] =  new THREE.Color("rgb(" + newRgb[0] + "," + newRgb[1] + "," + newRgb[2] + ")");
                        edgeVerteces.push(trajectoryPointIndex);

                        trajectoryPointIndex++;
                    }
                    geometry[currentClusterId].addAttribute('position', new THREE.BufferAttribute(positionsTrajec, 3));
                    geometry[currentClusterId].addAttribute('color', new THREE.BufferAttribute(colorarrayTrajec, 3));
                    points[trajectoryPointIndex] = [p0, p1, p2];
                    pointcolors[trajectoryPointIndex] = tempcolor;
                    edgeVerteces.push(trajectoryPointIndex + "");
                    trajectoryPointIndex++;
                    edge['v'] = edgeVerteces;
                    edges[edgeIndex] = edge;
                    edgeIndex++;
                }
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


    if (!calculatedmeans) {
        xmeantotal = xmeantotal / clusterCount;
        ymeantotal = ymeantotal / clusterCount;
        zmeantotal = zmeantotal / clusterCount;
        calculatedmeans = true;
    }

    for (var key in geometry) {
        if (geometry.hasOwnProperty(key)) {
            geometry[key].translate(-xmeantotal, -ymeantotal, -zmeantotal);
            if (key > highetClusterId) {
                particles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size, sections[key].shape, false, sections[key].color.a, true));
            } else {
                particles[key] = new THREE.Points(geometry[key], loadMatrial(sections[key].size, sections[key].shape, false, sections[key].color.a, false));
            }
        }
    }

    lineSets[data.seq] = drawEdges(edges, points, pointcolors);

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
}

// get the current points and check weather the given label exists
function checkLabelExists(labelList) {
    var currentValue = parseInt($("#plot-slider").prop("value"));
    if (currentValue in dataSets && dataSets[currentValue]) {
        var data = dataSets[currentValue];
        var clusters = data.clusters;
        var upperCaseTrajectoryPointLabels = labelList.map(function(value) {
            return value.toUpperCase();
        });
        var foundLabels = [];
        for (var cid in clusters) {
            if (data.clusters.hasOwnProperty(cid)) {
                var clusterdata = data.clusters[cid];
                for (var pointIndex = 0; pointIndex < clusterdata.p.length; pointIndex++) {
                    var p = findPoint(data, clusterdata.p[pointIndex]);
                    if (!p) {
                        continue;
                    }
                    var label = p[3];
                    if (upperCaseTrajectoryPointLabels.indexOf(label.toUpperCase()) >= 0 && foundLabels.indexOf(label) < 0) {
                        foundLabels.push(label.toUpperCase());
                    }
                }
            }
        }
        return arrDiff(upperCaseTrajectoryPointLabels, foundLabels);
    }
}

function arrDiff(a1, a2) {
    var a = [], diff = [];
    for (var i = 0; i < a1.length; i++) {
        a[a1[i]] = true;
    }
    for (var i = 0; i < a2.length; i++) {
        if (a[a2[i]]) {
            delete a[a2[i]];
        } else {
            a[a2[i]] = true;
        }
    }
    for (var k in a) {
        diff.push(k);
    }
    return diff;
};

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
        (function (i) {
            $.getJSON(clusterUrl, function (data) {
                // convertDataToThreeJsFormat(data);
                dataSets[data.seq] = data;
            }).fail(function () {
                bufferRequestMade[i] = false;
            });
        })(i);
    }
}

function loadMatrial(size, shape, isglyph, alpha, traj) {
    var sprite;
    if (!isglyph) {
        sprite = null;
    }

    var spr = {};
    if (traj) {
        spr = trajSprites;
    } else {
        spr = sprites;
    }

    if (size > 1) {
        switch (parseInt(shape)) {
            case 0:
                sprite = spr["0"];
                break;
            case 1:
                sprite = spr["1"];
                break;
            case 2:
                sprite = spr["2"];
                break;
            case 3:
                sprite = spr["3"];
                break;
            case 4:
                sprite = spr["4"];
                break;
            case 5:
                sprite = spr["5"];
                break;
            case 6:
                sprite = spr["6"];
                break;
            default :
                sprite = spr["3"];
        }
    }
    var opacity = 1.0;
    if(alpha != null){
        opacity = Math.precision(alpha/255,2);
    }
    var material = new THREE.PointsMaterial({
        size: size / 200,
        map: sprite,
        vertexColors: THREE.VertexColors,
        transparent: true,
        opacity: opacity
    });
    return material;
}

/**
 * This function will try to render the plot with the index
 * If this plot is not loaded yet, it will simply do nothing.
 * It is the loading functions responsibility to load the data required
 * @param index
 */
function updatePlot(index) {
    if (index in dataSets && dataSets[index]) {
        convertDataToThreeJsFormat(dataSets[index]);
        // console.log("update plot: " + index);
        for (var i = 0; i < index - 1; i++) {
            var sc = scenes[i];

            if (sc) {
                $.each(sc.children, function(idx, obj) {
                    if (obj !== undefined) {
                        if (obj.geometry) {
                            obj.geometry.dispose();
                        }

                        if (obj.material) {
                            if (obj.material instanceof THREE.MeshFaceMaterial) {
                                $.each(obj.material.materials, function(idx, o) {
                                    o.dispose();
                                });
                            } else {
                                obj.material.dispose();
                            }
                        }

                        if (obj.dispose) {
                            obj.dispose();
                        }
                    }
                });

                while (sc.children.length > 0) {
                    sc.remove(sc.children[sc.children.length - 1]);
                }
            }
            scenes[i] = null;
        }

        scene3d = new THREE.Scene();
        scene3d.add(camera);
        scenes[index] = scene3d;
        currentParticles = particleSets[index];
        plotPoints = plotPointsSets[index];
        pointLabelxKey = pointLabelxKeySets[index];
        var localSection = sectionSets[index];
        sections = localSection;
        renderCustomCluster();
        for (var key in currentParticles) {
            if (currentParticles.hasOwnProperty(key)) {

                if (controlers.pointsize != 1 || controlers.glyphsize != 1) {
                    if (sections[key].size == 1) {
                        currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
                    } else {
                        currentParticles[key].material.size = (sections[key].size / 200) * controlers.glyphsize;
                    }
                    currentParticles[key].material.needsUpdate = true;
                }

                if(changedSizes.hasOwnProperty(key)){
                    currentParticles[key].material.size = (changedSizes[key] / 200) * controlers.glyphsize;
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
                    var opacity = Math.precision(trueColorList[key].a/255,2)
                    if(realpaedclusters.hasOwnProperty(key)){
                        opacity = Math.precision(realpaedclusters[key]/255,2)
                    }
                    currentParticles[key].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
                    currentParticles[key].geometry.colorsNeedUpdate = true;
                    currentParticles[key].material.opacity = opacity;
                    currentParticles[key].material.transparent = true;
                    currentParticles[key].material.needsUpdate = true;

                }

                if (changedGlyphs.hasOwnProperty(key)) {
                    currentParticles[key].material.map = sprites[changedGlyphs[key]];
                    currentParticles[key].material.needsUpdate = true;
                }
                if (!(removedclusters.hasOwnProperty(key))) {
                    scene3d.add(currentParticles[key]);
                }
            }
        }
        if (lineSets[index]) {
            scene3d.add(lineSets[index]);
        }
        // change only when the setting dispaly is on
        if (settingOn) {
            generateCheckList(sections, colorlist);
        }
        generateClusterList(sections, colorlist);
        fileName = fileNames[index];
        populatePlotInfo();
        sections = localSection;
        window.addEventListener('resize', onWindowResize, false);
        // render();
        animate();
        $("#plot-title").text(fileNames[index]);
        //savePlotSettings(controlers.settings);
        return true;
    } else {
        return false;
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
                // render();
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
        if (Object.keys(dataSets).length < timeSeriesLength && Object.keys(dataSets).length < MAX_PLOTS_STORED) {
            if (timeSeriesLength > MAX_PLOTS_STORED) {
                loadPlotData(0, MAX_PLOTS_STORED);
            } else {
                loadPlotData(0, timeSeriesLength);
            }
            initBufferAndLoad();
        } else {
            updatePlot(0);
            animate1();
            $("#progress").css({display: "none"});
            if (!bufferLoopStarted) {
                bufferLoopStarted = true;
                bufferLoop(null);
            }
        }
    }, controlers.delay);
}

function clearThreeJS(loadStartIndex, loadend) {
    for (var i = 0; i < loadStartIndex; i++) {
        if (bufferRequestMade[i]) {
            delete bufferRequestMade[i];
        }
        if (particleSets[i]) {
            var cp = particleSets[i];
            for (var key in cp) {
                if (cp.hasOwnProperty(key)) {
                    if (cp[key].material) {
                        cp[key].material.dispose();
                        cp[key].material = null;
                    }
                    if (cp[key].geometry) {
                        cp[key].geometry.dispose();
                        cp[key].geometry = null;
                    }
                }
            }
            delete particleSets[i];
            particleSets[i] = null;
        }
        if (dataSets[i]) {
            delete dataSets[i];
            dataSets[i] = null;
        }
        if (plotPointsSets[i]) {
            delete plotPointsSets[i];
            plotPointsSets[i] = null
        }
        if (pointLabelxKeySets[i]) {
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
            var cp = particleSets[i];
            for (var key in cp) {
                if (cp.hasOwnProperty(key)) {
                    if (cp[key].material) {
                        cp[key].material.dispose();
                    }
                    if (cp[key].geometry) {
                        cp[key].geometry.dispose();
                    }
                }
            }
            delete particleSets[i];
        }
        if (dataSets[i]) {
            delete dataSets[i];
            dataSets[i] = null;
        }
        if (plotPointsSets[i]) {
            delete plotPointsSets[i];
            plotPointsSets[i] = null
        }
        if (sectionSets[i]) {
            delete sectionSets[i];
        }
        if (pointLabelxKeySets[i]) {
            delete pointLabelxKeySets[i];
            pointLabelxKeySets[i] = null
        }
    }
}

function bufferLoop(indx) {
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
        clearThreeJS(loadStartIndex, loadend);
        loadPlotData(loadStartIndex, loadend);
        if (playStatus == playEnum.PAUSE && !currentPlotUpdated) {
            if (indx && indx != currentIndex) {
                updatePlot(currentIndex);
                currentPlotUpdated = true;
            }
        }
        bufferLoop(indx);

    }, controlers.delay * controlers.loadSize / 2);
}

function animateTimeSeriesPause() {
    playStatus = playEnum.PAUSE;
}

function resetSlider() {
    playStatus = playEnum.PAUSE;
    plotRangeSlider.update({from: 0});
}

var controlers = {
    delay: 300,
    pointsize: 1,
    glyphsize: 1,
    loadSize: 10,
    maxPlotsStored: 20,
    settings: "chrome"
};

function changePointSize() {
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

function changeGlyphSize() {
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            if (sections[key].size == 1) {
                currentParticles[key].material.size = (sections[key].size / 200) * controlers.pointsize;
            } else {
                currentParticles[key].material.size = (sections[key].size / 200) * controlers.glyphsize;
            }

            if(changedSizes.hasOwnProperty(key)){
                currentParticles[key].material.size = (changedSizes[key] / 200) * controlers.glyphsize;
            }

            currentParticles[key].material.needsUpdate = true;
        }
    }
    render();
}



function savePlot() {
    var res = false;
    $('#setting-exist')
        .find('option')
        .remove()
        .end();
    var kys = Object.keys(allSettings.settings);
    var selectKey = allSettings.selected;
    $.each(kys, function (i, item) {
        $('#setting-exist').append($('<option>', {
            value: item,
            text: item
        }));
    });
    $('#setting-exist').append($('<option>', {
        value: 'new',
        text: 'New Settings'
    }));
    $('#setting-new').prop("disabled", true);
    $('#setting-exist').val(selectKey);
    $('#saveModal').modal('show');
}

function saveSettingSelectChange() {
    if ($('#setting-exist').val() === "new") {
        $('#setting-new').prop("disabled", false);
    } else {
        $('#setting-new').prop("disabled", true);
    }
}

function callSave() {
    var url = '/timeseries/save ';
    $.ajax({
        type: "POST",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        data: JSON.stringify(allSettings),
        url: url,
        success: function (data) {
            $('#saveModal').modal('hide');
        },
        error: function (data) {
            $('#saveModal').modal('hide');
        }
    });
}

function savePlotSettings(result) {
    res = result;
    if (res) {
        controlers.settings = result;
        allSettings['tid'] = timeseriesId;
        allSettings['fid'] = resultSetId;
        var sett = allSettings.settings;
        allSettings['selected'] = res;
        var c = camera.toJSON();
        var obj = {};
        obj['camera'] = camera.toJSON();
        obj['tid'] = timeseriesId;
        obj['fid'] = resultSetId;
        obj['pointSize'] = controlers.pointsize;
        obj['glyphSize'] = controlers.glyphsize;
        obj['changedSizes'] = changedSizes;
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
        sett[res] = obj;
    }
}


//Html Content Generators

/**
 * Generates the Information box content
 */
function populatePlotInfo() {
    if (!infoPage) {
        document.getElementById('plot-info-description').innerHTML = "<b>Name: " + artifactName + "</br>" + "<b>Frame: </b>" + fileName + "</br>" +
            "<b>Desc: </b>" + desc + "</br>" +
            "<b>Group: </b>" + group;
    } else {
        $("#np").text(Object.keys(plotPoints).length);
        $("#nc").text(sections.length);
        if (timeSeriesLength) {
            $("#nf").text(timeSeriesLength)
        }
    }
}

/**
 * Generate the check box list for clusters
 * @param list sections
 * @param initcolors color list
 * @returns {string} generated check list in HTML
 */
function generateCheckList(list, initcolors) {
    if (infoPage) return;

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
        var sprite = getGlyphName(list[key]);
        if (sprite != null) {
            glyphList.push(key);
        } else {
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
    var found = false;
    var tabletop,tablerows,tableend

    tabletop = "<table class='table table-striped table-bordered responsive-utilities jambo_table bulk_action' id='cluster_table'>"
        + "<thead>"
        + "<tr class='headings'>"
        + "<th>"
        + "<input type='checkbox' id='check-all' class='flat' checked> Cluster"
        + "</th>"
        + "<th class='column-title' >Label</th>"
        + "<th class='column-title'>Cluster Count</th>"
        + "</tr>"
        + "</thead>"
        + "<tbody>";

        var ss = $("#cluster_table");
    if($("#cluster_table").length && !customclusternotadded){
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            if (!(removedclusters.hasOwnProperty(key))) {
                $("#cluster_table tbody > #" + key + " input:checkbox").prop('checked', true);
            } else {
                $("#cluster_table > tbody > #" + key + " input:checkbox").prop('checked', false);
            }
            var sprite = getGlyphName(list[key]);
            var currentshape;
            if (changedGlyphs.hasOwnProperty(key)) {
                currentshape = changedGlyphs[key];
            }else{
                currentshape = list[key].shape;
            }
            $("#cluster_table > tbody > #" + key + " span#color-picker-addon").attr('style', "background-color:#" + initcolors[key])
            $("#cluster_table > tbody > #" + key + " span#color-picker-addon").attr('alpha', trueColorList[key].a)
            if(sprite != null){
                $("#cluster_table > tbody > #" + key + " select").val(currentshape);
                $("#cluster_table > tbody > #" + key + " td#cluster-size label#size-label").text(list[key].length)
                $("#cluster_table > tbody > #" + key + " td#cluster-size input").attr('value',list[key].size)
            }else{
                $("#cluster_table > tbody > #" + key + " td#cluster-size label").text(list[key].length)
            }
        }
        found =  true;
    }else {

        tablerows = "";
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            tablerows += "<tr class='even pointer' id='" + key + "'>"
                + "<td class='a-center'>";
            if (!(removedclusters.hasOwnProperty(key))) {
                tablerows += "<input type='checkbox' class='flat' name='table_records' checked value='" + key + "'>";
            } else {
                tablerows += "<input type='checkbox' class='flat' name='table_records' value='" + key + "'>";
            }
            var sprite = getGlyphName(list[key]);
            tablerows += "<label class='color-box-label'>" + key + "</label> "
                + "<div class='input-group' style='width: 15px;height: 15px; display: inline-flex; float: right;padding-right: 20px;'>"
                + "<input value='" + initcolors[key] + "' class='form-control color-pic1' type='hidden' key='" + key + "' id='color-box" + key + "'>"
                + "<span id='color-picker-addon' value='" + key + "' alpha='"+trueColorList[key].a+"' class='color-picker-addon' style='background-color:#" + initcolors[key] + "'></span>"
                + "</div>"
                + "</td>";
            if (sprite != null) {
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

                tablerows += "<td class='l1' id='cluster-size'><label id='size-label'>" + list[key].length
                    + "</label><div class='glyph-size'><label class='glyph-size-label'>Size</label><input type='text' class='glyph-size-control' value='" + list[key].size + "' key='" + key + "' id='size-box" + key + "'></div>"
                    + "</td>"
                    + "</tr>";
            } else {
                tablerows += "<td class=' '><span>" + list[key].label + "</span></td>";
                tablerows += "<td class='l1' id='cluster-size'><label id='size-label'>" + list[key].length + "</label></td>"
                    + "</tr>";
            }

        }

        tableend = "</tbody>"
            + "</table>";
        customclusternotadded = false;
    }

    if(!found){
        document.getElementById('cluster_table_div').innerHTML = tabletop + tablerows + tableend;
        enablesearch()
    }
    return tabletop + tablerows + tableend;
}

/**
 * Geneates the cluster list
 * @param list
 * @param initcolors
 * @returns {string} generated list in HTML
 */
function generateClusterList(list, initcolors) {
    if (infoPage) return;

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
    var found = false;
    if (list && nonEmptyList.length < 50) {
        for (var i = 0; i < nonEmptyList.length; i++) {
            var key = nonEmptyList[i];
            if (list.hasOwnProperty(key)) {
                var colorWithouthHash = initcolors[key].replace(/#/g, '');
                var sprite = null;
                if (changedGlyphs.hasOwnProperty(key)) {
                    sprite = getFontIconByShape(changedGlyphs[key]);
                } else{
                    if (list[key].size > 1) {
                        sprite = getFontIconByShape(list[key].shape);
                    }
                }
                // try to find the element first
                if ($("#plot-clusters > #" + key).length && !customclusternotaddedtolist && trajectoryPointLabels.length == 0) {
                    if ($("#plot-clusters > #" + key + " span").length) {
                        if (sprite != null) {
                            $("#pc" + key).css("background-color", "#ffffff");
                            $("#pcs").text(list[key].label + ":" + list[key].length + "   ");
                            $("#pcs").append("<i class='demo-icon " + sprite + "' style='font-size: 1em; color:#"+ colorWithouthHash +"'></i>");
                        } else {
                            var rgb = hexToRgb("#" + colorWithouthHash);
                            var tex = "ffffff";
                            if (rgb.r + rgb.g + rgb.b > (255 * 3 - (rgb.r + rgb.g + rgb.b))) {
                                tex = "000000";
                            }
                            $("#pc" + key).css("color", "#" + tex);
                            $("#pcs" + key).css("background-color", "#" + colorWithouthHash);
                            $("#pcs" + key).text(list[key].label + ":" + list[key].length);
                        }
                    }
                    found = true;
                } else {
                    found = false;
                    if (sprite != null) {
                        if (list[key]['traj']) {
                            grid += "<div class='element-item transition metal' data-category='transition' id='pc" + key + "' style='background-color: #ffffff'>" +
                                "<p style='font-size: 0.8em'><span style='font-weight: bold' id='pcs" + key + "'><i class='fa fa-ellipsis-h'></i>" + list[key].label + "<i class='demo-icon " + sprite + "' style='font-size: 1em; color:#" + colorWithouthHash + "'></i>" + "</span></p></div>"
                        } else {
                            grid += "<div class='element-item transition metal' data-category='transition' id='pc" + key + "' style='background-color: #ffffff'>" +
                                "<p style='font-size: 0.8em'><span style='font-weight: bold' id='pcs" + key + "'>" + list[key].label + ":" + list[key].length + "<i class='demo-icon " + sprite + "' style='font-size: 1em; color:#" + colorWithouthHash + "'></i>" + "</span></p></div>"
                        }
                    } else {
                        var rgb = hexToRgb("#" + colorWithouthHash);
                        var tex = "ffffff";
                        if (rgb.r + rgb.g + rgb.b > (255 * 3 - (rgb.r + rgb.g + rgb.b))) {
                            tex = "000000";
                        }
                        grid += "<div class='element-item transition metal' data-category='transition' id='pc" + key + "' style='background-color: #" + colorWithouthHash + " '>" +
                            "<p style='font-size: 0.8em'><span style='font-weight: bold;color: #" + tex + "' id='pcs"+key+"'>" + list[key].label + ":" + list[key].length + "</span></p></div>"
                    }
                }
            }
        }
    }
    if (!found) {
        document.getElementById('plot-clusters').innerHTML = grid;
        customclusternotaddedtolist = false;
    }
    return grid;
}

function hexToRgb(hex) {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

//Control Utils

function resetView() {
    controls.reset();
}


function recolorSection(id, color, alpha) {
    if (id == "cccolor") return;
    if (id == "multi") {
        recolorMultipleSections(color,alpha)
        return;
    }

    colorlist[id] = color;
    var opacity = Math.precision(alpha/255,3)

    //remove the color jpicker binding
    $("#cluster_table tbody > #" + id +" span.jPicker").remove();
    $("#cluster_table > tbody > #" + id + " span#color-picker-addon").attr('style', "background-color:#" + color)
    $("#cluster_table > tbody > #" + id + " span#color-picker-addon").attr('alpha', alpha)
    $("#cluster_table > tbody > #" + id + " span#color-picker-addon").removeClass('settinghidden');

    //change to custom color scheme since a color change has been made
    currentCustomColorScheme = colorlist;
    if ($("#color-scheme").val() != "custom") {
        $("#color-scheme").val('custom');
    }

    color = "#" + color
    var tempcolor = new THREE.Color(color);
    trueColorList[id] = {
        "r": tempcolor.toArray()[0] * 255,
        "g": tempcolor.toArray()[1] * 255,
        "b": tempcolor.toArray()[2] * 255,
        "a": alpha
    };
    var colorattri = currentParticles[id].geometry.getAttribute('color');
    if (colorattri) {
        var colorsd = new Float32Array(colorattri.length);
        for (var k = 0; k < colorattri.length / 3; k++) {
            colorsd[k * 3 + 0] = tempcolor.r;
            colorsd[k * 3 + 1] = tempcolor.g;
            colorsd[k * 3 + 2] = tempcolor.b;
        }
    }
    currentParticles[id].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
    sections[id].color.a = alpha;
    generateClusterList(sections, colorlist);

    recoloredclusters[id] = new THREE.Color(color);
    realpaedclusters[id] = alpha;
    currentParticles[id].material.opacity = opacity;
    currentParticles[id].material.transparent = true;
    currentParticles[id].geometry.colorsNeedUpdate = true;
    currentParticles[id].material.needsUpdate = true;

}

function recolorMultipleSections(color,alpha) {
    var rows = $('#cluster_table tr.selected');
    for (var key in rows) {
        if (rows.hasOwnProperty(key)) {
            var rowdata = rows[key]
            var id = rowdata.id;
            if (id != undefined || id != null) recolorSection(id, color,alpha);
        }
    }
    generateCheckList(sections, colorlist);
}

/**
 * Changes the current color scheme of the cluster. Saves the Custom color scheme
 * @param scheme
 */
function changeColorScheme(scheme) {
    if (currentCustomColorScheme == null) {
        currentCustomColorScheme = jQuery.extend({}, colorlist);
    }

    if (scheme == 'custom') {
        colorlist = jQuery.extend({}, currentCustomColorScheme);
        for (var key in currentParticles) {
            if (currentParticles.hasOwnProperty(key)) {
                var tempcolor = new THREE.Color("#" + colorlist[key]);
                trueColorList[key] = {
                    "r": tempcolor.toArray()[0] * 255,
                    "g": tempcolor.toArray()[1] * 255,
                    "b": tempcolor.toArray()[2] * 255
                };

                var colorattri = currentParticles[key].geometry.getAttribute('color');
                var colorsd = new Float32Array(colorattri.length);
                for (var k = 0; k < colorattri.length / 3; k++) {
                    colorsd[k * 3 + 0] = tempcolor.r;
                    colorsd[k * 3 + 1] = tempcolor.g;
                    colorsd[k * 3 + 2] = tempcolor.b;
                }
                currentParticles[key].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
                recoloredclusters[key] = tempcolor
                currentParticles[key].geometry.colorsNeedUpdate = true;
            }
        }
    } else if (scheme == 'rainbow' || scheme == 'rainbowrev') {
        clusterCount = Object.keys(currentParticles).length;
        var count;
        if (scheme == 'rainbow') {
            count = 0
        } else {
            count = clusterCount - 1;
        }
        for (var key in currentParticles) {
            if (currentParticles.hasOwnProperty(key)) {
                var tempcolor = new THREE.Color(rainBowColors(count, clusterCount));
                colorlist[key] = tempcolor.getHexString();
                trueColorList[key] = {
                    "r": tempcolor.toArray()[0] * 255,
                    "g": tempcolor.toArray()[1] * 255,
                    "b": tempcolor.toArray()[2] * 255
                };

                var colorattri = currentParticles[key].geometry.getAttribute('color');
                var colorsd = new Float32Array(colorattri.length);
                for (var k = 0; k < colorattri.length / 3; k++) {
                    colorsd[k * 3 + 0] = tempcolor.r;
                    colorsd[k * 3 + 1] = tempcolor.g;
                    colorsd[k * 3 + 2] = tempcolor.b;
                }
                currentParticles[key].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
                recoloredclusters[key] = tempcolor
                currentParticles[key].geometry.colorsNeedUpdate = true;
                if (scheme == 'rainbow') {
                    count += 1;
                } else {
                    count -= 1;
                }
            }
        }
    } else {
        var colorScheme = colorSchemes[scheme];
        if (colorScheme == undefined || colorScheme == null) return

        var colorSchemeLength = colorScheme.length;
        var count = 0;
        for (var key in currentParticles) {
            if (currentParticles.hasOwnProperty(key)) {
                colorlist[key] = colorScheme[count % colorSchemeLength].substring(1);
                var tempcolor = new THREE.Color(colorScheme[count % colorSchemeLength]);
                trueColorList[key] = {
                    "r": tempcolor.toArray()[0] * 255,
                    "g": tempcolor.toArray()[1] * 255,
                    "b": tempcolor.toArray()[2] * 255
                };

                var colorattri = currentParticles[key].geometry.getAttribute('color');
                var colorsd = new Float32Array(colorattri.length);
                for (var k = 0; k < colorattri.length / 3; k++) {
                    colorsd[k * 3 + 0] = tempcolor.r;
                    colorsd[k * 3 + 1] = tempcolor.g;
                    colorsd[k * 3 + 2] = tempcolor.b;
                }
                currentParticles[key].geometry.addAttribute('color', new THREE.BufferAttribute(colorsd, 3));
                recoloredclusters[key] = tempcolor
                currentParticles[key].geometry.colorsNeedUpdate = true;
                count += 1;
            }
        }
    }
    generateCheckList(sections, colorlist);
    generateClusterList(sections, colorlist);

}

function removeSection(id) {
    scene3d.remove(currentParticles[id]);
    removedclusters[id] = id;
}

function removeAllSection() {
    removedclusters = [];
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            scene3d.remove(currentParticles[key]);
            removedclusters[key] = key;
        }
    }
}
function addAllSections() {
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

function addCustomCluster(isSingle) {
    var clusterLabel = $('#cclabel').val();
    var shape = $('#ccshape').val();
    var size = $('#ccsize').val();
    var color = $('#cccolor').val();
    var points = ($('#ccpoints').val()).split(",");

    var p = points;
    var clusterkey = maxClusterId + 1;
    setMaxClusterId(clusterkey);
    var cluster = {
        l: clusterLabel,
        f: shape,
        s: size,
        c: color,
        p: p
    };
    customclusters[clusterkey.toString()] = cluster;
    customclusternotadded = true;
    customclusternotaddedtolist = true;
    if (!isSingle) {
        var currentValue = parseInt($("#plot-slider").prop("value"));
        updatePlot(currentValue)
    } else {
        renderCustomCluster();
        generateCheckList(sections, colorlist);
        generateClusterList(sections, colorlist);
        addParticlesToScence()
    }
}

/**
 * Renders the custom cluster created and updates the plot
 */
function renderCustomCluster() {
    var geometry = {};
    var localSections = [];
    for (var cid in customclusters) {
        if (customclusters.hasOwnProperty(cid)) {
            var clusterdata = customclusters[cid];
            var clusterid = parseInt(cid)
            setMaxClusterId(clusterid);
            var clustercolor;

            if (!geometry.hasOwnProperty(clusterid)) {
                geometry[clusterid] = new THREE.BufferGeometry();
                particles[clusterid] = [];
            }

            if (!colorlist.hasOwnProperty(clusterid)) {
                var tempcolor = new THREE.Color(clusterdata.c);
                clustercolor = {
                    "r": tempcolor.toArray()[0] * 255,
                    "g": tempcolor.toArray()[1] * 255,
                    "b": tempcolor.toArray()[2] * 255
                };
                colorlist[clusterid] = clusterdata.c.substring(1);
                if (!trueColorList.hasOwnProperty(clusterid)) {
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
                var key = pointLabelxKey[clusterdata.p[k]];
                var p = plotPoints[key];
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
            geometry[key].translate(-xmeantotal, -ymeantotal, -zmeantotal);
            tempparticles = new THREE.Points(geometry[key], loadMatrial(sections[key].size, sections[key].shape, false, 1, false));

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


function changeGlyph(id, shape) {
    changedGlyphs[id] = shape;
    currentParticles[id].material.map = sprites[shape];
    currentParticles[id].material.needsUpdate = true;
    generateClusterList(sections, colorlist);
}

function changeSingleGlyphSize(id, size){
    currentParticles[id].material.size = (size / 200) * controlers.glyphsize;
    currentParticles[id].material.needsUpdate = true;
    sections[id].size = size;
    changedSizes[id] = size;
}

function changeMultipleGlyphSizes(size){
    var rows = $('#cluster_table tr.selected');
    for (var key in rows) {
        if (rows.hasOwnProperty(key)) {
            var rowdata = rows[key]
            var id = rowdata.id;
            if (id != undefined || id != null) changeSingleGlyphSize(id, size);
        }
    }
    generateCheckList(sections, colorlist);
}

function showSettings() {
    settingOn = true;
    generateCheckList(sections, colorlist);
}

function hideSettings() {
    settingOn = false;
}

function addParticlesToScence() {
    for (var key in currentParticles) {
        if (currentParticles.hasOwnProperty(key)) {
            scene3d.remove(currentParticles[key]);
            scene3d.add(currentParticles[key]);
        }
    }
}

function onWindowResize() {
    var width;
    var height;
    if (!infoPage) {
        width = window.innerWidth;
        height = window.innerHeight - 57 - 40 - 40 - 10;
    } else {
        width = window.innerWidth / 2 -10;
        height = (window.innerHeight - 57 - 40 - 40 - 10)/2;
    }
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width - 45, height);
    //controls.handleResize();
    render();
}

//General Util Methods

function progress() {
    var bar = 250;
    bar = Math.floor(bar * itemsLoaded / totalItemsToLoad);
    $("#bar").css({width: bar + "px"});
}

function getFontIconByShape(shape) {
    var glyph = null;
    switch (parseInt(shape)) {
        case 0:
            glyph = 'icon-disc';
            break;
        case 1:
            glyph = 'icon-circle';
            break;
        case 2:
            glyph = 'icon-star-1';
            break;
        case 3:
            glyph = 'icon-cube';
            break;
        case 4:
            glyph = 'icon-pyramid';
            break;
        case 5:
            glyph = 'icon-cone';
            break;
        case 6:
            glyph = 'icon-cylinder';
            break;
        default :
            glyph = 'icon-cylinder';
    }
    return glyph;
}

function getGlyphName(key) {
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

function checkIfSelected(key, shape, clusterkey) {
    if (changedGlyphs.hasOwnProperty(clusterkey)) {
        shape = changedGlyphs[clusterkey]
    }
    if (key == shape) {
        return "selected"
    } else {
        return ""
    }
}


function maplabelstokeys(points) {
    pointLabelxKey = {};
    plotPoints = {};
    for (var key in points) {
        if (points.hasOwnProperty(key)) {
            var p = points[key.toString()];
            pointLabelxKey[p[3]] = key;
            plotPoints[key] = [p[0], p[1], p[2]];
        }
    }
}

function findPoint(data, key) {
    return data.points[key.toString()];
}

function randomRBG() {
    return (Math.floor(Math.random() * (255 - 0 + 1)) + 0);
}

/**
 * Used to create new cluster id's for custom clusters
 * @param clusterid
 */
function setMaxClusterId(clusterid) {
    if (clusterid > maxClusterId) {
        maxClusterId = clusterid;
    }
}

function rainBowColors(length, maxLength) {

    var i = (length * 255 / maxLength);
    var r = Math.round(Math.sin(0.024 * i + 0) * 127 + 128);
    var g = Math.round(Math.sin(0.024 * i + 2) * 127 + 128);
    var b = Math.round(Math.sin(0.024 * i + 4) * 127 + 128);
    return 'rgb(' + r + ',' + g + ',' + b + ')';

}

//Three js Control methods

/**
 * Used to animate the plot
 */
function animate() {
    // we don't do anythin, need to remove this function and its references
}



function render() {
    // we don't do anythin, need to remove this function and its references
}

//Control Box Operations
var gui;

function updateSingleGui() {
    var kys = Object.keys(allSettings.settings);
    //if (gui.__controllers[2]) {
    //    gui.__controllers[2].remove();
    //}
    if (settingsDat) {
        gui.remove(settingsDat);
    }
    settingsDat = gui.add(controlers, 'settings', kys).name("Settings").onFinishChange(settingChange);

    for (var i in gui.__controllers) {
        gui.__controllers[i].updateDisplay();
    }
}

function updateTimeSeriesGui() {
    var kys = Object.keys(allSettings.settings);
    //if (gui.__controllers[3]) {
    //    gui.__controllers[3].remove();
    //}
    if (settingsDat) {
        gui.remove(settingsDat);
    }
    settingsDat = gui.add(controlers, 'settings', kys).name("Settings").onFinishChange(settingChange);

    for (var i in gui.__controllers) {
        gui.__controllers[i].updateDisplay();
    }
}

var settingsDat;

function setupGuiSingle() {
    if (infoPage) return;

    var kys = Object.keys(allSettings.settings);
    gui = new dat.GUI({autoPlace: false});
    var customContainer = document.getElementById('plot-controls');
    customContainer.appendChild(gui.domElement);
    gui.add(controlers, 'pointsize', 0.001, 5.0, 1.0).name("Point Size").onFinishChange(changePointSize);
    gui.add(controlers, 'glyphsize', 0.001, 5.0, 1.0).name("Glyph Size").onFinishChange(changeGlyphSize);
    settingsDat = gui.add(controlers, 'settings', kys).name("Settings").onFinishChange(settingChange);
}

function setupGuiTimeSeries() {
    if (infoPage) return;

    var kys = Object.keys(allSettings.settings);
    gui = new dat.GUI({autoPlace: false});
    var customContainer = document.getElementById('plot-controls');
    customContainer.appendChild(gui.domElement);
    gui.add(controlers, 'delay', 10.0, 2000.0, speed).name("Play Delay(ms)");
    gui.add(controlers, 'pointsize', 0.001, 5.0, pointSize).name("Point Size").onFinishChange(changePointSize);
    gui.add(controlers, 'glyphsize', 0.001, 5.0, glyphSize).name("Glyph Size").onFinishChange(changeGlyphSize);
    settingsDat = gui.add(controlers, 'settings', kys).name("Settings").onFinishChange(settingChange);
}

function settingChange() {
    allSettings.selected = controlers.settings;
    reInitialize = true;
}