package geohasher;


import ch.hsr.geohash.GeoHash;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * Created with IntelliJ IDEA.
 * User: Sean
 * Date: 5/14/13
 * Time: 6:07 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(JUnit4.class)
public class GeoHasherTest {

    private static List<Coordinate> coords = new ArrayList<Coordinate>();
    private static Map<String, List<Coordinate>> hashMap = new HashMap<String, List<Coordinate>>();

    private final static int CHARACTER_PRECISION = 6;
    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

    @BeforeClass
    public static void setUp(){
        for (double lat = 30; lat < 40; lat += 0.01){
            for (double lng = 30; lng < 40; lng += 0.01){
                Coordinate coord = new Coordinate(lat, lng);
                coords.add(coord);
                String hash = GeoHash.withCharacterPrecision(lat, lng, CHARACTER_PRECISION).toBase32();
                for (int i = 0; i < hash.length(); i++){
                    String prefix = hash.substring(0, i+1);
                    if (hashMap.get(prefix) == null){
                        hashMap.put(prefix, new ArrayList<Coordinate>());
                    }
                    hashMap.get(prefix).add(coord);
                }
            }
        }
    }

    @Test
    public void testPolygon1(){
        Coordinate[] testCoords = new Coordinate[]{
                 new Coordinate(30, 30),
                 new Coordinate(30, 32),
                 new Coordinate(31, 31),
                 new Coordinate(32, 32),
                 new Coordinate(32, 30),
                 new Coordinate(30, 30)
        };
        int numHits = runTest(testCoords);
        assertEquals(numHits, 29601);
    }

    @Test
    public void testPolygon2(){
        Coordinate[] testCoords = new Coordinate[]{
           new Coordinate(30, 30),
           new Coordinate(31, 32),
           new Coordinate(30, 32),
           new Coordinate(30, 30)
        };
        int numHits = runTest(testCoords);
        assertEquals(numHits, 9801);
    }

    @Test
    public void testPolygonWithNoIntersect(){
        Coordinate[] testCoords = new Coordinate[]{
                new Coordinate(50, 50),
                new Coordinate(51, 52),
                new Coordinate(50, 52),
                new Coordinate(50, 50)
        };
        int numHits = runTest(testCoords);
        assertEquals(numHits, 0);
    }

    @Test
    public void testCircle(){
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(64);
        shapeFactory.setCentre(new Coordinate(34, 34));
        shapeFactory.setSize(1.2);
        Geometry circle = shapeFactory.createCircle();
        int numHits = runTest(circle);
        assertEquals(numHits, 11273);
    }

    private int runTest(Geometry geo){
        List<GeoHash> hashes = GeoHasher.calculateGeohashes(geo, CHARACTER_PRECISION);

        int noHashCount = 0;
        for (Coordinate coord : coords){
            Point p = geometryFactory.createPoint(coord);
            if (geo.contains(p)){
                noHashCount++;
            }
        }

        int hashCount = 0;
        for (GeoHash hash : hashes){
            List<Coordinate> myCoords = hashMap.get(hash.toBase32());
            if (myCoords != null){
                for (Coordinate coord : myCoords){
                    Point p = geometryFactory.createPoint(coord);
                    if (geo.contains(p)){
                        hashCount++;
                    }
                }
            }
        }

        assertEquals(hashCount, noHashCount);

        return hashCount;
    }

    private int runTest(Coordinate[] coordinates){
        LinearRing ring = geometryFactory.createLinearRing( coordinates );
        LinearRing holes[] = null;
        Polygon polygon = geometryFactory.createPolygon(ring, holes );
        return runTest(polygon);
    }
}
