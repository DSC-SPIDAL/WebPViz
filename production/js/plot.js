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

function initScene() {
    var canvasWidth   = $( '#canvas3d').width();
    var canvasHeight  =  $( '#canvas3d').height();
    var container, stats;
    var camera, scene, renderer, sprite, colors = [], particles, material, controls, light;
    var mesh;
    var particleSystem;
    var colorMap = {};
    var resultSet;
    var heus = [0.05, 0.3, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95];

    var mouseX = 10, mouseY = 10;
    var windowHalfX = window.innerWidth / 2;
    var windowHalfY = window.innerHeight / 2;
    var removeclusters = [];
    var scene3d;
    init();
    animate();

    // init scene
    function init(){
        //new THREE.Scene
        var resultSet;
        scene3d = new THREE.Scene();
        stats = new Stats();
        //set the scene
        var canvas3d = $( '#canvas3d' );
        //new THREE.WebGLRenderer
        renderer = new THREE.WebGLRenderer( { canvas: canvas3d.get( 0 ), antialias: true } );
        renderer.setSize( canvasWidth, canvasHeight );
        renderer.setClearColor( 0x121224, 1 );

        //new THREE.PerspectiveCamera
        var camera = new THREE.PerspectiveCamera( 45, canvasWidth / canvasHeight, 0.1, 10000 );
        camera.name = 'camera';
        camera.position.z = 6;
        scene3d.add( camera );

        // new THREE.TrackballControls
        controls = new THREE.TrackballControls( camera, renderer.domElement );
        controls.rotateSpeed = 1.0;
        controls.zoomSpeed = 1.2;
        controls.panSpeed = 0.8;
        controls.noZoom = false;
        controls.noPan = false;
        controls.staticMoving = true;
        controls.dynamicDampingFactor = 0.3;
        controls.keys = [ 65, 83, 68 ];
        controls.addEventListener('change', render);
        //controls.target.set( pos.x, pos.y, pos.z );

        Papa.parse("data/AMF_Phylo_Seqs_599Nts_Apr17MAFFT_seqs_swgms_pid_1821_smacof_points_w_orig_pnum.csv", {
            download: true,
            complete: function (results) {
                resultSet = results
                generateGraph(removeclusters);
            }
        });

        function generateGraph(removeclusters) {
            var geometry = new THREE.Geometry();
            var colors = [];
            var sections = [];

            for (var i in resultSet.data) {
                var row = resultSet.data[i];
                if (removeclusters.indexOf(row[4]) == -1) {
                    var hsl;

                    hsl = [heus[row[4]], 1, 0.8];
                    if (sections.indexOf(row[4]) == -1)
                        sections.push(row[4]);

                    var vertex = new THREE.Vector3();
                    vertex.x = row[1] * 10;
                    vertex.y = row[2] * 10;
                    vertex.z = row[3] * 10;

                    geometry.vertices.push(vertex);

                    colors[i] = new THREE.Color(0xffffff);
                    colors[i].setHSL(hsl[0], hsl[1], hsl[2]);
                }

            }

            geometry.colors = colors;
            material = new THREE.PointCloudMaterial({ size: 0.05, map: sprite, vertexColors: THREE.VertexColors, transparent: true });
            material.color.setHSL(1.0, 1, 1);
            particles = new THREE.PointCloud(geometry, material);
            particles.sortParticles = true;
            scene3d.add(particles);


            stats.domElement.style.position = 'absolute';
            stats.domElement.style.top = '0px';
            //canvas3d.appendChild(stats.domElement);
            document.getElementById("stas").appendChild(stats.domElement);
            window.addEventListener('resize', onWindowResize, false);
        }
        ////new THREE.AmbientLight (soft white light)
        //var light   = new THREE.AmbientLight( 0x404040 );
        //light.name  = 'ambientLight';
        //scene3d.add( light );
        ////new THREE.PointLight ( white light )
        //var pointLight = new THREE.PointLight( 0xffffff, 0.8, 0 );
        //camera.add( pointLight );
        render();
    }
    function animate() {
        requestId = window.requestAnimationFrame( animate );
        controls.update();
        render();
    }
    function render() {
        var camera = scene3d.getObjectByName( 'camera' );
        renderer.render( scene3d, camera );
        stats.update();
    }
    function onWindowResize() {
        var width =  $( '#canvas3d').width();
        var height =  $( '#canvas3d').height();
        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        renderer.setSize(width, height);
        controls.handleResize();
        render();
    }
};

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