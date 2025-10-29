package bgu.spl.mics.application.objects;

import bgu.spl.mics.application.input.Configuration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

public class LiDarDataBase {
    private final Map<Integer, List<StampedCloudPoints>> Lidardata;
    private int last = 0;
    private static LiDarDataBase instance;

    public LiDarDataBase() {
        Lidardata = new ConcurrentHashMap<>();
    }

    private static class SingletonHolder { ;
        private static final LiDarDataBase instance = new LiDarDataBase();
    }

    public static LiDarDataBase getInstance() {
        return SingletonHolder.instance;
    }
    public List<StampedCloudPoints> getStampedCloudPointsAtTime(int time) {
        return Lidardata.getOrDefault(time, Collections.emptyList());
    }

    public int getLastTime() {
        return last;
    }


    public void loadLidarData(String filePath) {
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            try (FileReader reader = new FileReader(filePath)) {
                List<Map<String, Object>> records = gson.fromJson(reader, listType);

                for (Map<String, Object> record : records) {
                    int time = ((Double) record.get("time")).intValue();
                    String id = (String) record.get("id");
                    List<List<Double>> cloudPointsRaw = (List<List<Double>>) record.get("cloudPoints");
                    List<CloudPoint> cloudPoints = new ArrayList<>();

                    for (List<Double> point : cloudPointsRaw) {
                        double x = point.get(0);
                        double y = point.get(1);
                        cloudPoints.add(new CloudPoint(x, y));
                    }

                    StampedCloudPoints stampedCloudPoint = new StampedCloudPoints(id, time, cloudPoints);
                    Lidardata.putIfAbsent(time, new ArrayList<>());
                    Lidardata.get(time).add(stampedCloudPoint);

                    if (time > last) {
                        last = time;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading LiDar data: " + e.getMessage());
        }
    }

    public void addDetectedObject(StampedCloudPoints stampedCloudPoints) {
        if (stampedCloudPoints != null) {
            int timestamp = stampedCloudPoints.getTime();

            Lidardata.putIfAbsent(timestamp, new ArrayList<>());
            Lidardata.get(timestamp).add(stampedCloudPoints);
        }
    }
}