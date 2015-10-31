package models.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement
@XmlType(propOrder={"plot","clusters", "points"})
public class Plotviz {
    private Plot plot;
    private List<Cluster> clusters;
    private List<PVizPoint> points;

    public Plotviz(Plot plot, List<Cluster> clusters, List<PVizPoint> points) {
        this.plot = plot;
        this.clusters = clusters;
        this.points = points;
    }

    public Plotviz() {
    }

    public Plot getPlot() {
        return plot;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public List<PVizPoint> getPoints() {
        return points;
    }

    @XmlElement
    public void setPlot(Plot plot) {
        this.plot = plot;
    }

    @XmlElementWrapper (name = "clusters")
    @XmlElement (name = "cluster")
    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    @XmlElementWrapper (name = "points")
    @XmlElement (name="point")
    public void setPoints(List<PVizPoint> points) {
        this.points = points;
    }
}
