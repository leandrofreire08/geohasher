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

    private static final char[] base32 = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    public static List<GeoHash> calculateGeohashes(Geometry polygon, int numCharsInHash){
        return calculateGeohashes(polygon, numCharsInHash, 1, null);
    }

    public static List<GeoHash> calculateGeohashes(Coordinate[] coords, int numCharsInHash){
        LinearRing ring = geometryFactory.createLinearRing( coords );
        LinearRing holes[] = null;
        Polygon polygon = geometryFactory.createPolygon(ring, holes );
        return calculateGeohashes(polygon, numCharsInHash);
    }

    /**
     * Todo: Actually calculate the geohashes instead of relying on a NPE to
     *       tell the child geohash is invalid.
     * Todo: Allow for different bases of geohash (16/32/64)
     */
    private static List<GeoHash> getChildHashes(GeoHash geo){
        List<GeoHash> children = new ArrayList<GeoHash>();
        for (int i = 0; i < base32.length; i++){
            try{
                if (geo == null){
                    children.add(GeoHash.fromGeohashString(String.valueOf(base32[i])));
                } else {
                    children.add(GeoHash.fromGeohashString(geo.toBase32() + base32[i]));
                }
            } catch (NullPointerException ex){
                // Hash string was invalid
            }
        }
        return children;
    }

    public static List<GeoHash> calculateGeohashes(Geometry polygon, int numCharsInHash, int depth, GeoHash baseHash){

        List<GeoHash> ret = new ArrayList<GeoHash>();

        for (GeoHash hash : getChildHashes(baseHash)){

            if (doesGeometryContainHash(hash, polygon)){
                ret.add(hash);
            }
            else if (depth < numCharsInHash && doesGeometryIntersectHash(hash, polygon)){
                List<GeoHash> hashes = calculateGeohashes(polygon, numCharsInHash, depth + 1, hash);
                ret.addAll(hashes);
            } else if (depth >= numCharsInHash && doesGeometryIntersectHash(hash, polygon)){
                ret.add(hash);
            }

        }

        return ret;
    }

    private static BoundingBox getMinimumBoundingBox(Geometry polygon){
        Envelope bb = polygon.getEnvelopeInternal();
        return new BoundingBox(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
    }

    private static boolean doesGeometryIntersectHash(GeoHash hash, Geometry polygon){
        BoundingBox hashBB = hash.getBoundingBox();
        Coordinate[] hashBBArray = new Coordinate[] {new Coordinate(hashBB.getMaxLat(), hashBB.getMinLon()),
                new Coordinate(hashBB.getMaxLat(), hashBB.getMaxLon()), new Coordinate(hashBB.getMinLat(), hashBB.getMinLon())
                , new Coordinate(hashBB.getMinLat(), hashBB.getMaxLon()), new Coordinate(hashBB.getMaxLat(), hashBB.getMinLon())};
        LinearRing ring = geometryFactory.createLinearRing( hashBBArray );
        LinearRing holes[] = null; // use LinearRing[] to represent holes
        Polygon hashPolygon = geometryFactory.createPolygon(ring, holes );
        return polygon.intersects(hashPolygon);
    }

    private static boolean doesGeometryContainHash(GeoHash hash, Geometry polygon){
        BoundingBox hashBB = hash.getBoundingBox();
        Coordinate[] hashBBArray = new Coordinate[] {new Coordinate(hashBB.getMaxLat(), hashBB.getMinLon()),
                new Coordinate(hashBB.getMaxLat(), hashBB.getMaxLon()), new Coordinate(hashBB.getMinLat(), hashBB.getMinLon())
                , new Coordinate(hashBB.getMinLat(), hashBB.getMaxLon()), new Coordinate(hashBB.getMaxLat(), hashBB.getMinLon())};
        LinearRing ring = geometryFactory.createLinearRing( hashBBArray );
        LinearRing holes[] = null; // use LinearRing[] to represent holes
        Polygon hashPolygon = geometryFactory.createPolygon(ring, holes );
        return polygon.contains(hashPolygon);
    }
}
