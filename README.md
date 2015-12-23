WebPlotViz
=======

[PlotViz](http://salsahpc.indiana.edu/plotviz/) for web. Build using [Three.js](http://threejs.org) and [Play Framework](https://www.playframework.com).

WebPlotViz Input Format
-------

WebPlotViz accepts a XML format of files.

```xml
<plotviz>
  <plot>
    <title></title>
    <pointsize>2</pointsize>
  </plot>
  <clusters>
    <cluster>
      <key>0</key>
      <label>FN547657_Div_aur_K</label>
      <visible>1</visible>
      <color r="" g="255" b="255" a="255" />
      <size>1</size>
      <shape>3</shape>
    </cluster>
    ...
   </clusters> 
   <points>
    <point>
      <key>1</key>
      <clusterkey>0</clusterkey>
      <label>2251</label>
      <location x="0.06221308043245255" y="-0.18796120745922332" z="-0.1397611036896711" />
    </point>
    ....
   </points>
    <edges>
        <edge>
          <key>1</key>
          <vertices>
            <vertex key="1" />
            <vertex key="2" />
          </vertices>
        </edge>
        ....
      </edges>
   </points>
<plotviz>  
```

Each plot consists of a set of clusters, points and edges. Clusters define a grouping of points. You can assign colors and shapes for clusters.
This means every point in the cluster will have the shape and the color. A cluster has a unique key. This key is used by the points to assign itself to a cluster.

Points contain a list of points. Each point has a key and a cluster key. Cluster key defines which cluster the point belongs to.
Each point have a location where it defines the x,y,z coordinates.

An edge defines a line through points. Each edge has a set of vertices. These vertices are reference keys to points. A lines are drawn through the vertices.

Example File
---------

```xml
<plotviz>
  <plot>
    <title>MDSasChisq_SMACOF_3D_cpp</title>
    <pointsize>2</pointsize>
    <glyph>
      <visible>0</visible>
      <scale>1</scale>
    </glyph>
  </plot>
  <clusters>
    <cluster>
      <key>0</key>
      <label>FN547657_Div_aur_K</label>
      <visible>1</visible>
      <default>0</default>
      <color r="255" g="255" b="255" a="255" />
      <size>1</size>
      <shape>3</shape>
    </cluster>
    <cluster>
      <key>1</key>
      <label>FR750142_Rac_ful_K</label>
      <visible>1</visible>
      <default>0</default>
      <color r="255" g="0" b="0" a="255" />
      <size>1</size>
      <shape>3</shape>
    </cluster>
   </clusters> 
   <points>
    <point>
      <key>1</key>
      <clusterkey>0</clusterkey>
      <label>2251</label>
      <location x="0.06221308043245255" y="-0.18796120745922332" z="-0.1397611036896711" />
    </point>
    <point>
      <key>2</key>
      <clusterkey>1</clusterkey>
      <label>2252</label>
      <location x="0.055824241220745412" y="-0.18867336665241485" z="-0.14331119206545923" />
    </point>
    <point>
      <key>3</key>
      <clusterkey>0</clusterkey>
      <label>2253</label>
      <location x="0.05834231849450619" y="-0.18654874725457385" z="-0.14133175741622578" />
    </point>
    <point>
      <key>4</key>
      <clusterkey>2254</clusterkey>
      <label>1</label>
      <location x="0.056172878622444425" y="-0.19178016569894113" z="-0.13197211687622809" />
    </point>
    <point>
      <key>5</key>
      <clusterkey>2255</clusterkey>
      <label>2255</label>
      <location x="0.056639757130989794" y="-0.19540639540488691" z="-0.12330269641817594" />
    </point>
    <edges?
        <edge>
          <key>1</key>
          <vertices>
            <vertex key="1" />
            <vertex key="2" />
          </vertices>
        </edge>
        <edge>
          <key>2</key>
          <vertices>
            <vertex key="3" />
            <vertex key="4" />
          </vertices>
        </edge>
      </edges>
   </points>
<plotviz>
```



