package entities;

public class WeatherData {
    private final double temperatureCelsius;
    private final double windSpeedKmh;
    private final double rainMm;

    public WeatherData(double temperatureCelsius, double windSpeedKmh, double rainMm) {
        this.temperatureCelsius = temperatureCelsius;
        this.windSpeedKmh = windSpeedKmh;
        this.rainMm = rainMm;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public double getRainMm() {
        return rainMm;
    }

    public String buildRecommendation() {
        boolean dry = rainMm <= 0.2;
        boolean lightRain = rainMm <= 0.8;
        boolean calmWind = windSpeedKmh <= 20.0;
        boolean manageableWind = windSpeedKmh <= 28.0;
        boolean mildTemp = temperatureCelsius >= 15.0 && temperatureCelsius <= 32.0;

        if (dry && calmWind && mildTemp) {
            return "Good day for events, planting, and cleanup.";
        }

        if (lightRain && manageableWind && mildTemp) {
            return "Good day for planting and light outdoor events.";
        }

        return "Bad weather, better reschedule outdoor events.";
    }
}
