import com.google.maps.model.EncodedPolyline;

public class Response {
    EncodedPolyline polyline;
    double destLat;
    double destLng;

    public Response(EncodedPolyline polyline, double destLat, double destLng) {
        this.polyline = polyline;
        this.destLat = destLat;
        this.destLng = destLng;
    }
}
