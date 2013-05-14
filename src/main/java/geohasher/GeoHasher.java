package geohasher;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.util.BoundingBoxGeoHashIterator;
import ch.hsr.geohash.util.TwoGeoHashBoundingBox;
import com.vividsolutions.jts.geom.*;
import org.geotools.geometry.jts.JTSFactoryFinder;

import java.util.ArrayList;
import java.util.List;

public class GeoHasher {

    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

    public static List<GeoHash> calculateGeohashes(Geometry polygon, int numCharsInHash){
        List<GeoHash> ret = new ArrayList<GeoHash>();
        BoundingBox bb = getMinimumBoundingBox(polygon);
        TwoGeoHashBoundingBox geoBB = TwoGeoHashBoundingBox.withCharacterPrecision(bb, numCharsInHash);
        BoundingBoxGeoHashIterator itr = new BoundingBoxGeoHashIterator(geoBB);

        while (itr.hasNext()){
            GeoHash hash = itr.next();
            if (isHashInGeometry(hash, polygon)){
                ret.add(hash);
            }
        }

        return ret;
    }

    public static List<GeoHash> calculateGeohashes(Coordinate[] coords, int numCharsInHash){
        LinearRing ring = geometryFactory.createLinearRing( coords );
        LinearRing holes[] = null;
        Polygon polygon = geometryFactory.createPolygon(ring, holes );
        return calculateGeohashes(polygon, numCharsInHash);
    }


    private static BoundingBox getMinimumBoundingBox(Geometry polygon){
        Envelope bb = polygon.getEnvelopeInternal();
        return new BoundingBox(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
    }

    private static boolean isHashInGeometry(GeoHash hash, Geometry polygon){
        BoundingBox hashBB = hash.getBoundingBox();
        Coordinate[] hashBBArray = new Coordinate[] {new Coordinate(hashBB.getMaxLat(), hashBB.getMinLon()),
                new Coordinate(hashBB.getMaxLat(), hashBB.getMaxLon()), new Coordinate(hashBB.getMinLat(), hashBB.getMinLon())
                , new Coordinate(hashBB.getMinLat(), hashBB.getMaxLon()), new Coordinate(hashBB.getMaxLat(), hashBB.getMinLon())};
        LinearRing ring = geometryFactory.createLinearRing( hashBBArray );
        LinearRing holes[] = null; // use LinearRing[] to represent holes
        Polygon polygon2 = geometryFactory.createPolygon(ring, holes );
        return polygon.intersects(polygon2);
    }
}
