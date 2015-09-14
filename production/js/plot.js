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
            + "<input value='" + initcolors[i] + "' class='form-control' type='hidden' id='"+ i + "'>"
            + "<span class='input-group-addon'><i style='background-color: rgb(1, 343, 69);'></i></span>"
            + "</div>"
            + "</td>"
            + "<td class=' '>C12</span></td>"
            + "<td class=' '>121</td>"
            + "</tr>"
    }

    var tableend = "</tbody>"
        + "</table>"


    return tabletop + tablerows + tableend;
}

var container, stats;
var camera, scene, renderer, sprite, colors = [], particles = [], material, controls, light;
var mesh;
var particleSystem;
var colorMap = {};
var resultData;
var heus = [0.05, 0.3, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95];

var mouseX = 10, mouseY = 10;
var windowHalfX = window.innerWidth / 2;
var windowHalfY = window.innerHeight / 2;
var removeclusters = [];
var scene3d;
var colors = [];
var colorlist = [];

function initScene() {

    removeclusters.push("1");
    Papa.parse("data/nucleotide/sammon.points.txt", {
        download: true,
        complete: function (results) {
            resultData = results
            generateGraph(removeclusters);
        }
    });


};

function animate() {
    requestId = window.requestAnimationFrame(animate);
    controls.update();
    render();
}

function render() {
    var camera = scene3d.getObjectByName('camera');
    renderer.render(scene3d, camera);
    stats.update();
}
//TODO WInodow rezise does not work yet need to fix
function onWindowResize() {
    var width = $('#canvas3d').width();
    var height = $('#canvas3d').height();
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
    controls.handleResize();
    render();
}

function generateGraph(removeclusters) {

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
    var camera = new THREE.PerspectiveCamera(45, canvasWidth / canvasHeight, 0.1, 10000);
    camera.name = 'camera';
    camera.position.set(1,1,1);
    scene3d.add(camera);
    //removeclusters.push("1")
    // new THREE.TrackballControls
    controls = new THREE.OrbitControls(camera, renderer.domElement);

    var geometry = [];
    var hsl;
    var sections = [];
    //TODO try to make the loop here not dependent on the fact that row[4] starts from 0
    for (var i in resultData.data) {
        var row = resultData.data[i];

        if(geometry.length == row[4]){
            geometry.push(new THREE.Geometry());
            colors.push(new Array());
        }
        hsl = [heus[row[4]], 1, 0.8];
            var vertex = new THREE.Vector3();
            vertex.x = row[1] * 10;
            vertex.y = row[2] * 10;
            vertex.z = row[3] * 10;

            geometry[row[4]].vertices.push(vertex);

            if (sections.indexOf(row[4]) == -1)
                sections.push(row[4]);

        colors[row[4]].push(new THREE.Color(0xffffff).setHSL(hsl[0], hsl[1], hsl[2]));
    }


    material = new THREE.PointCloudMaterial({
        size: 0.005,
        map: sprite,
        vertexColors: THREE.VertexColors,
        transparent: true
    });
    material.color.setHSL(1.0, 1, 1);
    for(var i in geometry){
        colorlist.push(colors[i][0].getHexString());
        geometry[i].colors = colors[i];
        particles.push( new THREE.PointCloud(geometry[i], material));
        scene3d.add(particles[i]);

    }
    window.document.getElementById("cluster_table_div").innerHTML = generateCheckList(sections,colorlist);
    stats.domElement.style.position = 'absolute';
    document.getElementById("stats").appendChild(stats.domElement);
    window.addEventListener('resize', onWindowResize, false);
    $('.color-pic1').colorpicker();
    render();
    animate();

}

function removeSection(id){
    scene3d.remove(particles[id]);
    render();
   // animate();
}

function addSection(id){
    scene3d.add(particles[id]);
    render();
   // animate();
}

function recolorSection(id, color){
    scene3d.remove(particles[id]);
    colorlist[id] = color;
    for(var i in colors[id]){
        colors[id][i] = color;
    }
    particles[id].geometry.colors = colors[id];
    particles[id].geometry.verticesNeedUpdate = true;
    scene3d.add(particles[id]);
    render();

}