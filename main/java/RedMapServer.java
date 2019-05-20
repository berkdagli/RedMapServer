
import com.google.gson.Gson;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import static java.lang.Math.*;

public class RedMapServer {

    static GeoApiContext context;
    static final int SERVER_PORT = 8000;
    static ArrayList<RecyclePoint> recyclePoints = new ArrayList<RecyclePoint>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        context = new GeoApiContext.Builder().apiKey("AIzaSyB9qrTnYoF29fa4MbqDOyS81F_vUo2Hzxg").maxRetries(3)
            .build();
        initializeRecyclePoints();
        while(true) {
            Socket s = null;
            try {
                s = serverSocket.accept();
                DataInputStream in = new DataInputStream(s.getInputStream());
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                Thread t = new ClientHandler(s,in,out,context,recyclePoints);
                t.start();
            }
            catch (Exception e) {
                s.close();
                e.printStackTrace();
            }
        }
    }

    protected static void initializeRecyclePoints() {
        try {
            BufferedReader in = new BufferedReader(new FileReader("F:\\Users\\Berk\\Documents\\RedMapServer\\src\\main\\resources\\recycle_points.txt"));
            String str;
            String[] token;
            while ((str = in.readLine()) != null) {
                token = str.split(", ");
                recyclePoints.add(new RecyclePoint(Double.parseDouble(token[0]),Double.parseDouble(token[1])
                        ,Integer.parseInt(token[2])));
            }
        }
        catch (IOException e) {
            System.out.println("File Read Error");
        }
    }
}

class ClientHandler extends Thread {
    final DataInputStream in;
    final DataOutputStream out;
    final Socket socket;
    GeoApiContext context;
    ArrayList<RecyclePoint> recyclePoints;
    String received;

    public ClientHandler(Socket socket, DataInputStream in, DataOutputStream out, GeoApiContext context, ArrayList<RecyclePoint> recyclePoints) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.context = context;
        this.recyclePoints = recyclePoints;
    }

    @Override
    public void run() {
        JSONParser parser = new JSONParser();
        try {
            received = in.readUTF();
            JSONObject obj = (JSONObject) parser.parse(received);
            int op = ((Number)obj.get("op")).intValue();
            if(op == 0) {
                int id = ((Number)obj.get("point_type")).intValue();
                double currentLat = (Double)obj.get("lat");
                double currentLng = (Double)obj.get("lng");
                RecyclePoint closest = findClosestRecyclePoint(id,currentLat,currentLng,recyclePoints);
                DirectionsApiRequest request = new DirectionsApiRequest(context).origin(new LatLng(currentLat,currentLng))
                        .destination(new LatLng(closest.latitude,closest.longitude)).optimizeWaypoints(true);
                DirectionsResult result = request.await();
                DirectionsRoute[] routes = result.routes;
                Response response = new Response(routes[0].overviewPolyline,closest.latitude,closest.longitude);
                Gson gson = new Gson();
                String resultJSON = gson.toJson(response);
                out.writeUTF(resultJSON);//out.writeUTF("{\"points\":\"cibyFcw{pDeAUMUKIMCi@fGGHABOCkHaEs@_@WGeEg@e@G\"}");
                out.close();
                in.close();
                socket.close();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public RecyclePoint findClosestRecyclePoint(int id, double lat, double lng, ArrayList<RecyclePoint> recyclePoints) {
        RecyclePoint closest=null;
        double minDistance = Double.MAX_VALUE;
        double tempDistance;
        int listSize = recyclePoints.size();
        for(int i=0;i<listSize;i++) {
            if(recyclePoints.get(i).type == id) {
                tempDistance=calculateDistance(lat,lng,recyclePoints.get(i).latitude,recyclePoints.get(i).longitude);
                if(tempDistance < minDistance) {
                    minDistance = tempDistance;
                    closest = recyclePoints.get(i);
                }
            }
        }
        return closest;
    }

    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        int r = 6371000;
        double fi1 = toRadians(lat1);
        double fi2 = toRadians(lat2);
        double deltafi = toRadians(lat2-lat1);
        double deltalambda = toRadians(lng2-lng1);
        double a = pow(sin(deltafi/2),2) +
                cos(fi1)*cos(fi2)*pow(sin(deltalambda),2);
        double c = 2 * atan2(sqrt(a),sqrt(1-a));
        return c * r;
    }
}
