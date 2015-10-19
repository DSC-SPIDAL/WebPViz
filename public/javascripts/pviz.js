if (!Detector.webgl) Detector.addGetWebGLMessage();

var clusters;
var container, stats;
var camera, scene, renderer, material, i, h, color, sprite, size, controls, light;
var geometry = [];
var mouseX = 0, mouseY = 0;

var windowHalfX = window.innerWidth / 2;
var windowHalfY = (window.innerHeight - 51) / 2;

var clusterUrl;

function init() {
    container = document.getElementById('canvas3d');
    document.body.appendChild(container);

    camera = new THREE.PerspectiveCamera(50, window.innerWidth / (window.innerHeight - 51), 1, 100);
    camera.position.z = 6;

    scene = new THREE.Scene();
    //scene.fog = new THREE.FogExp2(0x000000, 0.001);

    controls = new THREE.TrackballControls(camera);
    controls.rotateSpeed = 1.0;
    controls.zoomSpeed = 1.2;
    controls.panSpeed = 0.8;
    controls.noZoom = false;
    controls.noPan = false;
    controls.staticMoving = true;
    controls.dynamicDampingFactor = 0.3;
    controls.keys = [ 65, 83, 68 ];
    controls.addEventListener('change', render);

    var randomClusterColors = randomColor({
        count: clusters.length,
        luminosity: 'light'
    });

    sprite = THREE.ImageUtils.loadTexture("/assets/textures/sprites/ball.png");

    material = new THREE.PointCloudMaterial({ size: 0.1, map: sprite, vertexColors: THREE.VertexColors, transparent: true });
    material.color.setHSL(1, 1, 1);

    clusters.forEach(function(cluster, j, array){
        $.getJSON(clusterUrl + cluster.cluster, function(data){
            alert(cluster.cluster);

            var g = new THREE.Geometry();
            geometry.push(g);
            var colors = [];

            for (var i in data.points){
                var p = data.points[i];

                //var hsl = [heus[data.cid], 1, 0.8];

                var vertex = new THREE.Vector3(p.x * 10, p.y * 10, p.z * 10);
                g.vertices.push(vertex);

                //var hsl = randomClusterColors[i];
                //colors[i] = new THREE.Color(0xffffff);
                //colors[i].setHSL(hsl[0], hsl[1], hsl[2]);
                var color = parseInt(randomClusterColors[j].replace(/^#/, ''), 16);
                colors[i] = new THREE.Color(color);
            }

            g.colors = colors;
            var particles = new THREE.PointCloud(g, material);
            particles.sortParticles = true;
            scene.add(particles);
        });
    });

    renderer = new THREE.WebGLRenderer({clearAlpha: 1, antialias: true, alpha: true});
    //renderer.setClearColor(0xffffff, 1);
    renderer.setSize(window.innerWidth, window.innerHeight - 51);
    container.appendChild(renderer.domElement);

    stats = new Stats();
    document.getElementById('webgl-stats').appendChild(stats.domElement);

    //document.addEventListener('mousemove', onDocumentMouseMove, false);
    //document.addEventListener('touchstart', onDocumentTouchStart, false);
    //document.addEventListener('touchmove', onDocumentTouchMove, false);

    window.addEventListener('resize', onWindowResize, false);
}

function onWindowResize() {
    windowHalfX = window.innerWidth / 2;
    windowHalfY = (window.innerHeight - 51) / 2;

    camera.aspect = window.innerWidth / (window.innerHeight - 51);
    camera.updateProjectionMatrix();

    renderer.setSize(window.innerWidth, (window.innerHeight - 51));
    controls.handleResize();
}
//

function animate() {
    requestAnimationFrame(animate);
    controls.update();
    render();
    stats.update();
}

function render() {
    renderer.render(scene, camera);
}

function visualize(resultSetUrl, resultSet){
    clusterUrl = resultSetUrl + "/cluster/";
    clusters = resultSet.clusters;
    init();
    animate();
}